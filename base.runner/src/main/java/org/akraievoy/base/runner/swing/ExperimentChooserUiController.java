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
import org.akraievoy.base.runner.domain.ExperimentRunner;
import org.akraievoy.base.runner.domain.ParamSetEnumerator;
import org.akraievoy.base.runner.domain.RunStateListener;
import org.akraievoy.base.runner.persist.ImportRunnable;
import org.akraievoy.base.runner.persist.RunnerDao;
import org.akraievoy.base.runner.vo.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class ExperimentChooserUiController implements Startable, ExperimentTableModel.SelectionCallback {
  private static final org.slf4j.Logger log = LoggerFactory.getLogger(ExperimentChooserUiController.class);

  protected final ExperimentChooserFrame experimentChooserFrame;
  protected final ContainerStopper stopper;
  protected final ExperimentTableModel experimentTableModel;
  protected final RunTableModel runTableModel;
  protected final AxisTableModel axisTableModel = new AxisTableModel();
  protected final KeyTableModel keyTableModel = new KeyTableModel();
  protected final ExecutorService executor;
  protected final ExperimentRunner experimentRunner;
  protected final RunnerDao runnerDao;
  protected final ImportRunnable importRunnable;

  protected final DefaultComboBoxModel emptyModel = new DefaultComboBoxModel(new Object[0]);
  protected final LaunchAction launchAction = new LaunchAction();
  protected final ChainAction chainAction = new ChainAction();

  protected boolean experimentRunning = false;
  protected Context viewedContext;

  public ExperimentChooserUiController(
      final ExperimentChooserFrame experimentChooserFrame,
      final ContainerStopper stopper,
      final ExperimentTableModel experimentTableModel,
      final RunTableModel runTableModel,
      final ExecutorService executor,
      final ExperimentRunner experimentRunner,
      final RunnerDao runnerDao,
      final ImportRunnable importRunnable
  ) throws HeadlessException {
    this.experimentChooserFrame = experimentChooserFrame;
    this.stopper = stopper;
    this.experimentTableModel = experimentTableModel;
    this.runTableModel = runTableModel;
    this.executor = executor;
    this.experimentRunner = experimentRunner;
    this.runnerDao = runnerDao;
    this.importRunnable = importRunnable;
  }

  public void start() {
    experimentChooserFrame.setup();
    experimentTableModel.setCallback(this);

    experimentChooserFrame.addWindowListener(new WindowAdapter());
    experimentChooserFrame.getLaunchButton().setAction(launchAction);
    experimentChooserFrame.getChainButton().setAction(chainAction);

    experimentChooserFrame.getExperimentTable().setModel(experimentTableModel);

    experimentChooserFrame.getRunsTable().setModel(runTableModel);
    experimentChooserFrame.getRunsTable().getSelectionModel().addListSelectionListener(new RunSelectionListener());

    experimentChooserFrame.getConfCombo().setModel(emptyModel);

    experimentChooserFrame.getAxisTable().setModel(axisTableModel);
    experimentChooserFrame.getKeyTable().setModel(keyTableModel);

    //  FIXME proceed with valueTable Model

    setupColumns();

    onRunSelectionChange(-1);
    experimentSelected(null);
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
    TableColumnModelShrinker.setup(experimentChooserFrame.getValueTable());
  }

  public void stop() {
    experimentChooserFrame.dispose();
  }

  protected void onRunSelectionChange(final int runRow) {
    chainAction.setEnabled(runRow >= 0);

    executor.execute(new Runnable() {
      public void run() {
        if (runRow >= 0) {

          try {
            final Run run = runTableModel.getRun(runRow);
            final Conf conf = runnerDao.findConfById(run.getConfUid());
            ExperimentRunner.RunContext runContext = experimentRunner.loadRunContext(
                run.getUid(), conf.getUid(), run.getChain()
            );
            viewedContext = new Context(runContext, runnerDao);

            final List<AxisTableModel.AxisRow> axisRows = new ArrayList<AxisTableModel.AxisRow>();
            final ParamSetEnumerator wideParams = viewedContext.getRunContext().getWideParams();
            for (int paramIndex = 0; paramIndex <  wideParams.getParameterCount(); paramIndex++) {
              final Parameter parameter = wideParams.getParameter(paramIndex);
              axisRows.add(new AxisTableModel.AxisRow(
                  parameter.getName(), 
                  parameter.getValueSpec(),
                  parameter.getValueCount()
              ));
            }

            final Map<String, String> pathMap = viewedContext.listPaths();
            final List<KeyTableModel.KeyRow> keyRows = new ArrayList<KeyTableModel.KeyRow>();
            for (String key : pathMap.keySet()) {
              keyRows.add(new KeyTableModel.KeyRow(
                key, pathMap.get(key)
              ));
            }
            SwingUtilities.invokeLater(new Runnable() {
              public void run() {
                axisTableModel.setAxisRows(axisRows.toArray(new AxisTableModel.AxisRow[axisRows.size()]));
                keyTableModel.setKeyRows(keyRows.toArray(new KeyTableModel.KeyRow[keyRows.size()]));
                //  FIXME populate the valuesTable
              }
            });
          } catch (SQLException e) {
            log.warn("failed to fetch experiment data: ", String.valueOf(e));
            log.debug("exception details", e);
          }
        } else {
          SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              axisTableModel.setAxisRows(new AxisTableModel.AxisRow[0]);
              keyTableModel.setKeyRows(new KeyTableModel.KeyRow[0]);
              //  FIXME clear the valuesTable
            }
          });
        }
      }
    });
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
      experimentChooserFrame.getExpNameLabel().setText("< none selected yet >");
      experimentChooserFrame.getConfCombo().setEnabled(false);
    }
  }

  protected boolean isExperimentSelected() {
    return experimentChooserFrame.getExperimentTable().getSelectedRow() >= 0;
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

  class ChainAction extends AbstractAction {
    ChainAction() {
      super("Chain");
    }

    public void actionPerformed(ActionEvent e) {
      final int selectedRow = experimentChooserFrame.getRunsTable().getSelectedRow();

      final Run run = runTableModel.getRun(selectedRow);

      if (run == null) {
        return;
      }

      final JTextField chainTF = experimentChooserFrame.getChainTextField();
      final String oriChainText = chainTF.getText();
      final String newText = oriChainText.trim() + " " + run.getUid();
      chainTF.setText(newText.trim());
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
        conf = runnerDao.findConfByName(exp.getUid(), selectedConfUid);
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

  class RunSelectionListener implements ListSelectionListener {
    public void valueChanged(ListSelectionEvent e) {
      if (e.getValueIsAdjusting()) {
        return;
      }

      final int selectedRow = experimentChooserFrame.getRunsTable().getSelectedRow();
      onRunSelectionChange(selectedRow);
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
        onExperimentRunningChange(false);
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
        col.setPreferredWidth(width);
        col.setMaxWidth(width); //  LATER add a flag to keep max constraints off
      }
    }

    protected static int prefWidth(final JTable autoTable, TableCellRenderer renderer, final Object cellValue) {
      if (renderer == null) {
        return 2; //  LATER make the margin configurable
      }

      final Component component = renderer.getTableCellRendererComponent(
          autoTable, cellValue, false, false, 0, 0
      );
      return component.getPreferredSize().width + 2;
    }

    public static void setup(JTable autoTable) {
      final TableColumnModelShrinker shrinker = new TableColumnModelShrinker(autoTable);
      autoTable.getColumnModel().addColumnModelListener(shrinker);
      autoTable.getModel().addTableModelListener(shrinker);
    }
  }
}
