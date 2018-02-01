/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2018, Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import workbench.log.LogMgr;

import workbench.util.WbFile;
import workbench.util.WbPersistence;

/**
 *
 * @author Thomas Kellerer
 */
public class XmlProfileStorage
  implements ProfileStorage
{
  public static final String DEFAULT_FILE_NAME = "WbProfiles.xml";

  @Override
  public List<ConnectionProfile> readProfiles(WbFile storage)
  {
    Object result = null;
    try
    {
      LogMgr.logInfo("XmlProfileStorage.readProfiles()", "Loading connection profiles from " + storage);
      WbPersistence reader = new WbPersistence(storage.getFullPath());
      result = reader.readObject();
    }
    catch (Exception e)
    {
      LogMgr.logError("XmlProfileStorage.readProfiles()", "Error when reading connection profiles from " + storage, e);
      result = null;
    }

    List<ConnectionProfile> profiles = new ArrayList<>();

    if (result instanceof Collection)
    {
      int noProfileCount = 0;

      Collection loaded = (Collection)result;
      for (Object item : loaded)
      {
        if (item instanceof ConnectionProfile)
        {
          profiles.add((ConnectionProfile)item);
        }
        else
        {
          noProfileCount ++;
        }
      }

      if (noProfileCount == loaded.size())
      {
        LogMgr.logDebug("XmlProfileStorage.readProfiles()", "No connection profiles found in " + storage);

        profiles = null;
      }
    }
    else if (result instanceof Object[])
    {
      // This is to support the very first version of the profile storage
      // probably obsolete by know, but you never know...
      Object[] l = (Object[])result;
      for (Object prof : l)
      {
        profiles.add((ConnectionProfile)prof);
      }
    }
    else
    {
      LogMgr.logDebug("XmlProfileStorage.readProfiles()", "Input file " + storage + " is not a profile storage XML");
      profiles = null;
    }
    return profiles;
  }

  @Override
  public void saveProfiles(List<ConnectionProfile> profiles, WbFile storage)
  {
    WbPersistence writer = new WbPersistence(storage.getFullPath());
    try
    {
      writer.writeObject(profiles);
    }
    catch (IOException e)
    {
      LogMgr.logError("XmlProfileStorage.saveProfiles()", "Error saving profiles to: " + storage, e);
    }
  }

}
