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
package workbench.storage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import workbench.log.LogMgr;

import workbench.db.JdbcUtils;
import workbench.db.TimestampTZHandler;
import workbench.db.WbConnection;

/**
 *
 * @author Thomas Kellerer
 */
class PostgresRowDataReader
  extends RowDataReader
{

  private boolean useOffsetDateTime;

  PostgresRowDataReader(ResultInfo info, WbConnection conn)
  {
    super(info, conn);
    useOffsetDateTime = TimestampTZHandler.supportsJava8Time(conn);
    if (useOffsetDateTime)
    {
      LogMgr.logInfo("PostgresRowDataReader.<init>", "Using ZonedDateTime to read TIMESTAMP WITH TIME ZONE columns");
    }
  }

  @Override
  protected Object readTimestampTZValue(ResultSet rs, int column)
    throws SQLException
  {
    if (useOffsetDateTime)
    {
      return readTimeZoneInfo(rs, column);
    }
    return super.readTimestampTZValue(rs, column);
  }

  private ZonedDateTime readTimeZoneInfo(ResultSet rs, int column)
    throws SQLException
  {
    OffsetDateTime odt = rs.getObject(column, OffsetDateTime.class);
    if (odt == null) return null;
    return odt.atZoneSameInstant(ZoneId.systemDefault());
  }
}
