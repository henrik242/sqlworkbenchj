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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.util.StringUtil;
import workbench.util.WbFile;
import workbench.util.WbProperties;

import static workbench.db.IniProfileStorage.*;


/**
 *
 * @author Thomas Kellerer
 */
public class SshConfigMgr
{
  private static final String PREFIX = "config";
  private static final String CONFIG_NAME = ".name";

  private final List<SshHostConfig> globalConfigs = new ArrayList<>();
  private boolean loaded = false;

  private static class InstanceHolder
  {
    protected static final SshConfigMgr INSTANCE = new SshConfigMgr();
  }

  public static final SshConfigMgr getInstance()
  {
    return InstanceHolder.INSTANCE;
  }

  private SshConfigMgr()
  {
  }

  public List<SshHostConfig> getGlobalConfigs()
  {
    ensureLoaded();
    return Collections.unmodifiableList(globalConfigs);
  }

  public void saveGlobalConfig()
  {
    WbProperties props = new WbProperties(0);
    synchronized (PREFIX)
    {
      for (int i=0; i < globalConfigs.size(); i++)
      {
        String key = StringUtil.formatInt(i + 1, 4).toString();
        writeConfig(globalConfigs.get(i), props, key);
      }
    }

    WbFile file = Settings.getInstance().getGlogalSshConfigFile();
    try
    {
      props.saveToFile(file);
      LogMgr.logInfo("SshConfigMgr.saveGlobalConfig()", "Global SSH host configurations saved to: " + file.getFullPath());
    }
    catch (Exception ex)
    {
      LogMgr.logError("SshConfigMgr.saveGlobalConfig()", "Could not save global SSH host configurations", ex);
    }
  }

  public SshHostConfig getHostConfig(String configName)
  {
    if (StringUtil.isBlank(configName)) return null;

    ensureLoaded();
    for (SshHostConfig config : globalConfigs)
    {
      if (StringUtil.equalStringIgnoreCase(configName, config.getConfigName()))
      {
        return config;
      }
    }
    return null;
  }

  private void ensureLoaded()
  {
    synchronized (PREFIX)
    {
      if (!loaded)
      {
        loadConfigs();
      }
    }
  }

  private void writeConfig(SshHostConfig config, WbProperties props, String key)
  {
    props.setProperty(PREFIX + key + PROP_SSH_HOST, config.getHostname());
    props.setProperty(PREFIX + key + PROP_SSH_USER, config.getUsername());
    props.setProperty(PREFIX + key + PROP_SSH_KEYFILE, config.getPrivateKeyFile());
    props.getProperty(PREFIX + key + PROP_SSH_PWD, config.getPassword());
    props.getProperty(PREFIX + key + CONFIG_NAME, config.getConfigName());
    props.setProperty(PREFIX + key + PROP_SSH_TRY_AGENT, config.getTryAgent());
  }

  private SshHostConfig readConfig(WbProperties props, String key)
  {
    String hostName = props.getProperty(PREFIX + key + PROP_SSH_HOST, null);
    String user = props.getProperty(PREFIX + key + PROP_SSH_USER, null);
    String keyFile = props.getProperty(PREFIX + key + PROP_SSH_KEYFILE, null);
    String pwd = props.getProperty(PREFIX + key + PROP_SSH_PWD, null);
    String name = props.getProperty(PREFIX + key + CONFIG_NAME, null);
    boolean tryAgent = props.getBoolProperty(PREFIX + key + PROP_SSH_TRY_AGENT, false);
    if (name != null && hostName != null && user != null)
    {
      SshHostConfig config = new SshHostConfig(name);
      config.setPassword(pwd);
      config.setHostname(hostName);
      config.setUsername(user);
      config.setPrivateKeyFile(keyFile);
      config.setTryAgent(tryAgent);
      return config;
    }
    return null;
  }

  private void loadConfigs()
  {
    WbFile file = Settings.getInstance().getGlogalSshConfigFile();
    if (!file.exists()) return;

    globalConfigs.clear();
    try
    {
      WbProperties props = new WbProperties(0);
      props.loadTextFile(file);
      List<String> keys = props.getKeysWithPrefix(PREFIX);
      for (String key : keys)
      {
        SshHostConfig config = readConfig(props, key);
        if (config != null)
        {
          globalConfigs.add(config);
        }
      }
      Collections.sort(globalConfigs, (SshHostConfig o1, SshHostConfig o2) -> StringUtil.compareStrings(o1.getConfigName(), o2.getConfigName(), true));

      loaded = true;
      LogMgr.logInfo("SshConfigMgr.loadConfigs()", "Loaded global SSH host configurations from " + file.getFullPath());
    }
    catch (Exception ex)
    {
      LogMgr.logWarning("SshConfigMgr.loadConfigs()", "Could not load global SSH host configurations", ex);
      loaded = false;
    }
  }

}
