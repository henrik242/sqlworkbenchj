/*
 * SqlServerIndexReaderTest.java
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
package workbench.db.mssql;

import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.IndexDefinition;
import workbench.db.IndexReader;
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
public class SqlServerIndexReaderTest
	extends WbTestCase
{
	public SqlServerIndexReaderTest()
	{
		super("SqlServerIndexReaderTest");
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		SQLServerTestUtil.initTestcase("SqlServerProcedureReaderTest");
		WbConnection conn = SQLServerTestUtil.getSQLServerConnection();
		if (conn == null) return;
		SQLServerTestUtil.dropAllObjects(conn);
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		WbConnection con = SQLServerTestUtil.getSQLServerConnection();
		if (con == null) return;
		SQLServerTestUtil.dropAllObjects(con);
	}

	@Test
	public void testReader()
		throws Exception
	{
		WbConnection con = SQLServerTestUtil.getSQLServerConnection();
		if (con == null) return;
		String sql =
				"create table foo \n" +
				"( \n" +
				"   id1 integer, \n" +
				"   id2 integer \n" +
				")\n"  +
			"create index ix_one on foo (id1) include (id2); " +
			"commit;\n";
		TestUtil.executeScript(con, sql);
		IndexReader reader = con.getMetadata().getIndexReader();
		TableIdentifier tbl = new TableIdentifier("dbo.foo");
		List<IndexDefinition> indexes = reader.getTableIndexList(tbl);
		assertEquals(1, indexes.size());
		IndexDefinition index = indexes.get(0);
		assertEquals("ix_one", index.getName());
		String source = reader.getIndexSource(tbl, index).toString();
		assertTrue(source.contains("INCLUDE (id2)"));

		List<IndexDefinition> indexList = reader.getIndexes(null, "dbo");
		assertNotNull(indexList);
		assertEquals(1, indexList.size());
		assertEquals("ix_one", indexList.get(0).getName());
	}
}