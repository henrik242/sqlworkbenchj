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
package workbench.storage;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.util.TimeZone;

import workbench.log.LogMgr;

import workbench.db.ConnectionMgr;
import workbench.db.JdbcUtils;
import workbench.db.WbConnection;
import workbench.db.oracle.OracleUtils;


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
  private Method stringValue;
  private Method internalToTimestamp;
  private Method offsetDateTimeValue;
  private Method getTimeZone;
  private Method localDateTimeValue;
  private Method timestampValue;

  private Connection sqlConnection;
  private DateTimeFormatter tsParser;
  private boolean useInternalConversion;
  private boolean useDefaultClassLoader;

  private boolean useJava8DateTime;

  public OracleRowDataReader(ResultInfo info, WbConnection conn)
    throws ClassNotFoundException
  {
    this(info, conn, false);
  }

  OracleRowDataReader(ResultInfo info, WbConnection conn, boolean useDefaultClassLoader)
    throws ClassNotFoundException
  {
    super(info, conn);
    useDefaultClassLoader = useDefaultClassLoader;

    useJava8DateTime = JdbcUtils.hasMiniumDriverVersion(conn, "12.2");
    sqlConnection = conn.getSqlConnection();
    useInternalConversion = OracleUtils.useInternalTimestampConversion();

    DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd HH:mm:ss");
    builder.appendFraction(ChronoField.MICRO_OF_SECOND, 0, 6, true);
    tsParser = builder.toFormatter().withResolverStyle(ResolverStyle.SMART);

    // we cannot have any "hardcoded" references to the Oracle classes
    // because that will throw a ClassNotFoundException as those classes were loaded through a different class loader.
    // Therefor I need to use reflection to access the Oracle specific methods
    try
    {
      Class oraDatum = loadClass(conn, "oracle.sql.Datum");
      stringValue = oraDatum.getMethod("stringValue", java.sql.Connection.class);
    }
    catch (Throwable t)
    {
      LogMgr.logWarning("OracleRowDataReader.initialize()", "Class oracle.sql.Datum not available!");
      LogMgr.logDebug("OracleRowDataReader.initialize()", "Could not access oracle.sql.Datum", t);
      throw new ClassNotFoundException("oracle.sql.Datum");
    }

    try
    {
      Class oraTS = loadClass(conn, "oracle.sql.TIMESTAMP");
      timestampValue = oraTS.getMethod("timestampValue", (Class[])null);
    }
    catch (Throwable t)
    {
      LogMgr.logWarning("OracleRowDataReader.initialize()", "Class oracle.sql.TIMESTAMP not available!");
      LogMgr.logDebug("OracleRowDataReader.initialize()", "Could not access oracle.sql.TIMESTAMP", t);
    }

    if (useInternalConversion)
    {
      try
      {
        Class tzClass = loadClass(conn, "oracle.sql.TIMESTAMPTZ");
        internalToTimestamp = tzClass.getMethod("timestampValue", java.sql.Connection.class);
      }
      catch (Throwable t)
      {
        useInternalConversion = false;
        LogMgr.logWarning("OracleRowDataReader.initialize()", "Class oracle.sql.TIMESTAMPTZ not available!");
        LogMgr.logDebug("OracleRowDataReader.initialize()", "Could not access oracle.sql.TIMESTAMPTZ", t);
      }
    }

    if (useJava8DateTime)
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
        useJava8DateTime = false;
        LogMgr.logWarning("OracleRowDataReader.initialize()", "Class oracle.sql.TIMESTAMPTZ not available!");
        LogMgr.logDebug("OracleRowDataReader.initialize()", "Could not access oracle.sql.TIMESTAMPTZ", t);
      }
    }
  }

  public static boolean useOffsetDateTime(WbConnection conn)
  {
    return OracleUtils.fixTimestampTZ() && JdbcUtils.hasMiniumDriverVersion(conn, "12.2");
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
      if (useJava8DateTime)
      {
        Object odt = convertToOffsetDateTime(value);
        if (odt != value) return odt;
      }

      if (useInternalConversion)
      {
        return timestampValue(value);
      }
      return adjustTIMESTAMP(value);
    }
    else if ("oracle.sql.TIMESTAMPLTZ".equals(clsName) && useJava8DateTime)
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

  private Object timestampValue(Object tz)
  {
    try
    {
      Timestamp ts = (Timestamp)internalToTimestamp.invoke(tz, sqlConnection);
      return ts;
    }
    catch (Throwable ex)
    {
      LogMgr.logDebug("OracleRowDataReader.timestampValue()", "Could not convert timestamp", ex);
    }
    return tz;
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
      return (LocalDateTime)localDateTimeValue.invoke(tz, sqlConnection);
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
      return timestampValue.invoke(tz, sqlConnection);
    }
    catch (Throwable ex)
    {
      LogMgr.logDebug("OracleRowDataReader.convertTIMESTAMP()", "Could not convert convertTIMESTAMP", ex);
    }
    return tz;
  }

  private Object adjustTIMESTAMP(Object tz)
  {
    try
    {
      String tsValue = (String) stringValue.invoke(tz, sqlConnection);

      // Strip of the timezone in order to parse the "plain" time
      // this loses the time zone information stored in Oracle's TIMESTAMPTZ or TIMESTAMPLTZ values
      // but otherwise the displayed time would be totally wrong.
      String cleanValue = removeTimezone(tsValue);
      LocalDateTime dt = LocalDateTime.parse(cleanValue, tsParser);
      Timestamp ts = java.sql.Timestamp.valueOf(dt);

      // TODO: extract the stripped nanoseconds from the passed object
      // and set them in the Timestamp instance using setNanos();
      return ts;
    }
    catch (Throwable ex)
    {
      LogMgr.logDebug("OracleRowDataReader.adjustTIMESTAMP()", "Could not read timestamp", ex);
    }
    return tz;
  }

  public static String removeTimezone(String tsValue)
  {
    int len = tsValue.length();
    if (len <= 19)
    {
      return tsValue;
    }
    int end = tsValue.indexOf(' ', 12);

    if (end > 0 && end < len)
    {
      tsValue = tsValue.substring(0, end);
    }
    return tsValue;
  }
}
