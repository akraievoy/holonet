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

package org.akraievoy.cnet.export.domain.export;

import com.google.common.base.Throwables;
import com.google.common.io.Closeables;
import org.akraievoy.base.Die;
import org.akraievoy.base.Format;
import org.akraievoy.base.ref.RefRO;
import org.akraievoy.base.runner.api.Context;
import org.akraievoy.base.runner.api.ContextInjectable;
import org.akraievoy.base.runner.api.ParamSetEnumerator;
import org.akraievoy.base.runner.api.StandaloneIterator;
import org.akraievoy.cnet.metrics.domain.MetricEDataFiller;
import org.akraievoy.cnet.metrics.domain.MetricVDataFiller;
import org.akraievoy.cnet.net.ref.RefEdgeData;
import org.akraievoy.cnet.net.ref.RefVertexData;
import org.akraievoy.cnet.net.vo.EdgeData;
import org.akraievoy.cnet.net.vo.Resizable;
import org.akraievoy.cnet.net.vo.VertexData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class ExportPajek implements Runnable, ContextInjectable, StandaloneIterator {
  private static final Logger log = LoggerFactory.getLogger(ExportPajek.class);

  protected final String newLine = "\r\n";

  protected Context ctx;

  protected String targetDir = System.getProperty("user.dir");

  protected List<String> skippedParameters = new ArrayList<String>();
  protected List<String> systemParameters = new ArrayList<String>();
  protected List<String> hiddenParameters = new ArrayList<String>();

  protected List<String> zapped;

  protected RefRO<VertexData> vLabelSource = new RefVertexData();
  protected RefRO<VertexData> vCoordXSource = new RefVertexData();
  protected RefRO<VertexData> vCoordYSource = new RefVertexData();
  protected RefRO<VertexData> vCoordZSource = new MetricVDataFiller(0.0);
  protected RefRO<VertexData> vRadiusSource = new MetricVDataFiller(3.0);
  protected RefRO<VertexData> vColorSource = new MetricVDataFiller(1.0);

  protected RefRO<EdgeData> eSource = new RefEdgeData();
  protected RefRO<EdgeData> eLabelSource = new RefEdgeData();
  protected RefRO<EdgeData> eWidthSource = new MetricEDataFiller(1.0);
  protected RefRO<EdgeData> eColorSource = new MetricEDataFiller(1.0);

  protected PajekColorer vColorer = new PajekColorer();
  protected PajekColorer eColorer = new PajekColorer("blue");

  protected PrintWriter fw;

  public List<String> getIteratedParamNames() {
    return skippedParameters;
  }

  public void setCtx(Context ctx) {
    this.ctx = ctx;
  }

  public void setSkippedParameters(List<String> skippedParameters) {
    this.skippedParameters = skippedParameters;
  }

  public void setHiddenParameters(List<String> hiddenParameters) {
    this.hiddenParameters = hiddenParameters;
  }

  public void setSystemParameters(List<String> systemParameters) {
    this.systemParameters = systemParameters;
  }

  public void setTargetDir(String targetDir) {
    this.targetDir = targetDir;
  }

  public void setEColorer(PajekColorer eColorer) {
    this.eColorer = eColorer;
  }

  public void setEColorSource(RefRO<EdgeData> eColorSource) {
    this.eColorSource = eColorSource;
  }

  public void setELabelSource(RefRO<EdgeData> eLabelSource) {
    this.eLabelSource = eLabelSource;
  }

  public void setESource(RefRO<EdgeData> eSource) {
    this.eSource = eSource;
  }

  public void setEWidthSource(RefRO<EdgeData> eWidthSource) {
    this.eWidthSource = eWidthSource;
  }

  public void setVColorSource(RefRO<VertexData> vColorSource) {
    this.vColorSource = vColorSource;
  }

  public void setVColorer(PajekColorer vColorer) {
    this.vColorer = vColorer;
  }

  public void setVCoordXSource(RefRO<VertexData> vCoordXSource) {
    this.vCoordXSource = vCoordXSource;
  }

  public void setVCoordYSource(RefRO<VertexData> vCoordYSource) {
    this.vCoordYSource = vCoordYSource;
  }

  public void setVCoordZSource(RefRO<VertexData> vCoordZSource) {
    this.vCoordZSource = vCoordZSource;
  }

  public void setVLabelSource(RefRO<VertexData> vLabelSource) {
    this.vLabelSource = vLabelSource;
  }

  public void setVRadiusSource(RefRO<VertexData> vRadiusSource) {
    this.vRadiusSource = vRadiusSource;
  }

  public void run() {
    final ParamSetEnumerator enumerator = ctx.getEnumerator();
    final String[][] paramValues = enumerator.getParamValues(null);

    zapped = new ArrayList<String>();

    zapped.addAll(skippedParameters);
    zapped.addAll(hiddenParameters);
    zapped.addAll(systemParameters);

    fw = null;
    try {
      final File target = resolveTarget(targetDir, paramValues, zapped, ".net");
      log.info("writing report to file {}", target.getCanonicalPath());

      fw = new PrintWriter(new FileWriter(target, false));

      final VertexData vLabel = vLabelSource.getValue();
      final VertexData vCoordX = vCoordXSource.getValue();
      final VertexData vCoordY = vCoordYSource.getValue();
      final VertexData vCoordZ = vCoordZSource.getValue();
      final VertexData vRadius = vRadiusSource.getValue();
      final VertexData vColor = vColorSource.getValue();

      final EdgeData e = eSource.getValue();
      final EdgeData eWidth = eWidthSource.getValue();
      final EdgeData eLabel = eLabelSource.getValue();
      final EdgeData eColor = eColorSource.getValue();

      final int size = getMaxSize(
          new Resizable[]{
              vLabel, vRadius, vColor,
              vCoordX, vCoordY, vCoordZ,
              e, eWidth, eLabel, eColor
          }
      );


      fw.print("*Vertices " + size);
      fw.print(newLine);

      for (int i = 0; i < size; i++) {
        final String label = vLabel == null ? String.valueOf(i) : String.valueOf(vLabel.get(i));
        fw.print("" +
            (1 + i) + " " +
            "\"" + label + "\" " +
            vCoordX.get(i) + " " +
            vCoordY.get(i) + " " +
            vCoordZ.get(i) + " " +
            "x_fact " + Math.round(vRadius.get(i)) + " " +
            "y_fact " + Math.round(vRadius.get(i)) + " " +
            "ic " + vColorer.getName(vColor.get(i))
        );
        fw.print(newLine);
      }

      if (e != null) {
        fw.print("*" + (e.isSymmetric() ? "Edges" : "Arcs"));
        fw.print(newLine);

        e.visitNotNull(new EdgeData.EdgeVisitor() {
          public void visit(int from, int into, double e) {
            final String label;
            if (eLabel == null) {
              label = String.valueOf(from + 1) + "->" + String.valueOf(from + 1);
            } else {
              label = Format.format4(eLabel.get(from, into));
            }

            fw.print("" +
                (1 + from) + " " + (1 + into) + " " + e + " " +
                "w " + eWidth.get(from, into) + " " +
                "c " + eColorer.getName(eColor.get(from, into)) + " " +
                "l \"" + label + "\""
            );
            fw.print(newLine);
          }
        });
      }
    } catch (IOException e) {
      log.error("failed: " + Throwables.getRootCause(e).toString());
    } finally {
      Closeables.closeQuietly(fw);
    }
  }

  protected int getMaxSize(final Resizable[] resizables) {
    int maxSize = 0;
    for (Resizable r : resizables) {
      maxSize = Math.max(maxSize, r == null ? 0 : r.getSize());
    }
    return maxSize;
  }

  protected static String compact(final String name) {
    String ans = new String();
    for (int i = 0; i < name.length(); ++i) {
      if (i == 0 || name.charAt(i) == '.' || name.charAt(i - 1) == '.' ||
          (name.charAt(i) >= 'A' && name.charAt(i) <= 'Z'))
        ans = ans + name.charAt(i);
    }
    return ans;
/*		
		String res = name
				.replaceAll("([A-Z])([^A-Z\\.])+", "\\\\1")
				.replaceAll("^([a-z])([^A-Z\\.])+", "\\\\1");
				
		System.out.println("'" + name + "' -> '" + res + "'");
		
		return res;
*/
  }

  protected static File resolveTarget(final String targetDir, String[][] paramValues, final List<String> zapped, String ext) {
    final File targetDirObj = new File(targetDir);
    if (!targetDirObj.exists()) {
      Die.ifFalse("dir creation result", targetDirObj.mkdirs());
    }

    final StringBuilder targetFileName = new StringBuilder(ext);
    for (String[] paramNameValue : paramValues) {
      if (zapped.indexOf(paramNameValue[0]) < 0) {
        targetFileName.insert(0, paramNameValue[1]);
        targetFileName.insert(0, "_");
        targetFileName.insert(0, compact(paramNameValue[0]));
        targetFileName.insert(0, "_");
      }
    }

    final String targetFile = targetFileName.toString();
    final File target = new File(targetDir, targetFile);
    if (target.exists()) {
      final String newName = targetFile + "." + System.currentTimeMillis();
      Die.ifFalse("backup result", target.renameTo(new File(targetDir, newName)));
    }
    return target;
  }
}
