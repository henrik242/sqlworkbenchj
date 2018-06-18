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

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

import workbench.db.ConnectionProfile;
import workbench.db.WbConnection;
import workbench.db.postgres.PostgresUtil;

import workbench.gui.MainWindow;
import workbench.gui.WbSwingUtilities;

/**
 *
 * @author Thomas Kellerer
 */
public class DbSwitcher
  extends JComboBox<String>
  implements ItemListener
{

  public DbSwitcher(WbConnection conn)
  {
    this.retrieve(conn);
    this.addItemListener(this);
  }

  public void retrieve(WbConnection conn)
  {
    List<String> dbs = PostgresUtil.getAccessibleDatabases(conn);
    String current = PostgresUtil.getCurrentDatabase(conn);
    this.setModel(new DefaultComboBoxModel<>(dbs.toArray(new String[0])));
    if (current != null)
    {
      this.setSelectedItem(current);
    }
  }

  public void clear()
  {
    this.setModel(new DefaultComboBoxModel<>());
  }

  @Override
  public void itemStateChanged(ItemEvent e)
  {
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
    if (window.getCurrentProfile() == null) return;

    ConnectionProfile profile = window.getCurrentProfile();
    if (profile == null) return;

    String newUrl = PostgresUtil.switchDatabaseURL(profile.getUrl(), getSelectedDatabase());
    profile.switchToTemporaryUrl(newUrl);
    window.connectTo(profile, false, false);
  }

}
