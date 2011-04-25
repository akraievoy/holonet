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

import org.akraievoy.cnet.metrics.api.MetricEData;
import org.akraievoy.cnet.metrics.api.MetricResultFetcher;
import org.akraievoy.cnet.metrics.api.MetricVData;
import org.akraievoy.cnet.net.vo.EdgeData;
import org.akraievoy.cnet.net.vo.EdgeDataFactory;
import org.akraievoy.cnet.net.vo.VertexData;
import org.akraievoy.cnet.stat.api.Median;
import org.akraievoy.cnet.stat.domain.MedianMean;

public class MetricEDataVertexDiff extends MetricEData {
  protected final MetricVData vertexMetric;
  protected boolean symmetric = true;

  protected Median median = new MedianMean();

  public MetricEDataVertexDiff(MetricVData vertexMetric) {
    this.vertexMetric = vertexMetric;
  }

  public void setSymmetric(boolean symmetric) {
    this.symmetric = symmetric;
  }

  public void setMedian(Median median) {
    this.median = median;
  }

  public void run() {
    final VertexData vertexData = (VertexData) MetricResultFetcher.fetch(vertexMetric);

    final int size = vertexData.getSize();
    final EdgeData data = EdgeDataFactory.dense(symmetric, 0.0, size);

    if (size == 0) {
      target.setValue(data);
      return;
    }

    double med = median.computeMedian(vertexData.getData());
    double min = vertexData.get(0);
    double max = vertexData.get(0);

    for (int i = 0; i < size; i++) {
      final Double iNum = vertexData.get(i);

      min = Math.min(min, iNum);
      max = Math.max(max, iNum);
    }

    for (int i = 0; i < size; i++) {
      final double iNum = vertexData.get(i);
      for (int j = 0; j <= i; j++) {
        final double jNum = vertexData.get(j);
        data.set(i, j, diff(iNum, jNum, min, med, max));
      }

      if (data.isSymmetric()) {
        continue;
      }

      for (int j = i + 1; j < size; j++) {
        final double jNum = vertexData.get(j);
        data.set(i, j, diff(jNum, iNum, min, med, max));
      }
    }

    target.setValue(data);
  }

  private double diff(double iNum, double jNum, double min, double med, double max) {
    if (min == max) {
      return 0;
    }

    final double normI = iNum > med ? max - med : med - min;
    final double normJ = jNum > med ? max - med : med - min;

    //	this should be in -1 .. 1 range
    final double diffI = iNum - med / normI;
    //	this should be in -1 .. 1 range
    final double diffJ = jNum - med / normJ;

    return diffI * diffJ;
  }

  public String getName() {
    return "Route Lengths";
  }
}