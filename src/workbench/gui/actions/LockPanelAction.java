/*
 * LockPanelAction.java
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
package workbench.gui.actions;

import java.awt.event.ActionEvent;
import java.util.Optional;

import workbench.interfaces.MainPanel;

/**
 *
 * @author Thomas Kellerer
 */
public class LockPanelAction
  extends CheckBoxAction
{
  private Optional<MainPanel> client;

  public LockPanelAction(Optional<MainPanel> panel)
  {
    super("MnuTxtLockPanel");
    client = panel;
  }

  @Override
  public void executeAction(ActionEvent e)
  {
    super.executeAction(e);
    client.ifPresent(p -> p.setLocked(this.isSwitchedOn()));
  }

  @Override
  public boolean useInToolbar()
  {
    return false;
  }
}
