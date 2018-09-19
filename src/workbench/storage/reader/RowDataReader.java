/*
 * RowDataReader.java
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
package workbench.storage.reader;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Struct;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.DbMetadata;
import workbench.db.DbSettings;
import workbench.db.WbConnection;
import workbench.db.mssql.SqlServerDataConverter;
import workbench.db.oracle.OracleDataConverter;
import workbench.db.postgres.PostgresDataConverter;

import workbench.storage.ArrayConverter;
import workbench.storage.DataConverter;
import workbench.storage.ResultInfo;
import workbench.storage.RowData;
import workbench.storage.StructConverter;

import workbench.util.FileUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A class to read the data from a ResultSet.
 *
 * Different data types are handled correctly and different strategies for "extended" types (BLOB, CLOB XML, ...) can
 * be chosen.
 * <br/>
 * Errors during the retrieval of one row are re-thrown to be shown in the frontend. This behaviour can
 * be disabled using the config property <tt>workbench.db.ignore.readerror</tt>.
 *
 * @author Thomas Kellerer
 *
 * @see ResultInfo#treatLongVarcharAsClob()
 * @see ResultInfo#useGetBytesForBlobs()
 * @see ResultInfo#useGetStringForClobs()
 * @see ResultInfo#useGetStringForBit()
 * @see ResultInfo#useGetXML()
 * @see ResultInfo#getConvertArrays()
 */
public class RowDataReader
{
  private final List<Closeable> streams;
  private DataConverter converter;
  private boolean ignoreReadErrors;
  private boolean useStreamsForBlobs;
  private	boolean useStreamsForClobs;
  private boolean longVarcharAsClob;
  private boolean useGetBytesForBlobs;
  private boolean useGetStringForClobs;
  private boolean useGetStringForBit;
  private boolean useGetObjectForDates;
  private boolean useGetObjectForTimestamps;
  private boolean useGetXML;
  private boolean adjustArrayDisplay;
  private boolean showArrayType;
  private boolean fixStupidMySQLZeroDate;
  private boolean isOracle;
  protected ResultInfo resultInfo;

  public RowDataReader(ResultInfo info, WbConnection conn)
  {
    ignoreReadErrors = Settings.getInstance().getBoolProperty("workbench.db.ignore.readerror", false);
    converter = getConverterInstance(conn);
    isOracle = conn == null ? false : conn.getMetadata().isOracle();
    resultInfo = info;
    DbSettings dbs = conn == null ? null : conn.getDbSettings();
    if (dbs != null)
    {
      longVarcharAsClob = dbs.longVarcharIsClob();
      useGetBytesForBlobs = dbs.useGetBytesForBlobs();
      useGetStringForClobs = dbs.useGetStringForClobs();
      useGetStringForBit = dbs.useGetStringForBit();
      useGetObjectForDates = dbs.useGetObjectForDates();
      useGetObjectForTimestamps = dbs.useGetObjectForTimestamps();
      showArrayType = dbs.showArrayType();
      adjustArrayDisplay = dbs.handleArrayDisplay();
      useGetXML = dbs.useGetXML();
      fixStupidMySQLZeroDate = dbs.fixStupidMySQLZeroDate();
    }
    streams = new ArrayList<>(countLobColumns());
  }

  private int countLobColumns()
  {
    if (resultInfo == null) return 0;
    int lobCount = 0;
    for (int i=0; i < resultInfo.getColumnCount(); i++)
    {
      int type = resultInfo.getColumnType(i);
      if (SqlUtil.isBlobType(type) || SqlUtil.isClobType(type))
      {
        lobCount ++;
      }
    }
    return lobCount;
  }

  /**
   * Register a DataConverter with this reader.
   *
   * @param conv  the convert to use
   * @see #read(java.sql.ResultSet, boolean)
   */
  public void setConverter(DataConverter conv)
  {
    this.converter = conv;
  }

  /**
   * Controls how BLOB columns are returned.
   * <br/>
   * By default they are converted to a byte[] array. If setUseStreamsForBlobs() is enabled.
   * then they will be returned as InputStreams (as returned by ResultSet.getBinaryStream()).
   * <br/>
   *
   * <b>If this is set to true, the consumer of the RowData instance is responsible for closing
   * all InputStreams returned by this class.</b>
   *
   * @param flag if true, return InputStreams instead of byte[]
   *
   * @see #read(java.sql.ResultSet, boolean)
   */
  public void setUseStreamsForBlobs(boolean flag)
  {
    useStreamsForBlobs = flag;
  }

  /**
   * Controls how CLOB columns are returned.
   * <br/>
   *
   * By default CLOB data is converted to a String.<br/>
   *
   * Setting useStreamsForClobs to true will return a <tt>Reader</tt> instance for the CLOB columns
   * (as returned by ResultSet.getCharacterStream()).
   *
   * <b>If this is set to true, the consumer of the RowData instance is responsible for closing
   * all Readers returned by this class.</b>
   *
   * Setting this to true will be ignored if <tt>getString()</tt> is used for the CLOB columns
   *
   * @param flag if true, return <tt>Reader</tt>s instead of <tt>String</tt>s
   *
   * @see #read(java.sql.ResultSet, boolean)
   * @see ResultInfo#useGetStringForClobs()
   */
  public void setUseStreamsForClobs(boolean flag)
  {
    useStreamsForClobs = flag;
  }

  /**
   * Read the current row from the ResultSet into a RowData instance
   * <br/>
   * It is assumed that ResultSet.next() has already been called on the ResultSet.
   * <br/>
   * BLOBs (and similar datatypes) will be read into a byte array unless setUseStreamsForBlobs(true) is called.
   * CLOBs (and similar datatypes) will be converted into a String object.
   * <br/>
   * All other types will be retrieved using getObject() from the result set, except for
   * timestamp and date to work around issues with the Oracle driver.
   * <br/>
   * If the driver returns a java.sql.Struct, this will be converted into a String
   * using {@linkplain StructConverter#getStructDisplay(java.sql.Struct)}
   * <br/>
   * After retrieving the value from the ResultSet it is passed to a registered DataConverter.
   * If a converter is registered, no further processing will be done with the column's value
   * <br/>
   * The status of the returned RowData will be NOT_MODIFIED.
   *
   * @param rs              the ResultSet that is positioned to the correct row
   * @param trimCharData    if true, values for Types.CHAR columns will be trimmed.
   *
   * @see #setConverter(workbench.storage.DataConverter)
   * @see #setUseStreamsForBlobs(boolean)
   * @see #setUseStreamsForClobs(boolean)
   */
  public RowData read(ResultSet rs, boolean trimCharData)
    throws SQLException
  {
    return read(new ResultSetHolder(rs), trimCharData);
  }

  public RowData read(ResultHolder rs, boolean trimCharData)
    throws SQLException
  {
    int colCount = resultInfo.getColumnCount();
    Object[] colData = new Object[colCount];

    Object value;
    for (int i=0; i < colCount; i++)
    {
      int type = resultInfo.getColumnType(i);

      if (converter != null)
      {
        String dbms = resultInfo.getDbmsTypeName(i);
        if (converter.convertsType(type, dbms))
        {
          value = rs.getObject(i + 1);
          colData[i] = converter.convertValue(type, dbms, value);
          continue;
        }
      }
      colData[i] = readColumnData(rs, type, i+1, trimCharData);
    }
    return new RowData(colData);
  }

  public Object readColumnData(ResultHolder rs, int type, int column, boolean trimCharData)
    throws SQLException
  {
    Object value;

    try
    {
      if (type == Types.VARCHAR || type == Types.NVARCHAR)
      {
        value = rs.getString(column);
      }
      else if (type == Types.TIMESTAMP)
      {
        value = readTimestampValue(rs, column);
      }
      else if (type == Types.TIMESTAMP_WITH_TIMEZONE)
      {
        value = readTimestampTZValue(rs, column);
      }
      else if (type == Types.DATE)
      {
        value = readDateValue(rs, column);
      }
      else if (useGetStringForBit && type == Types.BIT)
      {
        value = rs.getString(column);
      }
      else if (adjustArrayDisplay && type == java.sql.Types.ARRAY)
      {
        // this is mainly here for Oracle nested tables and VARRAYS, but should basically work
        // for other arrays as well.
        Object o = rs.getObject(column);
        value = ArrayConverter.getArrayDisplay(o, resultInfo.getDbmsTypeName(column-1), showArrayType, isOracle);
      }
      else if (type == java.sql.Types.STRUCT)
      {
        Object o = rs.getObject(column);
        if (o instanceof Struct)
        {
          value = StructConverter.getInstance().getStructDisplay((Struct)o, isOracle);
        }
        else
        {
          value = o;
        }
      }
      else if (SqlUtil.isBlobType(type))
      {
        if (useStreamsForBlobs)
        {
          // this is used by the RowDataConverter in order to avoid
          // reading large blobs into memory
          InputStream in = rs.getBinaryStream(column);
          if (in == null || rs.wasNull())
          {
            value = null;
          }
          else
          {
            addStream(in);
            value = in;
          }
        }
        else if (useGetBytesForBlobs)
        {
          value = rs.getBytes(column);
          if (rs.wasNull()) value = null;
        }
        else
        {
          value = readBinaryStream(rs, column);
        }
      }
      else if (type == Types.SQLXML)
      {
        value = readXML(rs, column, useGetXML);
      }
      else if (SqlUtil.isClobType(type, longVarcharAsClob))
      {
        if (useGetStringForClobs)
        {
          value = rs.getString(column);
        }
        else if (useStreamsForClobs)
        {
          Reader in = rs.getCharacterStream(column);
          if (in == null || rs.wasNull())
          {
            value = null;
          }
          else
          {
            value = in;
            addStream(in);
          }
        }
        else
        {
          value = readCharacterStream(rs, column);
        }
      }
      else if (type == Types.CHAR || type == Types.NCHAR)
      {
        value = rs.getString(column);
        if (trimCharData)
        {
          value = StringUtil.rtrim((String)value);
        }
      }
      else
      {
        value = rs.getObject(column);
      }
    }
    catch (SQLException e)
    {
      if (ignoreReadErrors)
      {
        value = null;
        LogMgr.logError("RowDataReader.read()", "Error retrieving data for column '" + resultInfo.getColumnName(column-1) + "'. Using NULL!", e);
      }
      else
      {
        throw e;
      }
    }
    catch (Throwable e)
    {
      // I'm catching Throwable here just in case
      // (e.g. Oracle's XML type library is missing a ClassNotFoundException is thrown that would otherwise not be visible)
      String error = e.getClass().getName();
      if (e.getMessage() != null)
      {
        error += ": " + e.getMessage();
      }
      throw new SQLException(error, e);
    }
    return value;
  }
  
  /**
   * Read a timestamp value from the current row and given column.
   * <br/>
   * <br/>
   * This is made a separate method to allow DBMS specifc implementations of the RowDataReader to  read timestamps differently.<br/>
   * <br/>
   * Currently this is used to adjust the timezone information returned by the Oracle driver
   * (see {@linkplain OracleRowDataReader#readTimestampValue(java.sql.ResultSet, int)})
   *
   * @param rs      the ResultSet to read from
   * @param column  the column index (1-based) to read
   * @return the value retrieved.
   *
   * @throws SQLException
   * @see OracleRowDataReader#readTimestampValue(java.sql.ResultSet, int)
   */
  protected Object readTimestampValue(ResultHolder rs, int column)
    throws SQLException
  {
    try
    {
      if (useGetObjectForTimestamps)
      {
        return rs.getObject(column);
      }
      return rs.getTimestamp(column);
    }
    catch (SQLException ex)
    {
      if (fixStupidMySQLZeroDate && "S1009".equals(ex.getSQLState()))
      {
        return rs.getString(column);
      }
      throw ex;
    }
  }

  protected Object readTimestampTZValue(ResultHolder rs, int column)
    throws SQLException
  {
    return readTimestampValue(rs, column);
  }

  protected Object readDateValue(ResultHolder rs, int column)
    throws SQLException
  {
    if (useGetObjectForDates)
    {
      return rs.getObject(column);
    }
    return rs.getDate(column);
  }

  private void addStream(Closeable in)
  {
    if (in == null) return;
    synchronized (streams)
    {
      streams.add(in);
    }
  }

  public void closeStreams()
  {
    synchronized (streams)
    {
      if (streams.size() > 0)
      {
        FileUtil.closeStreams(streams);
        streams.clear();
      }
    }
  }

  private Object readXML(ResultHolder rs, int column, boolean useGetXML)
    throws SQLException
  {
    Object value = null;
    if (useGetXML)
    {
      SQLXML xml = null;
      try
      {
        xml = rs.getSQLXML(column);
        value = xml.getString();
      }
      finally
      {
        if (xml != null) xml.free();
      }
    }
    else
    {
      value = readCharacterStream(rs, column);
    }
    return value;
  }

  private Object readBinaryStream(ResultHolder rs, int column)
    throws SQLException
  {
    Object value;
    InputStream in;
    try
    {
      in = rs.getBinaryStream(column);
      if (in == null || rs.wasNull())
      {
        value = null;
      }
      else
      {
        // readBytes will close the InputStream
        value = FileUtil.readBytes(in);
      }
    }
    catch (IOException e)
    {
      LogMgr.logError("RowDataReader.readBinaryStream()", "Error retrieving binary data for column #" + resultInfo.getColumnName(column), e);
      value = rs.getObject(column);
    }
    return value;
  }

  private Object readCharacterStream(ResultHolder rs, int column)
    throws SQLException
  {
    Object value;
    Reader in;
    try
    {
      in = rs.getCharacterStream(column);
      if (in != null && !rs.wasNull())
      {
        // readCharacters will close the Reader
        value = FileUtil.readCharacters(in);
      }
      else
      {
        value = null;
      }
    }
    catch (IOException e)
    {
      LogMgr.logWarning("RowDataReader.readCharacterStream()", "Error retrieving clob data for column #" + column, e);
      value = rs.getObject(column);
    }
    return value;
  }

  /**
   * Creates instances of necessary DataConverters.
   * <br/>
   * The following datatypes are currently supported:
   * <ul>
   * <li>For SQL Server's timestamp type</li>
   * <li>For Oracle: RAW and ROWID types</li>
   * </ul>
   *
   * @param conn the connection for which to create the DataConverter
   * @return a suitable converter or null if nothing should be converted
   *
   * @see workbench.resource.Settings#getFixSqlServerTimestampDisplay()
   * @see workbench.resource.Settings#getConvertOracleTypes()
   * @see workbench.db.oracle.OracleDataConverter
   * @see workbench.db.mssql.SqlServerDataConverter
   */
  public static DataConverter getConverterInstance(WbConnection conn)
  {
    if (conn == null) return null;

    DbMetadata meta = conn.getMetadata();
    if (meta == null) return null;

    if (meta.isOracle() && Settings.getInstance().getConvertOracleTypes())
    {
      return OracleDataConverter.getInstance();
    }
    if (meta.isSqlServer() && Settings.getInstance().getFixSqlServerTimestampDisplay())
    {
      return SqlServerDataConverter.getInstance();
    }
    if (meta.isPostgres())
    {
      return PostgresDataConverter.getInstance();
    }
    return null;
  }


}
