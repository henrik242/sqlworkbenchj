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
package workbench.sql.generator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import workbench.db.ColumnIdentifier;
import workbench.db.QuoteHandler;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.db.importer.ConstantColumnValues;

import workbench.storage.ColumnData;
import workbench.storage.RowData;
import workbench.storage.SqlLiteralFormatter;

import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class DefaultInsertGenerator
{
  private final TableIdentifier table;
  private final List<ColumnIdentifier> targetColumns;
  private InsertType type;
  private String insertSqlStart;
  private ConstantColumnValues columnConstants;
  private QuoteHandler quoteHandler;
  private final List<RowData> rows = new ArrayList<>();

  public DefaultInsertGenerator(TableIdentifier table, List<ColumnIdentifier> targetColumns)
  {
    this(table, targetColumns, null);
  }

  public DefaultInsertGenerator(TableIdentifier table, List<ColumnIdentifier> targetColumns, WbConnection conn)
  {
    this.table = table;
    this.targetColumns = new ArrayList<>(targetColumns);
    this.type = InsertType.Insert;
    if (conn == null)
    {
      quoteHandler = QuoteHandler.STANDARD_HANDLER;
    }
    else
    {
      quoteHandler = conn.getMetadata();
    }
  }

  public void setInsertStartSQL(String insertStart)
  {
    this.insertSqlStart = insertStart;
  }

  public String createLiteralSQL(SqlLiteralFormatter literalFormatter)
  {
    return createLiteralSQL(rows, literalFormatter);
  }

  public String createLiteralSQL(List<RowData> data, SqlLiteralFormatter literalFormatter)
  {
    StringBuilder result = new StringBuilder(targetColumns.size() * 50 + targetColumns.size() * data.size() * 15 + 50);
    result.append(buildInsertPart());
    result.append("\nVALUES\n  ");
    result.append(buildValuesList(data, literalFormatter));
    result.append(buildFinalPart());
    return result.toString();

  }
  public String createPreparedSQL(int numRows)
  {
    StringBuilder result = new StringBuilder(targetColumns.size() * 50 + targetColumns.size() * numRows * 5 + 50);
    result.append(buildInsertPart());
    result.append("\nVALUES\n  ");
    result.append(buildParameterList(numRows));
    result.append(buildFinalPart());
    return result.toString();
  }

  protected CharSequence buildInsertPart()
  {
    StringBuilder text = new StringBuilder(targetColumns.size() * 50);
    String sql = insertSqlStart;
    if (StringUtil.isNonBlank(sql))
    {
      text.append(sql);
      text.append(' ');
    }
    else
    {
      text.append("INSERT INTO ");
    }
    text.append(table.getTableExpression());
    text.append("\n  (");

    for (int i=0; i < targetColumns.size(); i++)
    {
      ColumnIdentifier col = this.targetColumns.get(i);
      if (i > 0)
      {
        text.append(',');
      }
      String colname = col.getDisplayName();
      colname = quoteHandler.quoteObjectname(colname);
      text.append(colname);
    }

    if (columnConstants != null)
    {
      int cols = columnConstants.getColumnCount();
      for (int i = 0; i < cols; i++)
      {
        text.append(',');
        text.append(columnConstants.getColumn(i).getColumnName());
      }
    }
    text.append(")");
    return text;
  }

  protected CharSequence buildParameterList(int numRows)
  {
    StringBuilder list = new StringBuilder(this.targetColumns.size() * 5 + numRows);
    for (int i=0; i < numRows; i++)
    {
      if (i>0)
      {
        list.append(",\n  ");
      }

      list.append('(');
      for (int c = 0; c < targetColumns.size(); c++)
      {
        if (c > 0)
        {
          list.append(',');
        }
        list.append('?');
      }
      list.append(')');
    }
    return list;
  }

  protected CharSequence buildValuesList(List<RowData> data, SqlLiteralFormatter formatter)
  {
    StringBuilder list = new StringBuilder(this.targetColumns.size() * 5 + data.size());
    for (int i=0; i < data.size(); i++)
    {
      if (i>0)
      {
        list.append(",\n  ");
      }

      RowData row = data.get(i);
      list.append('(');
      for (int c = 0; c < targetColumns.size(); c++)
      {
        if (c > 0)
        {
          list.append(',');
        }
        ColumnData colData = new ColumnData(row.getValue(c), targetColumns.get(c));
        CharSequence literal = formatter.getDefaultLiteral(colData);
        list.append(literal);
      }
      list.append(')');
    }
    return list;
  }

  protected String buildRowValues()
  {
    StringBuilder values = new StringBuilder(targetColumns.size() * 50);
    return values.toString();
  }

  protected CharSequence buildFinalPart()
  {
    return "";
  }

  protected String getColumnConstant(ColumnIdentifier col)
  {
    if (this.columnConstants == null) return null;
    return null;
  }

  public TableIdentifier getTargetTable()
  {
    return table;
  }

  public boolean supportsType(InsertType type)
  {
    return type == InsertType.Insert;
  }

  public void setInsertType(InsertType type)
  {
    if (!this.supportsType(type)) throw new IllegalArgumentException("Insert type " + type + " not supported!");
    if (this.type != type)
    {
      this.type = type;
      buildInsertPart();
    }
  }

  public boolean supportsMultiRowInserts()
  {
    return true;
  }

  public int addRow(RowData row)
  {
    rows.add(row);
    return rows.size();
}

  public List<RowData> getValues()
  {
    return Collections.unmodifiableList(rows);
  }

  public void clearRowData()
  {
    this.rows.clear();
  }

}
