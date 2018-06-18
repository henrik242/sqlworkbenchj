/*
 * UseCommand.java
 *
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
package workbench.sql.wbcommands;


import java.sql.SQLException;

import workbench.resource.ResourceMgr;

import workbench.db.CatalogInformationReader;
import workbench.db.postgres.PostgresUtil;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

import workbench.util.ExceptionUtil;

/**
 * A command to switch the current database in Postgres by creating a new connection.
 *
 * @author  Thomas Kellerer
 */
public class WbSwitchDB
	extends SqlCommand
  implements CatalogInformationReader
{
	public static final String VERB = "WbSwitchDB";

	@Override
	public StatementRunnerResult execute(String sql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult(sql);
		try
		{
			// everything after the WbSwitchDB command is the database name
			String dbName = getCommandLine(sql);
      String newUrl = PostgresUtil.switchDatabaseURL(this.currentConnection.getUrl(), dbName);
      this.currentConnection.switchURL(newUrl, this);

			String msg = ResourceMgr.getFormattedString("MsgCatalogChanged", ResourceMgr.getString("TxtDatabase"), dbName);
			result.addMessage(msg);
			result.setSuccess();

			result.setSuccess();
		}
		catch (Exception e)
		{
			result.addMessageByKey("MsgExecuteError");
			result.addErrorMessage(ExceptionUtil.getAllExceptions(e).toString());
		}
		finally
		{
			this.done();
		}

		return result;
	}

	@Override
	public String getVerb()
	{
		return VERB;
	}

  @Override
  public String getCurrentCatalog()
  {
    return PostgresUtil.getCurrentDatabase(currentConnection);
  }

  @Override
  public void clearCache()
  {
  }

}
