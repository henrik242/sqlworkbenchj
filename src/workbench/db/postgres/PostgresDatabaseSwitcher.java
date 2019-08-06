/*
 * This file is part of SQL Workbench/J, https://www.sql-workbench.eu
 *
 * Copyright 2002-2019, Thomas Kellerer
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
package workbench.db.postgres;

import java.sql.SQLException;
import java.util.List;

import workbench.db.CatalogInformationReader;
import workbench.db.DbSwitcher;
import workbench.db.WbConnection;

/**
 *
 * @author Thomas Kellerer
 */
public class PostgresDatabaseSwitcher
  implements DbSwitcher
{
  @Override
  public boolean supportsSwitching(WbConnection connection)
  {
    return true;
  }

  @Override
  public boolean needsReconnect()
  {
    return true;
  }

  @Override
  public boolean switchDatabase(final WbConnection connection, String dbName)
    throws SQLException
  {
    String newUrl = getUrlForDatabase(connection.getUrl(), dbName);
    final CatalogInformationReader reader = new CatalogInformationReader()
        {
          @Override
          public String getCurrentCatalog()
          {
            return PostgresUtil.getCurrentDatabase(connection);
          }
          @Override
          public void clearCache()
          {
          }
        };
    connection.switchURL(newUrl, reader);
    return true;
  }

  @Override
  public String getUrlForDatabase(String originalUrl, String dbName)
  {
    return PostgresUtil.switchDatabaseURL(originalUrl, dbName);
  }

  @Override
  public List<String> getAvailableDatabases(WbConnection connection)
  {
    return PostgresUtil.getAccessibleDatabases(connection);
  }

  @Override
  public String getCurrentDatabase(WbConnection connection)
  {
    return PostgresUtil.getCurrentDatabase(connection);
  }


}
