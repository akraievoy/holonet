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

public class KeyTableModel extends AbstractTableModel {
  protected static final String COL_NAME = "Name";
  protected static final String COL_TYPE = "Type";

  protected static final String[] COL = {COL_NAME, COL_TYPE};
  private KeyRow[] keyRows;

  public KeyTableModel() {
  }

  public int getRowCount() {
    return getKeys().length;
  }

  protected KeyRow[] getKeys() {
    return keyRows;
  }

  public void setKeyRows(KeyRow[] keyRows) {
    this.keyRows = keyRows;
  }

  public int getColumnCount() {
    return COL.length;
  }

  public String getColumnName(int column) {
    return COL[column];
  }

  public Class<?> getColumnClass(int columnIndex) {
    return String.class;
  }

  public Object getValueAt(int rowIndex, int columnIndex) {
    final KeyRow keyRow = getKeyRow(rowIndex);

    if (keyRow == null) {
      return ""; //	oops, actually
    }

    final String colName = getColumnName(columnIndex);
    if (COL_NAME.equals(colName)) {
      return keyRow.getName();
    }
    if (COL_TYPE.equals(colName)) {
      return keyRow.getType();
    }

    return "";  //	ooops, actually
  }

  public KeyRow getKeyRow(int rowIndex) {
    final KeyRow[] keyRows = getKeys();
    if (rowIndex < 0 || rowIndex >= keyRows.length) {
      return null;
    }

    return keyRows[rowIndex];
  }

  public static class KeyRow {
    private final String name;
    private final String type;

    public KeyRow(String name, String type) {
      this.name = name;
      this.type = type;
    }

    public String getName() { return name; }
    public String getType() { return type; }
  }
}