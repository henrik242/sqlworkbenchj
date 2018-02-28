/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2018 Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0 (the "License")
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.ssh;

import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class SshHostConfig
  implements Comparable<SshHostConfig>
{
  private String sshHost;
  private String password;
  private String username;
  private String privateKeyFile;
  private String configName;
  private boolean tryAgent;

  public SshHostConfig(String configName)
  {
    this.configName = configName;
  }

  public boolean getTryAgent()
  {
    return tryAgent;
  }

  public void setTryAgent(boolean flag)
  {
    tryAgent = flag;
  }

  public String getSshHost()
  {
    return sshHost;
  }

  public void setSshHost(String sshHost)
  {
    this.sshHost = sshHost;
  }

  public String getPassword()
  {
    return password;
  }

  public void setPassword(String password)
  {
    this.password = password;
  }

  public String getUsername()
  {
    return username;
  }

  public void setUsername(String username)
  {
    this.username = username;
  }

  public String getPrivateKeyFile()
  {
    return privateKeyFile;
  }

  public void setPrivateKeyFile(String privateKeyFile)
  {
    this.privateKeyFile = privateKeyFile;
  }

  public String getConfigName()
  {
    return configName;
  }

  public void setConfigName(String configName)
  {
    this.configName = configName;
  }


  @Override
  public int compareTo(SshHostConfig o)
  {
    if (o == null) return 1;
    if (this.configName == null) return -1;
    if (o.configName == null) return 1;
    return StringUtil.naturalCompare(this.configName, o.configName, true);
  }

}
