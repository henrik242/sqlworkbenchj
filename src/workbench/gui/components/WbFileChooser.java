/*
 * WbFileChooser.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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
package workbench.gui.components;

import java.awt.Component;
import java.awt.HeadlessException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileSystemView;

import workbench.interfaces.ValidatingComponent;
import workbench.resource.GuiSettings;
import workbench.resource.Settings;

/**
 *
 * @author Thomas Kellerer
 */
public class WbFileChooser
		extends JFileChooser
		implements PropertyChangeListener
{
	private String windowSettingsId;
	private JDialog dialog;

	public WbFileChooser()
	{
		super();
		init();
	}

	public WbFileChooser(File currentDirectoryPath)
	{
		super(currentDirectoryPath);
		init();
	}

	public WbFileChooser(File currentDirectoryPath, FileSystemView fsv)
	{
		super(currentDirectoryPath, fsv);
		init();
	}

	public WbFileChooser(String currentDirectoryPath)
	{
		super(currentDirectoryPath);
		init();
	}

	private void init()
	{
		addPropertyChangeListener("JFileChooserDialogIsClosingProperty", this);
		putClientProperty("FileChooser.useShellFolder", GuiSettings.getUseShellFolders());
	}

	public void setSettingsID(String id)
	{
		this.windowSettingsId = id;
	}

	public JDialog getCurrentDialog()
	{
		return dialog;
	}

	@Override
	public JDialog createDialog(Component parent)
		throws HeadlessException
	{
		this.dialog = super.createDialog(parent);
		if (windowSettingsId != null)
		{
			Settings.getInstance().restoreWindowSize(dialog, windowSettingsId);
		}
		return dialog;
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		if (evt.getPropertyName().equals("JFileChooserDialogIsClosingProperty") && windowSettingsId != null)
		{
			try
			{
				JDialog d = (JDialog)evt.getOldValue();
				Settings.getInstance().storeWindowSize(d, windowSettingsId);
			}
			catch (Throwable th)
			{
				// ignore
			}
		}
	}

	public boolean validateInput()
	{
		JComponent accessory = getAccessory();
		if (accessory instanceof ValidatingComponent)
		{
			ValidatingComponent vc = (ValidatingComponent)accessory;
			return vc.validateInput();
		}
		return true;
	}

	@Override
	public void approveSelection()
	{
		if (validateInput())
		{
			super.approveSelection();
		}
	}

}
