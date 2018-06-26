/*
 * ResultColumnMetaData.java
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
package workbench.storage;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import workbench.db.ColumnIdentifier;
import workbench.db.DbMetadata;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.util.Alias;
import workbench.util.CaseInsensitiveComparator;
import workbench.util.CollectionUtil;
import workbench.util.SelectColumn;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A class to retrieve additional column meta data for result (query)
 * columns from a datastore.
 *
 * Currently this only retrieves the remarks for queries based on a single
 * table select statement
 *
 * @author Thomas Kellerer
 */
public class ResultColumnMetaData
{
  private List<Alias> tables;
  private List<String> queryColumns;
  private ColumnIdentifier[] resultColumns;
  private WbConnection connection;

  public ResultColumnMetaData(DataStore ds)
  {
    this(ds.getGeneratingSql(), ds.getOriginalConnection());
    resultColumns = ds.getColumns();
  }

  public ResultColumnMetaData(String sql, WbConnection conn)
  {
    connection = conn;
    if (StringUtil.isBlank(sql)) return;

    tables = SqlUtil.getTables(sql, true, conn);
    queryColumns = SqlUtil.getSelectColumns(sql, true, conn);
  }

  public void retrieveColumnRemarks(ResultInfo info)
    throws SQLException
  {
    retrieveColumnRemarks(info, null);
  }

  public void retrieveColumnRemarks(ResultInfo info, TableDefinition tableDef)
    throws SQLException
  {
    DbMetadata meta = connection.getMetadata();

    Map<String, TableDefinition> tableDefs = new TreeMap<>(CaseInsensitiveComparator.INSTANCE);
    for (Alias alias : tables)
    {
      if (StringUtil.isBlank(alias.getNameToUse())) continue;

      if (tableDef != null && tableDef.getTable().getTableName().equals(alias.getObjectName()))
      {
        tableDefs.put(tableDef.getTable().getRawTableName(), tableDef);
      }
      else
      {
        TableIdentifier tbl = new TableIdentifier(alias.getObjectName(), connection);
        TableDefinition def = meta.getTableDefinition(tbl);
        tableDefs.put(alias.getNameToUse().toLowerCase(), def);
      }
    }

    if (shouldUseResultColumns())
    {
      updateFromResultColumns(info, tableDefs.values());
    }
    else
    {
      updateFromQueryColumns(info, tableDefs);
    }
  }

  private boolean shouldUseResultColumns()
  {
    if (resultColumns == null || resultColumns.length == 0) return false;
    if (connection.getDbSettings().supportsResultMetaGetTable())
    {
      return true;
    }

    Set<String> tableName = CollectionUtil.caseInsensitiveSet();
    for (ColumnIdentifier col : resultColumns)
    {
      if (col.getSourceTableName() != null)
      {
        tableName.add(col.getSourceTableName());
      }
    }
    // At least some tables can be identified
    // Prefer this over detecting the columns from the SQL query
    return tableName.size() > 0;
  }

  /**
   * Try to expand wildcard "columns" to the real columns.
   */
  private List<SelectColumn> expandQueryColumns(Map<String, TableDefinition> tableDefs)
  {
    List<SelectColumn> result = new ArrayList<>();
    if (queryColumns.size() == 1 && queryColumns.get(0).equals("*"))
    {
      // easy case, just process all tables in the order they were specified
      for (Alias alias : tables)
      {
        TableDefinition tdef = tableDefs.get(alias.getNameToUse());
        if (tdef == null) continue;
        for (ColumnIdentifier col : tdef.getColumns())
        {
          SelectColumn c = new SelectColumn(col.getColumnName());
          c.setColumnTable(tdef.getTable().getRawTableName());
          result.add(c);
        }
      }
      return result;
    }

    for (String col : queryColumns)
    {
      SelectColumn c = new SelectColumn(col);
      String tname = c.getColumnTable();
      if (StringUtil.isBlank(tname))
      {
        result.add(c);
        continue;
      }

      TableDefinition tdef = tableDefs.get(c.getColumnTable());
      if (tdef != null)
      {
        if (c.getObjectName().equals("*"))
        {
          for (ColumnIdentifier cid : tdef.getColumns())
          {
            SelectColumn sc = new SelectColumn(cid.getColumnName());
            sc.setColumnTable(tdef.getTable().getRawTableName());
            result.add(sc);
          }
        }
        else
        {
          c.setColumnTable(tname);
          result.add(c);
        }
      }
    }
    return result;
  }

  private void updateFromQueryColumns(ResultInfo info, Map<String, TableDefinition> tableDefs)
  {
    if (CollectionUtil.isEmpty(queryColumns)) return;

    List<SelectColumn> columns = expandQueryColumns(tableDefs);

    for (SelectColumn c : columns)
    {
      TableDefinition def = findTableForColumn(c, tableDefs);
      if (def != null)
      {
        ColumnIdentifier id = def.findColumn(c.getObjectName());
        int index = info.findColumn(id.getColumnName());
        if (index > -1)
        {
          info.getColumn(index).setComment(id.getComment());
          info.getColumn(index).setSourceTableName(def.getTable().getRawTableName());
        }
      }
    }
  }

  private TableDefinition findTableForColumn(SelectColumn column, Map<String, TableDefinition> tableDefs)
  {
    for (TableDefinition def : tableDefs.values())
    {
      ColumnIdentifier c = ColumnIdentifier.findColumnInList(def.getColumns(), column.getObjectName());
      if (c != null)
      {
        return def;
      }
    }
    return null;
  }

  private void updateFromResultColumns(ResultInfo info, Collection<TableDefinition> tableDefs)
  {
    for (TableDefinition tbl : tableDefs)
    {
      String tableName = tbl.getTable().getRawTableName();
      for (ColumnIdentifier col : tbl.getColumns())
      {
        int index = info.findTableColumn(tableName, col.getColumnName(), connection.getMetadata());
        if (index > -1)
        {
          ColumnIdentifier resultColumn = info.getColumn(index);
          resultColumn.setComment(col.getComment());
          if (resultColumn.getSourceTableName() == null)
          {
            resultColumn.setSourceTableName(tableName);
          }
        }
      }
    }
  }

}
