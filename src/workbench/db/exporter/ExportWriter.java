/*
 * ExportWriter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2004, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.db.exporter;

import java.io.IOException;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.SQLException;

import workbench.storage.ResultInfo;
import workbench.storage.RowActionMonitor;
import workbench.storage.RowData;
import workbench.util.StrBuffer;

/**
 *
 * @author  info@sql-workbench.net
 */
public abstract class ExportWriter
{
	protected DataExporter exporter;
	protected boolean cancel = false;
	protected long rows;
	protected String tableToUse;
	protected RowActionMonitor rowMonitor;

	public ExportWriter(DataExporter exp)
	{
		this.exporter = exp;
	}
	
	public abstract RowDataConverter createConverter(ResultInfo info);
	
	public void setRowMonitor(RowActionMonitor monitor)
	{
		this.rowMonitor = monitor;
	}
	
	public long getNumberOfRecords()
	{
		return rows;
	}
	
	public void writeExport(Writer out, ResultSet rs, ResultInfo info)
		throws SQLException, IOException
	{
		RowDataConverter converter = createConverter(info);
		converter.setEncoding(exporter.getEncoding());
		converter.setDefaultDateFormatter(exporter.getDateFormatter());
		converter.setDefaultTimestampFormatter(exporter.getTimestampFormatter());
		converter.setDefaultNumberFormatter(exporter.getDecimalFormatter());
		converter.setGeneratingSql(exporter.getSql());
		converter.setOriginalConnection(this.exporter.getConnection());
		
		this.cancel = false;
		this.rows = 0;
	
		if (this.rowMonitor != null)
		{
			this.rowMonitor.setMonitorType(RowActionMonitor.MONITOR_EXPORT);
		}
		StrBuffer data = converter.getStart();
		if (data != null)
		{
			data.writeTo(out);
		}
		int colCount = info.getColumnCount();
		while (rs.next())
		{
			if (this.cancel) break;
			if (this.rowMonitor != null)
			{
				this.rowMonitor.setCurrentRow((int)this.rows, -1);
			}
			RowData row = new RowData(colCount);
			row.read(rs, info);
			data = converter.convertRowData(row, rows);
			data.writeTo(out);
			rows ++;
		}
		data = converter.getEnd(rows);
		if (data != null)
		{
			data.writeTo(out);
		}
	}
	
	public void exportFinished()
	{
	}
	
	public void cancel()
	{
		this.cancel = true;
	}
	
	/**
	 * Getter for property tableToUse.
	 * @return Value of property tableToUse.
	 */
	public java.lang.String getTableToUse()
	{
		return tableToUse;
	}
	
	/**
	 * Setter for property tableToUse.
	 * @param tableToUse New value of property tableToUse.
	 */
	public void setTableToUse(java.lang.String tableToUse)
	{
		this.tableToUse = tableToUse;
	}
	
}
