/*
 * This file is part of SQL Workbench/J, https://www.sql-workbench.eu
 *
 * Copyright 2002-2018, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     https://www.sql-workbench.eu/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.eu
 *
 */
package workbench.db.oracle;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import workbench.log.CallerInfo;
import workbench.log.LogMgr;

import workbench.db.DbSwitcher;
import workbench.db.JdbcUtils;
import workbench.db.WbConnection;

import workbench.storage.DataStore;

import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleDatabaseSwitcher
  implements DbSwitcher
{
  @Override
  public boolean supportsSwitching(WbConnection connection)
  {
    if (!JdbcUtils.hasMinimumServerVersion(connection, "12.1")) return false;
    try
    {
      DataStore pdbs = OracleUtils.getPDBs(connection);
      return pdbs != null && pdbs.getRowCount() > 1;
    }
    catch (SQLException ex)
    {
      LogMgr.logWarning(new CallerInfo(){}, "Could not check PDBs", ex);
      return false;
    }
  }

  @Override
  public boolean needsReconnect()
  {
    return false;
  }

  @Override
  public boolean switchDatabase(final WbConnection connection, String dbName)
    throws SQLException
  {
    Statement stmt = null;
    String sql = "alter session set container = " + dbName;
    try
    {
      stmt = connection.createStatement();
      stmt.execute(sql);
    }
    catch (SQLException ex)
    {
      LogMgr.logWarning(new CallerInfo(){}, "Could not change PDB to " + dbName + " using:\n" + sql, ex);
      throw ex;
    }
    finally
    {
      SqlUtil.close(stmt);
    }
    return true;
  }

  @Override
  public String getUrlForDatabase(WbConnection originalConnection, String dbName)
  {
    return null;
  }

  @Override
  public List<String> getAvailableDatabases(WbConnection connection)
  {
    try
    {
      DataStore pdbs = OracleUtils.getPDBs(connection);
      List<String> result = new ArrayList<>(pdbs.getRowCount());
      for (int row=0; row < pdbs.getRowCount(); row++)
      {
        result.add(pdbs.getValueAsString(0, "name"));
      }
      return result;
    }
    catch (SQLException ex)
    {
      LogMgr.logWarning(new CallerInfo(){}, "Could not retrieve PDBs", ex);
      return null;
    }
  }

  @Override
  public String getCurrentDatabase(WbConnection connection)
  {
    return null;
  }


}
