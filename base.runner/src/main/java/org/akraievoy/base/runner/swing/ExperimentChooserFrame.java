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

    rootPanel.getExperimentTable().getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
  }

  protected void onStart() {
    pack();
    Swing.centerOnScreen(this);

    setVisible(true);
  }

  protected void onStop() {
    dispose();
  }

  public JButton getCloseButton() {
    return rootPanel.getCloseButton();
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

  public JButton getChainButton() {
    return rootPanel.getChainButton();
  }

  public JTextField getChainTextField() {
    return rootPanel.getChainTextField();
  }

  public JTable getRunsTable() {
    return rootPanel.getRunsTable();
  }

  public JButton getImportButton() {
    return rootPanel.getImportButton();
  }

  public JButton getSelectButton() {
    return rootPanel.getSelectButton();
  }

  public JTabbedPane getTabPane() {
    return rootPanel.getTabPane();
  }

  public JLabel getExpNameLabel() {
    return rootPanel.getExpNameLabel();
  }
}
