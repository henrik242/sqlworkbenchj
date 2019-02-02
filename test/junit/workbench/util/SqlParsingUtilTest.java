/*
 * This file is part of SQL Workbench/J, https://www.sql-workbench.eu
 *
 * Copyright 2002-2019, Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0
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
package workbench.util;

import workbench.sql.parser.ParserType;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class SqlParsingUtilTest
{

	public SqlParsingUtilTest()
	{
	}

	@Test
	public void testGetSqlVerb()
	{
    for (ParserType type : ParserType.values())
    {
      SqlParsingUtil util = new SqlParsingUtil(type);
      String result = util.getSqlVerb("-- foobar\nselect 42");
      assertEquals("SELECT", result);
    }
	}

	@Test
	public void testStripVerb()
	{
    for (ParserType type : ParserType.values())
    {
      String sql = "-- foobar\nselect 42";
      SqlParsingUtil util = new SqlParsingUtil(type);
      assertEquals("42", util.stripVerb(sql));

      sql = "-- foobar\n /* bla */ select 42";
      assertEquals("42", util.stripVerb(sql));
    }
	}

	@Test
	public void testGetPos()
	{
    for (ParserType type : ParserType.values())
    {
      String sql = "select /* from */ some_column from some_table join other_table using (x)";
      SqlParsingUtil util = new SqlParsingUtil(type);
      int pos = util.getKeywordPosition("FROM", sql);
      assertEquals(30, pos);
      String fromPart = util.getFromPart(sql);
      assertEquals(" some_table join other_table using (x)", fromPart);

      sql = "select /* from */ \n" +
        "some_column, \n" +
        " -- from blabla \n" +
        " from some_table join other_table using (x)";
      fromPart = util.getFromPart(sql);
      assertEquals(" some_table join other_table using (x)", fromPart);
    }
	}

  @Test
  public void testStripComments()
  {
    for (ParserType type : ParserType.values())
    {
      SqlParsingUtil util = new SqlParsingUtil(type);
      String clean = util.stripStartingComment("-- @WbResult\nmacro name");
      assertEquals("macro name", clean);

      clean = util.stripStartingComment("/* this \n is \n a comment */\n-- and another comment\nselect 42");
      assertEquals("select 42", clean);

    }

  }
}
