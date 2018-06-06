/*
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
package workbench.db.postgres;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Savepoint;

import workbench.log.CallerInfo;
import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class PostgresPolicyReader
{

  public String getTablePolicies(WbConnection conn, TableIdentifier table)
  {
    String query =
      "select polname, \n" +
      "       pg_get_expr(polqual, polrelid, true) as expression, \n" +
      "       case polcmd \n" +
      "         when 'r' then 'SELECT' \n" +
      "         when 'a' then 'INSERT' \n" +
      "         when 'w' then 'UPDATE' \n" +
      "         when 'd' then 'DELETE' \n" +
      "         else 'ALL' \n" +
      "       end as command, \n" +
      "       polpermissive, \n" +
      "       (select string_agg(quote_ident(rolname), ',') from pg_roles r where r.oid = any(p.polroles)) as roles, \n" +
      "       pg_get_expr(polwithcheck, polrelid, true) as with_check \n" +
      "from pg_policy p \n" +
      "where p.polrelid = cast(? as regclass)";

    String tname = table.getFullyQualifiedName(conn);

    PreparedStatement pstmt = null;
    ResultSet rs = null;
    Savepoint sp = null;

    final CallerInfo ci = new CallerInfo(){};

    if (Settings.getInstance().getDebugMetadataSql())
    {
      LogMgr.logDebug(ci, "Retrieving table policies using:\n" + SqlUtil.replaceParameters(query, tname));
    }

    StringBuilder policies = new StringBuilder(100);
    String rlsConfig = table.getSourceOptions().getConfigSettings().get("RLS");
    boolean rlsEnabled = "enable".equals(rlsConfig) || "force".equals(rlsConfig);
    boolean forceRls = "force".equals(rlsConfig);

    try
    {
      sp = conn.setSavepoint();
      pstmt = conn.getSqlConnection().prepareStatement(query);
      pstmt.setString(1, tname);
      rs = pstmt.executeQuery();

      while (rs.next())
      {
        String name = rs.getString("polname");
        String expr = rs.getString("expression");
        String command = rs.getString("command");
        boolean permissive = rs.getBoolean("polpermissive");
        String withCheck = rs.getString("with_check");
        String roles = rs.getString("roles");

        String policy = "CREATE POLICY " + SqlUtil.quoteObjectname(name) + " ON " + tname + "\n" +
          "  AS " + (permissive ? "PERMISSIVE" : "RESTRICTIVE") + "\n"+
          "  FOR " + command;

        if (StringUtil.isNonBlank(roles))
        {
          policy += "\n  TO " + roles;
        }
        if (StringUtil.isNonBlank(expr))
        {
          policy += "\n  USING (" + expr + ")";
        }

        if (StringUtil.isNonBlank(withCheck))
        {
          policy += "\n  WITH CHECK (" + withCheck + ")";
        }
        policy += ";\n";

        if (policies.length() >  0)
        {
          policies.append('\n');
        }
        policies.append(policy);
      }
      conn.releaseSavepoint(sp);
    }
    catch (Exception ex)
    {
      conn.rollback(sp);
      LogMgr.logError(ci, "Error retrieving table policies using:\n" + SqlUtil.replaceParameters(query, tname), ex);
    }
    finally
    {
      SqlUtil.close(rs, pstmt);
    }

    String rlsOption = null;
    if (policies.length() > 0 && !rlsEnabled)
    {
      rlsOption = " DISABLE";
    }
    else if (forceRls)
    {
      rlsOption = " FORCE";
    }
    else if (rlsEnabled)
    {
      rlsOption = " ENABLE";
    }

    if (rlsOption != null)
    {
      policies.insert(0, "ALTER TABLE " + tname + rlsOption + " ROW LEVEL SECURITY;\n\n");
    }

    return policies.toString();
  }
}
