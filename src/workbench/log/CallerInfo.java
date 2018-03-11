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
    if (info == null)
    {
      generateInfo();
    }
    return info;
  }

  private void generateInfo()
  {
    try
    {
      info = getClass().getEnclosingClass().getSimpleName() + ".";
      Method m = getClass().getEnclosingMethod();
      if (m == null)
      {
        info += "<init>";
      }
      else
      {
        info += m.getName() + "()";
      }
    }
    catch (Throwable th)
    {
      // ignore
    }
  }
}
