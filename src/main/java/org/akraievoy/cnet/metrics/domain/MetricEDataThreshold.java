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

package org.akraievoy.cnet.metrics.domain;

import org.akraievoy.cnet.metrics.api.Metric;
import org.akraievoy.cnet.metrics.api.MetricEData;
import org.akraievoy.cnet.net.vo.EdgeData;
import org.akraievoy.cnet.net.vo.EdgeDataFactory;

public class MetricEDataThreshold extends MetricEData {
  protected final MetricEData metric;

  protected double minToMaxRatio = 0.05;
  protected double minAbsValue = 0.0;

  public MetricEDataThreshold(final MetricEData metric) {
    this.metric = metric;
  }

  public void setMinToMaxRatio(double minToMaxRatio) {
    this.minToMaxRatio = minToMaxRatio;
  }

  public void setMinAbsValue(double minAbsValue) {
    this.minAbsValue = minAbsValue;
  }

  public void run() {
    final EdgeData data = Metric.fetch(metric);

    final EdgeMaxVisitor max = new EdgeMaxVisitor();
    data.visitNonDef(max);

    if (!max.isMaxSet() || max.getMax() == 0) {
      target.setValue(data);
      return;
    }

    final EdgeData result = EdgeDataFactory.sparse(
        data.isSymmetric(),
        data.getDefElem(),
        data.getSize()
    );

    data.visitNonDef(new ThresholdVisitor(minToMaxRatio, minAbsValue, max.getMax(), result));

    target.setValue(result);
  }

  public String getName() {
    return metric.getName() + " threshold: min=" + minAbsValue + " minRatio=" + minToMaxRatio;
  }

  protected static class EdgeMaxVisitor implements EdgeData.EdgeVisitor {
    protected boolean maxSet = false;
    protected double max = 0;

    public void visit(int from, int into, double e) {
      if (maxSet) {
        max = Math.max(e, max);
      } else {
        max = e;
        maxSet = true;
      }
    }

    public boolean isMaxSet() {
      return maxSet;
    }

    public double getMax() {
      return max;
    }
  }

  protected static class ThresholdVisitor implements EdgeData.EdgeVisitor {
    protected final double minToMaxRatio;
    protected final double minAbsValue;
    protected final double max;
    protected final EdgeData result;

    public ThresholdVisitor(double minToMaxRatio, double minAbsValue, double max, EdgeData result) {
      this.minToMaxRatio = minToMaxRatio;
      this.minAbsValue = minAbsValue;
      this.max = max;
      this.result = result;
    }

    public void visit(int from, int into, double e) {
      if (e < minAbsValue) {
        return;
      }

      final double toMaxRatio = e / max;
      if (toMaxRatio < minToMaxRatio) {
        return;
      }

      result.set(from, into, e);
    }
  }
}