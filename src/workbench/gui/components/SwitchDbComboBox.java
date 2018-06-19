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
package workbench.gui.components;

import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

import workbench.interfaces.MainPanel;
import workbench.log.CallerInfo;
import workbench.log.LogMgr;

import workbench.db.ConnectionProfile;
import workbench.db.DbSwitcher;
import workbench.db.WbConnection;

import workbench.gui.MainWindow;
import workbench.gui.WbSwingUtilities;

import workbench.util.CollectionUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class SwitchDbComboBox
  extends JComboBox<String>
  implements ItemListener
{

  private boolean ignoreItemChange = false;
  private final DbSwitcher switcher;

  public SwitchDbComboBox(WbConnection conn)
  {
    switcher = DbSwitcher.Factory.createDatabaseSwitcher(conn);
    this.retrieve(conn);
    this.addItemListener(this);
  }

  public void retrieve(WbConnection conn)
  {
    if (switcher == null) return;
    int width = WbSwingUtilities.calculateCharWidth(this, 20);
    Dimension d = getPreferredSize();
    d.setSize(width, d.height);
    this.setMaximumSize(d);

    List<String> dbs = switcher.getAvailableDatabases(conn);
    this.setModel(new DefaultComboBoxModel<>(dbs.toArray(new String[0])));
    selectCurrentDatabase(conn);
  }

  public void selectCurrentDatabase(WbConnection conn)
  {
    try
    {
      ignoreItemChange = true;
      String current = switcher.getCurrentDatabase(conn);
      if (current != null)
      {
        this.setSelectedItem(current);
      }
    }
    finally
    {
      ignoreItemChange = false;
    }
  }

  public void clear()
  {
    this.setModel(new DefaultComboBoxModel<>());
  }

  private boolean isConnectInProgress()
  {
    MainWindow window = WbSwingUtilities.getMainWindow(this);
    if (window == null) return false;

    return window.isConnectInProgress();
  }

  @Override
  public void itemStateChanged(ItemEvent e)
  {
    if (ignoreItemChange) return;
    if (isConnectInProgress()) return;

    if (e == null) return;

    if (e.getSource() == this && e.getStateChange() == ItemEvent.SELECTED)
    {
      changeDatabase();
    }
  }

  public String getSelectedDatabase()
  {
    return (String)getSelectedItem();
  }

  private void changeDatabase()
  {
    MainWindow window = WbSwingUtilities.getMainWindow(this);
    if (window == null) return;

    if (window.isBusy()) return;
    if (window.getCurrentProfile() == null) return;

    ConnectionProfile profile = window.getCurrentProfile();
    if (profile == null) return;

    String dbName = getSelectedDatabase();
    if (dbName == null) return;

    if (switcher.needsReconnect())
    {
      String newUrl = switcher.getUrlForDatabase(profile.getUrl(), dbName);
      profile.switchToTemporaryUrl(newUrl);
      try
      {
        ignoreItemChange = true;
        window.connectTo(profile, false, false);
      }
      finally
      {
        ignoreItemChange = false;
      }
    }
    else
    {
      switchDB(window, dbName);
    }
  }

  private void switchDB(MainWindow window, String dbName)
  {
    final CallerInfo ci = new CallerInfo(){};
    Set<String> changedConnections = CollectionUtil.caseInsensitiveSet();
    int tabCount = window.getTabCount();
    for (int i=0; i < tabCount; i++)
    {
      Optional<MainPanel> panel = window.getSqlPanel(i);
      if (panel.isPresent())
      {
        MainPanel p = panel.get();
        WbConnection connection = p.getConnection();
        if (connection != null)
        {
          if (!changedConnections.contains(connection.getId()))
          {
            LogMgr.logDebug(ci, "Switching database for panel " + p.getTabTitle() + " (" + p.getId() + ")");
            try
            {
              switcher.switchDatabase(connection, dbName);
              changedConnections.add(connection.getId());
            }
            catch (SQLException ex)
            {
              LogMgr.logError(ci, "Could not switch database for panel " + p.getTabTitle() + " (" + p.getId() + ")", ex);
            }
          }
        }
      }

    }
  }

}
