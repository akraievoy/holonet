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
import org.akraievoy.base.Util;
import org.akraievoy.base.runner.ContainerStopper;
import org.akraievoy.base.runner.api.Context;
import org.akraievoy.base.runner.domain.BatchRunner;
import org.akraievoy.base.runner.domain.ExperimentRunner;
import org.akraievoy.base.runner.domain.RunStateListener;
import org.akraievoy.base.runner.persist.ImportRunnable;
import org.akraievoy.base.runner.persist.RunnerDao;
import org.akraievoy.base.runner.vo.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class ExperimentChooserUiController implements Startable, ExperimentTableModel.SelectionCallback {
  private static final org.slf4j.Logger log = LoggerFactory.getLogger(ExperimentChooserUiController.class);

  protected final ExperimentChooserFrame experimentChooserFrame;
  protected final ContainerStopper stopper;
  protected final ExperimentTableModel experimentTableModel;
  protected final RunTableModel runTableModel;
  protected final BatchTableModel modelBatch;
  protected final AxisTableModel axisTableModel = new AxisTableModel();
  protected final KeyTableModel keyTableModel = new KeyTableModel();
  protected final ValueTableModel valueTableModel;
  protected final ExecutorService executor;
  protected final ExperimentRunner experimentRunner;
  protected final RunnerDao runnerDao;
  protected final ImportRunnable importRunnable;
  protected final BatchRunner batchRunner;

  protected final DefaultComboBoxModel emptyModel = new DefaultComboBoxModel(new Object[0]);
  protected final LaunchAction launchAction = new LaunchAction();

  protected boolean experimentRunning = false;

  public ExperimentChooserUiController(
      final ExperimentChooserFrame experimentChooserFrame,
      final ContainerStopper stopper,
      final ExperimentTableModel experimentTableModel,
      final RunTableModel runTableModel,
      final ExecutorService executor,
      final ExperimentRunner experimentRunner,
      final RunnerDao runnerDao,
      final ImportRunnable importRunnable,
      final BatchRunner batchRunner
  ) throws HeadlessException {
    this.experimentChooserFrame = experimentChooserFrame;
    this.stopper = stopper;
    this.experimentTableModel = experimentTableModel;
    this.runTableModel = runTableModel;
    this.executor = executor;
    this.experimentRunner = experimentRunner;
    this.runnerDao = runnerDao;
    this.importRunnable = importRunnable;
    this.batchRunner = batchRunner;
    this.valueTableModel = new ValueTableModel(executor);
    this.modelBatch = new BatchTableModel();
  }

  public void start() {
    experimentChooserFrame.setup();
    experimentTableModel.setCallback(this);
    keyTableModel.setParent(axisTableModel);
    axisTableModel.setCallback(keyTableModel);
    keyTableModel.setCallback(valueTableModel);
    valueTableModel.setCallback(new ReportProgressCallback());

    final RunSelectionCallback runSelectionCallback = new RunSelectionCallback();
    runTableModel.setSelectionCallback(runSelectionCallback);
    final ChainSelectionCallback chainSelectionCallback = new ChainSelectionCallback();
    runTableModel.setChainSelectionCallback(chainSelectionCallback);

    experimentChooserFrame.addWindowListener(new WindowAdapter());
    experimentChooserFrame.getLaunchButton().setAction(launchAction);

    experimentChooserFrame.getExperimentTable().setModel(experimentTableModel);

    experimentChooserFrame.getRunsTable().setModel(runTableModel);

    experimentChooserFrame.getConfCombo().setModel(emptyModel);

    experimentChooserFrame.getAxisTable().setModel(axisTableModel);
    experimentChooserFrame.getKeyTable().setModel(keyTableModel);
    experimentChooserFrame.getValueTable().setModel(valueTableModel);

    experimentChooserFrame.getTableBatch().setModel(modelBatch);
    Color progressBg = experimentChooserFrame.getProgressBatch().getBackground();
    Color progressFg = experimentChooserFrame.getProgressBatch().getForeground();
    final BatchSelectionCallback batchSelectionCallback =
        new BatchSelectionCallback(progressBg, progressFg);
    modelBatch.setSelectionCallback(batchSelectionCallback);

    BatchRunner.Callback batchRunnerCallback =
        new BatchRunnerCallback(progressBg, progressFg);
    batchRunner.setCallback(batchRunnerCallback);
    experimentChooserFrame.getButtonBatch().addActionListener(
        new BatchActionListener()
    );

    setupColumns();

    batchRunnerCallback.batchChanged(null, null, true);
    batchSelectionCallback.batchSelected(null);
    runSelectionCallback.runSelected(null);
    chainSelectionCallback.chainSelectionChanged(Collections.<Run>emptyList());
    experimentSelected(null);
    axisTableModel.setViewedContext(null);
    experimentChooserFrame.onStart();

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

    final FeedbackAppender feedbackAppender = new FeedbackAppender(experimentChooserFrame.getOutputPane());
    feedbackAppender.setThreshold(Level.ALL);

    Logger.getRootLogger().addAppender(feedbackAppender);

    log.info("Copyright (c) Anton Kraievoy 2009, 2011");
  }

  protected void setupColumns() {
    TableColumnModelShrinker.setup(experimentChooserFrame.getExperimentTable());
    TableColumnModelShrinker.setup(experimentChooserFrame.getRunsTable());
    TableColumnModelShrinker.setup(experimentChooserFrame.getAxisTable());
    TableColumnModelShrinker.setup(experimentChooserFrame.getKeyTable());
    TableColumnModelShrinker.setup(experimentChooserFrame.getTableBatch());
  }

  public void stop() {
    experimentChooserFrame.dispose();
  }

  protected class BatchActionListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      final BatchTableModel.BatchDef selected = modelBatch.getSelectedBatch();
      final BatchRunner.Batch running = batchRunner.getRunningBatch();
      if (selected != null && running == null) {
        batchRunner.runBatch(executor, selected);
        experimentChooserFrame.getButtonBatch().setEnabled(false);
      }
    }
  }

  protected class BatchRunnerCallback implements BatchRunner.Callback {
    private final Color bgPassing;
    private final Color bgFailing;
    private final Color fgPassing;
    private final Color fgFailing;

    public BatchRunnerCallback(Color progressBg, Color progressFg) {
      Color progressBgBrighter = progressBg.brighter();
      Color progressFgBrighter = progressFg.brighter();
      bgPassing = new Color(
          progressBg.getRed(),
          progressBgBrighter.getGreen(),
          progressBg.getBlue()
      );
      fgPassing = new Color(
          progressFg.getRed(),
          progressFgBrighter.getGreen(),
          progressFg.getBlue()
      );
      bgFailing = new Color(
          progressBgBrighter.getRed(),
          progressBg.getGreen(),
          progressBg.getBlue()
      );
      fgFailing = new Color(
          progressFgBrighter.getRed(),
          progressFg.getGreen(),
          progressFg.getBlue()
      );
    }

    public void batchAdvanced(
        BatchTableModel.BatchDef def, 
        int pos, 
        int count, 
        boolean success) {
      final JProgressBar progress = experimentChooserFrame.getProgressBatch();
      final BoundedRangeModel model = progress.getModel();

      model.setMinimum(0);
      model.setMaximum(count);
      model.setValue(pos);

      progressState(
          progress,
          success ? bgPassing : bgFailing,
          success ? fgPassing : fgFailing
      );
      progress.setString(def.getPath());
    }

    public void batchChanged(
        @Nullable BatchTableModel.BatchDef def,
        @Nullable BatchRunner.Batch batch,
        boolean success) {
      final JProgressBar progress = experimentChooserFrame.getProgressBatch();
      final BoundedRangeModel model = progress.getModel();
      Color bg = success ? bgPassing : bgFailing;
      Color fg = success ? fgPassing : fgFailing;
      if (def == null || batch == null) {
        onBatchSelectionChange(modelBatch.getSelectedBatch(), batch, false);
        progressState(progress, bg, fg);
      } else {
        model.setMinimum(0);
        model.setMaximum(batch.getComponents().size());
        model.setValue(0);

        progressState(progress, bg, fg);
        progress.setString(def.getPath());
      }
    }
  }

  protected static void progressState(
      JProgressBar progress,
      Color bg,
      Color fg
  ) {
    progress.setBackground(bg);
    progress.setForeground(fg);
  }

  protected class BatchSelectionCallback implements BatchTableModel.SelectionCallback {
    private final Color bgDef;
    private final Color fgDef;

    public BatchSelectionCallback(Color bgDef, Color fgDef) {
      this.bgDef = bgDef;
      this.fgDef = fgDef;
    }

    public void batchSelected(@Nullable BatchTableModel.BatchDef selected) {
      final BatchRunner.Batch running = batchRunner.getRunningBatch();
      final boolean enabled = selected != null && running == null;
      experimentChooserFrame.getButtonBatch().setEnabled(enabled);

      onBatchSelectionChange(selected, running, running == null);
      if (running == null) {
        progressState(
            experimentChooserFrame.getProgressBatch(),
            bgDef,
            fgDef
        );
      }
    }
  }

  private void onBatchSelectionChange(
      BatchTableModel.BatchDef selected,
      BatchRunner.Batch running,
      final boolean updateProgressStr
  ) {
    if (running == null) {
      boolean selectedSome = selected != null;
      experimentChooserFrame.getButtonBatch().setEnabled(selectedSome);
      if (updateProgressStr) {
        final String progressStr =
            selectedSome ?
                "-- run selected batch --" :
                "-- no batch selected --";
        experimentChooserFrame.getProgressBatch().setString(progressStr);
      }
    }
  }

  protected class RunSelectionCallback implements RunTableModel.SelectionCallback {
    public void runSelected(@Nullable final Run run) {
      experimentChooserFrame.getRunNameLabel().setText(
          renderRun(run, "-- no run selected --")
      );
      executor.execute(new Runnable() {
        public void run() {
          if (run != null) {
            try {
              final Conf conf = runnerDao.findConfById(run.getConfUid());
              final ExperimentRunner.RunContext runContext = experimentRunner.loadRunContext(
                  run.getUid(), conf.getUid(), run.getChain()
              );
              final Context viewedContext = new Context(runContext, runnerDao);

              SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                  axisTableModel.setViewedContext(viewedContext);
                }
              });
            } catch (SQLException e) {
              log.warn("failed to fetch run data: ", String.valueOf(e));
              log.debug("exception details", e);
            }
          } else {
            SwingUtilities.invokeLater(new Runnable() {
              public void run() {
                axisTableModel.setViewedContext(null);
              }
            });
          }
        }
      });
    }
  }

  protected static String renderRun(Run run, final String nothing) {
    if (run == null) {
      return nothing;
    }
    return run.getExpDesc() + " / " + run.getConfDesc() + " (" + run.getUid() + ")";
  }

  protected class ChainSelectionCallback implements RunTableModel.ChainSelectionCallback {
    public void chainSelectionChanged(@Nonnull List<Run> runs) {
      final StringBuilder builder = new StringBuilder();

      for (Run run : runs) {
        if (builder.length() > 0) {
          builder.append(" ");
        }
        builder.append(run.getUid());
      }

      experimentChooserFrame.getChainTextField().setText(builder.toString());
    }
  }

  protected void onExperimentRunningChange(boolean newExperimentRunning) {
    this.experimentRunning = newExperimentRunning;

    launchAction.setEnabled(!experimentRunning && isExperimentSelected());
    final boolean comboEnabled =
            !experimentRunning &&
            isExperimentSelected() &&
            experimentChooserFrame.getConfCombo().getModel().getSize() > 0;

    experimentChooserFrame.getConfCombo().setEnabled(comboEnabled);
  }

  public void experimentSelected(@Nullable Experiment exp) {
    experimentChooserFrame.getConfCombo().setModel(emptyModel);

    if (exp != null) {
      boolean enabled = !experimentRunning;
      ComboBoxModel confComboModel = emptyModel;
      try {
        final java.util.List<IdName> confIds = runnerDao.listConfs(exp.getUid());
        confComboModel = new DefaultComboBoxModel(confIds.toArray(new IdName[confIds.size()]));
      } catch (SQLException sqlE) {
        log.warn("failed while listing configurations: {}", Throwables.getRootCause(sqlE).toString());
        enabled = false;
      }

      launchAction.setEnabled(enabled);
      experimentChooserFrame.getExpNameLabel().setText(exp.getDesc());
      experimentChooserFrame.getConfCombo().setModel(confComboModel);
      experimentChooserFrame.getConfCombo().setEnabled(enabled && confComboModel.getSize() > 0);
    } else {
      launchAction.setEnabled(false);
      experimentChooserFrame.getExpNameLabel().setText("-- no experiment selected --");
      experimentChooserFrame.getConfCombo().setEnabled(false);
    }
  }

  protected boolean isExperimentSelected() {
    return experimentChooserFrame.getExperimentTable().getSelectedRow() >= 0;
  }

  private class ReportProgressCallback implements ValueTableModel.ReportProgressCallback {
    public void notify(long runId, long index, long count, final String message) {
      final JProgressBar progressReport =
          experimentChooserFrame.getProgressReport();

      final BoundedRangeModel reportModel =
          progressReport.getModel();

      final Run run;
      try {
        run = runnerDao.findRun(runId);
      } catch (SQLException e) {
        log.warn("failed to setup report progress", e);
        reportModel.setMaximum(0);
        reportModel.setMinimum(0);
        reportModel.setValue(0);
        return;
      }

      reportModel.setMinimum(0);
      reportModel.setMaximum((int) count);
      reportModel.setValue((int) index + 1);

      progressReport.setString(
          renderRun(
            run,
            message == null ? "-- no report configured --" : message
          )
      );
    }
  }

  class WindowAdapter extends java.awt.event.WindowAdapter {
    public void windowClosing(WindowEvent e) {
      if (
          !experimentRunning ||
          JOptionPane.showConfirmDialog(
              experimentChooserFrame,
              "Experiment is running, really close?",
              "Confirm Exit",
              JOptionPane.OK_CANCEL_OPTION
          ) == JOptionPane.OK_OPTION
      ) {
        stopper.forceStop();
      }
    }

    @Override
    public void windowActivated(WindowEvent e) {
      if (!experimentRunning) {
        executor.execute(importRunnable);
      }
    }

    @Override
    public void windowOpened(WindowEvent e) {
      if (!experimentRunning) {
        executor.execute(importRunnable);
      }
    }
  }

  class LaunchAction extends AbstractAction {
    LaunchAction() {
      super("Launch");
    }

    public void actionPerformed(ActionEvent e) {
      if (experimentRunning) {
        return;
      }

      final Experiment exp = experimentTableModel.getSelectedExperiment();
      if (exp == null) {
        return;
      }

      final IdName selectedConf = (IdName) experimentChooserFrame.getConfCombo().getModel().getSelectedItem();
      if (selectedConf == null) {
        return;
      }
      final Long selectedConfUid = Parse.oneLong(selectedConf.getId(), null);
      if (selectedConfUid == null) {
        return;
      }

      Conf conf;
      try {
        conf = runnerDao.findConfByUid(exp.getUid(), selectedConfUid);
      } catch (SQLException sqlE) {
        log.warn("failed to lookup conf: {}", Throwables.getRootCause(sqlE).toString());
        return;
      }

      onExperimentRunningChange(true);
      executor.execute(new RunExperimentTask(exp, conf, safeChainSpec()));
    }

    protected String safeChainSpec() {
      final String chainStr = experimentChooserFrame.getChainTextField().getText();

      final String safeChainStr = chainStr.replaceAll("[^ 0-9]+", "").replaceAll("\\s+", " ").trim();

      experimentChooserFrame.getChainTextField().setText(safeChainStr);
      return safeChainStr;
    }
  }

  class RunExperimentTask implements Runnable {
    protected final Experiment info;
    protected final Conf conf;
    protected final String safeChainSpec;

    RunExperimentTask(Experiment info, Conf conf, String safeChainSpec) {
      this.info = info;
      this.conf = conf;
      this.safeChainSpec = safeChainSpec;
    }

    public void run() {
      try {
        experimentRunner.run(info, conf.getUid(), RunBean.parseChainSpec(safeChainSpec));
      } catch (Throwable t) {
        ExperimentRunner.log.warn("experiment failed", t);
      } finally {
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            onExperimentRunningChange(false);
          }
        });
      }
    }
  }

  class RunStateListenerImpl implements RunStateListener {
    public void onPsetAdvance(long runId, long index) {
      final int runRow = runTableModel.findRunRow(runId);
      if (runRow >= 0) {
        runTableModel.fireTableRowsUpdated(runRow, runRow);

        final BoundedRangeModel execModel =
            experimentChooserFrame.getProgressRun().getModel();

        if (execModel.getMaximum() > 0 && execModel.getValue() < index) {
          execModel.setValue((int) index);
        }
      }
    }

    public void onRunCreation(long runId) {
      runTableModel.fireTableRowsInserted(0, 0);

      final JProgressBar progressExec =
          experimentChooserFrame.getProgressRun();

      final BoundedRangeModel execModel =
          progressExec.getModel();

      final Run run;
      try {
        run = runnerDao.findRun(runId);
      } catch (SQLException e) {
        log.warn("failed to setup execution progress", e);
        execModel.setMaximum(0);
        execModel.setMinimum(0);
        execModel.setValue(0);
        return;
      }

      execModel.setMinimum(0);
      execModel.setMaximum((int) run.getPsetCount());
      execModel.setValue(0);

      progressExec.setString(renderRun(run, "-- no experiments is running --"));
    }
  }

  @Util("swing")
  public static class TableColumnModelShrinker implements TableColumnModelListener, TableModelListener {
    private final JTable autoTable;

    private TableColumnModelShrinker(JTable autoTable) {this.autoTable = autoTable;}

    public void columnAdded(TableColumnModelEvent e) { /* nothing to do */ }
    public void columnSelectionChanged(ListSelectionEvent e) { /* nothing to do */ }
    public void columnRemoved(TableColumnModelEvent e) { /* nothing to do */ }
    public void columnMoved(TableColumnModelEvent e) { /* nothing to do */ }

    public void tableChanged(TableModelEvent e) {
      shrink(autoTable);  //  LATER not all events imply the neccessity of the call
    }

    public void columnMarginChanged(ChangeEvent e) {
      shrink(autoTable);
    }

    public static void shrink(final JTable table) {
      final TableColumnModel columnModel = table.getColumnModel();
      final TableModel model = table.getModel();
      final int rowCount = model.getRowCount();
      final TableCellRenderer headerRenderer = table.getTableHeader().getDefaultRenderer();
      for (int j = 0; j < columnModel.getColumnCount(); j++) {
        final TableColumn col = columnModel.getColumn(j);
        int width = prefWidth(table, headerRenderer, col.getHeaderValue());
        for (int i = 0; i < rowCount; i++) {
          final TableCellRenderer cellRenderer = table.getCellRenderer(i, j);
          width = Math.max(width, prefWidth(table, cellRenderer, model.getValueAt(i, j)));
        }
        col.setMaxWidth(width);
      }
    }

    protected static int prefWidth(final JTable autoTable, TableCellRenderer renderer, final Object cellValue) {
      if (renderer == null) {
        return 1; //  LATER make the margin configurable
      }

      final Component component = renderer.getTableCellRendererComponent(
          autoTable, cellValue, false, false, 0, 0
      );
      return component.getPreferredSize().width + 1;
    }

    public static void setup(JTable autoTable) {
      final TableColumnModelShrinker shrinker = new TableColumnModelShrinker(autoTable);
      autoTable.getColumnModel().addColumnModelListener(shrinker);
      autoTable.getModel().addTableModelListener(shrinker);
    }
  }
}
