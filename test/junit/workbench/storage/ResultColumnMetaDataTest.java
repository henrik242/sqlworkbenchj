/*
 * ResultColumnMetaDataTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2018, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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

import java.sql.ResultSet;
import java.sql.Statement;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.WbConnection;

import org.junit.After;
import org.junit.Before;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 *
 * @author Thomas Kellerer
 */
public class ResultColumnMetaDataTest
  extends WbTestCase
{

  private WbConnection connection;

  public ResultColumnMetaDataTest()
  {
    super("ResultColumnMetaDataTest");
  }

  @Before
  public void setup()
    throws Exception
  {
    TestUtil util = getTestUtil();
    connection = util.getConnection();
  }

  @After
  public void tearDown()
  {
    connection.disconnect();
  }

  @Test
  public void testSimpleQuery()
    throws Exception
  {
    TestUtil.executeScript(connection,
      "CREATE TABLE PERSON (id integer primary key, first_name varchar(50), last_name varchar(50));\n" +
      "comment on column person.id is 'Primary key';\n" +
      "comment on column person.first_name is 'The first name';" +
      "comment on column person.last_name is 'The last name';\n" +
      "commit;\n");

    String sql = "select id, first_name, last_name from person";
    try (Statement stmt = connection.createStatement();
         ResultSet rs = stmt.executeQuery(sql);)
    {
      ResultInfo info = new ResultInfo(rs.getMetaData(), connection);
      ResultColumnMetaData meta = new ResultColumnMetaData(sql, connection);
      meta.retrieveColumnRemarks(info);
      assertEquals("Primary key", info.getColumn(0).getComment());
      assertEquals("The first name", info.getColumn(1).getComment());
      assertEquals("The last name", info.getColumn(2).getComment());
    }

    TestUtil.executeScript(connection,
      "create table address (person_id integer not null, address_info varchar(500));\n" +
      "comment on column address.person_id is 'The person ID';\n" +
      "comment on column address.address_info is 'The address';\n" +
      "commit;\n");

    sql =
      "select p.id as person_id, a.person_id as address_pid, p.first_name, p.last_name, a.address_info \n" +
      "from person p \n" +
      "   join address a on p.id = a.person_id";

    try (Statement stmt = connection.createStatement();
         ResultSet rs = stmt.executeQuery(sql);)
    {
      ResultInfo info = new ResultInfo(rs.getMetaData(), connection);
      ResultColumnMetaData meta = new ResultColumnMetaData(sql, connection);
      meta.retrieveColumnRemarks(info);
      assertEquals("Primary key", info.getColumn(0).getComment());
      assertEquals("The person ID", info.getColumn(1).getComment());
      assertEquals("The first name", info.getColumn(2).getComment());
      assertEquals("The last name", info.getColumn(3).getComment());
      assertEquals("The address", info.getColumn(4).getComment());
    }

    sql = "select * from person";

    try (Statement stmt = connection.createStatement();
         ResultSet rs = stmt.executeQuery(sql);)
    {
      ResultInfo info = new ResultInfo(rs.getMetaData(), connection);
      ResultColumnMetaData meta = new ResultColumnMetaData(sql, connection);
      meta.retrieveColumnRemarks(info);
      assertEquals("Primary key", info.getColumn(0).getComment());
      assertEquals("The first name", info.getColumn(1).getComment());
      assertEquals("The last name", info.getColumn(2).getComment());
    }

    sql = "select * from person as p";
    try (Statement stmt = connection.createStatement();
         ResultSet rs = stmt.executeQuery(sql);)
    {
      ResultInfo info = new ResultInfo(rs.getMetaData(), connection);
      ResultColumnMetaData meta = new ResultColumnMetaData(sql, connection);
      meta.retrieveColumnRemarks(info);
      assertEquals("Primary key", info.getColumn(0).getComment());
      assertEquals("The first name", info.getColumn(1).getComment());
      assertEquals("The last name", info.getColumn(2).getComment());
    }

    sql = "select id, first_name from person p limit 1";
    try (Statement stmt = connection.createStatement();
         ResultSet rs = stmt.executeQuery(sql);)
    {
      ResultInfo info = new ResultInfo(rs.getMetaData(), connection);
      ResultColumnMetaData meta = new ResultColumnMetaData(sql, connection);
      meta.retrieveColumnRemarks(info);
      assertEquals("Primary key", info.getColumn(0).getComment());
      assertEquals("The first name", info.getColumn(1).getComment());
    }

    sql = "select p.*, a.* " +
      " from person p join address a on p.id = a.person_id";
    try (Statement stmt = connection.createStatement();
         ResultSet rs = stmt.executeQuery(sql);)
    {
      ResultInfo info = new ResultInfo(rs.getMetaData(), connection);
      ResultColumnMetaData meta = new ResultColumnMetaData(sql, connection);
      meta.retrieveColumnRemarks(info);
      assertEquals(5, info.getColumnCount());
      assertEquals("Primary key", info.getColumn(0).getComment());
      assertEquals("The first name", info.getColumn(1).getComment());
      assertEquals("The last name", info.getColumn(2).getComment());
      assertEquals("The person ID", info.getColumn(3).getComment());
      assertEquals("The address", info.getColumn(4).getComment());
    }

    sql = "select * " +
      " from person p join address a on p.id = a.person_id";

    try (Statement stmt = connection.createStatement();
         ResultSet rs = stmt.executeQuery(sql);)
    {
      ResultInfo info = new ResultInfo(rs.getMetaData(), connection);
      ResultColumnMetaData meta = new ResultColumnMetaData(sql, connection);
      meta.retrieveColumnRemarks(info);
      assertEquals(5, info.getColumnCount());
      assertEquals("Primary key", info.getColumn(0).getComment());
      assertEquals("The first name", info.getColumn(1).getComment());
      assertEquals("The last name", info.getColumn(2).getComment());
      assertEquals("The person ID", info.getColumn(3).getComment());
      assertEquals("The address", info.getColumn(4).getComment());
    }
  }
}
