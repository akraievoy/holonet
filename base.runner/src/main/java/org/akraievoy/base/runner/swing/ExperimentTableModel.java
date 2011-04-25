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
import org.akraievoy.base.Die;
import org.akraievoy.base.Format;
import org.akraievoy.base.ref.Ref;
import org.akraievoy.base.ref.RefSimple;
import org.akraievoy.base.runner.persist.ExperimentRegistry;
import org.akraievoy.base.runner.vo.Experiment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.table.AbstractTableModel;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class ExperimentTableModel extends AbstractTableModel {
  private static final Logger log = LoggerFactory.getLogger(ExperimentTableModel.class);

  protected final ExperimentRegistry registry;

  protected static final String COL_ID = "ID";
  protected static final String COL_STAMP = "Stamp";
  protected static final String COL_NAME = "Name";
  protected static final String COL_PATH = "Path";
  protected static final String COL_KEY = "Key";

  protected static final String[] COL = {COL_ID, COL_STAMP, COL_NAME, COL_KEY, COL_PATH};

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

    final List<String> keys = keyList;
    return keys;
  }

  public int getColumnCount() {
    return COL.length;
  }

  public String getColumnName(int column) {
    return COL[column];
  }

  public Object getValueAt(int rowIndex, int columnIndex) {
    final RefSimple<String> key = new RefSimple<String>(null);
    final Experiment experiment = getExperiment(rowIndex, key);

    if (experiment == null || key.getValue() == null) {
      return ""; //	oops, actually
    }

    final String colName = getColumnName(columnIndex);
    if (COL_ID.equals(colName)) {
      return experiment.getUid();
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

  public Experiment getExperiment(int rowIndex, Ref<String> key) {
    Die.ifNull("key", key);

    final Experiment experiment;
    final List<String> paths = getPaths();

    if (rowIndex >= paths.size()) {
      key.setValue(null);
      return null;
    }

    final String k = paths.get(rowIndex);
    key.setValue(k);

    try {
      experiment = registry.findExperimentByPath(k);
      return experiment;
    } catch (SQLException e) {
      log.warn("error on retrieving experiment: {}", Throwables.getRootCause(e).toString());
      return null;
    }
  }
}
