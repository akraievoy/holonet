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

import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import org.akraievoy.base.Die;
import org.akraievoy.base.Format;
import org.akraievoy.base.ref.Ref;
import org.akraievoy.base.ref.RefSimple;
import org.akraievoy.base.runner.persist.ExperimentRegistry;
import org.akraievoy.base.runner.vo.Experiment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.table.AbstractTableModel;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class ExperimentTableModel extends AbstractTableModel {
  private static final Logger log = LoggerFactory.getLogger(ExperimentTableModel.class);

  public static interface SelectionCallback {
    void experimentSelected(@Nullable Experiment exp);
  }

  protected final ExperimentRegistry registry;
  protected String selectedPath = null;
  protected SelectionCallback callback = new SelectionCallback() {
    public void experimentSelected(@Nullable Experiment exp) {
      //  left empty
    }
  };

  protected static final String COL_SELECTED = "*";
  protected static final String COL_ID = "ID";
  protected static final String COL_STAMP = "Stamp";
  protected static final String COL_NAME = "Name";
  protected static final String COL_PATH = "Path";
  protected static final String COL_KEY = "Key";

  protected static final String[] COL = {COL_SELECTED, COL_ID, COL_STAMP, COL_NAME, COL_KEY, COL_PATH};

  public ExperimentTableModel(ExperimentRegistry registry) {
    this.registry = registry;
  }

  public int getRowCount() {
    return getPaths().size();
  }

  protected List<String> getPaths() {
    List<String> keyList;

    try {
      keyList = registry.listExperimentPaths();
    } catch (SQLException e) {
      log.warn("error on listing experiments: {}", Throwables.getRootCause(e).toString());
      keyList = Collections.emptyList();
    }

    return keyList;
  }

  public int getColumnCount() {
    return COL.length;
  }

  public String getColumnName(int column) {
    return COL[column];
  }

  @Override
  public Class<?> getColumnClass(int columnIndex) {
    return COL_SELECTED.equals(getColumnName(columnIndex)) ? Boolean.class : String.class;
  }

  public Object getValueAt(int rowIndex, int columnIndex) {
    final RefSimple<String> key = new RefSimple<String>(null);
    final Experiment experiment = getExperiment(rowIndex, key);
    final String colName = getColumnName(columnIndex);

    if (experiment == null || key.getValue() == null) {
      return COL_ID.equals(colName) ? false : ""; //	oops, actually
    }
    if (COL_ID.equals(colName)) {
      return experiment.getUid();
    }
    if (COL_SELECTED.equals(colName)) {
      return Objects.equal(selectedPath, experiment.getPath());
    }
    if (COL_STAMP.equals(colName)) {
      return Format.format(new Date(experiment.getMillis()), true);
    }
    if (COL_NAME.equals(colName)) {
      return experiment.getDesc();
    }
    if (COL_KEY.equals(colName)) {
      return key.getValue();
    }
    if (COL_PATH.equals(colName)) {
      return experiment.getPath();
    }

    return "";  //	ooops, actually
  }

  @Override
  public boolean isCellEditable(int rowIndex, int columnIndex) {
    return COL_SELECTED.equals(getColumnName(columnIndex));
  }

  @Override
  public void setValueAt(Object aValue, int row, int col) {
    if (!COL_SELECTED.equals(getColumnName(col))) {
      return;
    }

    final String newSelectedPath =
        Boolean.TRUE.equals(aValue) ? getPaths().get(row) : null;
    final boolean fireCallback = !Objects.equal(selectedPath, newSelectedPath);

    final int prevRow = getPaths().indexOf(selectedPath);
    selectedPath = newSelectedPath;
    if (fireCallback) {
      callback.experimentSelected(getSelectedExperiment());
      if (prevRow >= 0) {
        //  NOTE: if you ever fire that event for the -1th row you'll loose TableColumnModel
        fireTableCellUpdated(prevRow, col);
      }
      fireTableCellUpdated(row, col);
    }
  }

  public Experiment getSelectedExperiment() {
    return getExperimentByPath(selectedPath);
  }

  public Experiment getExperiment(int rowIndex, Ref<String> key) {
    Die.ifNull("key", key);

    final List<String> paths = getPaths();

    if (rowIndex >= paths.size()) {
      key.setValue(null);
      return null;
    }

    final String k = paths.get(rowIndex);
    key.setValue(k);

    return getExperimentByPath(k);
  }

  protected Experiment getExperimentByPath(String path) {
    if (path == null) {
      return null;
    }

    Experiment experiment;
    try {
      experiment = registry.findExperimentByPath(path);
      return experiment;
    } catch (SQLException e) {
      log.warn("error on retrieving experiment: {}", Throwables.getRootCause(e).toString());
      return null;
    }
  }

  public void setCallback(@Nonnull SelectionCallback callback) {
    this.callback = callback;
  }
}
