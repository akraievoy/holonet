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
import org.akraievoy.base.runner.api.Context;
import org.akraievoy.base.runner.api.ContextInjectable;
import org.akraievoy.base.runner.domain.ParamSetEnumerator;
import org.akraievoy.base.runner.vo.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ExportNumeric implements Runnable, ContextInjectable {
  private static final Logger log = LoggerFactory.getLogger(ExportNumeric.class);

  protected Context ctx;

  protected String targetDir = System.getProperty("user.dir");
  protected String targetFile = "ctx.txt";

  protected List<String> axisParameters = new ArrayList<String>();
  protected List<String> skippedParameters = new ArrayList<String>();
  protected List<String> systemParameters = new ArrayList<String>();
  protected List<String> hiddenParameters = new ArrayList<String>();
  protected List<String> keys = new ArrayList<String>();

  protected PrintWriter fw;
  protected List<String> zapped;

  protected String separator = "\t";

  public List<String> getSkippedParams() {
    final List<String> iterated = new ArrayList<String>();

    iterated.addAll(axisParameters);
    iterated.addAll(skippedParameters);

    return iterated;
  }

  public void setCtx(Context ctx) {
    this.ctx = ctx;
  }

  public void setAxisParameters(List<String> iteratedParamNames) {
    this.axisParameters = iteratedParamNames;
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

  public void setTargetFile(String targetFile) {
    this.targetFile = targetFile;
  }

  public void setKeys(List<String> keys) {
    this.keys = keys;
  }

  public void run() {
    final ParamSetEnumerator enumerator = ctx.getEnumerator();
    final String[][] paramValues = enumerator.getParamValues(null);

    zapped = new ArrayList<String>();
    zapped.addAll(axisParameters);
    zapped.addAll(skippedParameters);
    zapped.addAll(hiddenParameters);
    zapped.addAll(systemParameters);

    if (keys.isEmpty()) {
      keys = Arrays.asList(ctx.listPaths());
    }

    fw = null;
    try {
      final File targetDirObj = new File(targetDir);
      if (!targetDirObj.exists()) {
        Die.ifFalse("mkdirs result", targetDirObj.mkdirs());
      }
      final File target = new File(targetDir, targetFile);
      if (enumerator.isFirst()) {
        if (target.exists()) {
          final String newName = targetFile + "." + System.currentTimeMillis();
          Die.ifFalse("backup result", target.renameTo(new File(targetDir, newName)));
        }
      }
      if (targetFile.endsWith(".csv")) {
        separator = ",";  //	no string quoting for now...
      }

      fw = new PrintWriter(new FileWriter(target, true));

      if (axisParameters.isEmpty()) {

        if (enumerator.isFirst()) {  //	header once
          header(paramValues);
        }

        row(paramValues);

      } else if (axisParameters.size() == 1) {

        final Parameter hParam = enumerator.getParameter(enumerator.getParameterIndex(axisParameters.get(0)));

        if (enumerator.isFirst()) {  //	header once
          header(paramValues, hParam);
        }

        row(hParam, paramValues);

      } else if (axisParameters.size() == 2) {

        final Parameter hParam = enumerator.getParameter(enumerator.getParameterIndex(axisParameters.get(1)));
        final Parameter vParam = enumerator.getParameter(enumerator.getParameterIndex(axisParameters.get(0)));

        if (enumerator.isFirst()) {  //	header once
          header(paramValues, vParam, hParam);
        }

        row(paramValues, hParam, vParam);

      } else {

        throw new UnsupportedOperationException("no more than two params for tabular view");

      }
    } catch (IOException e) {
      log.error("failed: " + Throwables.getRootCause(e).toString());
    } finally {
      if (fw != null) {
        fw.flush();
      }
      Closeables.closeQuietly(fw);
    }
  }

  protected void row(String[][] paramValues, Parameter hParam, Parameter vParam) {
    for (int v = 0; v < vParam.getValueCount(); v++) {
      for (String[] paramValue : paramValues) {
        final String name = paramValue[0];
        if (zapped.indexOf(name) < 0 && !name.equals(hParam.getName()) && !name.equals(vParam.getName())) {
          printCell(paramValue[1]);
        }
      }
      printCell(vParam.getValue(v));
      for (String key : keys) {
        for (int h = 0; h < hParam.getValueCount(); h++) {
          final Object obj = ctx.get(key, Object.class, Context.offset(hParam.getName(), h, vParam.getName(), v));
          printDataCell(obj);
        }
      }
      newLine();
    }
  }

  protected void header(String[][] paramValues, Parameter vParam, Parameter hParam) {
    for (String[] paramValue : paramValues) {
      final String name = paramValue[0];
      if (zapped.indexOf(name) < 0 && !name.equals(hParam.getName()) && !name.equals(vParam.getName())) {
        printCell(name);
      }
    }

    printCell(vParam.getName());

    for (String key : keys) {
      for (int h = 0; h < hParam.getValueCount(); h++) {
        if (h == 0) {
          printCell(key + ";" + hParam.getName() + " = " + hParam.getValue(0));
        } else {
          printCell(hParam.getValue(h));
        }
      }
    }

    newLine();
  }

  protected void row(Parameter hParam, String[][] paramValues) {
    for (String[] paramValue : paramValues) {
      final String name = paramValue[0];
      if (zapped.indexOf(name) < 0 && !name.equals(hParam.getName())) {
        printCell(paramValue[1]);
      }
    }
    for (String key : keys) {
      for (int h = 0; h < hParam.getValueCount(); h++) {
        final Object obj = ctx.get(key, Object.class, Context.offset(hParam.getName(), h));
        printDataCell(obj);
      }
    }
    newLine();
  }

  protected void header(String[][] paramValues, Parameter hParam) {
    for (String[] paramValue : paramValues) {
      final String name = paramValue[0];
      if (zapped.indexOf(name) < 0 && !name.equals(hParam.getName())) {
        printCell(name);
      }
    }

    for (String key : keys) {
      for (int h = 0; h < hParam.getValueCount(); h++) {
        if (h == 0) {
          printCell(key + ";" + hParam.getName() + " = " + hParam.getValue(0));
        } else {
          printCell(hParam.getValue(h));
        }
      }
    }
    newLine();
  }

  protected void row(String[][] paramValues) {
    for (String[] paramValue : paramValues) {
      if (zapped.indexOf(paramValue[0]) < 0) {
        printCell(paramValue[1]);
      }
    }
    for (String key : keys) {
      final Object obj = ctx.get(key, Object.class, true);

      printDataCell(obj);
    }
    newLine();
  }

  protected void header(String[][] paramValues) {
    for (String[] paramValue : paramValues) {
      if (zapped.indexOf(paramValue[0]) < 0) {
        printCell(paramValue[0]);
      }
    }
    for (String key : keys) {
      printCell(key);
    }
    newLine();
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
