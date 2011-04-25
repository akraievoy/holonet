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

import org.akraievoy.base.ref.RefRO;
import org.akraievoy.cnet.metrics.api.MetricEData;
import org.akraievoy.cnet.net.ref.RefEdgeData;
import org.akraievoy.cnet.net.vo.EdgeData;
import org.akraievoy.cnet.net.vo.RefKeys;

public class MetricEDataRenorm extends MetricEData {
  protected RefRO<EdgeData> source = RefEdgeData.forPath(RefKeys.LAYER_STRUCTURE);

  protected double min = 0.0;
  protected double max = 1.0;

  public String getName() {
    return "Renormalization";
  }

  public void setMax(double max) {
    this.max = max;
  }

  public void setMin(double min) {
    this.min = min;
  }

  public void setSource(RefRO<EdgeData> source) {
    this.source = source;
  }

  public void run() {
    final EdgeData edgeData = source.getValue();
    final int size = edgeData.getSize();

    final EdgeData result = edgeData.proto();

    if (size == 0) {
      target.setValue(result);
      return;
    }

    final double[] dataMaxRef = new double[]{Double.NaN};
    final double[] dataMinRef = new double[]{Double.NaN};
    edgeData.visitNotNull(new EdgeData.EdgeVisitor() {
      public void visit(int from, int into, double e) {
        if (Double.isNaN(dataMaxRef[0]) || e > dataMaxRef[0]) {
          dataMaxRef[0] = e;
        }
        if (Double.isNaN(dataMinRef[0]) || e < dataMinRef[0]) {
          dataMinRef[0] = e;
        }
      }
    });

    final double dataDiff = dataMaxRef[0] - dataMinRef[0];
    final double dataMin = dataMinRef[0];
    final double diff = max - min;

    result.setSize(size);
    edgeData.visitNotNull(new EdgeData.EdgeVisitor() {
      public void visit(int from, int into, double e) {
        result.set(from, into, diff * (e - dataMin) / dataDiff + min);
      }
    });

    target.setValue(result);
  }

}