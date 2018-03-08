/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package workbench.log;

import java.lang.reflect.Method;

/**
 *
 * @author Thomas Kellerer
 */
public abstract class CallerInfo
{
  private String info;

  public CallerInfo()
  {
  }

  @Override
  public String toString()
  {
    // by lazily initializing the info string, this is only
    // done when toString() is actually called
    if (info == null)
    {
      generateInfo();
    }
    return info;
  }

  private void generateInfo()
  {
    String baseInfo = getClass().getEnclosingClass().getSimpleName() + ".";
    Method m = getClass().getEnclosingMethod();
    if (m == null)
    {
      info = baseInfo + "<init>";
    }
    else
    {
      info = m.getName() + "()";
    }
  }
}
