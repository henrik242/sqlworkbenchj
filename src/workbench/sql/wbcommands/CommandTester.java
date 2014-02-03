/*
 * CommandTester.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
package workbench.sql.wbcommands;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import workbench.sql.wbcommands.console.WbAbout;
import workbench.sql.wbcommands.console.WbDefineMacro;
import workbench.sql.wbcommands.console.WbDeleteProfile;
import workbench.sql.wbcommands.console.WbDisconnect;
import workbench.sql.wbcommands.console.WbDisplay;
import workbench.sql.wbcommands.console.WbListMacros;
import workbench.sql.wbcommands.console.WbListProfiles;
import workbench.sql.wbcommands.console.WbRun;
import workbench.sql.wbcommands.console.WbStoreProfile;

import workbench.util.CaseInsensitiveComparator;
import workbench.util.CollectionUtil;

/**
 * A class to test whether a given SQL Verb is an internal
 * Workbench command. This is used by the SqlFormatter, because
 * the verbs for WbXXXX commands are not formatted in uppercase.
 *
 * This is also used by the code completion to check for WB specific commands.
 *
 * @see workbench.sql.formatter.SqlFormatter
 * @see workbench.gui.completion.StatementContext
 *
 * @author Thomas Kellerer
 */
public class CommandTester
{

	private final Set<String> commands;
	private Map<String, String> formattedWords;

	public CommandTester()
	{
		commands = CollectionUtil.caseInsensitiveSet();
		commands.add(WbCall.VERB);
		commands.add(WbConfirm.VERB);
		commands.add(WbCopy.VERB);
		commands.add(WbDataDiff.VERB);
		commands.add(WbDefinePk.VERB);
		commands.add(WbDefineVar.VERB);
		commands.add(WbDeleteProfile.VERB);
		commands.add(WbDescribeObject.VERB);
		commands.add(WbDescribeObject.VERB_LONG);
		commands.add(WbDisableOraOutput.VERB);
		commands.add(WbDisplay.VERB);
		commands.add(WbEnableOraOutput.VERB);
		commands.add(WbEndBatch.VERB);
		commands.add(WbExport.VERB);
		commands.add(WbFeedback.VERB);
		commands.add(WbImport.VERB);
		commands.add(WbInclude.VERB);
		commands.add(WbListPkDef.VERB);
		commands.add(WbListVars.VERB);
		commands.add(WbListTables.VERB);
		commands.add(WbListProcedures.VERB);
		commands.add(WbListCatalogs.VERB);
		commands.add(WbListCatalogs.VERB_ALTERNATE);
		commands.add(WbListSchemas.VERB);
		commands.add(WbLoadPkMapping.VERB);
		commands.add(WbRemoveVar.VERB);
		commands.add(WbSavePkMapping.VERB);
		commands.add(WbSchemaDiff.VERB);
		commands.add(WbSchemaReport.VERB);
		commands.add(WbSelectBlob.VERB);
		commands.add(WbStartBatch.VERB);
		commands.add(WbXslt.VERB);
		commands.add(WbConnect.VERB);
		commands.add(WbDisconnect.VERB);
		commands.add(WbHideWarnings.VERB);
		commands.add(WbListProfiles.VERB);
		commands.add(WbStoreProfile.VERB);
		commands.add(WbHelp.VERB);
		commands.add(WbProcSource.VERB);
		commands.add(WbRun.VERB);
		commands.add(WbListTriggers.VERB);
		commands.add(WbTriggerSource.VERB);
		commands.add(WbGrepSource.VERB);
		commands.add(WbGrepData.VERB);
		commands.add(WbMode.VERB);
		commands.add(WbFetchSize.VERB);
		commands.add(WbAbout.VERB);
		commands.add(WbRunLB.VERB);
		commands.add(WbIsolationLevel.VERB);
		commands.add(WbConnInfo.VERB);
		commands.add(WbSysExec.VERB);
		commands.add(WbSysProps.VERB);
		commands.add(WbOraShow.VERB);
		commands.add(WbGenDrop.VERB);
		commands.add(WbSetProp.VERB);
		commands.add(WbSetProp.ALTERNATE_VERB);
		commands.add(WbGenerateScript.VERB);
		commands.add(WbSysOpen.VERB);
		commands.add(WbGenDelete.VERB);
		commands.add(WbGenInsert.VERB);
		commands.add(WbEcho.VERB);
		commands.add(WbHistory.VERB);
		commands.add(WbDefineMacro.VERB);
		commands.add(WbListMacros.VERB);
		commands.add(WbListIndexes.VERB);
	}

	private Map<String, String> getFormattedVerbs()
	{
		if (formattedWords == null)
		{
			formattedWords = new TreeMap<String, String>(CaseInsensitiveComparator.INSTANCE);
			formattedWords.put(WbSavePkMapping.VERB, WbSavePkMapping.FORMATTED_VERB);
			formattedWords.put(WbLoadPkMapping.VERB, WbLoadPkMapping.FORMATTED_VERB);
			formattedWords.put(WbDefineVar.VERB, "WbVarDef");
			formattedWords.put(WbListPkDef.VERB, WbListPkDef.FORMATTED_VERB);
			formattedWords.put(WbEndBatch.VERB, "WbEndBatch");
			formattedWords.put(WbStartBatch.VERB, "WbStartBatch");
			formattedWords.put(WbSchemaDiff.VERB, "WbSchemaDiff");
			formattedWords.put(WbSchemaReport.VERB, "WbSchemaReport");
			formattedWords.put(WbDataDiff.VERB, "WbDataDiff");
			formattedWords.put(WbDescribeObject.VERB, WbDescribeObject.VERB.toUpperCase());
			formattedWords.put(WbDescribeObject.VERB_LONG, WbDescribeObject.VERB_LONG.toUpperCase());
			formattedWords.put(WbHideWarnings.VERB, "WbEnableWarnings");
			formattedWords.put(WbStoreProfile.VERB, "WbStoreProfile");
			formattedWords.put(WbDeleteProfile.VERB, "WbDeleteProfile");
			formattedWords.put(WbListProfiles.VERB, "WbListProfiles");
			formattedWords.put(WbDefinePk.VERB, "WbDefinePK");
			formattedWords.put(WbSelectBlob.VERB, "WbSelectBlob");
			formattedWords.put(WbRemoveVar.VERB, "WbVarDelete");
			formattedWords.put(WbProcSource.VERB, "WbProcSource");
			formattedWords.put(WbListProcedures.VERB, "WbListProcs");
			formattedWords.put(WbListTriggers.VERB, WbListTriggers.FORMATTED_VERB);
			formattedWords.put(WbTriggerSource.VERB, WbTriggerSource.FORMATTED_VERB);
			formattedWords.put(WbListCatalogs.VERB, "WbListDB");
			formattedWords.put(WbListCatalogs.VERB_ALTERNATE, "WbListCat");
			formattedWords.put(WbListSchemas.VERB, "WbListSchemas");
			formattedWords.put(WbGrepData.VERB, "WbGrepData");
			formattedWords.put(WbGrepSource.VERB, "WbGrepSource");
			formattedWords.put(WbFetchSize.VERB, "WbFetchSize");
			formattedWords.put(WbRunLB.VERB, "WbRunLB");
			formattedWords.put(WbIsolationLevel.VERB, "WbIsolationLevel");
			formattedWords.put(WbConnInfo.VERB, "WbConnInfo");
			formattedWords.put(WbSysExec.VERB, "WbSysExec");
			formattedWords.put(WbGenDrop.VERB, "WbGenerateDrop");
			formattedWords.put(WbSetProp.VERB, "WbSetProp");
			formattedWords.put(WbSetProp.ALTERNATE_VERB, "WbSetConfig");
			formattedWords.put(WbGenerateScript.VERB, "WbGenerateScript");
			formattedWords.put(WbSysOpen.VERB, "WbSysOpen");
			formattedWords.put(WbGenDelete.VERB, "WbGenerateDelete");
			formattedWords.put(WbGenInsert.VERB, "WbGenerateInsert");
			formattedWords.put(WbDefineMacro.VERB, "WbDefineMacro");
			formattedWords.put(WbListMacros.VERB, "WbListMacros");
			formattedWords.put(WbListIndexes.VERB, "WbListIndexes");
		}
		return formattedWords;
	}

	public Collection<String> getCommands()
	{
		return Collections.unmodifiableSet(commands);
	}

	public boolean isWbCommand(String verb)
	{
		if (verb == null)
		{
			return false;
		}
		return commands.contains(verb.trim());
	}

	public String formatVerb(String verb)
	{
		if (verb == null)
		{
			return null;
		}
		String f = getFormattedVerbs().get(verb.trim());
		if (f != null)
		{
			return f;
		}
		else
		{
			return fixCase(verb);
		}
	}

	private String fixCase(String verb)
	{
		if (!verb.toLowerCase().startsWith("wb"))
		{
			return verb;
		}
		String s = "Wb" + Character.toUpperCase(verb.charAt(2)) + verb.substring(3).toLowerCase();
		return s;
	}
}
