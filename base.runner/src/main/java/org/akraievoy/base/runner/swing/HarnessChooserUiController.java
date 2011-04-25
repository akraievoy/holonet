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

import com.google.common.base.Throwables;
import org.akraievoy.base.Parse;
import org.akraievoy.base.Startable;
import org.akraievoy.base.ref.RefSimple;
import org.akraievoy.base.runner.ContainerStopper;
import org.akraievoy.base.runner.domain.ExperimentRunner;
import org.akraievoy.base.runner.domain.RunStateListener;
import org.akraievoy.base.runner.persist.ImportRunnable;
import org.akraievoy.base.runner.persist.RunnerDao;
import org.akraievoy.base.runner.vo.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutorService;

public class HarnessChooserUiController implements Startable {
  private static final org.slf4j.Logger log = LoggerFactory.getLogger(HarnessChooserUiController.class);

  protected final HarnessChooserFrame harnessChooserFrame;
  protected final ContainerStopper stopper;
  protected final ExperimentTableModel experimentTableModel;
  protected final RunTableModel runTableModel;
  protected final ExecutorService executor;
  protected final ExperimentRunner experimentRunner;
  protected final RunnerDao runnerDao;
  protected final ImportRunnable importRunnable;

  protected final DefaultComboBoxModel emptyModel = new DefaultComboBoxModel(new Object[0]);
  protected final CloseAction closeAction = new CloseAction();
  protected final LaunchAction launchAction = new LaunchAction();
  protected final SelectAction selectAction = new SelectAction();
  protected final ChainAction chainAction = new ChainAction();
  protected final ImportAction importAction = new ImportAction();

  protected boolean harnessRunning = false;
  protected Experiment selectedExperiment;

  public HarnessChooserUiController(
      final HarnessChooserFrame harnessChooserFrame,
      final ContainerStopper stopper,
      final ExperimentTableModel experimentTableModel,
      final RunTableModel runTableModel,
      final ExecutorService executor,
      final ExperimentRunner experimentRunner,
      final RunnerDao runnerDao,
      final ImportRunnable importRunnable
  ) throws HeadlessException {
    this.harnessChooserFrame = harnessChooserFrame;
    this.stopper = stopper;
    this.experimentTableModel = experimentTableModel;
    this.runTableModel = runTableModel;
    this.executor = executor;
    this.experimentRunner = experimentRunner;
    this.runnerDao = runnerDao;
    this.importRunnable = importRunnable;
  }

  public void start() {
    harnessChooserFrame.setup();

    harnessChooserFrame.addWindowListener(new WindowAdapter());
    harnessChooserFrame.getCloseButton().setAction(closeAction);
    harnessChooserFrame.getLaunchButton().setAction(launchAction);
    harnessChooserFrame.getSelectButton().setAction(selectAction);
    harnessChooserFrame.getChainButton().setAction(chainAction);
    harnessChooserFrame.getImportButton().setAction(importAction);

    harnessChooserFrame.getHarnessTable().setModel(experimentTableModel);
    harnessChooserFrame.getHarnessTable().getSelectionModel().addListSelectionListener(new HarnessSelectionListener());

    harnessChooserFrame.getRunsTable().setModel(runTableModel);
    harnessChooserFrame.getRunsTable().getSelectionModel().addListSelectionListener(new RunSelectionListener());

    harnessChooserFrame.getConfCombo().setModel(emptyModel);

    setupColumns();

    onHarnessSelectionChange(-1);
    onRunSelectionChange(-1);
    updateSelectedExperiment(-1);
    harnessChooserFrame.onStart();
    importRunnable.setAfterImport(new Runnable() {
      public void run() {
        if (SwingUtilities.isEventDispatchThread()) {
          experimentTableModel.fireTableDataChanged();
        } else {
          SwingUtilities.invokeLater(this);
        }
      }
    });

    experimentRunner.setListener(new RunStateListenerImpl());

    final FeedbackAppender feedbackAppender = new FeedbackAppender(harnessChooserFrame.getOutputPane());
    feedbackAppender.setThreshold(Level.ALL);

    Logger.getRootLogger().addAppender(feedbackAppender);
  }

  protected void setupColumns() {
    final TableColumnModel tcm = harnessChooserFrame.getHarnessTable().getColumnModel();
    tcm.getColumn(tcm.getColumnIndex(ExperimentTableModel.COL_NAME)).setPreferredWidth(256);
    tcm.getColumn(tcm.getColumnIndex(ExperimentTableModel.COL_PATH)).setPreferredWidth(96);
    tcm.getColumn(tcm.getColumnIndex(ExperimentTableModel.COL_KEY)).setPreferredWidth(48);
    tcm.getColumn(tcm.getColumnIndex(ExperimentTableModel.COL_KEY)).setMaxWidth(256);
  }

  public void stop() {
    harnessChooserFrame.onStop();
  }

  protected void onHarnessSelectionChange(int selectedRow) {
    selectAction.setEnabled(selectedRow >= 0);
  }

  protected void onRunSelectionChange(int selectedRow) {
    chainAction.setEnabled(selectedRow >= 0);
  }

  protected void onHarnessRunningChange(boolean newHarnessRunning) {
    this.harnessRunning = newHarnessRunning;

    launchAction.setEnabled(!harnessRunning && isHarnessSelected());
    final boolean comboEnabled = !harnessRunning && isHarnessSelected() && harnessChooserFrame.getConfCombo().getModel().getSize() > 0;
    harnessChooserFrame.getConfCombo().setEnabled(comboEnabled);

  }

  protected void updateSelectedExperiment(int selectedRow) {
    harnessChooserFrame.getConfCombo().setModel(emptyModel);

    if (selectedRow >= 0) {
      selectedExperiment = experimentTableModel.getExperiment(selectedRow, new RefSimple<String>(null));

      boolean enabled = !harnessRunning && selectedExperiment != null;
      ComboBoxModel confComboModel = emptyModel;
      try {
        if (selectedExperiment != null) {
          final java.util.List<IdName> confIds = runnerDao.listConfs(selectedExperiment.getUid());
          confComboModel = new DefaultComboBoxModel(confIds.toArray(new IdName[confIds.size()]));
        }

      } catch (SQLException sqlE) {
        log.warn("failed while listing configurations: {}", Throwables.getRootCause(sqlE).toString());
        enabled = false;
      }

      launchAction.setEnabled(enabled);
      harnessChooserFrame.getExpNameLabel().setText(selectedExperiment.getDesc());
      harnessChooserFrame.getConfCombo().setModel(confComboModel);
      harnessChooserFrame.getConfCombo().setEnabled(enabled && confComboModel.getSize() > 0);
    } else {
      selectedExperiment = null;
      launchAction.setEnabled(false);
      harnessChooserFrame.getExpNameLabel().setText("< none selected yet >");
      harnessChooserFrame.getConfCombo().setEnabled(false);
    }
  }

  protected boolean isHarnessSelected() {
    return harnessChooserFrame.getHarnessTable().getSelectedRow() >= 0;
  }

  class WindowAdapter extends java.awt.event.WindowAdapter {
    public void windowClosing(WindowEvent e) {
      stopper.forceStop();
    }
  }

  class ChainAction extends AbstractAction {
    ChainAction() {
      super("Chain");
    }

    public void actionPerformed(ActionEvent e) {
      final int selectedRow = harnessChooserFrame.getRunsTable().getSelectedRow();

      final Run run = runTableModel.getRun(selectedRow);

      if (run == null) {
        return;
      }

      final JTextField chainTF = harnessChooserFrame.getChainTextField();
      final String oriChainText = chainTF.getText();
      final String newText = oriChainText.trim() + " " + run.getUid();
      chainTF.setText(newText.trim());
    }
  }

  class SelectAction extends AbstractAction {
    SelectAction() {
      super("Select");
    }

    public void actionPerformed(ActionEvent e) {
      final int selectedRow = harnessChooserFrame.getHarnessTable().getSelectedRow();
      updateSelectedExperiment(selectedRow);

      if (selectedRow >= 0) {
        harnessChooserFrame.getTabPane().setSelectedIndex(1);
      }
    }
  }

  class CloseAction extends AbstractAction {
    CloseAction() {
      super("Close");
    }

    public void actionPerformed(ActionEvent e) {
      stopper.forceStop();
    }
  }

  class ImportAction extends AbstractAction {
    ImportAction() {
      super("Import");
    }

    public void actionPerformed(ActionEvent e) {
      harnessChooserFrame.getTabPane().setSelectedIndex(2);

      executor.execute(importRunnable);
    }
  }

  class LaunchAction extends AbstractAction {
    LaunchAction() {
      super("Launch");
    }

    public void actionPerformed(ActionEvent e) {
      if (harnessRunning) {
        return;
      }

      if (selectedExperiment == null) {
        return;
      }

      final IdName selectedConf = (IdName) harnessChooserFrame.getConfCombo().getModel().getSelectedItem();
      if (selectedConf == null) {
        return;
      }
      final Long selectedConfUid = Parse.oneLong(selectedConf.getId(), (Long) null);
      if (selectedConfUid == null) {
        return;
      }

      Conf conf = null;
      try {
        conf = runnerDao.findConfByName(selectedExperiment.getUid(), selectedConfUid);
      } catch (SQLException sqlE) {
        log.warn("failed to lookup conf: {}", Throwables.getRootCause(sqlE).toString());
        return;
      }

      onHarnessRunningChange(true);
      executor.execute(new RunHarnessTask(selectedExperiment, conf, getChainedRuns()));

      harnessChooserFrame.getTabPane().setSelectedIndex(2);
    }

    protected RunInfo[] getChainedRuns() {
      final String chainStr = harnessChooserFrame.getChainTextField().getText();

      final String safeChainStr = chainStr.replaceAll("[^ 0-9]+", "").replaceAll("\\s+", " ").trim();

      if (safeChainStr.length() == 0) {
        return new RunInfo[0];
      }

      harnessChooserFrame.getChainTextField().setText(safeChainStr);

      final java.util.List<Long> runIds =
          new ArrayList<Long>(Arrays.asList(Parse.longs(safeChainStr.split(" "), null)));
      runIds.removeAll(Collections.singletonList((Long) null));

      return runnerDao.loadChainedRuns(runIds);
    }
  }

  class HarnessSelectionListener implements ListSelectionListener {
    public void valueChanged(ListSelectionEvent e) {
      if (e.getValueIsAdjusting()) {
        return;
      }

      final int selectedRow = harnessChooserFrame.getHarnessTable().getSelectedRow();
      onHarnessSelectionChange(selectedRow);
    }
  }

  class RunSelectionListener implements ListSelectionListener {
    public void valueChanged(ListSelectionEvent e) {
      if (e.getValueIsAdjusting()) {
        return;
      }

      final int selectedRow = harnessChooserFrame.getRunsTable().getSelectedRow();
      onRunSelectionChange(selectedRow);
    }
  }

  class RunHarnessTask implements Runnable {
    protected final Experiment info;
    protected final Conf conf;
    protected final RunInfo[] chainedRuns;

    RunHarnessTask(Experiment info, Conf conf, RunInfo[] chainedRuns) {
      this.info = info;
      this.conf = conf;
      this.chainedRuns = chainedRuns;
    }

    public void run() {
      try {
        experimentRunner.run(info, conf, chainedRuns);
      } catch (Throwable t) {
        ExperimentRunner.log.warn("harness failed", t);
      } finally {
        onHarnessRunningChange(false);
      }
    }
  }

  class RunStateListenerImpl implements RunStateListener {
    public void onPsetAdvance(long runId, long index) {
      final int runRow = runTableModel.findRunRow(runId);
      if (runRow >= 0) {
        runTableModel.fireTableRowsUpdated(runRow, runRow);
      }
    }

    public void onRunCreation(long runId) {
      final int rowCount = runTableModel.getRowCount();
      runTableModel.fireTableRowsInserted(rowCount, rowCount);
    }
  }
}
