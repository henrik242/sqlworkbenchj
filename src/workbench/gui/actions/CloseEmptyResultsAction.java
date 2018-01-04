/*
 * CloseOtherResultsAction.java
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
package workbench.gui.actions;

import java.awt.event.ActionEvent;

import workbench.gui.sql.DwPanel;
import workbench.gui.sql.ResultCloseFilter;
import workbench.gui.sql.SqlPanel;

/**
 *
 * @author Thomas Kellerer
 */
public class CloseEmptyResultsAction
  extends WbAction
{
  private SqlPanel client;

  public CloseEmptyResultsAction(SqlPanel panel)
  {
    client = panel;
    initMenuDefinition("MnuTxtCloseEmptyResults");
    this.setEnabled(client.getResultTabCount() > 0);
  }

  @Override
  public void executeAction(ActionEvent e)
  {
    ResultCloseFilter filter = (DwPanel panel, int panelIndex) -> panel.getDataStore().getRowCount() == 0;
    client.closeSelectedResults(filter);
  }

}
