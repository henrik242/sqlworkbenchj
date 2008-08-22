/*
 * WbManager.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.LookAndFeel;
import javax.swing.SwingConstants;
import javax.swing.UIDefaults;
import javax.swing.UIManager;

import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.gui.MainWindow;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.TabbedPaneUIFactory;
import workbench.gui.dbobjects.DbExplorerWindow;
import workbench.gui.lnf.LnFDefinition;
import workbench.gui.lnf.LnFLoader;
import workbench.interfaces.FontChangedListener;
import workbench.interfaces.ToolWindow;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.sql.BatchRunner;
import workbench.sql.MacroManager;
import workbench.sql.VariablePool;
import workbench.util.MacOSHelper;
import workbench.util.StringUtil;
import workbench.gui.dialogs.WbSplash;
import workbench.gui.filter.FilterDefinitionManager;
import workbench.gui.lnf.LnFManager;
import workbench.gui.profiles.ProfileKey;
import workbench.gui.tools.DataPumper;
import workbench.util.UpdateCheck;
import workbench.util.WbFile;
import workbench.util.WbThread;


/**
 * The main application "controller" for the SQL Workbench/J
 *
 * @author  support@sql-workbench.net
 */
public class WbManager
	implements FontChangedListener, Runnable, Thread.UncaughtExceptionHandler
{
	private static WbManager wb;
	private List<MainWindow> mainWindows = Collections.synchronizedList(new ArrayList<MainWindow>(5));
	private List<ToolWindow> toolWindows = Collections.synchronizedList(new ArrayList<ToolWindow>(5));
	private boolean batchMode = false;
	private boolean writeSettings = true;
	private boolean overWriteGlobalSettingsFile = true;
	private boolean outOfMemoryOcurred = false;
	private WbThread shutdownHook = new WbThread(this, "ShutdownHook");
	private AppArguments cmdLine = new AppArguments();

	private WbManager()
	{
		Runtime.getRuntime().addShutdownHook(this.shutdownHook);
		Thread.setDefaultUncaughtExceptionHandler(this);
	}

	public static WbManager getInstance()
	{
		return wb;
	}

	public void uncaughtException(Thread thread, Throwable error)
	{
		LogMgr.logError("WbManager.uncaughtException()", "Thread + " + thread.getName() + " caused an exception!", error);
	}

	public boolean getSettingsShouldBeSaved()
	{
		return this.writeSettings;
	}

	public void showDialog(String clazz)
	{
		JFrame parent = WbManager.getInstance().getCurrentWindow();
		JDialog dialog = null;
		try
		{
			// Use reflection to load various dialogs in order to
			// avoid unnecessary class loading during startup
			Class cls = Class.forName(clazz);
			Class[] types = new Class[] { java.awt.Frame.class  };
			Constructor cons = cls.getConstructor(types);
			Object[] args = new Object[] { parent };
			dialog = (JDialog)cons.newInstance(args);
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			if (dialog.isModal()) WbSwingUtilities.center(dialog, parent);
			dialog.setVisible(true);
		}
		catch (Exception ex)
		{
			LogMgr.logError("WbManager.showDialog()", "Error when creating dialog " + clazz, ex);
		}
		finally
		{
			if (dialog != null && dialog.isModal())
			{
				dialog.dispose();
				dialog = null;
			}
		}
	}

	public boolean outOfMemoryOcurred()
	{
		return this.outOfMemoryOcurred;
	}

	public void showOutOfMemoryError()
	{
		System.gc();
		outOfMemoryOcurred = true;
		WbSwingUtilities.showErrorMessageKey(getCurrentWindow(), "MsgOutOfMemoryError");
	}

	public MainWindow getCurrentWindow()
	{
		if (this.mainWindows == null) return null;
		if (this.mainWindows.size() == 1)
		{
			return this.mainWindows.get(0);
		}
		for (MainWindow w : mainWindows)
		{
			if (w.hasFocus()) return w;
		}
		return null;
	}

	public void registerToolWindow(ToolWindow aWindow)
	{
		synchronized (toolWindows)
		{
			this.toolWindows.add(aWindow);
		}
	}

	public void unregisterToolWindow(ToolWindow aWindow)
	{
		if (aWindow == null) return;
		synchronized (toolWindows)
		{
			int index = this.toolWindows.indexOf(aWindow);
			if (index > -1)
			{
				this.toolWindows.remove(index);
			}
			if (this.toolWindows.size() == 0 && this.mainWindows.size() == 0)
			{
				if (aWindow instanceof JFrame)
				{
					this.exitWorkbench((JFrame)aWindow);
				}
				else
				{
					this.exitWorkbench();
				}
			}
		}
	}

	private void closeToolWindows()
	{
		synchronized (toolWindows)
		{
			for (ToolWindow w : toolWindows)
			{
				w.closeWindow();
			}
			this.toolWindows.clear();
		}
	}

	public void fontChanged(String aFontKey, Font newFont)
	{
		if (aFontKey.equals(Settings.PROPERTY_DATA_FONT))
		{
			UIManager.put("Table.font", newFont);
			UIManager.put("TableHeader.font", newFont);
		}
	}

	public boolean isWindowsClassic() { return isWindowsClassic; }

	private boolean isWindowsClassic = false;

	private void initializeLookAndFeel()
	{
		String className = Settings.getInstance().getLookAndFeelClass();
		try
		{
			if (StringUtil.isEmptyString(className))
			{
				className = UIManager.getSystemLookAndFeelClassName();
			}
			LnFManager mgr = new LnFManager();
			LnFDefinition def = mgr.findLookAndFeel(className);

			if (def == null)
			{
				LogMgr.logError("WbManager.initializeLookAndFeel()", "Specified Look & Feel " + className + " not available!", null);
				return;
			}

			// JGoodies Looks settings
			UIManager.put("jgoodies.useNarrowButtons", Boolean.FALSE);
			UIManager.put("FileChooser.useSystemIcons", Boolean.TRUE);

			// Remove Synthetica's own window decorations
			//UIManager.put("Synthetica.window.decoration", Boolean.FALSE);

			// Remove the extra icons for read only text fields and
			// the "search bar" in the main menu for the Substance Look & Feel
			System.setProperty("substancelaf.noExtraElements", "");

			LnFLoader loader = new LnFLoader(def);
			LookAndFeel lnf = loader.getLookAndFeel();

			UIManager.setLookAndFeel(lnf);
			try
			{
				String clsname = lnf.getClass().getName();
				if (clsname.indexOf("com.sun.java.swing.plaf.windows") > -1)
				{
					String osVersion = System.getProperty("os.version", "1.0");
					Float version = Float.valueOf(osVersion);
					if (version.floatValue() <= 5.0)
					{
						isWindowsClassic = true;
					}
					else
					{
						isWindowsClassic = (clsname.indexOf("WindowsClassicLookAndFeel") > -1);
						if (!isWindowsClassic)
						{
							Toolkit toolkit = Toolkit.getDefaultToolkit();
							Boolean themeActive = (Boolean)toolkit.getDesktopProperty("win.xpstyle.themeActive");
							if (themeActive != null)
							{
								isWindowsClassic = !themeActive.booleanValue();
							}
							else
							{
								isWindowsClassic = true;
							}
						}
					}
				}
			}
			catch (Throwable e)
			{
				isWindowsClassic = false;
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("Settings.initializeLookAndFeel()", "Could not set look and feel", e);
			LogMgr.logWarning("Settings.initializeLookAndFeel()", "Current look and feel class [" + className + "] will be removed");
			Settings.getInstance().setLookAndFeelClass(null);
		}

		try
		{
			Toolkit.getDefaultToolkit().setDynamicLayout(Settings.getInstance().getUseDynamicLayout());
		}
		catch (Exception e)
		{
			LogMgr.logError("WbManager.initializeLookAndFeel()", "Error setting dynamic layout property", e);
		}
	}

	public File getJarFile()
	{
		URL url = this.getClass().getProtectionDomain().getCodeSource().getLocation();
		File f = null;
		try
		{
			// Sending the path through the URLDecoder is important
			// because otherwise a path with %20 will be created
			// if the directory contains spaces!
			String p = URLDecoder.decode(url.getFile(), "UTF-8");
			f = new File(p);
		}
		catch (Exception e)
		{
			// Fallback, should not happen
			String p = url.getFile().replace("%20", " ");
			f = new File(p);
		}
		return f;
	}

	public String getJarPath()
	{
		WbFile parent = new WbFile(getJarFile().getParentFile());
		return parent.getFullPath();
	}

	private void initUI()
	{
		UIManager.put("FileChooser.useSystemIcons", Boolean.TRUE);
		UIManager.put("swing.boldMetal", Boolean.FALSE); 
		
		this.initializeLookAndFeel();

		Settings settings = Settings.getInstance();
		UIDefaults def = UIManager.getDefaults();

		Font stdFont = settings.getStandardFont();
		if (stdFont != null)
		{
			def.put("Button.font", stdFont);
			def.put("CheckBox.font", stdFont);
			def.put("CheckBoxMenuItem.font", stdFont);
			def.put("ColorChooser.font", stdFont);
			def.put("ComboBox.font", stdFont);
			def.put("EditorPane.font", stdFont);
			def.put("FileChooser.font", stdFont);
			def.put("Label.font", stdFont);
			def.put("List.font", stdFont);
			def.put("Menu.font", stdFont);
			def.put("MenuItem.font", stdFont);
			def.put("OptionPane.font", stdFont);
			def.put("Panel.font", stdFont);
			def.put("PasswordField.font", stdFont);
			def.put("PopupMenu.font", stdFont);
			def.put("ProgressBar.font", stdFont);
			def.put("RadioButton.font", stdFont);
			def.put("TabbedPane.font", stdFont);
			def.put("TextArea.font", stdFont);
			def.put("TextField.font", stdFont);
			def.put("TextPane.font", stdFont);
			def.put("TitledBorder.font", stdFont);
			def.put("ToggleButton.font", stdFont);
			def.put("ToolBar.font", stdFont);
			def.put("ToolTip.font", stdFont);
			def.put("Tree.font", stdFont);
			def.put("ViewPort.font", stdFont);
		}

		Font dataFont = settings.getDataFont(false);
		if (dataFont != null)
		{
			def.put("Table.font", dataFont);
			def.put("TableHeader.font", dataFont);
		}

		// Polish up the standard look & feel settings
		Color c = settings.getColor("workbench.table.gridcolor", new Color(215,215,215));
		def.put("Table.gridColor", c);
		def.put("Button.showMnemonics", Boolean.valueOf(settings.getShowMnemonics()));

		// use our own classes for some GUI elements
		def.put("ToolTipUI", "workbench.gui.components.WbToolTipUI");
		def.put("SplitPaneUI", "workbench.gui.components.WbSplitPaneUI");

		String cls = TabbedPaneUIFactory.getTabbedPaneUIClass();
		if (cls != null) def.put("TabbedPaneUI", cls);

		settings.addFontChangedListener(this);
	}

	protected JDialog closeMessage;

	private boolean saveWindowSettings()
	{
		if (!this.writeSettings) return true;
		MainWindow w = this.getCurrentWindow();
		boolean settingsSaved = false;

		// the settings (i.e. size and position) should only be saved
		// for the first visible window
		if (w != null)
		{
			w.saveSettings();
			settingsSaved = true;
		}

		if (!this.checkProfiles(w)) return false;

		boolean result = true;
		for (MainWindow win : mainWindows)
		{
			if (win == null) continue;
			if (!settingsSaved)
			{
				win.saveSettings();
				settingsSaved = true;
			}
			if (win.isBusy())
			{
				if (!this.checkAbort(win)) return false;
			}
			result = win.saveWorkspace(true);
			if (!result) return false;
		}
		return true;
	}

	public boolean isBatchMode()
	{
		return this.batchMode;
	}

	public boolean canExit()
	{
		if (this.saveWindowSettings())
		{
			if (Settings.getInstance().wasExternallyModified())
			{
				String msg = ResourceMgr.getFormattedString("MsgSettingsChanged", Settings.getInstance().getConfigFile().getFullPath());
				int result = WbSwingUtilities.getYesNoCancel(getCurrentWindow(), msg);
				this.overWriteGlobalSettingsFile = (result == JOptionPane.OK_OPTION);
				return result != JOptionPane.CANCEL_OPTION;
			}
			return true;
		}
		else 
		{
			return false;
		}
	}

	public void exitWorkbench()
	{
		MainWindow w = this.getCurrentWindow();
		this.exitWorkbench(w);
	}

	public boolean exitWorkbench(JFrame window)
	{
		// saveSettings() will also prompt if any modified
		// files should be changed
		if (!canExit())
		{
			LogMgr.logInfo("WbManaer.exitWorkbench()", "Exiting application was cancelled during saveWindowSettings()");
			return false;
		}

		if (window == null)
		{
			ConnectionMgr.getInstance().disconnectAll();
			this.doShutdown(0);
			return true;
		}

		// When disconnecting it can happen that the disconnect itself
		// takes some time. Because of this, a small window is displayed
		// that the disconnect takes place, and the actual disconnect is
		// carried out in a different thread to not block the AWT thread.

		// If it takes too long the user can still abort the JVM ...
		this.createCloseMessageWindow(window);
		if (this.closeMessage != null) this.closeMessage.setVisible(true);

		MacroManager.getInstance().saveMacros();
		Thread t = new WbThread("WbManager disconnect")
		{
			public void run()
			{
				disconnectWindows();
				ConnectionMgr.getInstance().disconnectAll();
				disconnected();
			}
		};
		t.setDaemon(false);
		t.start();
		return true;
	}

	private void createCloseMessageWindow(JFrame parent)
	{
		if (parent == null) return;
		this.closeMessage = new JDialog(parent, false);
		this.closeMessage.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		JPanel p = new JPanel();
		p.setBorder(WbSwingUtilities.getBevelBorderRaised());
		p.setLayout(new BorderLayout());
		JLabel l = new JLabel(ResourceMgr.getString("MsgClosingConnections"));
		l.setFont(l.getFont().deriveFont(Font.BOLD));
		l.setHorizontalAlignment(SwingConstants.CENTER);
		p.add(l, BorderLayout.CENTER);

		JButton b = new JButton(ResourceMgr.getString("MsgAbortImmediately"));
		b.setToolTipText(ResourceMgr.getDescription("MsgAbortImmediately"));
		b.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt)
				{
					doShutdown(0);
				}
			});

		JPanel p2 = new JPanel();
		p2.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 10));
		p2.add(b);
		p.add(p2, BorderLayout.SOUTH);
		this.closeMessage.getContentPane().setLayout(new BorderLayout());
		this.closeMessage.getContentPane().add(p, BorderLayout.CENTER);
		this.closeMessage.setUndecorated(true);
		this.closeMessage.setSize(210,80);
		WbSwingUtilities.center(this.closeMessage, parent);
	}

	protected void disconnectWindows()
	{
		for (MainWindow w : mainWindows)
		{
			if (w == null) continue;
			w.abortAll();
			w.disconnect(false, true, false);
		}
	}

	/**
	 *	this gets called from exitWorkbench() when disconnecting everything
	 */
	protected void disconnected()
	{
		WbSwingUtilities.invoke(new Runnable()
		{
			public void run()
			{
				if (closeMessage != null)
				{
					closeMessage.setVisible(false);
					closeMessage.dispose();
					closeMessage = null;
				}
				closeAllWindows();
			}
		});
		doShutdown(0);
	}

	protected void closeAllWindows()
	{
		for (MainWindow w : mainWindows)
		{
			if (w != null)
			{
				try { w.setVisible(false); } catch (Throwable th) {}
				try { w.dispose(); } catch (Throwable th) {}
			}
		}
		closeToolWindows();
	}

	protected void saveSettings()
	{
		if (this.writeSettings && !this.isBatchMode())
		{
			Settings s = Settings.getInstance();
			FilterDefinitionManager.getInstance().saveMRUList();
			if (s != null && overWriteGlobalSettingsFile) s.saveSettings(outOfMemoryOcurred);
		}
	}

	protected void doShutdown(int errorCode)
	{
		Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
		this.closeAllWindows();
		saveSettings();
		LogMgr.logInfo("WbManager.doShutdown()", "Stopping " + ResourceMgr.TXT_PRODUCT_NAME + ", Build " + ResourceMgr.getString("TxtBuildNumber"));
		LogMgr.shutdown();
		// The property workbench.system.doexit can be used to embedd the workbench.jar
		// in other applications and still be able to call doShutdown()
		boolean doExit = "true".equals(System.getProperty("workbench.system.doexit", "true"));
		if (doExit) System.exit(errorCode);
	}

	private boolean checkAbort(MainWindow win)
	{
		return WbSwingUtilities.getYesNo(win, ResourceMgr.getString("MsgAbortRunningSql"));
	}

	private boolean checkProfiles(MainWindow win)
	{
		if (ConnectionMgr.getInstance().profilesAreModified())
		{
			int answer = JOptionPane.showConfirmDialog(win, ResourceMgr.getString("MsgConfirmUnsavedProfiles"), ResourceMgr.TXT_PRODUCT_NAME, JOptionPane.YES_NO_CANCEL_OPTION);
			if (answer == JOptionPane.OK_OPTION)
			{
				ConnectionMgr.getInstance().saveProfiles();
				return true;
			}
			else if (answer == JOptionPane.NO_OPTION)
			{
				return true;
			}
			else
			{
				return false;
			}
		}
		return true;
	}

	/**
	 * Called whenever a MainWindow is closed.
	 * 
	 * @see workbench.gui.MainWindow#windowClosing(java.awt.event.WindowEvent) 
	 * @see workbench.gui.MainWindow#connectCancelled() 
	 */
	public void windowClosing(final MainWindow win)
	{
		if (this.mainWindows.size() == 1)
		{
			// If only one window is present, shut down the application
			this.exitWorkbench(win);
		}
		else if (win != null)
		{
			if (!win.saveWorkspace()) return;
			this.mainWindows.remove(win);
			WbThread t = new WbThread("WindowDisconnect")
			{
				public void run()
				{
					// First parameter tells the window to disconnect in the
					// current thread as we are already in a background thread
					// second parameter tells the window not to close the workspace
					// third parameter tells the window not to save the workspace
					// this does not need to happen on the EDT
					win.disconnect(false, false, false);
					win.setVisible(false);
					win.dispose();
				}
			};
			t.start();
		}
	}

	/**
	 * Open a new main window, but do not check any command line parameters.
	 *
	 * This method will be called from the GUI
	 * when the user requests a new window
	 * 
	 * @see workbench.gui.actions.FileNewWindowAction
	 */
	public void openNewWindow()
	{
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				openNewWindow(false);
			}
		});

	}

	protected void openNewWindow(boolean checkCmdLine)
	{
		final MainWindow main = new MainWindow();
		this.mainWindows.add(main);
		main.display();
		boolean connected = false;

		if (checkCmdLine)
		{
			// get profile name from commandline
			String profilename = cmdLine.getValue(AppArguments.ARG_PROFILE);
			String group = cmdLine.getValue(AppArguments.ARG_PROFILE_GROUP);
			ConnectionProfile prof  = null;
			if (!StringUtil.isEmptyString(profilename))
			{
				ProfileKey def = new ProfileKey(profilename, group);
				prof = ConnectionMgr.getInstance().getProfile(def);
			}
			else
			{
				prof = BatchRunner.createCmdLineProfile(this.cmdLine);
			}

			if (prof != null)
			{
				LogMgr.logDebug("WbManager.openNewWindow()", "Connecting to " + prof.getName());
				// try to connect to the profile passed on the
				// command line. If this fails the connection
				// dialog will be show to the user
				main.connectTo(prof, true);

				// the main window will take care of displaying the connection dialog
				// if the connection to the requested profile fails.
				connected = true;
			}
		}

		boolean autoSelect = Settings.getInstance().getShowConnectDialogOnStartup();
		boolean exitOnCancel = Settings.getInstance().getExitOnFirstConnectCancel();

		// no connection? then display the connection dialog
		if (!connected && autoSelect)
		{
			main.selectConnection(exitOnCancel);
		}
	}

	private void readParameters(String[] args)
	{
		try
		{
			cmdLine.parse(args);

			String lang = cmdLine.getValue(AppArguments.ARG_LANG);
			if (!StringUtil.isEmptyString(lang))
			{
				System.setProperty("workbench.gui.language", lang);
			}

			String value = cmdLine.getValue(AppArguments.ARG_CONFIGDIR);
			if (!StringUtil.isEmptyString(value))
			{
				System.setProperty("workbench.configdir", value);
			}

			value = cmdLine.getValue(AppArguments.ARG_LIBDIR);
			if (!StringUtil.isEmptyString(value))
			{
				System.setProperty("workbench.libdir", value);
			}

			value = cmdLine.getValue(AppArguments.ARG_LOGFILE);
			if (!StringUtil.isEmptyString(value))
			{
				WbFile file = new WbFile(value);
				System.setProperty("workbench.log.filename", file.getFullPath());
			}

			// Make sure the Settings object is (re)initialized properly now that
			// some system properties have been read from the commandline
			// this is especially necessary during JUnit tests to make
			// sure a newly passed commandline overrules the previously initialized
			// Settings instance
			Settings.getInstance().initialize();

			String scriptname = cmdLine.getValue(AppArguments.ARG_SCRIPT);

			boolean readDriverTemplates = true;
			boolean showHelp = cmdLine.isArgPresent("help");

			if (StringUtil.isEmptyString(scriptname) && !showHelp)
			{
				this.batchMode = false;
				String url = cmdLine.getValue(AppArguments.ARG_CONN_URL);
				String jar = cmdLine.getValue(AppArguments.ARG_CONN_JAR);
				if (!StringUtil.isEmptyString(url) && !StringUtil.isEmptyString(jar))
				{
					// Do not read the driver templates if a connection is specified directly
					readDriverTemplates = false;
				}
			}
			else
			{
				this.batchMode = true;
				readDriverTemplates = false;
			}

			value = cmdLine.getValue(AppArguments.ARG_VARDEF);
			if (!StringUtil.isEmptyString(value))
			{
				try
				{
					VariablePool.getInstance().readDefinition(StringUtil.trimQuotes(value));
				}
				catch (IOException e)
				{
					LogMgr.logError("WbManager.initCmdLine()", "Error reading variable definition from file " + value, e);
				}
			}

			if (cmdLine.isArgPresent(AppArguments.ARG_NOTEMPLATES))
			{
				readDriverTemplates = false;
			}
			ConnectionMgr.getInstance().setReadTemplates(readDriverTemplates);

			// Setting the profile storage should be done after initializing
			// the configuration stuff correctly!
			value = cmdLine.getValue(AppArguments.ARG_PROFILE_STORAGE);
			Settings.getInstance().setProfileStorage(value);

			if (cmdLine.isArgPresent(AppArguments.ARG_NOSETTNGS))
			{
				this.writeSettings = false;
			}

			if (cmdLine.hasUnknownArguments())
			{
				String unknown = cmdLine.getUnknownArguments();
				LogMgr.logError("WbManager.readParameters()", "The following parameters are invalid: " + unknown, null);
				if (batchMode)
				{
					System.err.println("Invalid parameter(s): " + unknown);
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace(System.err);
			LogMgr.logError("WbManager.initCdmLine()", "Error initializing command line arguments!", e);
		}
	}

	public void startApplication()
	{
		LogMgr.logInfo("WbManager.init()", "Starting " + ResourceMgr.TXT_PRODUCT_NAME + ", " + ResourceMgr.getBuildInfo());
		LogMgr.logInfo("WbManager.init()", "Java version=" + System.getProperty("java.version")  + ", java.home=" + System.getProperty("java.home") + ", vendor=" + System.getProperty("java.vendor") );
		LogMgr.logInfo("WbManager.init()", "Operating System=" + System.getProperty("os.name")  + ", version=" + System.getProperty("os.version") + ", platform=" + System.getProperty("os.arch"));

		// batchMode flag is set by readParameters()
		if (this.batchMode)
		{
			runBatch();
		}
		else
		{
			// Start a background thread to read the XML files
			// for profiles and drivers. I think this improves
			// startup performance because this can be done in 
			// parallel while the MainWindow is initializing
			// especially on a multi-core computer this should
			// show some improvement - I hope :) 
//			WbThread init = new WbThread("Background Init")
//			{
//				public void run()
//				{
//					ConnectionMgr.getInstance().getDrivers();
//					ConnectionMgr.getInstance().getProfiles();
//				}
//			};
//			init.start();
			
			EventQueue.invokeLater(new Runnable()
			{
				public void run()
				{
					runGui();
				}
			});
		}
	}

	public void runGui()
	{
		WbSplash splash = null;
		if (Settings.getInstance().getShowSplash())
		{
			splash = new WbSplash();
			splash.setVisible(true);
		}

		// This will install the application listener if running under MacOS
		MacOSHelper m = new MacOSHelper();
		m.installApplicationHandler();

		try
		{
			this.initUI();
			
			boolean pumper = cmdLine.isArgPresent(AppArguments.ARG_SHOW_PUMPER);
			boolean explorer = cmdLine.isArgPresent(AppArguments.ARG_SHOW_DBEXP);

			if (pumper)
			{
				new DataPumper().showWindow();
			}
			else if (explorer)
			{
				DbExplorerWindow.showWindow();
			}
			else
			{
				openNewWindow(true);
			}

			UpdateCheck upd = new UpdateCheck();
			upd.startUpdateCheck();
		}
		finally
		{
			if (splash != null)
			{
				splash.setVisible(false);
				splash.dispose();
			}
		}
	}

	private void runBatch()
	{
		int exitCode = 0;

		// Make sure batch mode is always using English
		// System.setProperty("workbench.gui.language", "en");

		BatchRunner runner = BatchRunner.createBatchRunner(cmdLine);

		if (runner != null)
		{
			try
			{
				runner.connect();
			}
			catch (Exception e)
			{
				exitCode = 1;
				// no need to log connect errors, already done by BatchRunner and ConnectionMgr
				// runner.isSuccess() will also be false for the next step
			}

			try
			{
				// Do not check for runner.isConnected() as the in batch mode
				// the application might be started without a profile
				// (e.g. for a single WbCopy command)
				if (runner.isSuccess())
				{
					runner.execute();
					// Not all exceptions will be re-thrown by the batch runner
					// in order to be able to run the error script, so it is important
					// to check isSuccess() in order to return the correct status
					if (!runner.isSuccess()) exitCode = 2;
				}
			}
			catch (OutOfMemoryError e)
			{
				LogMgr.logError("WbManager.runBatch()", "Not enough memory to finish the operation. Aborting execution!", null);
				System.err.println("Not enough memory to finish the operation. Aborting execution!");
				exitCode = 10;
			}
			catch (Exception e)
			{
				exitCode = 2;
			}
			finally
			{
				ConnectionMgr mgr = ConnectionMgr.getInstance();
				if (mgr != null) mgr.disconnectAll();
			}
		}
		else
		{
			exitCode = 3;
		}
		this.doShutdown(exitCode);
	}

	/**
	 * Prepare the Workbench "environment" to be used inside another
	 * application (e.g. for Unit testing)
	 */
	public static void prepareForEmbedded()
	{
		wb = new WbManager();
		// Avoid saving the settings
		Runtime.getRuntime().removeShutdownHook(wb.shutdownHook);
		String args = "-notemplates -nosettings";
		System.setProperty("workbench.system.doexit", "false");
		System.setProperty("workbench.gui.testmode", "true");
		wb.readParameters(new String[] { args} );
	}
		
	public static void prepareForTest(String[] args)
	{
		wb = new WbManager();
		// Avoid saving the settings
		Runtime.getRuntime().removeShutdownHook(wb.shutdownHook);

		// This will be used in isTestMode(). Basically the test
		// mode is used by DbDriver to skip the test if a driver library
		// is accessible because in test mode the drivers are not loaded
		// through our own class loader because they are already present
		// on the classpath.
		System.setProperty("workbench.gui.testmode", "true");
		wb.readParameters(args);
	}

	public static void main(String[] args)
	{
		if (wb == null) wb = new WbManager();

		wb.cmdLine.parse(args);
		boolean showHelp = wb.cmdLine.isArgPresent("help");
		if (showHelp)
		{
			System.out.println(wb.cmdLine.getHelp());
			Runtime.getRuntime().removeShutdownHook(wb.shutdownHook);
			System.exit(0);
		}
		else
		{
			wb.readParameters(args);
			wb.startApplication();
		}
	}

	/**
	 *  this is for the shutdownhook
	 */
	public void run()
	{
		LogMgr.logDebug("WbManager.run()", "Shutdownhook called!");
		saveSettings();
	}

}
