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

import javax.swing.table.AbstractTableModel;

public class AxisTableModel extends AbstractTableModel {
  protected static final String COL_NAME = "Name";
  protected static final String COL_SPEC = "Spec";
  protected static final String COL_COUNT = "Count";

  protected static final String[] COL = {COL_NAME, COL_SPEC, COL_COUNT};
  private AxisRow[] axisRows = new AxisRow[0];

  public AxisTableModel() {
  }

  public int getRowCount() {
    return getAxisRows().length;
  }

  protected AxisRow[] getAxisRows() {
    return axisRows;
  }

  public void setAxisRows(AxisRow[] axisRows) {
    this.axisRows = axisRows;
    fireTableDataChanged();
  }

  public int getColumnCount() {
    return COL.length;
  }

  public String getColumnName(int column) {
    return COL[column];
  }

  public Class<?> getColumnClass(int columnIndex) {
    return COL_COUNT.equals(getColumnName(columnIndex)) ? Long.class : String.class;
  }

  public Object getValueAt(int rowIndex, int columnIndex) {
    final AxisRow axisRow = getAxisRow(rowIndex);

    if (axisRow == null) {
      return ""; //	oops, actually
    }

    final String colName = getColumnName(columnIndex);
    if (COL_NAME.equals(colName)) {
      return axisRow.getName();
    }
    if (COL_SPEC.equals(colName)) {
      return axisRow.getSpec();
    }
    if (COL_COUNT.equals(colName)) {
      return axisRow.getCount();
    }

    return "";  //	ooops, actually
  }

  public AxisRow getAxisRow(int rowIndex) {
    final AxisRow[] axisRows = getAxisRows();
    if (rowIndex < 0 || rowIndex >= axisRows.length) {
      return null;
    }

    return axisRows[rowIndex];
  }

  public static class AxisRow {
    private final String name;
    private final String spec;
    private final long count;

    public AxisRow(String name, String spec, long count) {
      this.name = name;
      this.spec = spec;
      this.count = count;
    }

    public String getName() { return name; }
    public String getSpec() { return spec; }
    public long getCount() { return count; }
  }
}