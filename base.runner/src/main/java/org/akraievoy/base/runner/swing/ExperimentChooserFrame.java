/*
 Copyright 2011 Anton Kraievoy akraievoy@gmail.com
 This file is part of Holonet.

 Holonet is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Holonet is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Holonet. If not, see <http://www.gnu.org/licenses/>.
 */

package org.akraievoy.base.runner.swing;

import org.akraievoy.Build;
import org.akraievoy.base.swing.Swing;

import javax.swing.*;
import java.awt.*;

public class ExperimentChooserFrame extends JFrame {
  protected final ExperimentChooserPanel rootPanel;

  public ExperimentChooserFrame(final ExperimentChooserPanel rootPanel) {
    this.rootPanel = rootPanel;
  }

  protected void setup() {
    getContentPane().setLayout(new BorderLayout());
    getContentPane().add(BorderLayout.CENTER, rootPanel.getRootPanel());
    setTitle("Experiment Registry & Runner [" + Build.getBuild() + "]");
    setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

    rootPanel.getExperimentTable().getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
  }

  protected void onStart() {
    pack();
    Swing.centerOnScreen(this);

    setVisible(true);
  }

  public JTable getExperimentTable() {
    return rootPanel.getExperimentTable();
  }

  public JButton getLaunchButton() {
    return rootPanel.getLaunchButton();
  }

  public JTextPane getOutputPane() {
    return rootPanel.getOutputPane();
  }

  public JComboBox getConfCombo() {
    return rootPanel.getConfCombo();
  }

  public JTextField getChainTextField() {
    return rootPanel.getChainTextField();
  }

  public JTable getRunsTable() {
    return rootPanel.getRunsTable();
  }

  public JLabel getExpNameLabel() {
    return rootPanel.getExpNameLabel();
  }

  public JComboBox getExportDestCombo() {
    return rootPanel.getExportDestCombo();
  }

  public JComboBox getExportFormatCombo() {
    return rootPanel.getExportFormatCombo();
  }

  public JTable getKeyTable() {
    return rootPanel.getKeyTable();
  }

  public JTable getAxisTable() {
    return rootPanel.getAxisTable();
  }

  public JTable getValueTable() {
    return rootPanel.getValueTable();
  }

  public JProgressBar getProgressRun() {
    return rootPanel.getProgressRun();
  }

  public JProgressBar getProgressReport() {
    return rootPanel.getProgressReport();
  }

  public JTabbedPane getTabbedPane() {
    return rootPanel.getTabbedPane();
  }

  public JLabel getRunNameLabel() {
    return rootPanel.getRunNameLabel();
  }
}
