/*
 * Db2ColumnEnhancerTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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
package workbench.db.ibm;

import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.ColumnIdentifier;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class Db2ColumnEnhancerTest
	extends WbTestCase
{

	public Db2ColumnEnhancerTest()
	{
		super("Db2ColumnEnhancerTest");
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		Db2TestUtil.initTestCase();

		WbConnection con = Db2TestUtil.getDb2Connection();
		if (con == null) return;

		String sql =
			"CREATE TABLE computed_cols ( \n" +
			"  ID INTEGER, \n" +
			"  ID2 INTEGER GENERATED BY DEFAULT AS IDENTITY (START WITH 12 INCREMENT BY 2 MINVALUE 12), \n " +
      "  ID3 GENERATED ALWAYS AS (ID * 2) \n" +
			");\n" +
		  "commit;\n";
		TestUtil.executeScript(con, sql);
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		WbConnection con = Db2TestUtil.getDb2Connection();
		if (con == null) return;

		String schema = Db2TestUtil.getSchemaName();
		String sql =
			"drop table " + schema + ".computed_cols;\n" +
		  "commit;\n";
		TestUtil.executeScript(con, sql);
	}


	@Test
	public void testUpdateColumnDefinition()
		throws Exception
	{
		WbConnection con = Db2TestUtil.getDb2Connection();
		if (con == null) fail("No connection available");

		TableDefinition tbl = con.getMetadata().getTableDefinition(new TableIdentifier(Db2TestUtil.getSchemaName(), "COMPUTED_COLS"));

		assertNotNull(tbl);
		List<ColumnIdentifier> columns = tbl.getColumns();
		assertNotNull(columns);
		assertEquals(3, columns.size());
		assertTrue(columns.get(1).isAutoincrement());
		assertEquals(columns.get(2).getComputedColumnExpression(), "GENERATED ALWAYS AS (ID * 2)");
	}
}
