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
import org.akraievoy.base.runner.api.Context;

import javax.annotation.Nullable;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class KeyTableModel extends AbstractTableModel implements AxisTableModel.Callback {
  public static interface Callback {
    public void keySelected(Context viewedContext, List<String> axisNames, final String selectedKey);
  }

  protected static final String COL_SELECTED = "*";
  protected static final String COL_NAME = "Name";
  protected static final String COL_TYPE = "Type";

  protected static final String[] COL = {COL_SELECTED, COL_NAME, COL_TYPE};

  private AxisTableModel parent;
  private KeyRow[] keyRows = new KeyRow[0];
  private String selectedKey = null;

  private Callback callback = new Callback() {
    public void keySelected(Context viewedContext, List<String> axisNames, String selectedKey) {
      //  nothing to do
    }
  };

  public KeyTableModel() {
  }

  public void setParent(AxisTableModel parent) {
    this.parent = parent;
  }

  public void setCallback(Callback impl) {
    callback = impl;
  }

  public void axisSelectionChanged(Context viewedContext, List<String> axisNames) {
    callback.keySelected(viewedContext, axisNames, selectedKey);
  }

  public void contextSwitched(@Nullable Context viewedContext, List<String> axisNames) {
    keyRows = new KeyRow[0];

    if (viewedContext == null) {
      selectedKey = null;
      callback.keySelected(viewedContext, axisNames, selectedKey);
      fireTableDataChanged();
      return;
    }

    final Map<String, String> pathMap = viewedContext.listPaths();
    final List<KeyTableModel.KeyRow> keyRows = new ArrayList<KeyRow>();
    for (String key : pathMap.keySet()) {
      keyRows.add(new KeyTableModel.KeyRow(
        key, pathMap.get(key)
      ));
    }

    this.keyRows = keyRows.toArray(new KeyRow[keyRows.size()]);
    if (rowForKey(selectedKey) < 0) {
      selectedKey = null;
    }
    callback.keySelected(viewedContext, axisNames, selectedKey);
    fireTableDataChanged();
  }

  public int getRowCount() {
    return getKeys().length;
  }

  protected KeyRow[] getKeys() {
    return keyRows;
  }

  public int getColumnCount() {
    return COL.length;
  }

  public String getColumnName(int column) {
    return COL[column];
  }

  public Class<?> getColumnClass(int columnIndex) {
    if (COL_SELECTED.equals(getColumnName(columnIndex))) {
      return Boolean.class;
    }

    return String.class;
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

    final String newSelectedKey =
        Boolean.TRUE.equals(aValue) ? getKeys()[row].getName() : null;
    final boolean fireCallback = !Objects.equal(selectedKey, newSelectedKey);

    int prevRow = rowForKey(selectedKey);
    selectedKey = newSelectedKey;
    if (fireCallback) {
      callback.keySelected(parent.getViewedContext(), parent.getUsed(), newSelectedKey);
      if (prevRow >= 0) {
        fireTableCellUpdated(prevRow, col);
      }
      fireTableCellUpdated(row, col);
    }
  }

  protected int rowForKey(final String key) {
    int prevRow = -1;
    for (int r = 0; r < getKeys().length; r++ ) {
      if (Objects.equal(getKeys()[r].getName(), key)) {
        prevRow = r;
        break;
      }
    }
    return prevRow;
  }

  public Object getValueAt(int rowIndex, int columnIndex) {
    final KeyRow keyRow = getKeyRow(rowIndex);

    if (keyRow == null) {
      return ""; //	oops, actually
    }

    final String colName = getColumnName(columnIndex);
    if (COL_SELECTED.equals(colName)) {
      return Objects.equal(selectedKey, keyRow.getName());
    }
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