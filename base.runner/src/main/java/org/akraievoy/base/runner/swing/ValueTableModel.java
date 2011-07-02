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

import javax.swing.table.AbstractTableModel;
import java.util.*;

/**
 * Numeric export, reimplemented as a TableModel
 */
public class ValueTableModel extends AbstractTableModel implements KeyTableModel.Callback{
  final SortedMap<Integer, SortedMap<Integer, String>> rowMap =
      new TreeMap<Integer, SortedMap<Integer, String>>();

  public void keySelected(Context viewedContext, List<String> axisNames, String selectedKey) {
    for (Integer row: rowMap.keySet()) {
      rowMap.get(row).clear();
    }

    if (viewedContext == null) {
      head(0, "Status");
      cell(0, 0, "Select a run");
      fireTableStructureChanged();
      return;
    }

    if (selectedKey == null) {
      head(0, "Status");
      cell(0, 0, "Select a key");
      fireTableStructureChanged();
      return;
    }

    final ParamSetEnumerator runParams = viewedContext.getRunContext().getWideParams();
    final List<Parameter> axisParams = new ArrayList<Parameter>();
    for (String name : axisNames) {
      final Parameter parameter = runParams.getParameter(name).copy();
      parameter.setStrategyCurrent(Parameter.Strategy.ITERATE);
      axisParams.add(parameter);
    }
    final Parameter hParam = axisParams.isEmpty() ? null : axisParams.get(axisParams.size() - 1);

    for (int i = 0, len = axisParams.size(); i < len - 1; i++) {
      head(i, axisParams.get(i).getName());
    }

    int row;
    if (hParam != null) {
      for (int c = 0; c < hParam.getValueCount(); c++) {
        head(axisParams.size() + c - 1, hParam.getName());
        cell(0, axisParams.size() + c - 1, hParam.getValue(c));
      }
      row = 0;
    } else {
      head(axisParams.size(), selectedKey);
      row = -1;
    }

    final ParamSetEnumerator reportPse = new ParamSetEnumerator();
    reportPse.load(axisParams, -1);
    do {
      long hPos = 0;
      if (hParam == null || (hPos = reportPse.getPos(hParam.getName())) == 0) {
        row++;
        for (int i = 0, len = axisParams.size(); i < len - 1; i++) {
          final Parameter param = axisParams.get(i);
          cell(row, i, param.getValue(reportPse.getPos(param.getName())));
        }
      }

      cell(
          row,
          (int) (axisParams.isEmpty() ? 0 : (axisParams.size() - 1) + hPos),
          String.valueOf(viewedContext.get(selectedKey, null, reportPse.asOffsets(runParams)))
      );
    } while (reportPse.increment(true, true));

    fireTableStructureChanged();
  }

  protected void head(int colIndex, String value) {
    cell(-1, colIndex, value);
  }

  protected void cell(int rowIndex, int colIndex, String value) {
    if (!rowMap.containsKey(rowIndex)) {
      rowMap.put(rowIndex, new TreeMap<Integer, String>());
    }

    rowMap.get(rowIndex).put(colIndex, value);
  }

  @Override
  public String getColumnName(int column) {
    final SortedMap<Integer, String> headMap = rowMap.get(-1);
    if (headMap != null) {
      final String headCell = headMap.get(column);
      if (headCell != null) {
        return headCell;
      }
    }
    return super.getColumnName(column);
  }

  public int getRowCount() {
    return rowMap.isEmpty() ? 0 : rowMap.lastKey() + 1;
  }

  public int getColumnCount() {
    int maxCol = 0;

    for (Integer row : rowMap.keySet()) {
      final SortedMap<Integer, String> cellMap = rowMap.get(row);
      maxCol = Math.max(maxCol, cellMap.isEmpty() ? 0 : cellMap.lastKey() + 1);
    }

    return maxCol;
  }

  public Object getValueAt(int rowIndex, int columnIndex) {
    if (!rowMap.containsKey(rowIndex)) {
      return "";
    }

    final SortedMap<Integer, String> row = rowMap.get(rowIndex);

    if (!row.containsKey(columnIndex)) {
      return "";
    }

    return row.get(columnIndex);
  }
}
