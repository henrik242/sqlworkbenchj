/*
 * SpreadSheetOptionsPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.dialogs.export;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

/**
 *
 * @author  Thomas Kellerer
 */
public class SpreadSheetOptionsPanel
	extends javax.swing.JPanel
	implements SpreadSheetOptions
{
	private String exportType;

	public SpreadSheetOptionsPanel(String type)
	{
		super();
		exportType = type;
		initComponents();
	}

	public void saveSettings()
	{
		Settings s = Settings.getInstance();
		s.setProperty("workbench.export." + exportType + ".pagetitle", this.getPageTitle());
		s.setProperty("workbench.export." + exportType + ".header", getExportHeaders());
	}

	public void restoreSettings()
	{
		Settings s = Settings.getInstance();
		this.setPageTitle(s.getProperty("workbench.export." + exportType + ".pagetitle", ""));
		boolean headerDefault = s.getBoolProperty("workbench.export." + exportType + ".default.header", false);
		boolean header = s.getBoolProperty("workbench.export." + exportType + ".header", headerDefault);
		this.setExportHeaders(header);
	}

	public boolean getExportHeaders()
	{
		return exportHeaders.isSelected();
	}

	public void setExportHeaders(boolean flag)
	{
		exportHeaders.setSelected(flag);
	}

	public String getPageTitle()
	{
		return pageTitle.getText();
	}

	public void setPageTitle(String title)
	{
		pageTitle.setText(title);
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {
		GridBagConstraints gridBagConstraints;

    pageTitleLabel = new JLabel();
    pageTitle = new JTextField();
    jPanel1 = new JPanel();
    exportHeaders = new JCheckBox();

    setLayout(new GridBagLayout());

    pageTitleLabel.setText(ResourceMgr.getString("LblSheetName")); // NOI18N
    pageTitleLabel.setHorizontalTextPosition(SwingConstants.LEADING);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(7, 6, 3, 6);
    add(pageTitleLabel, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new Insets(0, 6, 0, 6);
    add(pageTitle, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    add(jPanel1, gridBagConstraints);

    exportHeaders.setText(ResourceMgr.getString("LblExportIncludeHeaders")); // NOI18N
    exportHeaders.setBorder(null);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(7, 6, 3, 6);
    add(exportHeaders, gridBagConstraints);
  }// </editor-fold>//GEN-END:initComponents


  // Variables declaration - do not modify//GEN-BEGIN:variables
  private JCheckBox exportHeaders;
  private JPanel jPanel1;
  private JTextField pageTitle;
  private JLabel pageTitleLabel;
  // End of variables declaration//GEN-END:variables

}
