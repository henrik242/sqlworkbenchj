/*
 * This file is part of SQL Workbench/J, https://www.sql-workbench.eu
 *
 * Copyright 2002-2019, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     https://www.sql-workbench.eu/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.eu
 *
 */
package workbench.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import workbench.WbManager;
import workbench.interfaces.Connectable;
import workbench.interfaces.DbExecutionListener;
import workbench.interfaces.FilenameChangeListener;
import workbench.interfaces.MacroChangeListener;
import workbench.interfaces.MainPanel;
import workbench.interfaces.Moveable;
import workbench.interfaces.StatusBar;
import workbench.interfaces.ToolWindow;
import workbench.log.CallerInfo;
import workbench.log.LogMgr;
import workbench.resource.DbExplorerSettings;
import workbench.resource.GuiSettings;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.resource.ShortcutManager;

import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.WbConnection;
import workbench.db.objectcache.DbObjectCacheFactory;

import workbench.gui.actions.AboutAction;
import workbench.gui.actions.AddMacroAction;
import workbench.gui.actions.AddTabAction;
import workbench.gui.actions.AssignWorkspaceAction;
import workbench.gui.actions.BookmarksAction;
import workbench.gui.actions.CloseWorkspaceAction;
import workbench.gui.actions.ConfigureShortcutsAction;
import workbench.gui.actions.ConfigureToolbarAction;
import workbench.gui.actions.CreateNewConnection;
import workbench.gui.actions.DataPumperAction;
import workbench.gui.actions.DisconnectTabAction;
import workbench.gui.actions.EditWorkspaceVarsAction;
import workbench.gui.actions.FileCloseAction;
import workbench.gui.actions.FileConnectAction;
import workbench.gui.actions.FileDiscardAction;
import workbench.gui.actions.FileDisconnectAction;
import workbench.gui.actions.FileExitAction;
import workbench.gui.actions.FileNewWindowAction;
import workbench.gui.actions.FileReconnectAction;
import workbench.gui.actions.FileSaveProfiles;
import workbench.gui.actions.HelpConnectionInfoAction;
import workbench.gui.actions.HelpContactAction;
import workbench.gui.actions.ImportProfilesAction;
import workbench.gui.actions.InsertTabAction;
import workbench.gui.actions.LoadMacrosAction;
import workbench.gui.actions.LoadWorkspaceAction;
import workbench.gui.actions.ManageDriversAction;
import workbench.gui.actions.ManageMacroAction;
import workbench.gui.actions.NewDbExplorerPanelAction;
import workbench.gui.actions.NewDbExplorerWindowAction;
import workbench.gui.actions.NextTabAction;
import workbench.gui.actions.ObjectSearchAction;
import workbench.gui.actions.OpenFileAction;
import workbench.gui.actions.OptionsDialogAction;
import workbench.gui.actions.PrevTabAction;
import workbench.gui.actions.ReloadProfileWkspAction;
import workbench.gui.actions.RemoveTabAction;
import workbench.gui.actions.RenameTabAction;
import workbench.gui.actions.SaveAsNewWorkspaceAction;
import workbench.gui.actions.SaveMacrosAction;
import workbench.gui.actions.SaveWorkspaceAction;
import workbench.gui.actions.SearchAllEditorsAction;
import workbench.gui.actions.SelectTabAction;
import workbench.gui.actions.ShowDbExplorerAction;
import workbench.gui.actions.ShowDbTreeAction;
import workbench.gui.actions.ShowDbmsManualAction;
import workbench.gui.actions.ShowHelpAction;
import workbench.gui.actions.ShowMacroPopupAction;
import workbench.gui.actions.ShowManualAction;
import workbench.gui.actions.VersionCheckAction;
import workbench.gui.actions.ViewLineNumbers;
import workbench.gui.actions.ViewLogfileAction;
import workbench.gui.actions.ViewToolbarAction;
import workbench.gui.actions.WbAction;
import workbench.gui.actions.WhatsNewAction;
import workbench.gui.bookmarks.BookmarkManager;
import workbench.gui.bookmarks.NamedScriptLocation;
import workbench.gui.components.ConnectionSelector;
import workbench.gui.components.DividerBorder;
import workbench.gui.components.GuiPosition;
import workbench.gui.components.MenuScroller;
import workbench.gui.components.RunningJobIndicator;
import workbench.gui.components.TabCloser;
import workbench.gui.components.TabbedPaneHistory;
import workbench.gui.components.WbMenu;
import workbench.gui.components.WbSplitPane;
import workbench.gui.components.WbTabbedPane;
import workbench.gui.components.WbToolbar;
import workbench.gui.dbobjects.DbExplorerPanel;
import workbench.gui.dbobjects.DbExplorerWindow;
import workbench.gui.dbobjects.objecttree.DbTreePanel;
import workbench.gui.dbobjects.objecttree.DbTreeSettings;
import workbench.gui.dbobjects.objecttree.TreePosition;
import workbench.gui.fontzoom.DecreaseFontSize;
import workbench.gui.fontzoom.FontZoomer;
import workbench.gui.fontzoom.IncreaseFontSize;
import workbench.gui.fontzoom.ResetFontSize;
import workbench.gui.macros.MacroMenuBuilder;
import workbench.gui.menu.RecentFileManager;
import workbench.gui.menu.SqlTabPopup;
import workbench.gui.profiles.ConnectionGuiHelper;
import workbench.gui.sql.EditorPanel;
import workbench.gui.sql.PanelType;
import workbench.gui.sql.RenameableTab;
import workbench.gui.sql.SqlPanel;
import workbench.gui.tabhistory.ClosedTabManager;
import workbench.gui.toolbar.ToolbarBuilder;

import workbench.sql.VariablePool;
import workbench.sql.macros.MacroManager;

import workbench.util.ClasspathUtil;
import workbench.util.CollectionUtil;
import workbench.util.ExceptionUtil;
import workbench.util.FileDialogUtil;
import workbench.util.FileUtil;
import workbench.util.FileVersioner;
import workbench.util.HtmlUtil;
import workbench.util.NumberStringCache;
import workbench.util.StringUtil;
import workbench.util.VersionNumber;
import workbench.util.WbFile;
import workbench.util.WbProperties;
import workbench.util.WbThread;
import workbench.util.WbWorkspace;

/**
 * The main window for SQL Workbench.
 * <p>
 * It will display several {@link workbench.gui.sql.SqlPanel}s in
 * a tabbed pane. Additionally one or more {@link workbench.gui.dbobjects.DbExplorerPanel}
 * might also be displayed inside the JTabbedPane
 *
 * @author Thomas Kellerer
 */
public class MainWindow
  extends JFrame
  implements MouseListener, WindowListener, ChangeListener,
             MacroChangeListener, DbExecutionListener, Connectable, PropertyChangeListener,
             Moveable, RenameableTab, TabCloser, FilenameChangeListener
{
  private static final String DEFAULT_WORKSPACE = "Default.wksp";
  private static final String RECENTMACROS_NAME = "recent-macros";
  private static final String DB_TREE_PROPS = "dbtree";

  private static int instanceCount;
  private final int windowId;

  private boolean exitOnCancel = false;

  private WbConnection currentConnection;
  private ConnectionProfile currentProfile;
  protected ConnectionSelector connectionSelector;

  private EditWorkspaceVarsAction editWorkspaceVariables;
  private CloseWorkspaceAction closeWorkspaceAction;
  private SaveWorkspaceAction saveWorkspaceAction;
  private SaveAsNewWorkspaceAction saveAsWorkspaceAction;
  private LoadWorkspaceAction loadWorkspaceAction;
  private AssignWorkspaceAction assignWorkspaceAction;
  private ReloadProfileWkspAction reloadWorkspace;
  private HelpConnectionInfoAction connectionInfoAction;
  private ShowDbmsManualAction showDbmsManual;
  private FileDisconnectAction disconnectAction;
  private FileConnectAction connectAction;
  private FileReconnectAction reconnectAction;
  private CreateNewConnection createNewConnection;
  private DisconnectTabAction disconnectTab;
  private ShowDbExplorerAction dbExplorerAction;
  private NewDbExplorerPanelAction newDbExplorerPanel;
  private NewDbExplorerWindowAction newDbExplorerWindow;
  private FileSaveProfiles saveProfilesAction;
  private FileNewWindowAction newWindowAction;
  private ShowDbTreeAction showDbTree;
  private FileCloseAction fileCloseAction;
  private OpenFileAction fileOpenAction;
  private FileExitAction fileExitAction;

  private final WbTabbedPane sqlTab;
  private final TabbedPaneHistory tabHistory;
  private WbToolbar currentToolbar;
  private final List<JMenuBar> panelMenus = Collections.synchronizedList(new ArrayList<>(15));

  private final Object workspaceLock = new Object();
  private WbWorkspace currentWorkspace;

  private final NextTabAction nextTab;
  private final PrevTabAction prevTab;

  private enum LoadWorkspaceChoice
  {
    CREATE,
    LOAD_OTHER,
    IGNORE;
  }

  private boolean ignoreTabChange;

  // will indicate a connect or disconnect in progress
  // connecting and disconnecting is done in a separate thread
  // so that slow connections do not block the GUI
  private boolean connectInProgress;

  private AddMacroAction createMacro;
  private ManageMacroAction manageMacros;
  private LoadMacrosAction loadMacros;
  private SaveMacrosAction saveMacros;
  private ShowMacroPopupAction showMacroPopup;

  private final List<ToolWindow> explorerWindows = new ArrayList<>();

  private RunningJobIndicator jobIndicator;
  protected WbThread connectThread;
  private DropHandler dropHandler;
  private DbTreePanel treePanel;
  private boolean shouldShowTree;

  private final ClosedTabManager closedTabHistory;

  public MainWindow()
  {
    super(ResourceMgr.TXT_PRODUCT_NAME);

    closedTabHistory = new ClosedTabManager(this);

    // Control the brushed metal look for MacOS, this must be set as soon as possible on the
    // root pane in order to have an effect
    getRootPane().putClientProperty("apple.awt.brushMetalLook", GuiSettings.getUseBrushedMetal());

    this.windowId = ++instanceCount;

    sqlTab = new WbTabbedPane();
    tabHistory = new TabbedPaneHistory(sqlTab);

    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

    // There is no need to register the actions with the ActionMap
    // as they will be handed over to the FocusManager in windowActivated()
    nextTab = new NextTabAction(sqlTab);
    prevTab = new PrevTabAction(sqlTab);

    MacroManager.getInstance().addChangeListener(this, getMacroClientId());

    initMenu();

    ResourceMgr.setWindowIcons(this, "workbench");

    getContentPane().add(this.sqlTab, BorderLayout.CENTER);

    restoreSettings();

    updateTabPolicy();

    Color color = GuiSettings.getEditorTabHighlightColor();
    if (color != null)
    {
      int hwidth = GuiSettings.getEditorTabHighlightWidth();
      GuiPosition location = GuiSettings.getEditorTabHighlightLocation();
      sqlTab.setTabHighlight(color, hwidth, location);
    }

    sqlTab.addChangeListener(this);
    sqlTab.addMouseListener(this);
    sqlTab.hideDisabledButtons(false);
    if (GuiSettings.getShowSqlTabCloseButton())
    {
      sqlTab.showCloseButton(this);
    }

    addWindowListener(this);

    dropHandler = new DropHandler(this, sqlTab);
    sqlTab.enableDragDropReordering(this);

    Settings.getInstance().addPropertyChangeListener(this,
      Settings.PROPERTY_SHOW_TOOLBAR,
      ToolbarBuilder.CONFIG_PROPERTY,
      Settings.PROPERTY_SHOW_TAB_INDEX,
      GuiSettings.PROPERTY_SQLTAB_CLOSE_BUTTON,
      Settings.PROPERTY_TAB_POLICY,
      GuiSettings.PROP_TITLE_APP_AT_END,
      GuiSettings.PROP_TITLE_SHOW_WKSP,
      GuiSettings.PROP_TITLE_SHOW_URL,
      GuiSettings.PROP_TITLE_SHOW_PROF_GROUP,
      GuiSettings.PROP_TITLE_SHOW_EDITOR_FILE,
      GuiSettings.PROP_TITLE_GROUP_SEP,
      GuiSettings.PROP_TITLE_GROUP_BRACKET,
      GuiSettings.PROP_TITLE_SHOW_URL_USER
    );
    ShortcutManager.getInstance().addChangeListener(this);
  }

  protected final void updateTabPolicy()
  {
    final JComponent content = (JComponent)this.getContentPane();
    WbSwingUtilities.invoke(() ->
    {
      int tabPolicy = Settings.getInstance().getIntProperty(Settings.PROPERTY_TAB_POLICY, JTabbedPane.WRAP_TAB_LAYOUT);
      sqlTab.setTabLayoutPolicy(tabPolicy);
      sqlTab.invalidate();
      content.revalidate();
    });
    WbSwingUtilities.repaintLater(this);
  }

  public void display()
  {
    this.restoreState();
    this.setVisible(true);
    this.addTab();
    this.updateWindowTitle();

    boolean macroVisible = Settings.getInstance().getBoolProperty(this.getClass().getName() + ".macropopup.visible", false);
    if (macroVisible)
    {
      EventQueue.invokeLater(showMacroPopup::showPopup);
    }
  }

  public boolean isDbTreeVisible()
  {
    return (treePanel != null && treePanel.isVisible());
  }

  public void showDbTree()
  {
    showDbTree(true);
  }

  public void showDbTree(boolean requestFocus)
  {
    if (treePanel == null)
    {
      treePanel = new DbTreePanel();

      getContentPane().remove(sqlTab);

      WbSplitPane split = new WbSplitPane();
      split.setDividerBorder(WbSwingUtilities.EMPTY_BORDER);
      split.setOneTouchExpandable(true);

      TreePosition position = DbTreeSettings.getDbTreePosition();
      if (position == TreePosition.left)
      {
        treePanel.setBorder(new DividerBorder(DividerBorder.RIGHT));
        sqlTab.setBorder(new DividerBorder(DividerBorder.LEFT));
        split.setLeftComponent(treePanel);
        split.setRightComponent(sqlTab);
      }
      else
      {
        treePanel.setBorder(new DividerBorder(DividerBorder.LEFT));
        sqlTab.setBorder(new DividerBorder(DividerBorder.RIGHT));
        split.setLeftComponent(sqlTab);
        split.setRightComponent(treePanel);
      }

      treePanel.restoreSettings(getToolProperties(DB_TREE_PROPS));

      getContentPane().add(split, BorderLayout.CENTER);

      invalidate();
      sqlTab.invalidate();

      EventQueue.invokeLater(this::validate);
    }

    if (DbTreeSettings.useTabConnection())
    {
      treePanel.setConnectionToUse(getCurrentConnection());
    }
    else if (treePanel.getConnection() == null)
    {
      treePanel.connect(currentProfile);
    }

    if (requestFocus)
    {
      treePanel.requestFocusInWindow();
    }

    int count = getTabCount();
    for (int i = 0; i < count; i++)
    {
      getPanel(i).ifPresent(p -> p.registerObjectFinder(treePanel));
    }
  }

  public void hideDbTree()
  {
    if (treePanel == null) return;
    if (!treePanel.isVisible()) return;

    treePanel.saveSettings(getToolProperties(DB_TREE_PROPS));
    treePanel.setVisible(false);
  }

  public void restoreDbTree()
  {
    if (treePanel == null) return;
    if (treePanel.isVisible()) return;
    treePanel.restoreSettings(getToolProperties(DB_TREE_PROPS));
    treePanel.setVisible(true);
  }

  public DbTreePanel getDbTree()
  {
    return treePanel;
  }

  public void closeDbTree()
  {
    if (treePanel != null)
    {
      treePanel.saveSettings(getToolProperties(DB_TREE_PROPS));

      JSplitPane split = (JSplitPane)treePanel.getParent();
      split.remove(sqlTab);
      getContentPane().remove(split);
      getContentPane().add(sqlTab, BorderLayout.CENTER);
      sqlTab.invalidate();
      sqlTab.setBorder(WbSwingUtilities.EMPTY_BORDER);
      invalidate();

      treePanel.disconnectInBackground();
      treePanel = null;

      int count = getTabCount();
      for (int i = 0; i < count; i++)
      {
        getPanel(i).ifPresent(p -> p.registerObjectFinder(null));
      }
      EventQueue.invokeLater(this::validate);
    }
  }

  @Override
  public void fileNameChanged(Object sender, String newFilename)
  {
    if (ignoreTabChange) return;

    updateWindowTitle();
    if (sender instanceof SqlPanel)
    {
      if (newFilename != null)
      {
        updateRecentFiles();
      }
      SqlPanel panel = (SqlPanel)sender;
      BookmarkManager.getInstance().updateInBackground(this, panel, true);
    }
  }

  /**
   * The listener will be notified when the name of a tab changes.
   * This is used in the {@link workbench.gui.dbobjects.TableListPanel}
   * to display the available panels in the context menu
   *
   * @see workbench.gui.dbobjects.EditorTabSelectMenu#fileNameChanged(Object, String)
   */
  public void addFilenameChangeListener(FilenameChangeListener aListener)
  {
    for (int i = 0; i < this.sqlTab.getTabCount(); i++)
    {
       getSqlPanel(i).ifPresent(panel -> panel.addFilenameChangeListener(aListener));
    }
  }


  /**
   * Remove the file name change listener.
   *
   * @see #addFilenameChangeListener(FilenameChangeListener )
   */
  public void removeFilenameChangeListener(FilenameChangeListener aListener)
  {
    for (int i = 0; i < this.sqlTab.getTabCount(); i++)
    {
       getSqlPanel(i).ifPresent(panel -> panel.removeFilenameChangeListener(aListener));
    }
  }

  /**
   * The listener will be notified when the current tab changes.
   * This is used in the {@link workbench.gui.dbobjects.TableListPanel}
   * to highlight the current tab the context menu
   *
   * @see workbench.gui.dbobjects.TableListPanel#stateChanged(ChangeEvent)
   */
  @Override
  public void addTabChangeListener(ChangeListener aListener)
  {
    this.sqlTab.addChangeListener(aListener);
  }

  public void removeIndexChangeListener(ChangeListener aListener)
  {
    this.sqlTab.removeChangeListener(aListener);
  }

  public void addExecutionListener(DbExecutionListener l)
  {
    int count = this.sqlTab.getTabCount();
    for (int i = 0; i < count; i++)
    {
       getSqlPanel(i).ifPresent(panel -> panel.addDbExecutionListener(l));
    }
  }

  public void removeExecutionListener(DbExecutionListener l)
  {
    int count = this.sqlTab.getTabCount();
    for (int i = 0; i < count; i++)
    {
       getSqlPanel(i).ifPresent(panel -> panel.removeDbExecutionListener(l));
    }
  }

  public boolean hasProfileWorkspace()
  {
    return currentProfile != null && StringUtil.isNonEmpty(currentProfile.getWorkspaceFile());
  }

  protected void checkWorkspaceActions()
  {
    final boolean workspaceSet = this.currentWorkspace != null;
    this.saveWorkspaceAction.setEnabled(workspaceSet);
    this.assignWorkspaceAction.setEnabled(workspaceSet && this.currentProfile != null);
    this.closeWorkspaceAction.setEnabled(workspaceSet);
    this.editWorkspaceVariables.setEnabled(workspaceSet);
    checkReloadWkspAction();
  }

  private void initMenu()
  {
    this.disconnectAction = new FileDisconnectAction(this);
    this.reconnectAction = new FileReconnectAction(this);
    this.assignWorkspaceAction = new AssignWorkspaceAction(this);
    this.reloadWorkspace = new ReloadProfileWkspAction(this);
    this.closeWorkspaceAction = new CloseWorkspaceAction(this);
    this.editWorkspaceVariables = new EditWorkspaceVarsAction(this);
    this.saveAsWorkspaceAction = new SaveAsNewWorkspaceAction(this);

    this.createNewConnection = new CreateNewConnection(this);
    this.disconnectTab = new DisconnectTabAction(this);

    this.loadWorkspaceAction = new LoadWorkspaceAction(this);
    this.saveWorkspaceAction = new SaveWorkspaceAction(this);

    this.createMacro = new AddMacroAction(getMacroClientId());
    this.manageMacros = new ManageMacroAction(this);
    this.loadMacros = new LoadMacrosAction(getMacroClientId());
    this.saveMacros = new SaveMacrosAction(getMacroClientId());
    showMacroPopup = new ShowMacroPopupAction(this);

    this.dbExplorerAction = new ShowDbExplorerAction(this);
    this.newDbExplorerPanel = new NewDbExplorerPanelAction(this);
    this.newDbExplorerWindow = new NewDbExplorerWindowAction(this);
    this.showDbTree = new ShowDbTreeAction(this);

    this.showDbmsManual = new ShowDbmsManualAction();
    this.connectionInfoAction = new HelpConnectionInfoAction(this);
    this.connectionInfoAction.setEnabled(false);

    int tabCount = this.sqlTab.getTabCount();
    for (int tab = 0; tab < tabCount; tab++)
    {
      MainPanel sql = (MainPanel)this.sqlTab.getComponentAt(tab);
      JMenuBar menuBar = this.createMenuForPanel(sql);
      this.panelMenus.add(menuBar);
    }
  }

  private void adjustMenuHeight(JMenuBar bar)
  {
    if (!GuiSettings.limitMenuLength()) return;
    int maxItems = Math.min(WbSwingUtilities.calculateMaxMenuItems(this) - 4, GuiSettings.maxMenuItems());
    int count = bar.getMenuCount();
    for (int i = 0; i < count; i++)
    {
      JMenu menu = bar.getMenu(i);
      if (menu == null) continue;
      int items = menu.getItemCount();
      if (items > maxItems)
      {
        MenuScroller.setScrollerFor(menu, maxItems - 4);
      }
    }
  }

  private JMenuBar createMenuForPanel(MainPanel panel)
  {
    HashMap<String, JMenu> menus = new HashMap<>(10);

    JMenuBar menuBar = new JMenuBar();
    menuBar.setBorderPainted(false);
    menuBar.putClientProperty("jgoodies.headerStyle", "Single");

    // Create the file menu for all tabs
    JMenu menu = new WbMenu(ResourceMgr.getString(ResourceMgr.MNU_TXT_FILE));
    menu.setName(ResourceMgr.MNU_TXT_FILE);
    menuBar.add(menu);
    menus.put(ResourceMgr.MNU_TXT_FILE, menu);

    connectAction = new FileConnectAction(this);
    connectAction.addToMenu(menu);
    disconnectAction.addToMenu(menu);
    reconnectAction.addToMenu(menu);
    fileCloseAction = new FileCloseAction(this);
    fileCloseAction.addToMenu(menu);
    menu.addSeparator();
    this.createNewConnection.addToMenu(menu);
    this.disconnectTab.addToMenu(menu);
    menu.addSeparator();

    saveProfilesAction = new FileSaveProfiles();
    saveProfilesAction.addToMenu(menu);

    newWindowAction = new FileNewWindowAction();
    newWindowAction.addToMenu(menu);

    fileOpenAction = new OpenFileAction(this);
    menu.addSeparator();
    fileOpenAction.addToMenu(menu);

    // now create the menus for the current tab
    List<Object> menuItems = panel.getMenuItems();

    // Create the menus in the correct order
    if (isSQLPanel(panel))
    {
      menu = new WbMenu(ResourceMgr.getString(ResourceMgr.MNU_TXT_EDIT));
      menu.setName(ResourceMgr.MNU_TXT_EDIT);
      menu.setVisible(false);
      menuBar.add(menu);
      menus.put(ResourceMgr.MNU_TXT_EDIT, menu);
    }

    menu = new WbMenu(ResourceMgr.getString(ResourceMgr.MNU_TXT_VIEW));
    menu.setName(ResourceMgr.MNU_TXT_VIEW);
    menu.setVisible(true);
    menuBar.add(menu);
    menus.put(ResourceMgr.MNU_TXT_VIEW, menu);

    int tabCount = this.sqlTab.getTabCount();
    for (int i = 0; i < tabCount; i++)
    {
      menu.add(new SelectTabAction(this.sqlTab, i));
    }
    menu.addSeparator();
    menu.add(nextTab.getMenuItem());
    menu.add(prevTab.getMenuItem());

    if (isSQLPanel(panel))
    {
      menu = new WbMenu(ResourceMgr.getString(ResourceMgr.MNU_TXT_DATA));
      menu.setName(ResourceMgr.MNU_TXT_DATA);
      menu.setVisible(false);
      menuBar.add(menu);
      menus.put(ResourceMgr.MNU_TXT_DATA, menu);

      menu = new WbMenu(ResourceMgr.getString(ResourceMgr.MNU_TXT_SQL));
      menu.setName(ResourceMgr.MNU_TXT_SQL);
      menu.setVisible(false);
      menuBar.add(menu);
      menus.put(ResourceMgr.MNU_TXT_SQL, menu);

      final WbMenu macroMenu = new WbMenu(ResourceMgr.getString(ResourceMgr.MNU_TXT_MACRO));
      macroMenu.setName(ResourceMgr.MNU_TXT_MACRO);
      macroMenu.setVisible(true);
      menuBar.add(macroMenu);
      menus.put(ResourceMgr.MNU_TXT_MACRO, macroMenu);
      buildMacroMenu(macroMenu);
    }

    menu = new WbMenu(ResourceMgr.getString(ResourceMgr.MNU_TXT_WORKSPACE));
    menu.setName(ResourceMgr.MNU_TXT_WORKSPACE);
    menuBar.add(menu);
    menus.put(ResourceMgr.MNU_TXT_WORKSPACE, menu);
    menu.add(this.saveWorkspaceAction);
    menu.add(this.saveAsWorkspaceAction);
    menu.add(this.loadWorkspaceAction);
    menu.add(this.reloadWorkspace);
    menu.add(this.editWorkspaceVariables);
    menu.addSeparator();
    menu.add(this.closeWorkspaceAction);
    menu.add(this.assignWorkspaceAction);
    menu.addSeparator();
    JMenu recentWorkspace = new JMenu(ResourceMgr.getString("MnuTxtRecentWorkspace"));
    recentWorkspace.setName("recent-workspace");
    RecentFileManager.getInstance().populateRecentWorkspaceMenu(recentWorkspace, this);
    menu.add(recentWorkspace);

    for (Object entry : menuItems)
    {
      WbMenu subMenu = null;
      String menuName = null;
      WbAction menuAction = null;

      boolean menuSep = false;
      if (entry instanceof WbAction)
      {
        menuAction = (WbAction)entry;
        menuName = menuAction.getMenuItemName();
        menuSep = menuAction.getCreateMenuSeparator();
      }
      else if (entry instanceof WbMenu)
      {
        subMenu = (WbMenu)entry;
        menuName = subMenu.getParentMenuId();
        menuSep = subMenu.getCreateMenuSeparator();
      }

      if (menuName == null) continue;

      menu = menus.get(menuName);

      if (menu == null)
      {
        menu = new WbMenu(ResourceMgr.getString(menuName));
        menuBar.add(menu);
        menus.put(menuName, menu);
      }

      if (menuSep)
      {
        menu.addSeparator();
      }

      if (menuAction != null)
      {
        menuAction.addToMenu(menu);
        if (menuAction instanceof FileDiscardAction)
        {
          JMenu recentFiles = new JMenu(ResourceMgr.getString("MnuTxtRecentFiles"));
          recentFiles.setName("recent-files");
          RecentFileManager.getInstance().populateRecentFilesMenu(recentFiles, this);
          menu.add(recentFiles);
        }
      }
      else if (subMenu != null)
      {
        menu.add(subMenu);
        subMenu.setVisible(true);
      }
      menu.setVisible(true);
    }

    final JMenu filemenu = menus.get(ResourceMgr.MNU_TXT_FILE);

    filemenu.addSeparator();
    filemenu.add(new ManageDriversAction());
    filemenu.add(new ImportProfilesAction());
    filemenu.addSeparator();

    fileExitAction = new FileExitAction();
    filemenu.add(fileExitAction);

    final JMenu viewMenu = menus.get(workbench.resource.ResourceMgr.MNU_TXT_VIEW);
    AddTabAction add = new AddTabAction(this);
    viewMenu.addSeparator();
    viewMenu.add(add);
    InsertTabAction insert = new InsertTabAction(this);
    viewMenu.add(insert);

    RemoveTabAction rem = new RemoveTabAction(this);
    viewMenu.add(rem);
    viewMenu.add(new RenameTabAction(this));
    viewMenu.addSeparator();
    ViewLineNumbers v = new ViewLineNumbers();
    v.addToMenu(viewMenu);

    WbAction vTb = new ViewToolbarAction();
    vTb.addToMenu(viewMenu);

    if (isSQLPanel(panel))
    {
      JMenu zoom = new JMenu(ResourceMgr.getString("TxtZoom"));
      SqlPanel sqlpanel = (SqlPanel)panel;
      EditorPanel editor = sqlpanel.getEditor();
      FontZoomer zoomer = editor.getFontZoomer();

      IncreaseFontSize inc = new IncreaseFontSize(zoomer);
      DecreaseFontSize dec = new DecreaseFontSize(zoomer);
      ResetFontSize reset = new ResetFontSize(zoomer);

      zoom.add(new JMenuItem(inc));
      zoom.add(new JMenuItem(dec));
      zoom.addSeparator();
      zoom.add(new JMenuItem(reset));
      viewMenu.add(zoom);
    }

    menuBar.add(this.buildToolsMenu());
    menuBar.add(this.buildHelpMenu());

    adjustMenuHeight(menuBar);
    return menuBar;
  }

  public List<WbAction> getAllActions()
  {
    SqlPanel panel = null;
    int count = sqlTab.getTabCount();
    for (int i = 0; i < count; i++)
    {
      if (isSQLPanel(sqlTab.getComponent(i)))
      {
        panel = (SqlPanel) sqlTab.getComponent(i);
        break;
      }
    }
    if (panel == null)
    {
      // should not happen
      panel = new SqlPanel(-1);
    }

    List<WbAction> actions = new ArrayList<>(100);
    actions.addAll(getGlobalActions());
    actions.addAll(panel.getAllActions());
    return actions;
  }

  private List<WbAction> getGlobalActions()
  {
    List<WbAction> actions = new ArrayList<>(15);
    actions.add(editWorkspaceVariables);
    actions.add(closeWorkspaceAction);
    actions.add(saveWorkspaceAction);
    actions.add(saveAsWorkspaceAction);
    actions.add(loadWorkspaceAction);
    actions.add(assignWorkspaceAction);
    actions.add(reloadWorkspace);
    actions.add(connectionInfoAction);
    actions.add(showDbmsManual);
    actions.add(disconnectAction);
    actions.add(connectAction);
    actions.add(reconnectAction);
    actions.add(createNewConnection);
    actions.add(disconnectTab);
    actions.add(dbExplorerAction);
    actions.add(newDbExplorerPanel);
    actions.add(newDbExplorerWindow);
    actions.add(showDbTree);
    actions.add(fileCloseAction);
    actions.add(fileOpenAction);
    actions.add(newWindowAction);
    actions.add(saveProfilesAction);
    actions.add(fileExitAction);
    actions.add(manageMacros);
    actions.add(loadMacros);
    actions.add(saveMacros);
    actions.add(showMacroPopup);
    actions.add(disconnectTab);

    actions.add(new ManageDriversAction());
    actions.add(new DataPumperAction(this));
    actions.add(new ObjectSearchAction(this));
    actions.add(new BookmarksAction(this));
    actions.add(new SearchAllEditorsAction(this));
    actions.add(new ShowHelpAction());
    actions.add(new ShowManualAction());
    actions.add(ViewLogfileAction.getInstance());

    return actions;
  }

  /**
   * Removes or makes the toolbar visible depending on
   * {@link GuiSettings#getShowToolbar}.
   * <p>
   * This method will <i>validate</i> this' {@link #getContentPane content pane}
   * in case a change on the toolbar's visibility is performed.
   * <p>
   * This method should be called on the EDT.
   */
  private void updateToolbarVisibility(boolean createNew)
  {
    final JComponent content = (JComponent)this.getContentPane();

    if (this.currentToolbar != null)
    {
      content.remove(this.currentToolbar);
      this.currentToolbar = null;
    }

    if (GuiSettings.getShowToolbar())
    {
      this.getCurrentPanel().ifPresent(curPanel ->
      {
        currentToolbar = curPanel.getToolbar(getGlobalActions(), createNew);
        content.add(currentToolbar, BorderLayout.NORTH);
      });
    }
    content.revalidate();
  }

  public void forceRedraw()
  {
    WbSwingUtilities.invoke(() ->
    {
      JComponent content = (JComponent)getContentPane();
      sqlTab.validate();
      content.validate();
    });
    WbSwingUtilities.repaintLater(this);
  }

  @Override
  public void propertyChange(PropertyChangeEvent evt)
  {
    String property = evt.getPropertyName();
    if (Settings.PROPERTY_SHOW_TOOLBAR.equals(property) || ToolbarBuilder.CONFIG_PROPERTY.equals(property))
    {
      updateToolbarVisibility(ToolbarBuilder.CONFIG_PROPERTY.equals(property));
    }
    else if (Settings.PROPERTY_SHOW_TAB_INDEX.equals(property))
    {
      this.renumberTabs();
    }
    else if (GuiSettings.PROPERTY_SQLTAB_CLOSE_BUTTON.equals(property))
    {
      if (GuiSettings.getShowSqlTabCloseButton())
      {
        sqlTab.showCloseButton(this);
      }
      else
      {
        sqlTab.showCloseButton(null);
      }
    }
    else if (Settings.PROPERTY_TAB_POLICY.equals(property))
    {
      updateTabPolicy();
    }
    else if (GuiSettings.WINDOW_TITLE_PROPS.contains(property))
    {
      updateWindowTitle();
    }
  }

  private void checkMacroMenuForPanel(int index)
  {
    this.getPanel(index).ifPresent(p ->
    {
      try
      {
        JMenu macro = this.getMacroMenu(index);
        setMacroMenuItemStates(macro, p.isConnected());
      }
      catch (Exception e)
      {
        LogMgr.logError("MainWindow.checkMacroMenuForPanel()", "Error during macro update", e);
      }
    });
  }

  private void setMacroMenuEnabled(boolean enabled)
  {
    int count = this.sqlTab.getTabCount();
    for (int i = 0; i < count; i++)
    {
      JMenu macro = this.getMacroMenu(i);
      setMacroMenuItemStates(macro, enabled);
    }
  }

  private void setMacroMenuItemStates(JMenu menu, boolean enabled)
  {
    if (menu != null)
    {
      int itemCount = menu.getItemCount();

      int startIndex = -1;
      for (int i = 0; i < itemCount; i++)
      {
        JMenuItem item = menu.getItem(i);
        if (item == null) continue;
        if (item.getName() == null) continue;
        if (item.getName().equals(RECENTMACROS_NAME))
        {
          startIndex = i + 1;
        }
      }

      if (startIndex == -1)
      {
        LogMgr.logWarning("MainWindow.setMacroMenuItemStates()", "Start of macro menu items not found!");
        return;
      }

      for (int in = startIndex; in < itemCount; in++)
      {
        JMenuItem item = menu.getItem(in);
        if (item != null) item.setEnabled(enabled);
      }
    }
  }

  @Override
  public void macroListChanged()
  {
    EventQueue.invokeLater(this::updateMacros);
  }

  private void updateMacros()
  {
    int count = this.sqlTab.getTabCount();
    for (int i = 0; i < count; i++)
    {
      JMenu macros = this.getMacroMenu(i);
      if (macros != null)
      {
        buildMacroMenu(macros);
        this.getPanel(i).ifPresent(p -> this.setMacroMenuItemStates(macros, p.isConnected()));
      }
    }
  }

  private void buildMacroMenu(JMenu macroMenu)
  {
    macroMenu.removeAll();
    createMacro.addToMenu(macroMenu);
    manageMacros.addToMenu(macroMenu);
    showMacroPopup.addToMenu(macroMenu);

    macroMenu.addSeparator();
    loadMacros.addToMenu(macroMenu);
    saveMacros.addToMenu(macroMenu);

    JMenu recentWorkspace = new JMenu(ResourceMgr.getString("MnuTxtRecentMacros"));
    recentWorkspace.setName(RECENTMACROS_NAME);
    RecentFileManager.getInstance().populateRecentMacrosMenu(getMacroClientId(), recentWorkspace);
    macroMenu.add(recentWorkspace);

    MacroMenuBuilder builder = new MacroMenuBuilder();
    builder.buildMacroMenu(this, macroMenu);
  }

  public int getCurrentPanelIndex()
  {
    return this.sqlTab.getSelectedIndex();
  }

  public int getIndexForPanel(Optional<MainPanel> panel)
  {
    return panel.map(MainPanel::getId).map(this::getIndexForPanel).orElse(-1);
  }

  public int getIndexForPanel(String tabId)
  {
    if (tabId == null) return -1;
    int tabCount = this.sqlTab.getTabCount();
    for (int i = 0; i < tabCount; i++)
    {
      String id = this.getPanel(i).map(MainPanel::getId).orElse(null);
      if (tabId.equals(id)) return i;
    }
    return -1;
  }

  /**
   * Return properties for a specific tool.
   *
   * @param toolKey a (unique) key for the tool. It must not contain spaces or special characters
   *
   * @return the properties, never null
   */
  public WbProperties getToolProperties(String toolKey)
  {
    if (currentWorkspace == null) return new WbProperties(0);

    synchronized (workspaceLock)
    {
      Map<String, WbProperties> toolProperties = currentWorkspace.getToolProperties();

      WbProperties props = toolProperties.get(toolKey);
      if (props == null)
      {
        props = new WbProperties(1);
        toolProperties.put(toolKey, props);
      }
      return props;
    }
  }

  /**
   * Return a list of titles for all sql panels.
   * For indexes where a DbExplorer is open a NULL string will be returned
   * at that index position in the list.
   */
  public List<String> getPanelLabels()
  {
    int tabCount = this.sqlTab.getTabCount();

    List<String> result = new ArrayList<>(tabCount);
    for (int i = 0; i < tabCount; i++)
    {
      String title = this.getSqlPanel(i).map(SqlPanel::getTabTitle).orElse(null);
      result.add(title);
    }
    return result;
  }

  public Optional<MainPanel> getCurrentPanel()
  {
    int index = this.sqlTab.getSelectedIndex();
    if (index > -1)
    {
      return this.getPanel(index);
    }
    return Optional.empty();
  }

  public ClosedTabManager getClosedTabHistory()
  {
    return closedTabHistory;
  }

  public SqlPanel getCurrentSqlPanel()
  {
    return this.getCurrentPanel().filter(SqlPanel.class::isInstance).map(SqlPanel.class::cast).orElse(null);
  }

  public int getTabCount()
  {
    return this.sqlTab.getTabCount();
  }

  /**
   * Gets an optional of the {@link MainPanel} at the given instance
   * @param index
   * @return
   */
  public Optional<MainPanel> getPanel(int index)
  {
    if (index < 0 || index >= sqlTab.getTabCount()) return Optional.empty();
    try
    {
      return Optional.of((MainPanel)this.sqlTab.getComponentAt(index));
    }
    catch (Exception e)
    {
      LogMgr.logDebug("MainWindow.getSqlPanel()", "Invalid index [" + index + "] specified!", e);
      return Optional.empty();
    }
  }

  /**
   * Returns an {@link Optional} of the {@link SqlPanel} at the given index
   * @param i
   * @return
   */
  public Optional<SqlPanel> getSqlPanel(int i)
  {
     return this.getPanel(i)
                .filter(SqlPanel.class::isInstance)
                .map(SqlPanel.class::cast);
  }

  public void selectTab(int anIndex)
  {
    this.sqlTab.setSelectedIndex(anIndex);
  }

  public boolean isConnectInProgress()
  {
    return this.connectInProgress;
  }

  private void clearConnectIsInProgress()
  {
    this.connectInProgress = false;
  }

  private void setConnectIsInProgress()
  {
    this.connectInProgress = true;
  }

  public void checkConnectionForPanel(final Optional<MainPanel> panel)
  {
    if (this.isConnectInProgress()) return;

    panel.filter(((Predicate<MainPanel>)MainPanel::isConnected).negate()).ifPresent(p ->
    {
      try
      {
        if (this.currentProfile != null && this.currentProfile.getUseSeparateConnectionPerTab())
        {
          createNewConnectionForPanel(panel);
        }
        else if (this.currentConnection != null)
        {
          currentConnection.setShared(true);
          p.setConnection(this.currentConnection);
        }
      }
      catch (Exception e)
      {
        LogMgr.logError(new CallerInfo(){}, "Error when checking connection", e);
      }
    });
  }

  public void disconnectCurrentPanel()
  {
    if (this.currentProfile == null) return;
    if (this.currentProfile.getUseSeparateConnectionPerTab()) return;

    this.getCurrentPanel().ifPresent(p ->
    {
      WbConnection con = p.getConnection();
      if (con == this.currentConnection) return;

      Thread t = new WbThread("Disconnect panel " + p.getId())
      {
        @Override
        public void run()
        {
          disconnectPanel(Optional.of(p));
        }
      };
      t.start();
    });
  }

  protected void disconnectPanel(final Optional<MainPanel> panel)
  {
    if (this.isConnectInProgress()) return;
    boolean inProgress = isConnectInProgress();
    if (!inProgress) setConnectIsInProgress();

    showDisconnectInfo();
    showStatusMessage(ResourceMgr.getString("MsgDisconnecting"));

    try
    {
      panel.ifPresent(p ->
      {
        WbConnection old = p.getConnection();
        p.disconnect();

        // use WbConnection.disconnect() rather than ConnectionMgr.getInstance().disconnect()
        // to make sure the connection state listeners are notified
        old.disconnect();

        p.setConnection(currentConnection);
      });
      int index = this.getIndexForPanel(panel);
      sqlTab.setForegroundAt(index, null);
    }
    catch (Throwable e)
    {
      LogMgr.logError(new CallerInfo(){}, "Error when disconnecting panel " + panel.map(MainPanel::getId).orElse("-"), e);
      String error = ExceptionUtil.getDisplay(e);
      WbSwingUtilities.showErrorMessage(this, error);
    }
    finally
    {
      showStatusMessage("");
      closeConnectingInfo();
      if (!inProgress) clearConnectIsInProgress();
    }

    EventQueue.invokeLater(() ->
    {
      createNewConnection.checkState();
      disconnectTab.checkState();
    });
  }

  public boolean canUseSeparateConnection()
  {
    if (this.currentProfile == null) return false;
    return !this.currentProfile.getUseSeparateConnectionPerTab();
  }

  public boolean usesSeparateConnection()
  {
    if (!canUseSeparateConnection()) return false;
    return usesSeparateConnection(this.getCurrentPanel());
  }

  public boolean usesSeparateConnection(Optional<MainPanel> panel)
  {
    if (!canUseSeparateConnection()) return false;
    return panel.map(MainPanel::getConnection).map(t -> t.isShared() == false).orElse(false);
  }

  public void createNewConnectionForCurrentPanel()
  {
    Optional<MainPanel> panel = getCurrentPanel();
    createNewConnectionForPanel(panel);
    EventQueue.invokeLater(() ->
    {
      int index = getIndexForPanel(panel);
      sqlTab.setForegroundAt(index, Color.BLUE);
    });
  }

  protected void createNewConnectionForPanel(final Optional<MainPanel> panel)
  {
    if (this.isConnectInProgress()) return;
    if (this.connectThread != null) return;

    if (!panel.isPresent())
    {
      LogMgr.logDebug(new CallerInfo(){}, "createNewConnectionForPanel() called without a panel!", new Exception("Backtrace"));
      return;
    }

    this.showConnectingInfo();

    this.connectThread = new WbThread("Panel Connect " + panel.get().getId())
    {
      @Override
      public void run()
      {
        connectPanel(panel);
      }
    };
    this.connectThread.start();
  }

  /**
   * Connect the given panel to the database. This will always
   * create a new physical connection to the database.
   */
  protected void connectPanel(final Optional<MainPanel> aPanel)
  {
    if (this.isConnectInProgress()) return;
    this.setConnectIsInProgress();

    try
    {
      // prevent a manual tab change while connecting
      sqlTab.setEnabled(false);
      WbConnection conn = this.getConnectionForTab(aPanel, true);
      int index = this.getIndexForPanel(aPanel);
      this.tabConnected(aPanel, conn, index);
    }
    catch (Throwable e)
    {
      LogMgr.logError(new CallerInfo(){}, "Error when connecting panel " + aPanel.map(MainPanel::getId).orElse("-"), e);
      showStatusMessage("");
      String error = ExceptionUtil.getDisplay(e);
      WbSwingUtilities.showFriendlyErrorMessage(this, ResourceMgr.getString("ErrConnectFailed"), error);
    }
    finally
    {
      sqlTab.setEnabled(true);
      closeConnectingInfo();
      clearConnectIsInProgress();
      this.connectThread = null;
    }
  }

  public void waitForConnection()
  {
    if (this.connectThread != null)
    {
      try
      {
        this.connectThread.join();
      }
      catch (Exception e)
      {
        LogMgr.logError(new CallerInfo(){}, "Error joining connection thread", e);
      }
    }
  }

  private boolean isSQLPanel(Object panel)
  {
    return panel instanceof SqlPanel;
  }

  private void tabConnected(final Optional<MainPanel> panel, WbConnection conn, final int anIndex)
  {
    this.closeConnectingInfo();
    panel.ifPresent(p -> p.setConnection(conn));

    if (isSQLPanel(panel.orElse(null)) && isDbTreeVisible() && DbTreeSettings.useTabConnection())
    {
      treePanel.setConnectionToUse(conn);
    }

    WbSwingUtilities.invoke(() ->
    {
      updateGuiForTab(anIndex);
    });
  }

  private void updateGuiForTab(final int index)
  {
    if (index < 0) return;
    if (index > this.sqlTab.getTabCount() - 1) return;

    this.getPanel(index).ifPresent(current ->
    {
      JMenuBar menu = null;
      if (index > -1 && index < panelMenus.size())
      {
        menu = this.panelMenus.get(index);
      }

      // this can happen if a tab selected event occurs during initialization of a new tab
      if (menu == null)
      {
        return;
      }

      setJMenuBar(menu);
      updateToolbarVisibility(false);
      createNewConnection.checkState();
      disconnectTab.checkState();
      checkMacroMenuForPanel(index);
      forceRedraw();

      SwingUtilities.invokeLater(current::panelSelected);
    });
  }

  public void currentTabChanged()
  {
    int index = getCurrentPanelIndex();
    tabSelected(index);
  }

  protected void tabSelected(final int index)
  {
    if (index < 0) return;
    if (index >= sqlTab.getTabCount()) return;

    // Make sure this is executed on the EDT
    WbSwingUtilities.invoke(() ->
    {
      updateCurrentTab(index);
    });

    int lastIndex = sqlTab.getPreviousTabIndex();
    if (lastIndex > -1 && lastIndex < sqlTab.getTabCount())
    {
      BookmarkManager.getInstance().updateInBackground(MainWindow.this, getPanel(lastIndex).orElse(null), false);
    }

    Predicate<? super MainPanel> isDBExplorerPanel = p -> p instanceof DbExplorerPanel;
    if (getCurrentPanel().filter(isDBExplorerPanel).isPresent())
    {
      hideDbTree();
    }
    else
    {
      getPanel(lastIndex).filter(isDBExplorerPanel).map(DbExplorerPanel.class::cast).ifPresent(lastPanel ->
      {
        if (shouldShowTree)
        {
          showDbTree(false);
          shouldShowTree = false;
        }
        else
        {
          restoreDbTree();
        }
      });
    }
  }

  private void updateCurrentTab(int index)
  {
    Optional<MainPanel> current = getPanel(index);
    checkConnectionForPanel(current);
    updateAddMacroAction();
    updateGuiForTab(index);
    updateWindowTitle();
    updateRecentFiles();
  }

  protected void updateAddMacroAction()
  {
    SqlPanel sql = this.getCurrentSqlPanel();
    if (sql != null)
    {
      createMacro.setClient(sql.getEditor());
    }
  }

  public void restoreState()
  {
    String state = Settings.getInstance().getProperty(this.getClass().getName() + ".state", "0");
    int i = StringUtil.getIntValue(state, NORMAL);
    if (i == MAXIMIZED_BOTH)
    {
      setExtendedState(i);
    }
  }

  public final void restoreSettings()
  {
    Settings s = Settings.getInstance();

    if (!s.restoreWindowSize(this))
    {
      Dimension screenSize = WbSwingUtilities.getScreenSize();
      int w = (int)(screenSize.width * 0.75);
      int h = (int)(w * 0.75);
      this.setSize(w, h);
    }

    if (!s.restoreWindowPosition(this))
    {
      WbSwingUtilities.center(this, null);
    }
  }

  public void saveSettings()
  {
    Settings sett = Settings.getInstance();
    int state = this.getExtendedState();
    sett.setProperty(this.getClass().getName() + ".state", state);

    if (state != MAXIMIZED_BOTH)
    {
      sett.storeWindowPosition(this);
      sett.storeWindowSize(this);
    }
    boolean macroVisible = (showMacroPopup != null && showMacroPopup.isPopupVisible());
    sett.setProperty(this.getClass().getName() + ".macropopup.visible", macroVisible);
  }

  @Override
  public void windowOpened(WindowEvent windowEvent)
  {
  }

  @Override
  public void windowClosed(WindowEvent e)
  {
  }

  @Override
  public void windowDeiconified(WindowEvent windowEvent)
  {
  }

  @Override
  public void windowClosing(WindowEvent windowEvent)
  {
    WbManager.getInstance().closeMainWindow(this);
  }

  @Override
  public void windowDeactivated(WindowEvent windowEvent)
  {
    if (GuiSettings.installFocusManager())
    {
      WbKeyDispatcher.getInstance().grabActions(null, null);
    }
  }

  @Override
  public void windowActivated(WindowEvent windowEvent)
  {
    if (GuiSettings.installFocusManager())
    {
      WbKeyDispatcher.getInstance().grabActions(nextTab, prevTab);
    }
  }

  @Override
  public void windowIconified(WindowEvent windowEvent)
  {
  }

  /**
   * Display a message in the status bar
   */
  public void showStatusMessage(final String aMsg)
  {
    this.getCurrentPanel().filter(p -> p instanceof StatusBar).map(StatusBar.class::cast).ifPresent(status ->
    {
      WbSwingUtilities.invoke(() ->
      {
        if (StringUtil.isEmptyString(aMsg))
        {
          status.clearStatusMessage();
        }
        else
        {
          status.setStatusMessage(aMsg);
        }
      });
    });
  }

  public void showLogMessage(String aMsg)
  {
    this.getCurrentPanel().ifPresent(current -> current.showLogMessage(aMsg));
  }

  @Override
  public boolean connectBegin(final ConnectionProfile aProfile, final StatusBar info, final boolean loadWorkspace)
  {
    if (this.isBusy() || this.isCancelling())
    {
      WbSwingUtilities.showErrorMessageKey(this, "MsgDisconnectBusy");
      return false;
    }

    if (this.currentWorkspace != null && WbManager.getInstance().getSettingsShouldBeSaved())
    {
      if (!this.saveWorkspace(currentWorkspace.getFilename(), true))
      {
        return false;
      }
    }

    if (this.isConnected())
    {
      showDisconnectInfo();
    }
    disconnect(false, false, false, false);

    // it is important to set the connectInProgress flag,
    // otherwise loading the workspace will already trigger a
    // panel switch which might cause a connect
    // to the current profile before the ConnectionSelector
    // has actually finished.
    // this has to be set AFTER calling disconnect(), because
    // disconnect respects this flag and does nothing...
    this.setConnectIsInProgress();

    this.currentProfile = aProfile;

    showStatusMessage(ResourceMgr.getString("MsgLoadingWorkspace"));
    if (info != null)
    {
      info.setStatusMessage(ResourceMgr.getString("MsgLoadingWorkspace"));
    }

    if (loadWorkspace)
    {
      loadCurrentProfileWorkspace();
    }
    Settings.getInstance().setLastConnection(currentProfile);
    showStatusMessage(ResourceMgr.getFormattedString("MsgConnectingTo", currentProfile.getName()));
    return true;
  }

  public int getMacroClientId()
  {
    return windowId;
  }

  public String getWindowId()
  {
    return NumberStringCache.getNumberString(windowId);
  }

  private String getConnectionIdForPanel(String prefix, Optional<MainPanel> p)
  {
    if (!p.isPresent())
    {
      LogMgr.logError(new CallerInfo(){}, "Requested connection ID for NULL panel!", new Exception());
      return prefix;
    }
    if (GuiSettings.useTabIndexForConnectionId())
    {
      return prefix + " TAB-" + (getIndexForPanel(p) + 1);
    }
    return prefix + "-" + p.map(MainPanel::getId).orElse(null);
  }

  @Override
  public String getDefaultIconName()
  {
    return "workbench";
  }

  /**
   * Return the internal ID that should be used when connecting
   * to the given connection profile
   *
   * @return an id specific for the current tab or a "global" id the connection
   *         is shared between all tabs of this window
   */
  @Override
  public String getConnectionId(ConnectionProfile aProfile)
  {
    String prefix = getConnIdPrefix();
    if (aProfile != null && aProfile.getUseSeparateConnectionPerTab())
    {
      return getConnectionIdForPanel(prefix, this.getCurrentPanel());
    }
    return prefix;
  }

  private String getConnIdPrefix()
  {
    return "WbWin-" + getWindowId();
  }

  private ConnectionSelector getSelector()
  {
    if (connectionSelector == null)
    {
      connectionSelector = new ConnectionSelector(this, this);
    }
    return connectionSelector;
  }

  public void connectTo(ConnectionProfile profile, boolean showDialog, boolean loadWorkspace)
  {
    if (!ConnectionGuiHelper.doPrompt(this, profile))
    {
      LogMgr.logWarning("MainWindow.connectTo()", "Can't directly connect to a profile that requires prompting for a password or username");
      return;
    }
    getSelector().connectTo(profile, showDialog, loadWorkspace);
  }

  private void loadMacrosForProfile()
  {
    if (currentProfile == null) return;
    WbFile macroFile = currentProfile.getMacroFile();

    MacroManager.getInstance().removeChangeListener(this, getMacroClientId());

    if (macroFile != null && macroFile.exists())
    {
      MacroManager.getInstance().loadMacros(getMacroClientId(), macroFile);
    }
    else
    {
      MacroManager.getInstance().loadDefaultMacros(getMacroClientId());
    }
    macroListChanged();
    MacroManager.getInstance().addChangeListener(this, getMacroClientId());
  }

  /**
   * Call-back function which gets executed on the AWT thread after
   * the initial connection has been completed
   */
  @Override
  public void connected(WbConnection conn)
  {
    Optional<MainPanel> panel = this.getCurrentPanel();
    if (!panel.isPresent())
    {
      Thread.yield();
      panel = this.getCurrentPanel();
      LogMgr.logError(new CallerInfo(){}, "Connection established but no current panel!", new NullPointerException("Backtrace"));
    }

    if (this.currentProfile.getUseSeparateConnectionPerTab())
    {
      panel.ifPresent(p -> p.setConnection(conn));
    }
    else
    {
      this.setConnection(conn);
    }

    this.setMacroMenuEnabled(true);
    this.updateWindowTitle();

    this.dbExplorerAction.setEnabled(true);
    this.newDbExplorerPanel.setEnabled(true);
    this.newDbExplorerWindow.setEnabled(true);
    if (showDbTree != null) showDbTree.setEnabled(true);

    this.disconnectAction.setEnabled(true);
    this.reconnectAction.setEnabled(true);
    this.createNewConnection.checkState();
    this.disconnectTab.checkState();
    this.showMacroPopup.workspaceChanged();

    panel.ifPresent(p ->
    {
      p.clearLog();
      p.showResultPanel();
    });

    VersionNumber version = conn.getDatabaseVersion();
    showDbmsManual.setDbms(conn.getDbId(), version);

    connectionInfoAction.setEnabled(true);
    showConnectionWarnings(conn, panel);

    if (isDbTreeVisible())
    {
      if (DbTreeSettings.useTabConnection())
      {
        treePanel.setConnectionToUse(conn);
      }
      else
      {
        treePanel.connect(currentProfile);
      }
    }

    selectCurrentEditor();
  }

  @Override
  public void connectFailed(String error)
  {
    disconnected(true);
    tabSelected(0);

    if (error == null) return;
    WbSwingUtilities.showFriendlyErrorMessage(this, ResourceMgr.getString("ErrConnectFailed"), error.trim());
  }

  @Override
  public void connectCancelled()
  {
    if (this.exitOnCancel)
    {
      WbManager.getInstance().closeMainWindow(this);
    }
  }

  @Override
  public void connectEnded()
  {
    for (int i = 0; i < sqlTab.getTabCount(); i++)
    {
      getPanel(i).filter(p -> p instanceof StatusBar).map(StatusBar.class::cast).ifPresent(StatusBar::clearStatusMessage);
    }

    logVariables();
    this.clearConnectIsInProgress();
  }

  private LoadWorkspaceChoice checkNonExistingWorkspace()
  {
    return promptWorkspaceAction(ResourceMgr.getString("MsgProfileWorkspaceNotFound"));
  }

  private LoadWorkspaceChoice promptWorkspaceAction(String title)
  {
    String[] options = new String[] { ResourceMgr.getString("LblCreateWorkspace"), ResourceMgr.getString("LblLoadWorkspace"), ResourceMgr.getString("LblIgnore")};
    JOptionPane ignorePane = new JOptionPane(title, JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_CANCEL_OPTION, null, options);
    JDialog dialog = ignorePane.createDialog(this, ResourceMgr.TXT_PRODUCT_NAME);
    try
    {
      dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
      dialog.setResizable(true);
      dialog.pack();
      dialog.setVisible(true);
    }
    finally
    {
      dialog.dispose();
    }
    Object result = ignorePane.getValue();
    if (result == null || result.equals(options[0]))
    {
      return LoadWorkspaceChoice.CREATE;
    }
    else if (result.equals(options[1]))
    {
      return LoadWorkspaceChoice.LOAD_OTHER;
    }
    return LoadWorkspaceChoice.IGNORE;
  }

  private String getRealWorkspaceFilename(String filename)
  {
    if (filename == null) return filename;
    filename = FileDialogUtil.replaceConfigDir(filename);

    WbFile wfile = new WbFile(filename);
    if (!wfile.isAbsolute())
    {
      wfile = new WbFile(Settings.getInstance().getConfigDir(), filename);
      filename = wfile.getFullPath();
    }
    return filename;
  }

  public boolean loadWorkspace(String filename, boolean updateRecent)
  {
    if (this.isBusy())
    {
      WbSwingUtilities.showMessageKey(this, "ErrLoadWkspBusy");
      return false;
    }

    if (filename == null) return false;
    String realFilename = getRealWorkspaceFilename(filename);

    WbFile f = new WbFile(realFilename);

    if (!f.exists())
    {
      resetWorkspace(realFilename);
      return true;
    }

    WbWorkspace toLoad = null;
    boolean opened = false;
    while (!opened)
    {
      toLoad = new WbWorkspace(realFilename);
      try
      {
        opened = toLoad.openForReading();
      }
      catch (Throwable the)
      {
        opened = false;
      }

      if (!opened)
      {
        FileUtil.closeQuietely(toLoad);
        String msg = ResourceMgr.getFormattedString("ErrLoadingWorkspace", toLoad.getLoadError());
        LoadWorkspaceChoice choice = promptWorkspaceAction(msg);
        switch (choice)
        {
          case IGNORE:
            currentWorkspace = null;
            currentProfile.setWorkspaceFile(null);
            return false;
          case CREATE:
            resetWorkspace(realFilename);
            return true;
          case LOAD_OTHER:
            FileDialogUtil util = new FileDialogUtil();
            String fname = util.getWorkspaceFilename(this, false, true);
            realFilename = getRealWorkspaceFilename(fname);
        }
      }
    }

    if (toLoad != null)
    {
      final WbWorkspace wksp = toLoad;
      WbSwingUtilities.invoke(() ->
      {
        loadWorkspace(wksp, updateRecent);
      });
    }

    return currentWorkspace != null;
  }

  private void resetWorkspace(String realFilename)
  {
    // if the file does not exist, set all variables as if it did
    // thus the file will be created automatically.
    this.closeWorkspace();

    // resetWorkspace also sets currentWorkspace to null
    // but we want to prevent a workspace has been loaded here
    currentWorkspace = new WbWorkspace(realFilename);
    this.updateWindowTitle();
    this.checkWorkspaceActions();
  }

  private void loadWorkspace(WbWorkspace toLoad, boolean updateRecent)
  {
    final CallerInfo ci = new CallerInfo(){};
    long start = System.currentTimeMillis();
    try
    {
      removeAllPanels(false);

      // Ignore all stateChanged() events from the SQL Tab during loading
      setIgnoreTabChange(true);

      final int entryCount = toLoad.getEntryCount();

      for (int i = 0; i < entryCount; i++)
      {
        if (toLoad.getPanelType(i) == PanelType.dbExplorer)
        {
          newDbExplorerPanel(false);
        }
        else
        {
          addTabAtIndex(false, false, false, -1);
        }

        Optional<MainPanel> sqlPanel = getPanel(i);
        if (sqlPanel.isPresent())
        {
          MainPanel p = sqlPanel.get();
          ((JComponent)p).validate();
          p.readFromWorkspace(toLoad, i);
        }
      }

      if (entryCount == 0)
      {
        LogMgr.logWarning(ci, "No panels stored in the workspace: " + toLoad.getFilename());
        addTabAtIndex(false, false, false, -1);
      }
      // this needs to be done before checking workspace actions
      currentWorkspace = toLoad;

      renumberTabs();
      updateWindowTitle();
      checkWorkspaceActions();
      updateAddMacroAction();
      applyWorkspaceVariables();

      setIgnoreTabChange(false);

      int newIndex = entryCount > 0 ? toLoad.getSelectedTab() : 0;
      if (newIndex < sqlTab.getTabCount())
      {
        sqlTab.setSelectedIndex(newIndex);
      }

      Optional<MainPanel> p = getCurrentPanel();
      checkConnectionForPanel(p);
      setMacroMenuEnabled(true);
    }
    catch (Throwable e)
    {
      LogMgr.logWarning(ci, "Error loading workspace  " + toLoad.getFilename(), e);
      currentWorkspace = null;
    }
    finally
    {
      updateTabHistoryMenu();
      checkReloadWkspAction();
      setIgnoreTabChange(false);
      FileUtil.closeQuietely(toLoad);
      updateGuiForTab(sqlTab.getSelectedIndex());
    }

    if (updateRecent)
    {
      RecentFileManager.getInstance().workspaceLoaded(new WbFile(toLoad.getFilename()));
      EventQueue.invokeLater(this::updateRecentWorkspaces);
    }

    shouldShowTree = getToolProperties(DB_TREE_PROPS).getBoolProperty(DbTreePanel.PROP_VISIBLE, false);

    if (shouldShowTree && getCurrentSqlPanel() != null)
    {
      EventQueue.invokeLater(() ->
      {
        showDbTree(false);
        shouldShowTree = false;
      });
    }

    long duration = System.currentTimeMillis() - start;
    LogMgr.logDebug(new CallerInfo(){}, "Loading workspace " + currentWorkspace + " took " + duration + "ms");

    BookmarkManager.getInstance().updateInBackground(this);
  }

  private void applyWorkspaceVariables()
  {
    if (currentWorkspace == null) return;
    WbProperties variables = currentWorkspace.getVariables();
    if (CollectionUtil.isNonEmpty(variables))
    {
      LogMgr.logInfo(new CallerInfo(){}, "Applying variables defined in the workspace: " + variables);
      VariablePool.getInstance().readFromProperties(variables, "workspace " + currentWorkspace.getFilename());
    }
  }

  private void logVariables()
  {
    this.getCurrentPanel().ifPresent(p ->
    {
      StringBuilder msg = new StringBuilder();

      VariablePool vp = VariablePool.getInstance();
      if (currentProfile != null)
      {
        appendVariables(msg, vp.removeGlobalVars(currentProfile.getConnectionVariables()), ResourceMgr.getString("TxtProfile"));
      }
      if (currentWorkspace != null)
      {
        if (msg.length() > 0)
        {
          msg.append("\n");
        }
        appendVariables(msg, vp.removeGlobalVars(currentWorkspace.getVariables()), ResourceMgr.getString("TxtWorkspace"));
      }
      if (msg.length() > 0)
      {
        p.appendToLog(msg.toString());
      }
    });
  }

  private void appendVariables(StringBuilder msg, Properties variables, String source)
  {
    if (CollectionUtil.isNonEmpty(variables))
    {
      msg.append(ResourceMgr.getFormattedString("MsgVarsLoaded", source) + ":\n");
      Set<Map.Entry<Object, Object>> entrySet = variables.entrySet();
      for (Map.Entry<Object, Object> entry : entrySet)
      {
        msg.append(entry.getKey() + "=" + entry.getValue() + "\n");
      }
    }
  }

  private void checkReloadWkspAction()
  {
    String profileWkspName = currentProfile != null ? currentProfile.getWorkspaceFile() : null;

    if (StringUtil.isNonEmpty(profileWkspName))
    {
      boolean isProfileWorkspace = false;
      WbFile profileWksp = new WbFile(getRealWorkspaceFilename(profileWkspName));
      if (this.currentWorkspace != null)
      {
        WbFile current = new WbFile(currentWorkspace.getFilename());
        isProfileWorkspace = current.equals(profileWksp);
      }
      this.reloadWorkspace.setEnabled(!isProfileWorkspace);
    }
    else
    {
      this.reloadWorkspace.setEnabled(false);
    }
  }

  public void loadCurrentProfileWorkspace()
  {
    if (this.currentProfile == null)
    {
      LogMgr.logError(new CallerInfo(){}, "No current profile defined!", new IllegalStateException("No current profile"));
      return;
    }

    loadMacrosForProfile();

    String realFilename = null;
    boolean useDefault = false;
    String workspaceFilename = currentProfile.getWorkspaceFile();
    if (StringUtil.isBlank(workspaceFilename))
    {
      workspaceFilename = DEFAULT_WORKSPACE;
      useDefault = true;
    }

    realFilename = getRealWorkspaceFilename(workspaceFilename);

    WbFile f = new WbFile(realFilename);

    if (realFilename.length() > 0 && !f.exists())
    {
      LoadWorkspaceChoice choice = useDefault ? LoadWorkspaceChoice.CREATE : this.checkNonExistingWorkspace();
      switch (choice)
      {
        case LOAD_OTHER:
          FileDialogUtil util = new FileDialogUtil();
          workspaceFilename = util.getWorkspaceFilename(this, false, true);
          currentProfile.setWorkspaceFile(workspaceFilename);
          break;
        case IGNORE:
          workspaceFilename = null;
          currentProfile.setWorkspaceFile(null);
          break;
        default:
          // start with an empty workspace
          // and create a new workspace file.
          closeWorkspace();
      }
    }

    if (StringUtil.isNonBlank(workspaceFilename))
    {
      // loadWorkspace will replace the %ConfigDir% placeholder,
      // so we need to pass the original filename
      this.loadWorkspace(workspaceFilename, false);
    }
  }

  public void forceDisconnect()
  {
    if (this.isConnectInProgress()) return;

    saveWorkspace(false);

    setConnectIsInProgress();
    showDisconnectInfo();
    try
    {
      final List<WbConnection> toAbort = new ArrayList<>();

      for (int i = 0; i < this.sqlTab.getTabCount(); i++)
      {
        final MainPanel sql = (MainPanel)this.sqlTab.getComponentAt(i);
        if (sql == null) continue;

        if (isSQLPanel(sql))
        {
          ((SqlPanel)sql).forceAbort();
        }
        sql.disconnect();
        WbConnection con = sql.getConnection();
        if (con != null)
        {
          toAbort.add(con);
        }
        for (ToolWindow w : explorerWindows)
        {
          WbConnection conn = w.getConnection();
          if (conn != this.currentConnection && conn != null)
          {
            toAbort.add(conn);
          }
        }
      }
      closeExplorerWindows(false);
      WbThread abort = new WbThread("Abort connections")
      {
        @Override
        public void run()
        {
          ConnectionMgr.getInstance().abortAll(toAbort);
        }
      };
      abort.start();
    }
    finally
    {
      closeConnectingInfo();
      // this must be called on the AWT thread
      // and it must be called synchronously!
      WbSwingUtilities.invoke(() ->
      {
        disconnected(true);
      });
    }
  }

  @Override
  public void dispose()
  {
    if (treePanel != null)
    {
      treePanel.dispose();
    }
    sqlTab.removeAll();
    WbAction.dispose(
      this.assignWorkspaceAction, this.closeWorkspaceAction, this.reloadWorkspace, this.loadWorkspaceAction, this.saveAsWorkspaceAction, this.saveWorkspaceAction,
      this.dbExplorerAction, this.disconnectAction, this.reconnectAction, this.disconnectTab, this.createNewConnection,
      this.newDbExplorerPanel, this.newDbExplorerWindow, this.showDbTree, this.nextTab, this.prevTab, this.showDbmsManual,
      this.manageMacros, this.showMacroPopup, this.createMacro, this.loadMacros, this.saveMacros
    );
    for (JMenuBar bar : panelMenus)
    {
      disposeMenu(bar);
    }
    this.panelMenus.clear();
    this.explorerWindows.clear();
    JMenuBar bar = getJMenuBar();
    disposeMenu(bar);
    if (this.dropHandler != null)
    {
      this.dropHandler.dispose();
    }
    ShortcutManager.getInstance().removeChangeListener(this);
    super.dispose();
  }

  public void disconnect(final boolean background, final boolean closeWorkspace, final boolean saveWorkspace, final boolean showInfo)
  {
    if (this.isConnectInProgress())
    {
      LogMgr.logWarning(new CallerInfo(){}, "Cannot disconnect because a disconnect is already in progress");
      return;
    }

    setConnectIsInProgress();

    if (saveWorkspace) saveWorkspace(false);
    if (showInfo) showDisconnectInfo();

    Runnable run = () ->
    {
      try
      {
        doDisconnect();
        if (closeWorkspace) closeWorkspace(background);
      }
      finally
      {
        clearConnectIsInProgress();
        if (showInfo) closeConnectingInfo();
      }
    };

    if (background)
    {
      Thread t = new WbThread(run, "MainWindow Disconnect");
      t.start();
    }
    else
    {
      run.run();
    }
  }

  private void saveCache()
  {
    getCurrentPanel().map(MainPanel::getConnection).ifPresent(conn ->
    {
      DbObjectCacheFactory.getInstance().saveCache(conn);
    });
  }

  /**
   * This does the real disconnect action for all tabs.
   */
  protected void doDisconnect()
  {
    saveCache();

    try
    {
      if (treePanel != null)
      {
        treePanel.disconnect();
      }

      WbConnection conn = null;

      for (int i = 0; i < this.sqlTab.getTabCount(); i++)
      {
        final MainPanel panel = (MainPanel)this.sqlTab.getComponentAt(i);
        if (panel == null) continue;

        if (isSQLPanel(panel))
        {
          ((SqlPanel)panel).abortExecution();
        }
        conn = panel.getConnection();
        panel.disconnect();
        if (conn != null && !conn.isClosed())
        {
          showStatusMessage(ResourceMgr.getString("MsgDisconnecting"));
          conn.disconnect();
        }
      }
      closeExplorerWindows(true);
    }
    finally
    {
      // this must be called on the AWT thread
      // and it must be called synchronously!
      WbSwingUtilities.invoke(() ->
      {
        disconnected(false);
      });
    }
  }

  protected void disconnected(boolean closeWorkspace)
  {
    this.currentProfile = null;
    this.currentConnection = null;
    if (closeWorkspace)
    {
      this.closeWorkspace(false);
    }
    this.setMacroMenuEnabled(false);
    getJobIndicator().allJobsEnded();
    this.updateWindowTitle();
    this.disconnectAction.setEnabled(false);
    this.reconnectAction.setEnabled(false);
    this.showDbmsManual.clearDbms();
    connectionInfoAction.setEnabled(false);
    this.createNewConnection.checkState();
    this.disconnectTab.checkState();
    this.dbExplorerAction.setEnabled(false);
    this.newDbExplorerPanel.setEnabled(false);
    this.newDbExplorerWindow.setEnabled(false);
    if (showDbTree != null) showDbTree.setEnabled(false);
    this.showStatusMessage("");
    for (int i = 0; i < sqlTab.getTabCount(); i++)
    {
      sqlTab.setForegroundAt(i, null);
    }
  }

  public void abortAll()
  {
    try
    {
      for (int i = 0; i < this.sqlTab.getTabCount(); i++)
      {
        MainPanel sql = (MainPanel)this.sqlTab.getComponentAt(i);
        if (isSQLPanel(sql))
        {
          SqlPanel sp = (SqlPanel)sql;
          sp.forceAbort();
        }
      }
    }
    catch (Exception e)
    {
      LogMgr.logWarning(new CallerInfo(){}, "Error stopping execution", e);
    }
  }

  public void requestEditorFocus()
  {
    SqlPanel sql = this.getCurrentSqlPanel();
    if (sql != null)
    {
      sql.requestEditorFocus();
    }
  }

  private void selectCurrentEditor()
  {
    SqlPanel sql = this.getCurrentSqlPanel();
    if (sql != null)
    {
      sql.selectEditorLater();
    }
  }

  protected String getCurrentEditorFile()
  {
    String filename = null;

    SqlPanel sql = this.getCurrentSqlPanel();
    if (sql != null)
    {
      filename = sql.getCurrentFileName();
    }
    return filename;
  }

  protected synchronized RunningJobIndicator getJobIndicator()
  {
    if (this.jobIndicator == null)
    {
      this.jobIndicator = new RunningJobIndicator(this);
    }

    return this.jobIndicator;
  }

  protected void updateWindowTitle()
  {
    EventQueue.invokeLater(() ->
    {
      WindowTitleBuilder titleBuilder = new WindowTitleBuilder();
      String title1 = titleBuilder.getWindowTitle(currentProfile, getCurrentWorkspaceFile(), getCurrentEditorFile());
      setTitle(title1);
      getJobIndicator().baseTitleChanged();
    });
  }

  protected void closeConnectingInfo()
  {
    getSelector().closeConnectingInfo();
  }

  protected void showDisconnectInfo()
  {
    getSelector().showDisconnectInfo();
  }

  /**
   * Display a little PopupWindow to tell the user that the
   * workbench is currently connecting to the DB
   */
  protected void showConnectingInfo()
  {
    getSelector().showConnectingInfo();
  }

  private WbConnection getCurrentConnection()
  {
    if (currentConnection != null)
    {
      return currentConnection;
    }
    Optional<MainPanel> panel = getCurrentPanel();
    if (panel.isPresent())
    {
      return panel.get().getConnection();
    }
    return null;
  }

  private void setConnection(WbConnection con)
  {
    int count = this.sqlTab.getTabCount();
    if (con != null)
    {
      con.setShared(true);
    }

    for (int i = 0; i < count; i++)
    {
      MainPanel sql = (MainPanel)this.sqlTab.getComponentAt(i);
      sql.setConnection(con);
    }

    this.currentConnection = con;
    if (this.currentProfile == null && con != null)
    {
      this.currentProfile = con.getProfile();
    }
  }

  public void selectConnection()
  {
    selectConnection(false, false);
  }

  public void selectConnection(boolean exit, boolean checkExtDir)
  {
    if (checkExtDir)
    {
      ClasspathUtil cp = new ClasspathUtil();
      List<File> libs = cp.checkLibsToMove();
      if (libs.size() > 1)
      {
        String names = libs.stream().map(f -> "<li>" + f.getName() + "</li>").collect(Collectors.joining(""));
        WbFile extDir = new WbFile(cp.getJarPath(), "ext");
        String msg = ResourceMgr.getFormattedString("MsgExtDirWarning", cp.getJarPath(), names, extDir.getFullPath());
        WbSwingUtilities.showMessage(this, msg, JOptionPane.WARNING_MESSAGE);

        String logMsg = "Please move the following files to " + extDir.getFullPath() + "\n" +
          libs.stream().map(f -> f.getAbsolutePath()).collect(Collectors.joining("\n"));
        LogMgr.logWarning(new CallerInfo(){}, logMsg);
      }
    }
    exitOnCancel = exit;
    getSelector().selectConnection();
  }

  public JMenu getRecentWorkspaceMenu(int panelIndex)
  {
    return getSubMenuItem(ResourceMgr.MNU_TXT_WORKSPACE, "recent-workspace", panelIndex);
  }

  public JMenu getRecentFilesMenu(int panelIndex)
  {
    return getSubMenuItem(ResourceMgr.MNU_TXT_FILE, "recent-files", panelIndex);
  }

  private JMenu getSubMenuItem(String mainMenu, String itemName, int panelIndex)
  {
    JMenu main = this.getMenu(mainMenu, panelIndex);
    if (main == null) return null;
    int count = main.getItemCount();
    for (int i = 0; i < count; i++)
    {
      JMenuItem item = main.getItem(i);
      if (item == null) continue;
      if (itemName.equals(item.getName()))
      {
        return (JMenu)item;
      }
    }
    return null;
  }

  private JMenu getTabHistoryMenu(int panelIndex)
  {
    return this.getSubMenuItem(ResourceMgr.MNU_TXT_TOOLS, ResourceMgr.MNU_TXT_TAB_HISTORY, panelIndex);
  }

  public JMenu getMacroMenu(int panelIndex)
  {
    JMenu menu = this.getMenu(ResourceMgr.MNU_TXT_MACRO, panelIndex);
    return menu;
  }

  public JMenu getViewMenu(int panelIndex)
  {
    return this.getMenu(ResourceMgr.MNU_TXT_VIEW, panelIndex);
  }

  public JMenu getMenu(String aName, int panelIndex)
  {
    if (panelIndex < 0 || panelIndex >= this.panelMenus.size()) return null;
    if (aName == null) return null;
    JMenuBar menubar = this.panelMenus.get(panelIndex);
    int count = menubar.getMenuCount();
    for (int k = 0; k < count; k++)
    {
      JMenu item = menubar.getMenu(k);
      if (item == null) continue;
      if (aName.equals(item.getName())) return item;
    }
    return null;
  }

  public void updateTabHistoryMenu()
  {
    for (int i = 0; i < getTabCount(); i++)
    {
      JMenu menu = getTabHistoryMenu(i);
      closedTabHistory.updateMenu(menu);
    }
  }

  protected void updateRecentWorkspaces()
  {
    for (int i = 0; i < getTabCount(); i++)
    {
      JMenu menu = getRecentWorkspaceMenu(i);
      RecentFileManager.getInstance().populateRecentWorkspaceMenu(menu, this);
    }
  }

  protected void updateRecentFiles()
  {
    for (int i = 0; i < getTabCount(); i++)
    {
      JMenu menu = getRecentFilesMenu(i);
      RecentFileManager.getInstance().populateRecentFilesMenu(menu, this);
    }
  }

  protected void updateViewMenu(int sqlTabIndex, String aName)
  {
    int panelCount = this.panelMenus.size();
    if (aName == null) aName = ResourceMgr.getDefaultTabLabel();
    for (int i = 0; i < panelCount; i++)
    {
      JMenu view = this.getViewMenu(i);
      if (view == null) continue;

      int count = view.getItemCount();
      for (int k = 0; k < count; k++)
      {
        JMenuItem item = view.getItem(k);
        if (item == null) continue;
        Action ac = item.getAction();
        if (ac == null) continue;

        if (ac instanceof SelectTabAction)
        {
          SelectTabAction a = (SelectTabAction)ac;
          if (a.getIndex() == sqlTabIndex)
          {
            a.setMenuText(aName);
            break;
          }
        }
      }
      WbSwingUtilities.repaintNow(view);
    }
  }

  /**
   * Add the approriate menu item to select a given tab
   * to the View menu.
   */
  public void addToViewMenu(SelectTabAction anAction)
  {
    int panelCount = this.panelMenus.size();
    int lastActionIndex = -1;

    SelectTabAction lastAction = null;

    for (int i = 0; i < panelCount; i++)
    {
      JMenu view = this.getViewMenu(i);

      // insert the item at the correct index
      // (if it is a SelectTabAction)
      // otherwise insert it after the last SelectTabAction
      int count = view.getItemCount();
      int inserted = -1;
      for (int k = 0; k < count; k++)
      {
        JMenuItem item = view.getItem(k);
        if (item == null) continue;
        Action ac = item.getAction();
        if (ac == null) continue;
        if (!(ac instanceof SelectTabAction))
        {
          break;
        }
        SelectTabAction a = (SelectTabAction)ac;
        lastAction = a;
        lastActionIndex = k;

        if (a.getIndex() > anAction.getIndex())
        {
          view.insert(anAction, k);
          inserted = k;
          break;
        }
      }

      if (inserted == -1)
      {
        if (lastActionIndex == -1)
        {
          // no index found which is greater than or equal to the new one
          // so add it to the end
          if (!(view.getItem(count - 1).getAction() instanceof SelectTabAction))
          {
            view.addSeparator();
          }
          view.add(anAction);
        }
        else if (lastAction != null && lastAction.getIndex() != anAction.getIndex())
        {
          // we found at least one SelectTabAction, so we'll
          // insert the new one right behind the last one.
          // (there might be other items in the view menu!)

          view.insert(anAction, lastActionIndex + 1);
        }
      }
      else
      {
        // renumber the shortcuts for the remaining actions
        int newIndex = anAction.getIndex() + 1;
        for (int k = inserted + 1; k < panelCount; k++)
        {
          SelectTabAction a = (SelectTabAction)view.getItem(k).getAction();
          a.setNewIndex(newIndex);
          newIndex++;
        }
      }
    }
  }

  /**
   * Tell all SelectTabAction that the shortcuts have changed, so that they can update accordingly.
   *
   * @see SelectTabAction#setNewIndex(int)
   */
  private void updateViewShortcuts()
  {
    int panelCount = sqlTab.getTabCount();
    for (int i = 0; i < panelCount; i++)
    {
      JMenu view = this.getViewMenu(i);
      for (int k = 0; k < panelCount; k++)
      {
        JMenuItem item = view.getItem(k);
        Action a = item.getAction();
        if (a instanceof SelectTabAction)
        {
          SelectTabAction tab = (SelectTabAction)a;
          tab.setNewIndex(k);
        }
      }
    }
  }

  private WbConnection getConnectionForTab(Optional<MainPanel> aPanel, boolean returnNew)
    throws Exception
  {
    if (this.currentConnection != null && !returnNew) return this.currentConnection;
    String id = this.getConnectionIdForPanel(getConnIdPrefix(), aPanel);

    StatusBar status = aPanel.filter(p -> p instanceof StatusBar).map(StatusBar.class::cast).orElse(null);
    if (status != null)
    {
      status.setStatusMessage(ResourceMgr.getFormattedString("MsgConnectingTo", this.currentProfile.getName()));
    }

    ConnectionMgr mgr = ConnectionMgr.getInstance();
    WbConnection conn = null;
    try
    {
      conn = mgr.getConnection(this.currentProfile, id);
      conn.setShared(false);
    }
    finally
    {
      if (status != null) status.clearStatusMessage();
    }
    showConnectionWarnings(conn, aPanel);
    return conn;
  }

  private void showConnectionWarnings(WbConnection conn, Optional<MainPanel> panel)
  {
    panel.ifPresent(p ->
    {
      String warn = (conn != null ? conn.getWarnings() : null);
      if (warn != null)
      {
        p.showResultPanel();
        p.showLogMessage(ResourceMgr.getString("MsgConnectMsg") + "\n");
        p.appendToLog(warn);
      }
    });
  }

  public void addDbExplorerTab(DbExplorerPanel explorer)
  {
    JMenuBar dbmenu = this.createMenuForPanel(explorer);

    this.sqlTab.add(explorer);

    explorer.setTabTitle(this.sqlTab, this.sqlTab.getTabCount() - 1);

    SelectTabAction action = new SelectTabAction(this.sqlTab, this.sqlTab.getTabCount() - 1);
    action.setMenuText(explorer.getTabTitle());
    this.panelMenus.add(dbmenu);
    this.addToViewMenu(action);
  }

  public List<ToolWindow> getExplorerWindows()
  {
    return Collections.unmodifiableList(explorerWindows);
  }

  public void closeExplorerWindows(boolean doDisconnect)
  {
    try
    {
      List<ToolWindow> copy = new ArrayList<>(explorerWindows);
      for (ToolWindow w : copy)
      {
        WbConnection conn = w.getConnection();
        if (conn != null && doDisconnect && conn != this.currentConnection)
        {
          conn.disconnect();
        }
        w.closeWindow();
      }
    }
    catch (Throwable th)
    {
      LogMgr.logError(new CallerInfo(){}, "Error when closing explorer windows", th);
    }
  }

  public void closeOtherPanels(Optional<MainPanel> toKeepOpt)
  {
    if (GuiSettings.getConfirmMultipleTabClose())
    {
      boolean doClose = WbSwingUtilities.getYesNo(sqlTab, ResourceMgr.getString("MsgConfirmCloseOtherTabs"));
      if (!doClose) return;
    }

    MainPanel toKeep = toKeepOpt.orElse(null);
    boolean inProgress = connectInProgress;
    if (!inProgress) this.setConnectIsInProgress();

    try
    {
      setIgnoreTabChange(true);
      int index = 0;
      while (index < sqlTab.getTabCount())
      {
        MainPanel p = getPanel(index).orElse(null);

        if (p != null && p != toKeep && !p.isLocked())
        {
          if (p.isModified())
          {
            // if the panel is modified the user will be asked
            // if the panel should really be closed, in that
            // case I think it makes sense to make that panel the current panel
            selectTab(index);
            // tabSelected will not be run because tabRemovalInProgress == true
            tabSelected(index);
          }
          if (p.canClosePanel(true))
          {
            removeTab(index, false, true);
          }
          else
          {
            // if canCloseTab() returned false, then the user
            // selected "Cancel" which means stop closing panels
            // if the user selected "No" canCloseTab() will return "true"
            // to indicate whatever is in progress can go on.
            break;
          }
        }
        else
        {
          index++;
        }
      }
      renumberTabs();
      // make sure the toolbar and menus are updated correctly
      updateCurrentTab(getCurrentPanelIndex());
      tabHistory.clear();
    }
    catch (Exception e)
    {
      LogMgr.logError(new CallerInfo(){}, "Error when removing all panels", e);
    }
    finally
    {
      setIgnoreTabChange(false);
      if (!inProgress) clearConnectIsInProgress();
    }
  }

  protected void removeAllPanels(boolean keepOne)
  {
    boolean inProgress = connectInProgress;
    if (!inProgress) this.setConnectIsInProgress();

    BookmarkManager.getInstance().clearBookmarksForWindow(getWindowId());

    try
    {
      setIgnoreTabChange(true);
      closeDbTree();
      int keep = (keepOne ? 1 : 0);
      while (sqlTab.getTabCount() > keep)
      {
        // I'm not using removeCurrentTab() as that will also
        // update the GUI and immediately check for a new
        // connection which is not necessary when removing all tabs.
        removeTab(keep, false, false);
      }

      // Reset the first panel, now we have a "clean" workspace
      if (keepOne)
      {
        Optional<MainPanel> p = getPanel(0);
        if (!p.isPresent())
        {
          addTabAtIndex(false, false, false, -1);
          p = getPanel(0);
        }
        else
        {
          p.get().reset();
        }
        resetTabTitles();

        // make sure the toolbar and menus are updated correctly
        updateCurrentTab(0);
      }
      tabHistory.clear();
    }
    catch (Exception e)
    {
      LogMgr.logError(new CallerInfo(){}, "Error when removing all panels", e);
    }
    finally
    {
      setIgnoreTabChange(false);
      if (!inProgress) clearConnectIsInProgress();
    }
  }

  /**
   * Returns the index of the first explorer tab
   */
  public int findFirstExplorerTab()
  {
    int count = this.sqlTab.getTabCount();
    if (count <= 0) return -1;

    for (int i = 0; i < count; i++)
    {
      Component c = this.sqlTab.getComponentAt(i);
      if (c instanceof DbExplorerPanel) return i;
    }
    return -1;
  }

  public void newDbExplorerWindow()
  {
    DbExplorerPanel explorer = new DbExplorerPanel(this);
    explorer.restoreSettings();
    DbExplorerWindow w = explorer.openWindow(this.currentProfile);

    boolean useNewConnection = DbExplorerSettings.getAlwaysUseSeparateConnForDbExpWindow() ||
       currentProfile.getUseSeparateConnectionPerTab() ||
       currentConnection == null;

    if (useNewConnection)
    {
      explorer.connect(this.currentProfile);
    }
    else
    {
      LogMgr.logDebug(new CallerInfo(){}, "Re-using current connection for DbExplorer Window");
      explorer.setConnection(this.currentConnection);
    }
    explorerWindows.add(w);
  }

  public void explorerWindowClosed(DbExplorerWindow w)
  {
    explorerWindows.remove(w);
  }

  public void newDbExplorerPanel(boolean select)
  {
    DbExplorerPanel explorer = new DbExplorerPanel(this);
    explorer.restoreSettings();
    this.addDbExplorerTab(explorer);
    if (select)
    {
      // Switching to the new tab will initiate the connection if necessary
      this.sqlTab.setSelectedIndex(this.sqlTab.getTabCount() - 1);
    }
  }

  public ConnectionProfile getCurrentProfile()
  {
    return this.currentProfile;
  }

  public JMenu buildHelpMenu()
  {
    JMenu result = new WbMenu(ResourceMgr.getString(ResourceMgr.MNU_TXT_HELP));
    result.setName(ResourceMgr.MNU_TXT_HELP);
    new ShowHelpAction().addToMenu(result);
    new ShowManualAction().addToMenu(result);
    result.add(showDbmsManual);
    result.add(connectionInfoAction);
    result.addSeparator();
    result.add(new HelpContactAction(this));
    result.add(WhatsNewAction.getInstance());
    result.addSeparator();

    result.add(ViewLogfileAction.getInstance());
    result.add(new VersionCheckAction());
    result.add(new AboutAction(this));

    return result;
  }

  /**
   * Create the tools menu for a panel menu. This will be called
   * for each panel that gets added to the main window.
   * Actions that are singletons (like the db explorer stuff)
   * should not be created here
   */
  public JMenu buildToolsMenu()
  {
    JMenu result = new WbMenu(ResourceMgr.getString(ResourceMgr.MNU_TXT_TOOLS));
    result.setName(ResourceMgr.MNU_TXT_TOOLS);

    result.add(this.dbExplorerAction);
    result.add(this.newDbExplorerPanel);
    result.add(this.newDbExplorerWindow);
    if (showDbTree != null)
    {
      result.add(this.showDbTree);
    }
    result.addSeparator();

    result.add(new DataPumperAction(this));
    result.add(new ObjectSearchAction(this));

    result.addSeparator();
    result.add(new BookmarksAction(this));
    result.add(new SearchAllEditorsAction(this));
    JMenu history = new WbMenu(ResourceMgr.getString(ResourceMgr.MNU_TXT_TAB_HISTORY));
    history.setName(ResourceMgr.MNU_TXT_TAB_HISTORY);
    result.add(history);

    result.addSeparator();
    new OptionsDialogAction().addToMenu(result);
    new ConfigureShortcutsAction().addToMenu(result);
    new ConfigureToolbarAction().addToMenu(result);

    return result;
  }

  private boolean checkMakeProfileWorkspace()
  {
    boolean assigned = false;
    boolean saveIt = WbSwingUtilities.getYesNo(this, ResourceMgr.getString("MsgAttachWorkspaceToProfile"));
    if (saveIt)
    {
      this.assignWorkspace();
      assigned = true;
    }
    return assigned;
  }

  /**
   * Sets the default title for all tab titles
   */
  private void resetTabTitles()
  {
    String defaultTitle = ResourceMgr.getDefaultTabLabel();
    int count = this.sqlTab.getTabCount();
    for (int i = 0; i < count; i++)
    {
      final int fi = i;
      getSqlPanel(i).
        ifPresent(panel ->
        {
          panel.closeFile(true, false);
          this.setTabTitle(fi, defaultTitle);
        });
    }
  }

  public boolean isCancelling()
  {
    int count = this.sqlTab.getTabCount();
    for (int i = 0; i < count; i++)
    {
      if (this.getPanel(i).map(MainPanel::isCancelling).orElse(false)) return true;
    }
    return false;
  }

  public boolean isConnected()
  {
    if (this.currentConnection != null)
    {
      return true;
    }
    int count = this.sqlTab.getTabCount();
    for (int i = 0; i < count; i++)
    {
      if (this.getPanel(i).map(MainPanel::isConnected).orElse(false)) return true;
    }
    return false;
  }

  /**
   * Returns true if at least one of the SQL panels is currently
   * executing a SQL statement.
   * This method calls isBusy() for each tab.
   */
  public boolean isBusy()
  {
    int count = this.sqlTab.getTabCount();
    for (int i = 0; i < count; i++)
    {
      if (this.getPanel(i).map(MainPanel::isBusy).orElse(false)) return true;
    }
    return false;
  }

  public void replaceWorkspaceVariables(Properties newVars)
  {
    if (currentWorkspace == null) return;
    currentWorkspace.setVariables(newVars);
  }

  public Properties getCurrentWorkspaceVariables()
  {
    if (currentWorkspace == null) return null;
    return currentWorkspace.getVariables();
  }

  public String getCurrentWorkspaceFile()
  {
    if (currentWorkspace == null) return null;
    return this.currentWorkspace.getFilename();
  }

  public void loadWorkspace()
  {
    this.saveWorkspace();
    FileDialogUtil dialog = new FileDialogUtil();
    String filename = dialog.getWorkspaceFilename(this, false, true);
    if (filename == null) return;
    boolean loaded = this.loadWorkspace(filename, true);
    if (loaded && Settings.getInstance().getBoolProperty("workbench.gui.workspace.load.askassign", true))
    {
      checkMakeProfileWorkspace();
    }
    WbSwingUtilities.repaintLater(this);
  }

  /**
   * Close the current worksace without checking if the panels can be closed.
   *
   * @see #closeWorkspace(boolean)
   */
  private void closeWorkspace()
  {
    this.closeWorkspace(false);
  }

  /**
   * Closes the current workspace.
   * <p>
   * All editor tabs are closed except for one. The SQL history for the tab will be emptied.
   * And the association with the current workspace filename will be "forgotten".
   * <p>
   * If checkUnsaved is true, each panel will be asked if it can be closed. If at least one panel
   * refuses to close, the workspace will <b>not</b> be closed.
   *
   * @param checkUnsaved if true editor tabs are checked if they can be closed.
   *
   * @return true if the workspace was closed
   *
   * @see MainPanel#canClosePanel(boolean)
   * @see #removeAllPanels(boolean)
   * @see #updateWindowTitle()
   * @see #checkWorkspaceActions()
   */
  public boolean closeWorkspace(boolean checkUnsaved)
  {
    if (checkUnsaved)
    {
      int count = this.sqlTab.getTabCount();
      boolean first = true;
      final boolean connectionAvailable = currentConnection != null;

      for (int i = 0; i < count; i++)
      {
        Optional<MainPanel> p = getPanel(i);
        if (connectionAvailable)
        {
          first = i == 0;
        }
        if (p.isPresent() && !p.get().canClosePanel(first))
        {
          return false;
        }
      }
    }

    WbSwingUtilities.invoke(() ->
    {
      try
      {
        removeAllPanels(true);
      }
      catch (Exception e)
      {
        LogMgr.logError(new CallerInfo(){}, "Error when resetting workspace", e);
      }
      updateWindowTitle();
      checkWorkspaceActions();
    });

    if (currentWorkspace != null)
    {
      VariablePool.getInstance().removeVariables(currentWorkspace.getVariables());
    }
    this.currentWorkspace = null;

    return true;
  }

  /**
   * This will assign the current workspace name to the current profile.
   */
  public void assignWorkspace()
  {
    if (this.currentWorkspace == null) return;
    if (this.currentProfile == null) return;
    FileDialogUtil util = new FileDialogUtil();
    String filename = FileDialogUtil.removeConfigDir(currentWorkspace.getFilename());
    currentProfile.setWorkspaceFile(filename);

    // The MainWindow gets a copy of the profile managed by the ConnectionMgr
    // so we need to update that one as well.
    ConnectionProfile realProfile = ConnectionMgr.getInstance().getProfile(currentProfile.getKey());
    if (realProfile != null)
    {
      realProfile.setWorkspaceFile(filename);
    }
    this.updateWindowTitle();
  }

  /**
   * Save the currently loaded workspace.
   */
  public boolean saveWorkspace()
  {
    return saveWorkspace(true);
  }

  public boolean saveWorkspace(boolean checkUnsaved)
  {
    if (this.currentWorkspace != null)
    {
      return this.saveWorkspace(currentWorkspace.getFilename(), checkUnsaved);
    }
    return true;
  }

  /**
   * Saves the current SQL history to a workspace with the given filename
   * If filename == null, a SaveAs dialog will be displayed.
   * <p>
   * If the workspace is saved with a new name (filename == null) the user
   * will be asked if the workspace should be assigned to the current profile
   */
  public boolean saveWorkspace(String filename, boolean checkUnsaved)
  {
    if (!WbManager.getInstance().getSettingsShouldBeSaved()) return true;

    final CallerInfo ci = new CallerInfo(){};
    if (currentWorkspace != null && currentWorkspace.isOpen())
    {
      LogMgr.logWarning(ci,
        "Current workspace " + currentWorkspace.getFilename() + " is already open in window \"" + getTitle() + "\". Can't save it now.",
        new Exception("Backtrace"));
      return true;
    }

    boolean interactive = false;

    if (filename == null)
    {
      interactive = true;
      FileDialogUtil util = new FileDialogUtil();
      filename = util.getWorkspaceFilename(this, true, true);
      if (filename == null) return true;
    }

    String realFilename = getRealWorkspaceFilename(filename);
    WbFile workspaceFile = new WbFile(realFilename);

    if (currentWorkspace == null)
    {
      currentWorkspace = new WbWorkspace(realFilename);
    }
    else
    {
      currentWorkspace.setFilename(realFilename);
      currentWorkspace.prepareForSaving();
    }

    File backupFile = null;
    boolean deleteBackup = false;
    boolean restoreBackup = false;
    boolean createTempBackup = !Settings.getInstance().getCreateWorkspaceBackup();

    if (Settings.getInstance().getCreateWorkspaceBackup())
    {
      int maxVersions = Settings.getInstance().getMaxBackupFiles();
      String dir = Settings.getInstance().getBackupDir();
      String sep = Settings.getInstance().getFileVersionDelimiter();
      FileVersioner version = new FileVersioner(maxVersions, dir, sep);
      try
      {
        backupFile = version.createBackup(workspaceFile);
      }
      catch (IOException e)
      {
        LogMgr.logWarning(ci, "Error when creating backup file!", e);
        createTempBackup = true;
      }
    }

    if (createTempBackup)
    {
      // create a backup of the current workspace in order to preserve it
      // in case something goes wrong when writing the new workspace, at least the last good version can be restored
      backupFile = workspaceFile.makeBackup();
      LogMgr.logDebug(ci, "Created temporary backup file: " + backupFile);
      deleteBackup = true;
    }

    this.showMacroPopup.saveWorkspaceSettings();

    getToolProperties(DB_TREE_PROPS).setProperty(DbTreePanel.PROP_VISIBLE, isDbTreeVisible());

    if (treePanel != null)
    {
      treePanel.saveSettings(getToolProperties(DB_TREE_PROPS));
    }

    try
    {
      int count = this.sqlTab.getTabCount();

      if (checkUnsaved)
      {
        boolean first = true;
        for (int i = 0; i < count; i++)
        {
          MainPanel p = (MainPanel)this.sqlTab.getComponentAt(i);
          if (currentConnection != null)
          {
            // for a global connection only the first tab needs to check open transactions
            first = (i == 0);
          }
          if (!p.canClosePanel(first)) return false;
        }
      }

      currentWorkspace.setSelectedTab(sqlTab.getSelectedIndex());
      currentWorkspace.setEntryCount(count);
      for (int i = 0; i < count; i++)
      {
        Optional<MainPanel> p = getPanel(i);
        if (p.isPresent())
        {
          p.get().storeInWorkspace(currentWorkspace, i);
        }
      }

      currentWorkspace.openForWriting();
      currentWorkspace.save();

      LogMgr.logDebug(ci, "Workspace " + workspaceFile + " saved");
      if (deleteBackup && backupFile != null)
      {
        LogMgr.logDebug(ci, "Deleting temporary backup file: " + backupFile.getAbsolutePath());
        backupFile.delete();
      }
    }
    catch (Throwable e)
    {
      LogMgr.logError(ci, "Error saving workspace: " + realFilename, e);
      WbSwingUtilities.showErrorMessage(this, ResourceMgr.getString("ErrSavingWorkspace") + "\n" + ExceptionUtil.getDisplay(e));
      restoreBackup = true;
    }
    finally
    {
      FileUtil.closeQuietely(currentWorkspace);
    }

    if (restoreBackup && backupFile != null)
    {
      LogMgr.logWarning(ci, "Restoring the old workspace file from backup: " + backupFile.getAbsolutePath());
      FileUtil.copySilently(backupFile, workspaceFile);
    }

    if (interactive)
    {
      this.checkMakeProfileWorkspace();
      RecentFileManager.getInstance().workspaceLoaded(workspaceFile);
      EventQueue.invokeLater(this::updateRecentWorkspaces);
    }

    this.updateWindowTitle();
    this.checkWorkspaceActions();
    return true;
  }

  /**
   * Invoked when the a different SQL panel has been selected.
   * <p>
   * This fires the tabSelected() method but only if ignoreTabChange is not set true.
   *
   * @param e a ChangeEvent object
   *
   */
  @Override
  public void stateChanged(ChangeEvent e)
  {
    if (e.getSource() == this.sqlTab)
    {
      if (this.ignoreTabChange) return;
      int index = this.sqlTab.getSelectedIndex();
      this.tabSelected(index);
    }
    else if (e.getSource() == ShortcutManager.getInstance())
    {
      updateViewShortcuts();
    }
  }

  public MainPanel insertTab()
  {
    return addTab(true, true, false, true);
  }

  public MainPanel addTab()
  {
    return this.addTab(true, true, true, true);
  }

  /**
   * Add a SqlPanel to this window.
   *
   * @param checkConnection if true, the panel will automatically be connected
   *                        this is important if a Profile is used where each panel gets its own
   *                        connection
   * @param append          if true, the tab will be appended at the end (after all other tabs), if false will be
   *                        inserted before the current tab.
   * @param renumber        should the tabs be renumbered after adding the new tab. If several tabs are added
   *                        in a loop renumber is only necessary at the end
   *
   * @see #renumberTabs()
   * @see #checkConnectionForPanel(workbench.interfaces.MainPanel)
   */
  public MainPanel addTab(boolean selectNew, boolean checkConnection, boolean append, boolean renumber)
  {
    int index = -1;
    if (append)
    {
      index = findFirstExplorerTab();
      if (index < sqlTab.getTabCount() - 1)
      {
        index = -1;
      }
    }
    else
    {
      index = this.sqlTab.getSelectedIndex() + 1;
    }

    try
    {
      setIgnoreTabChange(true);
      MainPanel p = addTabAtIndex(selectNew, checkConnection, renumber, index);
      if (selectNew)
      {
        currentTabChanged();
      }
      return p;
    }
    finally
    {
      setIgnoreTabChange(false);
    }
  }

  public SqlPanel restoreTab(int oldIndex)
  {
    try
    {
      setIgnoreTabChange(true);
      MainPanel p = addTabAtIndex(true, true, true, oldIndex);
      currentTabChanged();
      return (SqlPanel)p;
    }
    finally
    {
      setIgnoreTabChange(false);
    }
  }

  private MainPanel addTabAtIndex(boolean selectNew, boolean checkConnection, boolean renumber, int index)
  {
    final SqlPanel sql = new SqlPanel(getMacroClientId());
    sql.registerObjectFinder(treePanel);
    addTabAtIndex(sql, selectNew, checkConnection, renumber, index);
    return sql;
  }

  private void addTabAtIndex(SqlPanel sql, boolean selectNew, boolean checkConnection, boolean renumber, int index)
  {
    if (index == -1) index = sqlTab.getTabCount();

    sql.setConnectionClient(this);
    sql.addDbExecutionListener(this);
    sql.addFilenameChangeListener(this);
    this.sqlTab.add(sql, index);
    sql.setTabTitle(sqlTab, index);

    JMenuBar menuBar = this.createMenuForPanel(sql);
    this.panelMenus.add(index, menuBar);

    if (checkConnection) this.checkConnectionForPanel(Optional.of(sql));

    this.setMacroMenuEnabled(sql.isConnected());

    if (renumber) this.renumberTabs();
    sql.initDivider(sqlTab.getHeight() - sqlTab.getTabHeight());

    if (selectNew)
    {
      // if no connection was created initially the switch to a new
      // panel will initiate the connection.
      this.sqlTab.setSelectedIndex(index);
    }

    if (sqlTab.getTabCount() > 0)
    {
      sqlTab.setCloseButtonEnabled(0, this.sqlTab.getTabCount() > 1);
    }
  }

  public void jumpToBookmark(final NamedScriptLocation bookmark)
  {
    if (bookmark == null) return;
    final int index = getTabIndexById(bookmark.getTabId());
    if (index < 0)
    {
      LogMgr.logWarning(new CallerInfo(){}, "Tab with ID=" + bookmark.getTabId() + " not found!");
      return;
    }
    final Optional<MainPanel> p = getPanel(index);
    final boolean selectTab = index != sqlTab.getSelectedIndex();
    EventQueue.invokeLater(() ->
    {
      if (selectTab)
      {
        selectTab(index);
        invalidate();
      }
      p.ifPresent(pan -> pan.jumpToBookmark(bookmark));
    });
  }

  /**
   * Returns the real title of a tab (without the index number or any formatting)
   *
   * @see MainPanel#getTabTitle()
   */
  public String getTabTitle(int index)
  {
    return getPanel(index).map(MainPanel::getTabTitle).orElse(null);
  }

  private int getTabIndexById(String tabId)
  {
    int count = sqlTab.getTabCount();
    for (int i = 0; i < count; i++)
    {
      String id = getPanel(i).map(MainPanel::getId).orElse(null);
      if (id != null && id.equals(tabId)) return i;
    }
    return -1;
  }

  /**
   * Returns the tab title as displayed to the user (including the index number).
   *
   * @param tabId the ID of the panel
   *
   * @return the title as shown to the user
   */
  public String getTabTitleById(String tabId)
  {
    int index = getTabIndexById(tabId);
    if (index > -1)
    {
      return HtmlUtil.cleanHTML(sqlTab.getTitleAt(index));
    }
    return null;
  }

  /**
   * Returns the title of the currently selected tab.
   *
   * @see #getTabTitle(int)
   * @see MainPanel#getTabTitle()
   */
  @Override
  public String getCurrentTabTitle()
  {
    int index = this.sqlTab.getSelectedIndex();
    return this.getTabTitle(index);
  }

  @Override
  public void setCurrentTabTitle(String newName)
  {
    int index = this.sqlTab.getSelectedIndex();

    if (newName != null)
    {
      this.setTabTitle(index, newName);
    }
  }

  /**
   * Sets the title of a tab and appends the index number to
   * the title, so that a shortcut Ctrl-n can be defined
   */
  public void setTabTitle(int anIndex, String aName)
  {
    this.getPanel(anIndex).ifPresent(p ->
    {
      p.setTabName(aName);
      p.setTabTitle(this.sqlTab, anIndex);
      updateViewMenu(anIndex, p.getTabTitle());
    });
  }

  public void removeLastTab(boolean includeExplorer)
  {
    int index = this.sqlTab.getTabCount() - 1;
    if (!includeExplorer)
    {
      while (this.getPanel(index).get() instanceof DbExplorerPanel)
      {
        index--;
      }
    }
    this.tabCloseButtonClicked(index);
  }

  /**
   * Checks if the current tab is locked, or if it is the
   * last tab that is open.
   * <br/>
   * This does not check if the user actually wants to close
   * the tab!
   *
   * @return boolean if the current tab could be closed
   */
  public boolean canCloseTab()
  {
    int index = sqlTab.getSelectedIndex();
    return canCloseTab(index);
  }

  @Override
  public boolean canCloseTab(int index)
  {
    if (index < 0) return false;
    Optional<MainPanel> panel = this.getPanel(index);

    if (!panel.isPresent() || panel.get().isLocked()) return false;

    int numTabs = sqlTab.getTabCount();
    return numTabs > 1;
  }

  @Override
  public Component getComponent()
  {
    return this;
  }

  @Override
  public boolean canRenameTab()
  {
    return (this.currentWorkspace != null);
  }

  /**
   * Closes the currently active tab.
   *
   * @see #tabCloseButtonClicked(int)
   */
  public void removeCurrentTab()
  {
    int index = this.sqlTab.getSelectedIndex();
    this.tabCloseButtonClicked(index);
  }

  private void renumberTabs()
  {
    int count = this.sqlTab.getTabCount();
    for (int i = 0; i < count; i++)
    {
      final int fi = i;
      this.getPanel(i).ifPresent(p -> p.setTabTitle(sqlTab, fi));
    }
    for (int panel = 0; panel < count; panel++)
    {
      rebuildViewMenu(panel);
    }
  }

  /**
   * Rebuild the part of the view menu that handles the
   * selecting of tabs
   */
  private void rebuildViewMenu(int panel)
  {
    JMenu menu = this.getViewMenu(panel);
    JMenuItem item = menu.getItem(0);
    while (item != null && (item.getAction() instanceof SelectTabAction))
    {
      item.removeAll();
      menu.remove(0);
      item = menu.getItem(0);
    }
    int count = this.sqlTab.getTabCount();
    for (int i = 0; i < count; i++)
    {
      SelectTabAction a = new SelectTabAction(sqlTab, i);
      a.setMenuText(getTabTitle(i));
      menu.insert(a, i);
    }
    if (this.sqlTab.getSelectedIndex() == panel)
    {
      WbSwingUtilities.repaintNow(menu);
    }
  }

  /**
   * Moves the current sql tab to the left (i.e. index := index - 1)
   * If index == 0 nothing happens
   */
  public void moveTabLeft()
  {
    int index = this.getCurrentPanelIndex();
    if (index <= 0) return;
    moveTab(index, index - 1);
  }

  /**
   * Moves the current sql tab to the right (i.e. index := index + 1)
   * If oldIndex denotes the last SQL Tab, nothing happens
   */
  public void moveTabRight()
  {
    int index = this.getCurrentPanelIndex();
    int lastIndex = sqlTab.getTabCount();
    if (index >= lastIndex) return;
    moveTab(index, index + 1);
  }

  @Override
  public void moveCancelled()
  {
  }

  @Override
  public void endMove(int finalIndex)
  {
    tabSelected(finalIndex);
  }

  @Override
  public boolean startMove(int index)
  {
    return true;
  }

  private void setIgnoreTabChange(boolean flag)
  {
    this.ignoreTabChange = flag;
  }

  @Override
  public boolean moveTab(int oldIndex, int newIndex)
  {
    Optional<MainPanel> panel = this.getPanel(oldIndex);

    if (!panel.isPresent())
    {
      return false;
    }

    JMenuBar oldMenu = this.panelMenus.get(oldIndex);
    this.sqlTab.remove(oldIndex);
    this.panelMenus.remove(oldIndex);
    this.panelMenus.add(newIndex, oldMenu);

    this.sqlTab.add((JComponent)panel.get(), newIndex);
    this.sqlTab.setSelectedIndex(newIndex);

    renumberTabs();
    this.validate();
    return true;
  }

  /**
   * Removes the tab at the given location. If the current profile
   * uses a separate connection per tab, then a disconnect will be
   * triggered as well. This disconnect will be started in a
   * background thread.
   * <br/>
   * The user will not be
   */
  @Override
  public void tabCloseButtonClicked(int index)
  {
    Optional<MainPanel> panel = this.getPanel(index);

    if (!panel.map(p -> p.canClosePanel(true)).orElse(false)) return;

    if (GuiSettings.getConfirmTabClose())
    {
      boolean doClose = WbSwingUtilities.getYesNo(sqlTab, ResourceMgr.getString("MsgConfirmCloseTab"));
      if (!doClose) return;
    }

    if (GuiSettings.getUseLRUForTabs())
    {
      tabHistory.restoreLastTab();
    }
    removeTab(index, true, true);
  }

  /**
   * Removes the indicated tab without checking for modified file etc.
   * If the tab has a separate connection, the connection is closed (disconnected) as well (in a background thread).
   * <p>
   * If a single connection for all tabs is used, the connection is <b>not</b> closed.
   */
  protected void removeTab(int index, boolean updateGUI, boolean addToHistory)
  {
    this.getPanel(index).ifPresent(panel ->
    {
      int newTab = -1;

      boolean inProgress = this.isConnectInProgress();
      if (!inProgress) this.setConnectIsInProgress();

      if (addToHistory)
      {
        closedTabHistory.addToTabHistory(panel, index);
      }

      try
      {
        setIgnoreTabChange(true);

        WbConnection conn = panel.getConnection();
        boolean doDisconnect = conn != null && conn.isShared() == false; // usesSeparateConnection(panel) || (this.currentProfile != null && this.currentProfile.getUseSeparateConnectionPerTab());

        panel.dispose();

        BookmarkManager.getInstance().clearBookmarksForPanel(getWindowId(), panel.getId());

        if (doDisconnect)
        {
          disconnectInBackground(conn);
        }
        disposeMenu(panelMenus.get(index));
        this.panelMenus.remove(index);
        this.sqlTab.removeTabAt(index);

        if (updateGUI)
        {
          this.renumberTabs();
          newTab = this.sqlTab.getSelectedIndex();
          if (panel instanceof DbExplorerPanel)
          {
            restoreDbTree();
          }
        }
      }
      catch (Throwable e)
      {
        LogMgr.logError(new CallerInfo(){}, "Error removing tab index=" + index, e);
      }
      finally
      {
        setIgnoreTabChange(false);
        if (!inProgress) this.clearConnectIsInProgress();
      }
      if (newTab >= 0 && updateGUI)
      {
        this.tabSelected(newTab);
      }

      if (sqlTab.getTabCount() > 0)
      {
        sqlTab.setCloseButtonEnabled(0, this.sqlTab.getTabCount() > 1);
      }
      updateTabHistoryMenu();
    });
  }

  private void disconnectInBackground(final WbConnection conn)
  {
    if (conn == null) return;

    WbThread close = new WbThread("Disconnect " + conn.getId())
    {
      @Override
      public void run()
      {
        try
        {
          conn.disconnect();
        }
        catch (Throwable th)
        {
          LogMgr.logWarning(new CallerInfo(){}, "Error when closing connection");
        }
      }
    };
    close.start();
  }

  private void disposeMenu(JMenuBar menuBar)
  {
    if (menuBar == null) return;
    int count = menuBar.getMenuCount();
    for (int i = 0; i < count; i++)
    {
      JMenu menu = menuBar.getMenu(i);
      if (menu != null) menu.removeAll();
    }
    menuBar.removeAll();
  }

  @Override
  public void mouseClicked(MouseEvent e)
  {
    if (e.getSource() == this.sqlTab)
    {
      Point p = e.getPoint();
      int index = sqlTab.indexAtLocation(p.x, p.y);

      if (e.getButton() == MouseEvent.BUTTON2)
      {
        if (this.canCloseTab())
        {
          this.removeCurrentTab();
        }
      }

      if (e.getButton() == MouseEvent.BUTTON3)
      {
        SqlTabPopup pop = new SqlTabPopup(this);
        pop.show(this.sqlTab, e.getX(), e.getY());
      }
      else if (index == -1 && e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2)
      {
        this.addTab();
      }
    }
  }

  @Override
  public void mouseEntered(MouseEvent e)
  {
  }

  @Override
  public void mouseExited(MouseEvent e)
  {
  }

  @Override
  public void mousePressed(MouseEvent e)
  {
  }

  @Override
  public void mouseReleased(MouseEvent e)
  {
  }

  @Override
  public void executionEnd(WbConnection conn, Object source)
  {
    getJobIndicator().jobEnded();
  }

  @Override
  public void executionStart(WbConnection conn, Object source)
  {
    if (Settings.getInstance().getAutoSaveWorkspace())
    {
      this.saveWorkspace(false);
    }
    getJobIndicator().jobStarted();
  }
}
