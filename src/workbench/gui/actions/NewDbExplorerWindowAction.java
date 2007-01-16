/*
 * NewDbExplorerWindowAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.actions;

import java.awt.event.ActionEvent;


import workbench.gui.MainWindow;

/**
 *	@author  support@sql-workbench.net
 */
public class NewDbExplorerWindowAction
	extends WbAction
{
	private MainWindow mainWin;
	public NewDbExplorerWindowAction(MainWindow aWindow)
	{
		super();
		mainWin = aWindow;
		this.initMenuDefinition("MnuTxtNewExplorerWindow");
	}

	public void executeAction(ActionEvent e)
	{
		mainWin.newDbExplorerWindow();
	}
}
