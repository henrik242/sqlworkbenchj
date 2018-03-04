/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package workbench.log;

/**
 *
 * @author Thomas Kellerer
 */
public abstract class CallerInfo
{
  private final String info;

  public CallerInfo()
  {
    String baseInfo = getClass().getEnclosingClass().getSimpleName() + "." + getClass().getEnclosingMethod().getName();
    if (baseInfo.charAt(0) == '<')
    {
      info = baseInfo;
    }
    else
    {
      info = baseInfo + "()";
    }
  }

  @Override
  public String toString()
  {
    return info;
  }

}
