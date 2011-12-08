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

import org.akraievoy.base.runner.api.Context;
import org.akraievoy.base.runner.domain.ParamSetEnumerator;
import org.akraievoy.base.runner.vo.Parameter;

import javax.annotation.Nullable;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class AxisTableModel extends AbstractTableModel {
  public static interface Callback {
    void contextSwitched(@Nullable final Context viewedContext, final List<String> axisNames);
    void axisSelectionChanged(final Context viewedContext, final List<String> axisNames);
  }

  protected static final String COL_NAME = "Name";
  protected static final String COL_SPEC = "Spec";
  protected static final String COL_COUNT = "Count";
  protected static final String COL_STRATEGY = "Strategy";
  protected static final String COL_STRATEGY_CURR = "Str. Curr.";
  protected static final String COL_STRATEGY_CHAIN = "Str. Chain.";
  protected static final String COL_USE = "Use";

  protected static final String[] COL =
      {COL_USE, COL_NAME, COL_SPEC, COL_COUNT, COL_STRATEGY, COL_STRATEGY_CURR, COL_STRATEGY_CHAIN};

  private Context viewedContext;
  private List<Parameter> axisRows = new ArrayList<Parameter>();
  private List<String> used = new ArrayList<String>();

  private Callback callback = new Callback() {
    public void contextSwitched(@Nullable Context viewedContext, List<String> axisNames) {
      //  nothing to do
    }

    public void axisSelectionChanged(Context viewedContext, List<String> axisNames) {
      //  nothing to do
    }
  };

  public AxisTableModel() {
  }

  public void setCallback(Callback impl) {
    callback = impl;
  }

  public void setViewedContext(@Nullable Context viewedContext) {
    this.viewedContext = viewedContext;
    this.axisRows.clear();
    this.used.clear();

    if (viewedContext == null) {
      callback.contextSwitched(null, used);
      fireTableDataChanged();
      return;
    }

    final List<Parameter> axisRows = new ArrayList<Parameter>();
    final ParamSetEnumerator wideParams = viewedContext.getRunContext().getWideParams();
    for (int paramIndex = 0; paramIndex <  wideParams.getParameterCount(); paramIndex++) {
      axisRows.add(wideParams.getParameter(paramIndex));
    }

    this.axisRows.addAll(axisRows);
    for (Parameter axis : axisRows) {
      if (iterated(axis)) {
        this.used.add(axis.getName());
      }
    }

    callback.contextSwitched(viewedContext, used);
    fireTableDataChanged();
  }

  public Context getViewedContext() {
    return viewedContext;
  }

  public List<String> getUsed() {
    return used;
  }

  protected List<Parameter> getAxisRows() {
    return axisRows;
  }

  public int getRowCount() {
    return getAxisRows().size();
  }

  public int getColumnCount() {
    return COL.length;
  }

  public String getColumnName(int column) {
    return COL[column];
  }

  public Class<?> getColumnClass(int columnIndex) {
    if (COL_USE.equals(getColumnName(columnIndex))) {
      return Boolean.class;
    }
    if (COL_COUNT.equals(getColumnName(columnIndex))) {
      return Long.class;
    }
    return String.class;
  }

  @Override
  public boolean isCellEditable(int rowIndex, int columnIndex) {
    return COL_USE.equals(getColumnName(columnIndex));
  }

  @Override
  public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    if (!COL_USE.equals(getColumnName(columnIndex))) {
      return;
    }

    final Parameter row = getAxisRow(rowIndex);
    if (row == null) {
      return;
    }

    used.remove(row.getName());
    if (Boolean.TRUE.equals(aValue) || iterated(row)) {
      used.add(row.getName());
    }

    callback.axisSelectionChanged(viewedContext, used);
    fireTableCellUpdated(rowIndex, columnIndex);
  }

  protected static boolean iterated(Parameter row) {
    return
        !Parameter.Strategy.fixed(row.getStrategy()) &&
        row.getValueCount() > 1;
  }

  public Object getValueAt(int rowIndex, int columnIndex) {
    final Parameter row = getAxisRow(rowIndex);

    if (row == null) {
      return ""; //	oops, actually
    }

    final String colName = getColumnName(columnIndex);
    if (COL_USE.equals(colName)) {
      return used.contains(row.getName());
    }
    if (COL_NAME.equals(colName)) {
      return row.getName();
    }
    if (COL_SPEC.equals(colName)) {
      return row.getValueSpec();
    }
    if (COL_COUNT.equals(colName)) {
      return row.getValueCount();
    }
    if (COL_STRATEGY.equals(colName)) {
      return row.getStrategy();
    }
    if (COL_STRATEGY_CURR.equals(colName)) {
      return row.getStrategyCurrent();
    }
    if (COL_STRATEGY_CHAIN.equals(colName)) {
      return row.getStrategyChained();
    }

    return "";  //	ooops, actually
  }

  public Parameter getAxisRow(int rowIndex) {
    final List<Parameter> axisRows = getAxisRows();
    if (rowIndex < 0 || rowIndex >= axisRows.size()) {
      return null;
    }

    return axisRows.get(rowIndex);
  }
}