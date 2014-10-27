/*
 * WbAboutDialog.java
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
package workbench.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.gui.actions.EscAction;
import workbench.gui.actions.HelpContactAction;
import workbench.gui.components.WbLabelField;

import workbench.util.BrowserLauncher;
import workbench.util.MemoryWatcher;
import workbench.util.WbFile;

/**
 * The about box for SQL Workbench/J
 *
 * @author  Thomas Kellerer
 */
public class WbAboutDialog
	extends JDialog
	implements ActionListener
{
	private EscAction escAction;

	public WbAboutDialog(java.awt.Frame parent)
	{
		super(parent, true);
		initComponents();
		homepageLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		mailToLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		builtWithNbLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		jeditLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		licenseLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		getRootPane().setDefaultButton(closeButton);
		escAction = new EscAction(this, this);
		WbFile f = Settings.getInstance().getConfigFile();
		String s = ResourceMgr.getFormattedString("LblSettingsLocation", f.getFullPath());
		settingsLabel.setText(s);
		settingsLabel.setCaretPosition(0);
		settingsLabel.setBorder(new EmptyBorder(1, 0, 1, 0));
		WbFile logFile = LogMgr.getLogfile();
		logfileLabel.setText(ResourceMgr.getFormattedString("LblLogLocation", logFile == null ? "": logFile.getFullPath()));
		logfileLabel.setCaretPosition(0);
		logfileLabel.setBorder(new EmptyBorder(1, 0, 1, 0));
		long freeMem = MemoryWatcher.getFreeMemory() / (1024*1024);
		long maxMem = MemoryWatcher.MAX_MEMORY / (1024*1024);
		memoryLabel.setText(ResourceMgr.getString("LblMemory") + " " + freeMem + "MB/" + maxMem + "MB");
		pack();
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents()
  {
    GridBagConstraints gridBagConstraints;

    buttonPanel = new JPanel();
    closeButton = new JButton();
    contentPanel = new JPanel();
    logo = new JLabel();
    labelTitel = new JLabel();
    labelDesc = new JLabel();
    labelVersion = new JLabel();
    labelCopyright = new JLabel();
    builtWithNbLabel = new JLabel();
    jeditLabel = new JLabel();
    jdkVersion = new JLabel();
    homepageLabel = new JLabel();
    licenseLabel = new JLabel();
    mailToLabel = new JLabel();
    infoPanel = new JPanel();
    memoryLabel = new JLabel();
    settingsLabel = new WbLabelField();
    logfileLabel = new WbLabelField();

    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    setTitle(ResourceMgr.getString("TxtAbout") + " " + ResourceMgr.TXT_PRODUCT_NAME);
    setName("AboutDialog"); // NOI18N
    addWindowListener(new WindowAdapter()
    {
      public void windowClosing(WindowEvent evt)
      {
        closeDialog(evt);
      }
    });

    buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

    closeButton.setText(ResourceMgr.getString("LblClose")); // NOI18N
    closeButton.setName("close"); // NOI18N
    closeButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent evt)
      {
        closeButtonActionPerformed(evt);
      }
    });
    buttonPanel.add(closeButton);

    getContentPane().add(buttonPanel, BorderLayout.SOUTH);

    contentPanel.setLayout(new GridBagLayout());

    logo.setIcon(new ImageIcon(getClass().getResource("/workbench/resource/images/hitchguide.gif"))); // NOI18N
    logo.setBorder(BorderFactory.createEtchedBorder());
    logo.setIconTextGap(0);
    logo.setMaximumSize(new Dimension(172, 128));
    logo.setMinimumSize(new Dimension(172, 128));
    logo.setPreferredSize(new Dimension(172, 128));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridheight = 7;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(5, 5, 0, 0);
    contentPanel.add(logo, gridBagConstraints);

    labelTitel.setFont(new Font("Dialog", 1, 14)); // NOI18N
    labelTitel.setText(ResourceMgr.TXT_PRODUCT_NAME);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new Insets(6, 8, 0, 5);
    contentPanel.add(labelTitel, gridBagConstraints);

    labelDesc.setText(ResourceMgr.getString("TxtProductDescription")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(0, 8, 0, 5);
    contentPanel.add(labelDesc, gridBagConstraints);

    labelVersion.setText(ResourceMgr.getBuildInfo());
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(0, 8, 0, 5);
    contentPanel.add(labelVersion, gridBagConstraints);

    labelCopyright.setText(ResourceMgr.getString("TxtCopyright")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(12, 8, 0, 5);
    contentPanel.add(labelCopyright, gridBagConstraints);

    builtWithNbLabel.setText("<html>Built with NetBeans (<u>www.netbeans.org</u>)</html>");
    builtWithNbLabel.addMouseListener(new MouseAdapter()
    {
      public void mouseClicked(MouseEvent evt)
      {
        builtWithNbLabelMouseClicked(evt);
      }
    });
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 7;
    gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(4, 5, 3, 5);
    contentPanel.add(builtWithNbLabel, gridBagConstraints);

    jeditLabel.setText("<html>The editor is based on jEdit's 2.2.2 <u>syntax highlighting package</u></html>");
    jeditLabel.setToolTipText("http://syntax.jedit.org/");
    jeditLabel.addMouseListener(new MouseAdapter()
    {
      public void mouseClicked(MouseEvent evt)
      {
        jeditLabelMouseClicked(evt);
      }
    });
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 8;
    gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(0, 5, 4, 5);
    contentPanel.add(jeditLabel, gridBagConstraints);

    jdkVersion.setText(ResourceMgr.getJavaInfo());
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(0, 8, 0, 5);
    contentPanel.add(jdkVersion, gridBagConstraints);

    homepageLabel.setText("<html><u>www.sql-workbench.net</u></html>");
    homepageLabel.addMouseListener(new MouseAdapter()
    {
      public void mouseClicked(MouseEvent evt)
      {
        homepageLabelMouseClicked(evt);
      }
    });
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(0, 8, 0, 5);
    contentPanel.add(homepageLabel, gridBagConstraints);

    licenseLabel.setText(ResourceMgr.getString("TxtLicense")); // NOI18N
    licenseLabel.addMouseListener(new MouseAdapter()
    {
      public void mouseClicked(MouseEvent evt)
      {
        licenseLabelMouseClicked(evt);
      }
    });
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 9;
    gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new Insets(0, 5, 4, 5);
    contentPanel.add(licenseLabel, gridBagConstraints);

    mailToLabel.setText("support@sql-workbench.net");
    mailToLabel.addMouseListener(new MouseAdapter()
    {
      public void mouseClicked(MouseEvent evt)
      {
        mailToLabelMouseClicked(evt);
      }
    });
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(0, 8, 0, 5);
    contentPanel.add(mailToLabel, gridBagConstraints);

    infoPanel.setLayout(new GridBagLayout());

    memoryLabel.setText("Memory:");
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new Insets(0, 0, 1, 0);
    infoPanel.add(memoryLabel, gridBagConstraints);

    settingsLabel.setText("Settings");
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new Insets(3, 0, 0, 0);
    infoPanel.add(settingsLabel, gridBagConstraints);

    logfileLabel.setText("Logfile");
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new Insets(1, 0, 0, 0);
    infoPanel.add(logfileLabel, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 11;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.LAST_LINE_START;
    gridBagConstraints.insets = new Insets(11, 7, 0, 7);
    contentPanel.add(infoPanel, gridBagConstraints);

    getContentPane().add(contentPanel, BorderLayout.CENTER);

    pack();
  }// </editor-fold>//GEN-END:initComponents

	private void mailToLabelMouseClicked(java.awt.event.MouseEvent evt)//GEN-FIRST:event_mailToLabelMouseClicked
	{//GEN-HEADEREND:event_mailToLabelMouseClicked
		try
		{
			if (evt.getClickCount() == 1)
			{
				HelpContactAction.sendEmail();
			}
		}
		catch (Exception e)
		{
		}

	}//GEN-LAST:event_mailToLabelMouseClicked

	private void homepageLabelMouseClicked(java.awt.event.MouseEvent evt)//GEN-FIRST:event_homepageLabelMouseClicked
	{//GEN-HEADEREND:event_homepageLabelMouseClicked
		try
		{
			if (evt.getClickCount() == 1) BrowserLauncher.openURL("http://www.sql-workbench.net");
		}
		catch (Exception e)
		{
		}
	}//GEN-LAST:event_homepageLabelMouseClicked

	private void builtWithNbLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_builtWithNbLabelMouseClicked
		try
		{
			if (evt.getClickCount() == 1) BrowserLauncher.openURL("http://www.netbeans.org");
		}
		catch (Exception e)
		{
		}
}//GEN-LAST:event_builtWithNbLabelMouseClicked

	private void closeButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_closeButtonActionPerformed
	{//GEN-HEADEREND:event_closeButtonActionPerformed
		this.closeDialog(null);
	}//GEN-LAST:event_closeButtonActionPerformed

	private void closeDialog(java.awt.event.WindowEvent evt)
	{//GEN-FIRST:event_closeDialog
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

	private void jeditLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jeditLabelMouseClicked
		try
		{
			if (evt.getClickCount() == 1) BrowserLauncher.openURL("http://syntax.jedit.org/");
		}
		catch (Exception e)
		{
		}
}//GEN-LAST:event_jeditLabelMouseClicked

  private void licenseLabelMouseClicked(MouseEvent evt)//GEN-FIRST:event_licenseLabelMouseClicked
  {//GEN-HEADEREND:event_licenseLabelMouseClicked
		try
		{
			if (evt.getClickCount() == 1) BrowserLauncher.openURL("http://www.sql-workbench.net/manual/license.html");
		}
		catch (Exception e)
		{
		}
  }//GEN-LAST:event_licenseLabelMouseClicked

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == escAction)
		{
			closeDialog(null);
		}
	}

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private JLabel builtWithNbLabel;
  private JPanel buttonPanel;
  private JButton closeButton;
  private JPanel contentPanel;
  private JLabel homepageLabel;
  private JPanel infoPanel;
  private JLabel jdkVersion;
  private JLabel jeditLabel;
  private JLabel labelCopyright;
  private JLabel labelDesc;
  private JLabel labelTitel;
  private JLabel labelVersion;
  private JLabel licenseLabel;
  private JTextField logfileLabel;
  private JLabel logo;
  private JLabel mailToLabel;
  private JLabel memoryLabel;
  private JTextField settingsLabel;
  // End of variables declaration//GEN-END:variables

}
