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

import com.google.common.base.Charsets;
import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import com.google.common.io.CharStreams;
import com.google.common.io.Files;
import org.akraievoy.base.Format;
import org.akraievoy.base.runner.vo.Run;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.swing.table.AbstractTableModel;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

//  LATER extract data access object for the batch defs
public class BatchTableModel extends AbstractTableModel {
  private static final Logger log = LoggerFactory.getLogger(BatchTableModel.class);

  public static interface SelectionCallback {
    void batchSelected(@Nullable BatchDef batch);
  }

  public static interface BatchDef {
    String getPath();
    long getStamp();
    String getXml();
  }

  public static class BatchDefBean implements BatchDef {
    private String path;
    private long stamp;
    private String xml;

    public BatchDefBean(String path, long stamp, String xml) {
      this.path = path;
      this.stamp = stamp;
      this.xml = xml;
    }

    public String getPath() { return path; }
    public long getStamp() { return stamp; }
    public String getXml() { return xml; }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      BatchDefBean that = (BatchDefBean) o;

      return path.equals(that.path);
    }

    @Override
    public int hashCode() {
      return path.hashCode();
    }
  }

  protected String basePath = "data/batch";
  protected static final String BATCH_EXT = ".xml";

  protected static final String COL_SELECT = "*";
  protected static final String COL_PATH = "Path";
  protected static final String COL_STAMP = "Stamp";

  protected static final String[] COL = {
      COL_SELECT, COL_PATH, COL_STAMP
  };
  protected static final List<String> COL_BOOL =
      Arrays.asList(COL_SELECT);

  protected BatchDef selectedBatch = null;

  protected SelectionCallback selectionCallback = new SelectionCallback() {
    public void batchSelected(@Nullable BatchDef batch) {
      //  nothing to do here
    }
  };

  public BatchTableModel() {
    //  nothing to do here, yet
  }

  public void setSelectionCallback(SelectionCallback selectionCallback) {
    this.selectionCallback = selectionCallback;
  }

  public BatchDef getSelectedBatch() {
    return selectedBatch;
  }

  public int getRowCount() {
    return getBatches().length;
  }

  protected BatchDef[] getBatches() {

    try {
      final File base = new File(basePath);
      final File[] batchFiles =
          base.listFiles(new BatchFileFilter());

      if (batchFiles == null) {
        return new BatchDef[0];
      }

      BatchDef[] batchDefs = new BatchDef[batchFiles.length];
      for (int i = 0; i < batchFiles.length; i++) {
        final File batchFile = batchFiles[i];
        final String xml =
            CharStreams.toString(
                Files.newReader(
                    batchFile,
                    Charsets.UTF_8
                )
            );
        batchDefs[i] = new BatchDefBean(
            batchFile.getName(),
            batchFile.lastModified(),
            xml
        );
      }

      Arrays.sort(
          batchDefs,
          new Comparator<BatchDef>() {
            public int compare(BatchDef o1, BatchDef o2) {
              final long stamp1 = o1.getStamp();
              final long stamp2 = o2.getStamp();
              return stamp1 > stamp2 ? -1 : stamp1 == stamp2 ? 0 : 1;
            }
          }
      );

      return batchDefs;
    } catch (IOException e) {
      log.warn(
          "error on listing batch definitions: {}",
          Throwables.getRootCause(e).toString()
      );
      return new BatchDef[0];
    }
  }

  public int getColumnCount() {
    return COL.length;
  }

  public String getColumnName(int column) {
    return COL[column];
  }

  public Class<?> getColumnClass(int columnIndex) {
    return COL_BOOL.contains(getColumnName(columnIndex)) ? Boolean.class : String.class;
  }

  public Object getValueAt(int rowIndex, int columnIndex) {
    final BatchDef batch = getBatch(rowIndex);

    if (batch == null) {
      return ""; //	oops, actually
    }

    final String colName = getColumnName(columnIndex);
    if (COL_SELECT.equals(colName)) {
      return batch.equals(selectedBatch);
    }
    if (COL_PATH.equals(colName)) {
      return batch.getPath();
    }
    if (COL_STAMP.equals(colName)) {
      return Format.format(new Date(batch.getStamp()), true);
    }

    return "";  //	ooops, actually
  }

  @Override
  public boolean isCellEditable(int rowIndex, int columnIndex) {
    final String colName = getColumnName(columnIndex);
    return COL_SELECT.equals(colName);
  }

  @Override
  public void setValueAt(Object aValue, int row, int col) {
    final String colName = getColumnName(col);

    if (!COL_SELECT.equals(colName)) {
      return;
    }

    final BatchDef newSelectedBatch =
        Boolean.TRUE.equals(aValue) ? getBatch(row) : null;
    final boolean fireCallback =
        !Objects.equal(selectedBatch, newSelectedBatch);

    final int prevRow =
        selectedBatch == null ? -1 : findBatchRow(selectedBatch.getPath());
    selectedBatch = newSelectedBatch;
    if (fireCallback) {
      selectionCallback.batchSelected(selectedBatch);
      if (prevRow >= 0) {
        //  NOTE: if you ever fire that event for the -1th row
        //    you'll loose TableColumnModel
        fireTableCellUpdated(prevRow, col);
      }
      fireTableCellUpdated(row, col);
    }
  }

  public BatchDef getBatch(int rowIndex) {
    final BatchDef[] batchDefs = getBatches();
    if (rowIndex < 0 || rowIndex >= batchDefs.length) {
      return null;
    }

    return batchDefs[rowIndex];
  }

  public int findBatchRow(String path) {
    final BatchDef[] defs = getBatches();
    for (int i = 0, runsLength = defs.length; i < runsLength; i++) {
      BatchDef run = defs[i];
      if (run.getPath().equals(path)) {
        return i;
      }
    }

    return -1;
  }

  protected static class BatchFileFilter implements FileFilter {
    public boolean accept(File file) {
      final boolean nonEmptyFile =
          file.isFile() && file.length() > 0;
      final boolean accept =
          nonEmptyFile && file.getName().endsWith(BATCH_EXT);

      return accept;
    }
  }
}

