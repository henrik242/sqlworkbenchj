/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016 Thomas Kellerer.
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
package workbench.gui.profiles;



import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JSeparator;
import javax.swing.JTextField;

import workbench.resource.ResourceMgr;
import workbench.ssh.SshConfig;
import workbench.ssh.SshHostConfig;
import workbench.ssh.UrlParser;

import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class SshConfigPanel
  extends javax.swing.JPanel
{

  public SshConfigPanel()
  {
    initComponents();
    hostConfigPanel.checkAgentUsage();
  }

  public void setConfig(SshConfig config, String url)
  {
    clear();

    if (config != null)
    {
      hostConfigPanel.setConfig(config.getHostConfig());
      dbHostname.setText(StringUtil.coalesce(config.getDbHostname(), ""));
      int localPortNr = config.getLocalPort();
      if (localPortNr > 0)
      {
        localPort.setText(Integer.toString(localPortNr));
      }

      int dbPortNr = config.getDbPort();
      if (dbPortNr > 0)
      {
        dbPort.setText(Integer.toString(dbPortNr));
      }
    }

    UrlParser parser = new UrlParser(url);
    if (parser.isLocalURL() == false)
    {
      rewriteUrl.setSelected(true);
    }
  }

  private void clear()
  {
    hostConfigPanel.clear();
    dbPort.setText("");
    dbHostname.setText("");
    localPort.setText("");
    rewriteUrl.setSelected(false);
  }

  public boolean rewriteURL()
  {
    return rewriteUrl.isSelected();
  }

  public SshConfig getConfig()
  {
    SshHostConfig hostConfig = hostConfigPanel.getConfig();
    if (hostConfig == null) return null;

    String localPortNr = StringUtil.trimToNull(localPort.getText());

    SshConfig config = new SshConfig();
    config.setHostConfig(hostConfig);
    config.setLocalPort(StringUtil.getIntValue(localPortNr, 0));
    config.setDbHostname(StringUtil.trimToNull(dbHostname.getText()));
    config.setDbPort(StringUtil.getIntValue(dbPort.getText(), 0));
    return config;
  }

  /** This method is called from within the constructor to
   * initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is
   * always regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents()
  {
    GridBagConstraints gridBagConstraints;

    sshHostConfigPanel1 = new SshHostConfigPanel();
    labelLocalPort = new JLabel();
    localPort = new JTextField();
    labelDbPort = new JLabel();
    labelDbHostname = new JLabel();
    dbHostname = new JTextField();
    dbPort = new JTextField();
    jSeparator1 = new JSeparator();
    rewriteUrl = new JCheckBox();
    hostConfigPanel = new SshHostConfigPanel();

    setLayout(new GridBagLayout());

    labelLocalPort.setLabelFor(localPort);
    labelLocalPort.setText(ResourceMgr.getString("LblSshLocalPort")); // NOI18N
    labelLocalPort.setToolTipText(ResourceMgr.getString("d_LblSshLocalPort")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 7;
    gridBagConstraints.anchor = GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new Insets(5, 5, 0, 0);
    add(labelLocalPort, gridBagConstraints);

    localPort.setToolTipText(ResourceMgr.getString("d_LblSshLocalPort")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 7;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new Insets(5, 5, 0, 11);
    add(localPort, gridBagConstraints);

    labelDbPort.setLabelFor(dbHostname);
    labelDbPort.setText(ResourceMgr.getString("LblSshDbHostname")); // NOI18N
    labelDbPort.setToolTipText(ResourceMgr.getString("d_LblSshDbHostname")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 8;
    gridBagConstraints.anchor = GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new Insets(5, 5, 0, 0);
    add(labelDbPort, gridBagConstraints);

    labelDbHostname.setLabelFor(dbPort);
    labelDbHostname.setText(ResourceMgr.getString("LblSshDbPort")); // NOI18N
    labelDbHostname.setToolTipText(ResourceMgr.getString("d_LblSshDbPort")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 9;
    gridBagConstraints.anchor = GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new Insets(5, 5, 0, 0);
    add(labelDbHostname, gridBagConstraints);

    dbHostname.setToolTipText(ResourceMgr.getString("d_LblSshDbHostname")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 8;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new Insets(5, 5, 0, 11);
    add(dbHostname, gridBagConstraints);

    dbPort.setToolTipText(ResourceMgr.getString("d_LblSshDbPort")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 9;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new Insets(5, 5, 0, 11);
    add(dbPort, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new Insets(8, 5, 5, 11);
    add(jSeparator1, gridBagConstraints);

    rewriteUrl.setSelected(true);
    rewriteUrl.setText(ResourceMgr.getString("LblSshRewriteUrl")); // NOI18N
    rewriteUrl.setToolTipText(ResourceMgr.getString("d_LblSshRewriteUrl")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 10;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new Insets(7, 1, 0, 0);
    add(rewriteUrl, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    add(hostConfigPanel, gridBagConstraints);
  }// </editor-fold>//GEN-END:initComponents


  // Variables declaration - do not modify//GEN-BEGIN:variables
  private JTextField dbHostname;
  private JTextField dbPort;
  private SshHostConfigPanel hostConfigPanel;
  private JSeparator jSeparator1;
  private JLabel labelDbHostname;
  private JLabel labelDbPort;
  private JLabel labelLocalPort;
  private JTextField localPort;
  private JCheckBox rewriteUrl;
  private SshHostConfigPanel sshHostConfigPanel1;
  // End of variables declaration//GEN-END:variables
}
