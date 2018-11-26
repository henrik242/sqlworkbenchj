/*
 * This file is part of SQL Workbench/J, https://www.sql-workbench.eu
 *
 * Copyright 2002-2018 Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0 (the "License")
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.sql-workbench.eu/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.eu
 */
package workbench.sql.wbcommands;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import workbench.log.CallerInfo;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

import workbench.storage.DataStore;
import workbench.storage.RefCursorConsumer;
import workbench.storage.RowActionMonitor;
import workbench.storage.reader.ResultHolder;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;

import workbench.util.ExceptionUtil;
import workbench.util.SqlUtil;


/**
 *
 * @author Thomas Kellerer
 */
public class PgCallCommand
  extends SqlCommand
  implements RefCursorConsumer
{
  public static final String VERB = "CALL";
  private List<DataStore> refCursorData;
  private Map<Integer, String> refCursorColumns;

	@Override
	public String getVerb()
	{
		return VERB;
	}

  @Override
  protected void processResults(StatementRunnerResult result, boolean hasResult)
    throws SQLException
  {
    if (result == null) return;
    if (currentStatement == null) return;
    if (!hasResult) return;

    ResultSet mainResult = null;

    try
    {
      mainResult = currentStatement.getResultSet();
      refCursorColumns = refCursorNames(mainResult);

      RowActionMonitor monitorToUse = null;
      if (showDataLoading && runner.getStatementHook().displayResults())
      {
        monitorToUse = this.rowMonitor;
      }
      boolean fetchOnly = runner.getStatementHook().fetchResults() && !runner.getStatementHook().displayResults();

      currentRetrievalData = new DataStore(mainResult, false, monitorToUse, maxRows, this.currentConnection);
      currentRetrievalData.setResultName(ResourceMgr.getString("LblTabProcResult"));
      if (fetchOnly)
      {
        int rows = currentRetrievalData.fetchOnly(mainResult);
        result.setRowsProcessed(rows);
      }
      else
      {
        currentRetrievalData.initData(mainResult, maxRows, this);
      }
    }
    catch (SQLException e)
    {
      // Some JDBC driver throw an exception when a statement is
      // cancelled. But in this case, we do not want to throw away the
      // data that was retrieved until now. We only add a warning
      if (this.currentRetrievalData != null && this.currentRetrievalData.isCancelled())
      {
        result.addWarningByKey("MsgErrorDuringRetrieve");
        result.addMessage(ExceptionUtil.getAllExceptions(e));
      }
      else
      {
        // if the statement was not cancelled, make sure
        // the error is displayed to the user.
        throw e;
      }
    }
    finally
    {
      SqlUtil.closeResult(mainResult);
    }

    if (refCursorData != null)
    {
      if (refCursorData.size() < currentRetrievalData.getColumnCount())
      {
        // only show the procedure result if at least one column was not a ref cursor
        result.addDataStore(currentRetrievalData);
      }
      for (DataStore ds : refCursorData)
      {
        result.addDataStore(ds);
      }
    }
    else
    {
      result.addDataStore(currentRetrievalData);
    }
  }

  private Map<Integer, String> refCursorNames(ResultSet rs)
  {
    Map<Integer, String> result = new HashMap<>();

    if (rs == null) return result;

    try
    {
      ResultSetMetaData meta = rs.getMetaData();
      if (meta == null) return result;

      for (int i=1; i <= meta.getColumnCount(); i++)
      {
        if (meta.getColumnType(i) == Types.REF_CURSOR)
        {
          result.put(i, meta.getColumnName(i));
        }
      }
    }
    catch (Throwable th)
    {
      LogMgr.logWarning(new CallerInfo(){}, "Could not check for refcursor columns", th);
    }
    return result;
  }


  @Override
  public Object readRefCursor(ResultHolder rs, int columnIndex)
  {
    if (refCursorData == null)
    {
      refCursorData = new ArrayList<>();
    }
    String name = null;
    if (refCursorColumns != null)
    {
      name = refCursorColumns.get(columnIndex);
    }

    if (name == null)
    {
      name = "[" + ResourceMgr.getString("LblTabResult") + " " + refCursorData.size() + "]";
    }

    ResultSet refCursor = null;
    try
    {
      refCursor = (ResultSet)rs.getObject(columnIndex);
      DataStore ds = new DataStore(refCursor, currentConnection, true);
      ds.setResultName(name);
      this.refCursorData.add(ds);
    }
    catch (Exception ex)
    {
      LogMgr.logWarning(new CallerInfo(){}, "Could not retrieve ref cursor for column: " + columnIndex);
    }
    finally
    {
      SqlUtil.close(refCursor);
    }
    return name;
  }

  @Override
  public void done()
  {
    super.done();
    if (refCursorData != null)
    {
      refCursorData.clear();
      refCursorData = null;
    }
    if (refCursorColumns != null)
    {
      refCursorColumns.clear();
      refCursorColumns = null;
    }
  }

}
