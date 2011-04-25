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

import org.akraievoy.base.Format;
import org.akraievoy.base.ref.RefRO;
import org.akraievoy.cnet.metrics.api.MetricVData;
import org.akraievoy.cnet.net.ref.RefVertexData;
import org.akraievoy.cnet.net.vo.RefKeys;
import org.akraievoy.cnet.net.vo.VertexData;

public class MetricVDataLogScale extends MetricVData {
  protected RefRO<VertexData> source = RefVertexData.forPath(RefKeys.LAYER_STRUCTURE);

  protected double base = 2;
  protected double invalid = 0;

  public String getName() {
    return "Base " + Format.format(base) + " log scale";
  }

  public void setBase(double base) {
    this.base = base;
  }

  public void setInvalid(double invalid) {
    this.invalid = invalid;
  }

  public void setSource(RefRO<VertexData> source) {
    this.source = source;
  }

  public void run() {
    final VertexData vertexData = source.getValue();
    final int size = vertexData.getSize();

    final VertexData result = vertexData.proto();

    if (size == 0) {
      target.setValue(result);
      return;
    }

    result.setSize(size);
    final double baseScale = Math.log(base);
    for (int i = 0; i < size; i++) {
      final double newValue = Math.log(vertexData.get(i)) / baseScale;
      if (Double.isInfinite(newValue) || Double.isNaN(newValue)) {
        result.set(i, invalid);
      } else {
        result.set(i, newValue);
      }
    }

    target.setValue(result);
  }
}