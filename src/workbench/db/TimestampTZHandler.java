/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017 Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0 (the "License")
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import workbench.util.WbDateFormatter;

/**
 *
 * @author Thomas Kellerer
 */
public enum TimestampTZHandler
{
  /** Indicates that java.time.OffsetDateTime instances should be used */
  offset,

  /** Indicates that java.time.ZonedDateTime instances should be used */
  timezone,

  /**
   * Indicates that java.sql.Timestamp instances should be used.
   * This is intended for JDBC drivers that do not support Java 8 classes, but
   * this will lose the time zone information if present in the values.
   */
  timestamp,

  /**
   * Indicates that no conversion should take place.
   */
  none;

  public Object adjustValue(Object value)
  {
    if (this == none || value == null) return value;

    if (this == offset)
    {
      if (value instanceof ZonedDateTime)
      {
        ZonedDateTime zdt = (ZonedDateTime)value;
        return zdt.toOffsetDateTime();
      }
      if (value instanceof Timestamp)
      {
        Timestamp ts = (Timestamp)value;
        return OffsetDateTime.of(ts.toLocalDateTime(), WbDateFormatter.getSystemDefaultOffset());
      }
    }

    if (this == timezone)
    {
      if (value instanceof OffsetDateTime)
      {
        OffsetDateTime odt = (OffsetDateTime)value;
        return odt.toZonedDateTime();
      }

      if (value instanceof Timestamp)
      {
        Timestamp ts = (Timestamp)value;
        return ZonedDateTime.of(ts.toLocalDateTime(), ZoneId.systemDefault());
      }
    }

    if (this == timestamp)
    {
      if (value instanceof ZonedDateTime)
      {
        ZonedDateTime zdt = (ZonedDateTime)value;
        return Timestamp.valueOf(zdt.toLocalDateTime());
      }
      if (value instanceof OffsetDateTime)
      {
        OffsetDateTime odt = (OffsetDateTime)value;
        return Timestamp.valueOf(odt.toLocalDateTime());
      }
    }

    return value;
  }

  private static final Map<DBID, String> DRIVER_VERSIONS = new HashMap<>();
  static
  {
    DRIVER_VERSIONS.put(DBID.Postgres, "42.0");
    DRIVER_VERSIONS.put(DBID.Oracle, "12.2");
  }

  public static boolean supportsJava8Time(WbConnection conn)
  {
    if (conn == null) return false;

    String minVersion = DRIVER_VERSIONS.get(DBID.fromConnection(conn));
    return JdbcUtils.hasMiniumDriverVersion(conn, minVersion);
  }

  public static TimestampTZHandler getHandler(WbConnection conn)
  {
    if (conn == null) return none;

    if (supportsJava8Time(conn))
    {
      return conn.getDbSettings().getTimestampTZHandler();
    }
    return none;
  }

}
