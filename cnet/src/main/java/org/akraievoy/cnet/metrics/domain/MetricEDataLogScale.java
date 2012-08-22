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
import org.akraievoy.cnet.metrics.api.MetricEData;
import org.akraievoy.cnet.net.ref.RefEdgeData;
import org.akraievoy.cnet.net.vo.EdgeData;
import org.akraievoy.cnet.net.vo.RefKeys;

public class MetricEDataLogScale extends MetricEData {
  protected RefRO<EdgeData> source = RefEdgeData.forPath(RefKeys.LAYER_STRUCTURE);

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

  public void setSource(RefRO<EdgeData> source) {
    this.source = source;
  }

  public void run() {
    final EdgeData edgeData = source.getValue();
    final int size = edgeData.getSize();

    final EdgeData result = edgeData.proto(edgeData.getSize());

    if (size == 0) {
      target.setValue(result);
      return;
    }

    final double baseScale = Math.log(base);
    edgeData.visitNonDef(new EdgeData.EdgeVisitor() {
      public void visit(int from, int into, double e) {
        final double newValue = Math.log(e) / baseScale;
        if (Double.isInfinite(newValue) || Double.isNaN(newValue)) {
          result.set(from, into, invalid);
        } else {
          result.set(from, into, newValue);
        }
      }
    });

    target.setValue(result);
  }
}