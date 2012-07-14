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
import org.akraievoy.cnet.metrics.api.MetricVData;
import org.akraievoy.cnet.net.ref.RefVertexData;
import org.akraievoy.cnet.net.vo.RefKeys;
import org.akraievoy.cnet.net.vo.VertexData;

public class MetricVDataRenorm extends MetricVData {
  protected RefRO<VertexData> source = RefVertexData.forPath(RefKeys.LAYER_STRUCTURE);

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

  public void setSource(RefRO<VertexData> source) {
    this.source = source;
  }

  public void run() {
    final VertexData vertexData = source.getValue();
    final int size = vertexData.getSize();

    final VertexData result = vertexData.proto(size);

    if (size == 0) {
      target.setValue(result);
      return;
    }

    double dataMax = vertexData.get(0);
    double dataMin = vertexData.get(0);
    for (int i = 1; i < size; i++) {
      dataMax = Math.max(vertexData.get(i), dataMax);
      dataMin = Math.min(vertexData.get(i), dataMin);
    }

    final double dataDiff = dataMax - dataMin;
    final double diff = max - min;

    for (int i = 0; i < size; i++) {
      result.set(i, diff * (vertexData.get(i) - dataMin) / dataDiff + min);
    }

    target.setValue(result);
  }
}