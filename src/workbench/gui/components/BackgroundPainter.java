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

import java.awt.Color;
import java.awt.Graphics2D;

import javax.swing.JComponent;
import javax.swing.Painter;

/**
 *
 * @author Thomas Kellerer
 */
public class BackgroundPainter
  implements Painter<JComponent>
{
  private final Color color;

  public BackgroundPainter(Color c)
  {
    this.color = c;
  }

  @Override
  public void paint(Graphics2D g, JComponent object, int width, int height)
  {
    if (color != null)
    {
      g.setColor(color);
      g.fillRect(0, height - 4, width - 1, 4);
    }
  }

}
