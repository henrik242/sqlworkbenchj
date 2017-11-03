/*
 * OracleRowDataReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.storage.reader;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.util.Calendar;
import java.util.TimeZone;

import workbench.log.LogMgr;

import workbench.db.ConnectionMgr;
import workbench.db.JdbcUtils;
import workbench.db.WbConnection;
import workbench.db.oracle.OracleUtils;

import workbench.storage.ResultInfo;

import workbench.util.StringUtil;

/**
 * A class to properly read the value of a TIMESTAMP WITH TIME ZONE column.
 *
 * This code should actually be inside Oracle's JDBC driver's getTimestamp() method to properly adjust
 * the timestamp value.
 *
 * @author Thomas Kellerer
 */
public class OracleRowDataReader
  extends RowDataReader
{
  private Method stringValueTZ;
  private Method offsetDateTimeValue;
  private Method getTimeZone;
  private Method localDateTimeValue;
  private Method timestampValue;
  private Method timestampLTZValue;

  private Connection sqlConnection;
  private DateTimeFormatter tsParser;
  private boolean useDefaultClassLoader;

  private boolean is12_2_Driver;

  public OracleRowDataReader(ResultInfo info, WbConnection conn)
    throws ClassNotFoundException
  {
    this(info, conn, false);
  }

  public OracleRowDataReader(ResultInfo info, WbConnection conn, boolean useDefaultClassLoader)
    throws ClassNotFoundException
  {
    super(info, conn);
    this.useDefaultClassLoader = useDefaultClassLoader;

    is12_2_Driver = JdbcUtils.hasMiniumDriverVersion(conn, "12.2");
    sqlConnection = conn.getSqlConnection();

    DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd HH:mm:ss");
    builder.appendFraction(ChronoField.MICRO_OF_SECOND, 0, 6, true);
    tsParser = builder.toFormatter().withResolverStyle(ResolverStyle.SMART);

    // we cannot have any hardcoded references to the Oracle classes
    // because that will throw a ClassNotFoundException as those classes were loaded through a different class loader.
    // Therefor I need to use reflection to access the Oracle specific methods

    try
    {
      Class oraTS = loadClass(conn, "oracle.sql.TIMESTAMP");
      timestampValue = oraTS.getMethod("timestampValue", (Class[])null);
    }
    catch (Throwable t)
    {
      LogMgr.logWarning("OracleRowDataReader.initialize()", "Could not access oracle.sql.TIMESTAMP", t);
    }

    try
    {
      Class tzClass = loadClass(conn, "oracle.sql.TIMESTAMPTZ");
      stringValueTZ = tzClass.getMethod("stringValue", java.sql.Connection.class);
    }
    catch (Throwable t)
    {
      LogMgr.logWarning("OracleRowDataReader.initialize()", "Could not access oracle.sql.TIMESTAMPTZ", t);
    }

    try
    {
      Class tzlClass = loadClass(conn, "oracle.sql.TIMESTAMPLTZ");
      timestampLTZValue = tzlClass.getMethod("timestampValue", java.sql.Connection.class, Calendar.class);
    }
    catch (Throwable t)
    {
      LogMgr.logWarning("OracleRowDataReader.initialize()", "Class oracle.sql.TIMESTAMPLTZ not available!", t);
    }


    if (is12_2_Driver)
    {
      try
      {
        Class tzClass = loadClass(conn, "oracle.sql.TIMESTAMPTZ");
        offsetDateTimeValue = tzClass.getMethod("offsetDateTimeValue", java.sql.Connection.class);
        getTimeZone = tzClass.getMethod("getTimeZone", (Class[])null);

        Class tzlClass = loadClass(conn, "oracle.sql.TIMESTAMPLTZ");
        localDateTimeValue = tzlClass.getMethod("localDateTimeValue", java.sql.Connection.class);
      }
      catch (Throwable t)
      {
        is12_2_Driver = false;
        LogMgr.logWarning("OracleRowDataReader.initialize()", "Class oracle.sql.TIMESTAMPTZ not available!", t);
      }
    }

  }

  public static boolean useOffsetDateTime(WbConnection conn)
  {
    return JdbcUtils.hasMiniumDriverVersion(conn, "12.2");
  }

  private Class loadClass(WbConnection conn, String className)
    throws ClassNotFoundException
  {
    if (useDefaultClassLoader)
    {
      return Class.forName(className);
    }
    return ConnectionMgr.getInstance().loadClassFromDriverLib(conn.getProfile(), className);
  }

  @Override
  protected Object readTimestampTZValue(ResultSet rs, int column)
    throws SQLException
  {
    return readTimestampValue(rs, column);
  }

  @Override
  protected Object readTimestampValue(ResultSet rs, int column)
    throws SQLException
  {
    Object value = rs.getObject(column);

    if (value == null) return null;
    if (rs.wasNull()) return null;

    if (value instanceof java.sql.Timestamp)
    {
      return value;
    }

    String clsName = value.getClass().getName();
    if ("oracle.sql.TIMESTAMPTZ".equals(clsName))
    {
      if (is12_2_Driver)
      {
        Object odt = convertToOffsetDateTime(value);
        if (odt != value) return odt;
      }
      return convertTZFromString(value);
    }
    else if ("oracle.sql.TIMESTAMPLTZ".equals(clsName))
    {
      return convertTIMESTAMPLTZ(value);
    }
    else if ("oracle.sql.TIMESTAMP".equals(clsName) && timestampValue != null)
    {
      return convertTIMESTAMP(value);
    }

    // fallback
    return rs.getTimestamp(column);
  }

  private ZoneId getTimeZone(Object tz)
  {
    try
    {
      TimeZone zone = (TimeZone)getTimeZone.invoke(tz, (Object []) null);
      return zone.toZoneId();
    }
    catch (Throwable ex)
    {
      LogMgr.logDebug("OracleRowDataReader.getTimeZone()", "Could not retrieve time zone", ex);
    }
    return null;
  }

  private Object convertToOffsetDateTime(Object tz)
  {
    try
    {
      OffsetDateTime odt = (OffsetDateTime)offsetDateTimeValue.invoke(tz, sqlConnection);
      ZoneId zone = getTimeZone(tz);
      if (zone != null)
      {
        return odt.atZoneSameInstant(zone);
      }

      return odt;
    }
    catch (Throwable ex)
    {
      LogMgr.logDebug("OracleRowDataReader.convertToOffsetDateTime()", "Could not convert timestamp", ex);
    }
    return tz;
  }

  private Object convertTIMESTAMPLTZ(Object tz)
  {
    try
    {
      if (localDateTimeValue != null)
      {
        return localDateTimeValue.invoke(tz, sqlConnection);
      }
      if (timestampLTZValue != null)
      {
        Calendar cal = Calendar.getInstance();
        Timestamp ts =  (Timestamp)timestampLTZValue.invoke(tz, sqlConnection, cal);
        return ts;
      }
    }
    catch (Throwable ex)
    {
      LogMgr.logDebug("OracleRowDataReader.convertTIMESTAMPLTZ()", "Could not convert TIMESTAMPLTZ", ex);
    }
    return tz;
  }

  private Object convertTIMESTAMP(Object tz)
  {
    try
    {
      return timestampValue.invoke(tz);
    }
    catch (Throwable ex)
    {
      LogMgr.logDebug("OracleRowDataReader.convertTIMESTAMP()", "Could not convert convertTIMESTAMP", ex);
    }
    return tz;
  }

  private Object convertTZFromString(Object tz)
  {
    try
    {
      String tsValue = (String) stringValueTZ.invoke(tz, sqlConnection);

      String[] elements = parseTimestampString(tsValue);
      LocalDateTime dt = LocalDateTime.parse(elements[0], tsParser);
      TimeZone zone = null;
      if (StringUtil.isNonBlank(elements[1]))
      {
        zone = TimeZone.getTimeZone(elements[1]);
      }

      if (zone != null)
      {
        ZoneId id = zone.toZoneId();
        ZoneOffset offset = id.getRules().getOffset(dt);
        return OffsetDateTime.of(dt, offset);
      }
      return Timestamp.valueOf(dt);
    }
    catch (Throwable ex)
    {
      LogMgr.logDebug("OracleRowDataReader.parseTZString()", "Could not read timestamp", ex);
    }
    return null;
  }

  // This is public static so that it can be unit tested without initializing the whole object
  public static String[] parseTimestampString(String tsValue)
  {
    String[] result = new String[2];

    result[0] = tsValue;

    int len = tsValue.length();
    if (len <= 19)
    {
      result[0] = tsValue;
    }
    int end = tsValue.indexOf(' ', 12);

    if (end > 0 && end < len)
    {
      result[0] = tsValue.substring(0, end);
      result[1] = StringUtil.trimToNull(tsValue.substring(end));
    }
    return result;
  }

}
