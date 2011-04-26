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
import org.akraievoy.base.runner.api.*;
import org.akraievoy.base.runner.domain.ParamSetEnumerator;
import org.akraievoy.cnet.metrics.vo.Histogram;
import org.akraievoy.cnet.metrics.vo.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class ExportHistograms implements Runnable, ContextInjectable {
  private static final Logger log = LoggerFactory.getLogger(ExportHistograms.class);

  protected Context ctx;

  protected String targetDir = System.getProperty("user.dir");
  protected String ext = ".csv";
  protected String separator = ",";

  protected List<String> skippedParameters = new ArrayList<String>();
  protected List<String> systemParameters = new ArrayList<String>();
  protected List<String> hiddenParameters = new ArrayList<String>();

  protected List<String> zapped = new ArrayList<String>();

  protected RefObject[] histogramSources = new RefObject[0];

  protected PrintWriter fw;
  protected int minStripes = 30;
  protected double minStripeWidth = 0.5;

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

  public void setExt(String ext) {
    this.ext = ext;
  }

  public void setSeparator(String separator) {
    this.separator = separator;
  }

  public void setHistogramSources(RefObject[] histogramSources) {
    this.histogramSources = histogramSources;
  }

  public void setMinStripes(int minStripes) {
    this.minStripes = minStripes;
  }

  public void setMinStripeWidth(double minStripeWidth) {
    this.minStripeWidth = minStripeWidth;
  }

  public void run() {
    final ParamSetEnumerator enumerator = ctx.getEnumerator();
    final String[][] paramValues = enumerator.getParamValues(null);

    zapped.clear();
    zapped.addAll(skippedParameters);
    zapped.addAll(hiddenParameters);
    zapped.addAll(systemParameters);

    fw = null;
    try {
      final File target = ExportPajek.resolveTarget(targetDir, paramValues, zapped, ext);
      log.info("writing report to file {}", target.getCanonicalPath());

      fw = new PrintWriter(new FileWriter(target, false));

      for (RefObject ref : histogramSources) {
        Object obj = ref.getValue();
        final Histogram h;
        if (obj instanceof Histogram) {
          h = (Histogram) obj;
        } else {
          h = ((Stat) obj).createHistogram(minStripes, minStripeWidth);
        }

        printCell(ref.getPath());
        newLine();

        for (int i = 0; i < h.getLength(); i++) {
          printCell(String.valueOf(h.getArgumentAt(i)));
        }
        printCell(String.valueOf(h.getArgumentAt(h.getLength())));
        newLine();

        for (int i = 0; i < h.getLength(); i++) {
          printCell(String.valueOf(h.getValueAt(i)));
        }
        newLine();
      }

    } catch (IOException e) {
      log.error("failed: " + Throwables.getRootCause(e).toString());
    } finally {
      Closeables.closeQuietly(fw);
    }
  }

  protected void newLine() {
    fw.println();
  }

  protected void printCell(String txt) {
    fw.print(txt);
    fw.print(separator);
  }

  protected void printDataCell(Object obj) {
    fw.print(obj == null ? "" : String.valueOf(obj));
    fw.print(separator);
  }
}