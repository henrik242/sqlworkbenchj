/*
 * GenericObjectDropper.java
 *
 * This file is part of SQL Workbench/J, https://www.sql-workbench.eu
 *
 * Copyright 2002-2019, Thomas Kellerer
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
package workbench.db;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import workbench.interfaces.ObjectDropper;
import workbench.log.LogMgr;

import workbench.db.sqltemplates.TemplateHandler;

import workbench.storage.RowActionMonitor;

import workbench.util.ObjectUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A helper class to drop different types of objects.
 * To drop table columns, {@link ColumnDropper} should be used.
 *
 * @author  Thomas Kellerer
 */
public class GenericObjectDropper
  implements ObjectDropper
{
  private List<? extends DbObject> objects;
  private WbConnection connection;
  private Statement currentStatement;
  private boolean cascadeConstraints;
  private TableIdentifier objectTable;
  private RowActionMonitor monitor;
  private boolean cancel;
  private boolean transactional = true;

  public GenericObjectDropper()
  {
  }

  public void setUseTransaction(boolean flag)
  {
    transactional = flag;
  }

  @Override
  public List<? extends DbObject> getObjects()
  {
    return objects;
  }

  @Override
  public void setRowActionMonitor(RowActionMonitor mon)
  {
    this.monitor = mon;
  }

  @Override
  public boolean supportsFKSorting()
  {
    if (objects == null) return false;

    int numTypes = this.objects.size();
    for (int i=0; i < numTypes; i++)
    {
      DbObject obj = this.objects.get(i);
      if (!(obj instanceof TableIdentifier))
      {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean supportsCascade()
  {
    boolean canCascade = false;

    if (objects != null && this.connection != null)
    {
      int numTypes = this.objects.size();
      for (int i=0; i < numTypes; i++)
      {
        String type = this.objects.get(i).getObjectType();
        String verb = this.connection.getDbSettings().getCascadeConstraintsVerb(type);

        // if at least one type can be dropped with CASCADE, enable the checkbox
        if (StringUtil.isNonBlank(verb))
        {
          canCascade = true;
          break;
        }
      }
    }
    return canCascade;
  }

  @Override
  public void setObjects(List<? extends DbObject> toDrop)
  {
    if (toDrop == null)
    {
      objects = null;
    }
    else
    {
      objects = new ArrayList<>(toDrop);
    }
  }

  @Override
  public void setObjectTable(TableIdentifier tbl)
  {
    this.objectTable = tbl;
  }

  @Override
  public WbConnection getConnection()
  {
    return this.connection;
  }

  @Override
  public void setConnection(WbConnection aConn)
  {
    this.connection = aConn;
  }

  @Override
  public CharSequence getScript()
  {
    if (this.connection == null) throw new NullPointerException("No connection!");
    if (this.objects == null || this.objects.isEmpty()) return null;

    boolean needCommit = transactional && this.connection.generateCommitForDDL();
    int count = this.objects.size();
    StringBuffer result = new StringBuffer(count * 40);
    for (int i=0; i < count; i++)
    {
      CharSequence sql = getDropStatement(i);
      result.append(sql);
      result.append("\n\n");
    }
    if (needCommit) result.append("COMMIT;\n");
    return result;
  }

  private CharSequence getDropStatement(int index)
  {
    DbObject toDrop = this.objects.get(index);
    return getDropForObject(toDrop, cascadeConstraints);
  }

  @Override
  public CharSequence getDropForObject(DbObject toDrop)
  {
    return getDropForObject(toDrop, cascadeConstraints);
  }

  @Override
  public CharSequence getDropForObject(DbObject toDrop, boolean cascade)
  {
    String drop = toDrop.getDropStatement(connection, cascade);
    if (drop != null) return drop;

    String type = toDrop.getObjectType();

    StringBuilder sql = new StringBuilder(120);
    String ddl = this.connection.getDbSettings().getDropDDL(type, cascade);

    DbObject table = ObjectUtil.coalesce(objectTable, toDrop.getOwnerObject());

    ddl = TemplateHandler.replaceTablePlaceholder(ddl, table, connection, true);

    if (ddl.contains(MetaDataSqlManager.NAME_PLACEHOLDER))
    {
      ddl = ddl.replace(MetaDataSqlManager.NAME_PLACEHOLDER, toDrop.getObjectNameForDrop(this.connection));
    }

    if (ddl.contains(MetaDataSqlManager.FQ_NAME_PLACEHOLDER))
    {
      ddl = ddl.replace(MetaDataSqlManager.FQ_NAME_PLACEHOLDER, toDrop.getFullyQualifiedName(connection));
    }
    
    ddl = TemplateHandler.replaceTablePlaceholder(ddl, toDrop, connection, true);

    sql.append(ddl);

    if (!StringUtil.endsWith(sql, ';'))
    {
      sql.append(';');
    }
    return sql;
  }

  @Override
  public void dropObjects()
    throws SQLException
  {
    if (this.connection == null) throw new NullPointerException("No connection!");
    if (this.objects == null || this.objects.isEmpty()) return;

    cancel = false;
    try
    {
      int count = this.objects.size();

      currentStatement = this.connection.createStatement();

      for (int i=0; i < count; i++)
      {
        DbObject object = objects.get(i);

        String sql = SqlUtil.trimSemicolon(getDropStatement(i).toString());
        LogMgr.logDebug("GenericObjectDropper.execute()", "Dropping object using: " + sql);
        if (monitor != null)
        {
          String name = object.getObjectName();
          monitor.setCurrentObject(name, i + 1, count);
        }

        currentStatement.execute(sql);
        connection.getObjectCache().removeEntry(object);

        if (this.cancel) break;
      }

      if (connection.shouldCommitDDL())
      {
        this.connection.commit();
      }
    }
    catch (SQLException e)
    {
      if (connection.shouldCommitDDL())
      {
        this.connection.rollbackSilently();
      }
      throw e;
    }
    finally
    {
      SqlUtil.closeStatement(currentStatement);
      currentStatement = null;
    }
  }

  @Override
  public void cancel()
    throws SQLException
  {
    if (this.currentStatement == null) return;
    cancel = true;
    try
    {
      this.currentStatement.cancel();
    }
    finally
    {
      if (this.connection.shouldCommitDDL())
      {
        this.connection.rollbackSilently();
      }
    }
  }

  @Override
  public void setCascade(boolean flag)
  {
    if (this.supportsCascade())
    {
      this.cascadeConstraints = flag;
    }
  }

  @Override
  public boolean supportsObject(DbObject object)
  {
    return !(object instanceof ColumnIdentifier);
  }

}
