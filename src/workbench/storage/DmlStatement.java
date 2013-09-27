/*
 * DmlStatement.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.DbSettings;
import workbench.db.JdbcUtils;
import workbench.db.WbConnection;

import workbench.sql.formatter.SqlFormatter;

import workbench.util.FileUtil;
import workbench.util.NumberStringCache;
import workbench.util.SqlUtil;

/**
 * A class to execute a SQL Statement and to create the statement
 * from a given list of values.
 *
 * @author  Thomas Kellerer
 */
public class DmlStatement
{
	private CharSequence sql;
	private List<ColumnData> values;
	private String chrFunc;
	private String concatString;
	private String concatFunction;
	private boolean formatInserts;
	private boolean formatUpdates;
	private boolean formatDeletes;

	/**
	 *	Create a new DmlStatement with the given SQL template string
	 *	and the given values.
	 *
	 *	The SQL string is expected to contain a ? for each value
	 *	passed in aValueList. The SQL statement will be executed
	 *	using a prepared statement.
	 *
	 * @param aStatement
	 * @param aValueList
	 */
	public DmlStatement(CharSequence aStatement, List<ColumnData> aValueList)
	{
		if (aStatement == null) throw new NullPointerException();
		int count = this.countParameters(aStatement);
		if (count > 0 && aValueList != null && count != aValueList.size())
		{
			throw new IllegalArgumentException("Number of parameter tokens does not match number of parameters passed.");
		}

		this.sql = aStatement;

		if (aValueList == null)
		{
			this.values = Collections.emptyList();
		}
		else
		{
			this.values = aValueList;
		}
		initFormattingFlags();
	}

	public void setFormatSql(boolean flag)
	{
		formatInserts = flag;
		formatUpdates = flag;
		formatDeletes = flag;
	}

	private void initFormattingFlags()
	{
		formatInserts = Settings.getInstance().getDoFormatInserts();
		formatUpdates = Settings.getInstance().getDoFormatInserts();
		formatDeletes = Settings.getInstance().getDoFormatDeletes();
	}

	/**
	 * Execute the statement as a prepared statement
	 *
	 * @param aConnection the Connection to be used
	 * @return the number of rows affected
	 */
	public int execute(WbConnection aConnection)
		throws SQLException
	{
		List<Closeable> streamsToClose = new LinkedList<Closeable>();

		PreparedStatement stmt = null;
		int rows = -1;

		DbSettings dbs = aConnection.getDbSettings();
		boolean useSetNull = dbs.useSetNull();
		boolean useXmlApi = dbs.useXmlAPI();
		boolean useClobSetString = dbs.useSetStringForClobs();
		boolean useBlobSetBytes = dbs.useSetBytesForBlobs();

		try
		{
			stmt = aConnection.getSqlConnection().prepareStatement(this.sql.toString());

			for (int i=0; i < this.values.size(); i++)
			{
				ColumnData data = this.values.get(i);
				int type = data.getIdentifier().getDataType();
				String dbmsType = data.getIdentifier().getDbmsType();
				Object value = data.getValue();
				if (value == null)
				{
					if (useSetNull)
					{
						stmt.setNull(i+1, type);
					}
					else
					{
						stmt.setObject(i+1, null);
					}
				}
				else if (SqlUtil.isClobType(type) && value instanceof String)
				{
					if (useClobSetString)
					{
						stmt.setString(i+1, (String)value);
					}
					else
					{
						String s = (String)value;
						Reader in = new StringReader(s);
						stmt.setCharacterStream(i + 1, in, s.length());
						streamsToClose.add(in);
					}
				}
				else if (useXmlApi && SqlUtil.isXMLType(type) && !dbs.isDmlExpressionDefined(dbmsType) && (value instanceof String))
				{
					SQLXML xml = JdbcUtils.createXML((String)value, aConnection);
					stmt.setSQLXML(i+ 1, xml);
				}
				else if (value instanceof File)
				{
					// Wenn storing data into a blob field, the GUI will
					// put a File object into the DataStore
					File f = (File)value;
					try
					{
						InputStream in = new FileInputStream(f);
						if (useBlobSetBytes)
						{
							byte[] array = FileUtil.readBytes(in);
							stmt.setBytes(i + 1, array);
						}
						else
						{
							stmt.setBinaryStream(i + 1, in, (int)f.length());
							streamsToClose.add(in);
						}
					}
					catch (IOException e)
					{
						throw new SQLException("Input file (" + f.getAbsolutePath() + ") for LOB not found!");
					}
				}
				else
				{
					stmt.setObject(i + 1, value);
				}
			}
			rows = stmt.executeUpdate();
		}
		catch (SQLException e)
		{
			LogMgr.logError("DmlStatement.execute()", "Error executing statement " + sql.toString(), e);
			throw e;
		}
		finally
		{
			FileUtil.closeStreams(streamsToClose);
			SqlUtil.closeStatement(stmt);
		}

		return rows;
	}

	public void setConcatString(String concat)
	{
		if (concat == null) return;
		this.concatString = concat;
		this.concatFunction = null;
	}

	public void setConcatFunction(String func)
	{
		if (func == null) return;
		this.concatFunction = func;
		this.concatString = null;
	}

	public void setChrFunction(String aFunc)
	{
		this.chrFunc = aFunc;
	}

	/**
	 * Returns a "real" SQL Statement which can be executed
	 * directly. The statement contains the parameter values
	 * as literals. No placeholders are used.
	 *
	 * @param literalFormatter the Formatter for date and other literals
	 * @return a SQL statement that can be executed
	 */
	public CharSequence getExecutableStatement(SqlLiteralFormatter literalFormatter)
	{
		return getExecutableStatement(literalFormatter, null);
	}

	public CharSequence getExecutableStatement(SqlLiteralFormatter literalFormatter, WbConnection con)
	{
		CharSequence toUse = this.sql;
		if (this.values.size() > 0)
		{
			StringBuilder result = new StringBuilder(this.sql.length() + this.values.size() * 10);
			boolean inQuotes = false;
			int parmIndex = 0;
			for (int i = 0; i < this.sql.length(); ++i)
			{
				char c = sql.charAt(i);

				if (c == '\'') inQuotes = !inQuotes;
				if (c == '?' && !inQuotes && parmIndex < this.values.size())
				{
					ColumnData data = this.values.get(parmIndex);
					CharSequence literal = literalFormatter.getDefaultLiteral(data);
					if (this.chrFunc != null && SqlUtil.isCharacterType(data.getIdentifier().getDataType()))
					{
						literal = this.createInsertString(literal);
					}
					result.append(literal);
					parmIndex ++;
				}
				else
				{
					result.append(c);
				}
			}
			toUse = result;
		}

		boolean isInsert = toUse.subSequence(0, "INSERT".length()).equals("INSERT");
		boolean isUpdate = !isInsert && toUse.subSequence(0, "UPDATE".length()).equals("UPDATE");
		boolean isDelete = !isUpdate && !isInsert && toUse.subSequence(0, "DELETE".length()).equals("DELETE");

		if ((isInsert && formatInserts) || (isUpdate && formatUpdates) || (isDelete && formatDeletes))
		{
			SqlFormatter f = new SqlFormatter(toUse, con == null ? null : con.getDbId());
			if (con != null)
			{
				f.setCatalogSeparator(con.getMetadata().getCatalogSeparator());
			}
			return f.getFormattedSql();
		}
		return toUse;
	}

	private CharSequence createInsertString(CharSequence aValue)
	{
		if (aValue == null) return null;
		if (this.chrFunc == null) return aValue;
		boolean useConcatFunc = (this.concatFunction != null);

		if (!useConcatFunc && this.concatString == null) this.concatString = "||";
		StringBuilder result = new StringBuilder();
		boolean funcAppended = false;
		boolean quotePending = false;

		char last = 0;

		int len = aValue.length();
		for (int i=0; i < len; i++)
		{
			char c = aValue.charAt(i);
			if (c < 32)
			{
				if (useConcatFunc)
				{
					if (!funcAppended)
					{
						StringBuilder temp = new StringBuilder(concatFunction);
						temp.append('(');
						temp.append(result);
						result = temp;
						funcAppended = true;
					}
					if (quotePending && last >= 32)
					{
						result.append(",\'");
					}
					if (last >= 32) result.append('\'');
					result.append(',');
					result.append(this.chrFunc);
					result.append('(');
					result.append(NumberStringCache.getNumberString(c));
					result.append(')');
					quotePending = true;
				}
				else
				{
					if (last >= 32)
					{
						result.append('\'');
						result.append(this.concatString);
					}
					result.append(this.chrFunc);
					result.append('(');
					result.append(NumberStringCache.getNumberString(c));
					result.append(')');
					result.append(this.concatString);
					quotePending = true;
				}
			}
			else
			{
				if (quotePending)
				{
					if (useConcatFunc) result.append(',');
					result.append('\'');
				}
				result.append(c);
				quotePending = false;
			}
			last = c;
		}
		if (funcAppended)
		{
			result.append(')');
		}
		return result;
	}

	private int countParameters(CharSequence aSql)
	{
		if (aSql == null) return -1;
		boolean inQuotes = false;
		int count = 0;
		for (int i = 0; i < aSql.length(); i++)
		{
			char c = aSql.charAt(i);

			if (c == '\'') inQuotes = !inQuotes;
			if (c == '?' && !inQuotes)
			{
				count ++;
			}
		}
		return count;
	}

	public String getSql()
	{
		return sql.toString();
	}

	@Override
	public String toString()
	{
		return getSql();
	}
}
