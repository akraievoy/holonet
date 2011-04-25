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
import org.akraievoy.base.Format;
import org.akraievoy.base.runner.persist.RunRegistry;
import org.akraievoy.base.runner.vo.Run;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.table.AbstractTableModel;
import java.sql.SQLException;
import java.util.Date;

public class RunTableModel extends AbstractTableModel {
  private static final Logger log = LoggerFactory.getLogger(RunTableModel.class);

  protected final RunRegistry registry;

  protected static final String COL_ID = "ID";
  protected static final String COL_STAMP = "Stamp";
  protected static final String COL_PSET_COUNT = "Param Sets";
  protected static final String COL_PSET_COMPLETE = "Complete Sets";
  protected static final String COL_COMPLETE = " + ";
  protected static final String COL_CHAIN = "Chain";
  protected static final String COL_EXP_ID = "Exp.ID";
  protected static final String COL_EXP_NAME = "Experiment";
  protected static final String COL_CONF_ID = "Conf.ID";
  protected static final String COL_CONF_NAME = "Conf";

  protected static final String[] COL = {COL_ID, COL_STAMP, COL_EXP_ID, COL_EXP_NAME, COL_CONF_ID, COL_CONF_NAME, COL_PSET_COUNT, COL_PSET_COMPLETE, COL_COMPLETE, COL_CHAIN};

  public RunTableModel(RunRegistry registry) {
    this.registry = registry;
  }

  public int getRowCount() {
    return getRuns().length;
  }

  protected Run[] getRuns() {
    Run[] runs;

    try {
      runs = registry.listRuns();
    } catch (SQLException e) {
      log.warn("error on listing experiments: {}", Throwables.getRootCause(e).toString());
      runs = new Run[0];
    }

    return runs;
  }

  public int getColumnCount() {
    return COL.length;
  }

  public String getColumnName(int column) {
    return COL[column];
  }

  public Class<?> getColumnClass(int columnIndex) {
    return COL_COMPLETE.equals(getColumnName(columnIndex)) ? Boolean.class : String.class;
  }

  public Object getValueAt(int rowIndex, int columnIndex) {
    final Run run = getRun(rowIndex);

    if (run == null) {
      return ""; //	oops, actually
    }

    final String colName = getColumnName(columnIndex);
    if (COL_CHAIN.equals(colName)) {
      return run.getChain();
    }
    if (COL_ID.equals(colName)) {
      return run.getUid();
    }
    if (COL_COMPLETE.equals(colName)) {
      return run.isComplete();
    }
    if (COL_CONF_ID.equals(colName)) {
      return run.getConfUid() >= 0 ? run.getConfUid() : "N/A";
    }
    if (COL_CONF_NAME.equals(colName)) {
      return run.getConfUid() >= 0 ? run.getConfDesc() : "N/A";
    }
    if (COL_EXP_ID.equals(colName)) {
      return run.getExpUid();
    }
    if (COL_EXP_NAME.equals(colName)) {
      return run.getExpDesc();
    }
    if (COL_PSET_COUNT.equals(colName)) {
      return run.getPsetCount();
    }
    if (COL_PSET_COMPLETE.equals(colName)) {
      return run.getPsetComplete();
    }
    if (COL_STAMP.equals(colName)) {
      return Format.format(new Date(run.getMillis()), true);
    }

    return "";  //	ooops, actually
  }

  public Run getRun(int rowIndex) {
    final Run[] runs = getRuns();
    if (rowIndex < 0 || rowIndex >= runs.length) {
      return null;
    }

    return runs[rowIndex];
  }

  public int findRunRow(long runId) {
    final Run[] runs = getRuns();
    for (int i = 0, runsLength = getRuns().length; i < runsLength; i++) {
      Run run = getRuns()[i];
      if (run.getUid() == runId) {
        return i;
      }
    }

    return -1;
  }
}