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
import org.akraievoy.holonet.exp.store.RefObject;
import org.akraievoy.cnet.metrics.api.MetricVData;
import org.akraievoy.cnet.net.vo.EdgeData;
import org.akraievoy.cnet.net.vo.VertexData;

public class MetricVDataEigenGap extends MetricVData {
  protected RefRO<EdgeData> source = new RefObject<EdgeData>();

  private final EigenMetric eigenMetric = new EigenMetric();

  public MetricVDataEigenGap() {
    eigenMetric.init(10);
  }

  public String getName() {
    return "Eigenvalue Gap Vector";
  }

  public void setSource(RefRO<EdgeData> source) {
    this.source = source;
  }

  public void run() {
    final EdgeData data = source.getValue();
    final int nodes = data.getSize();

    eigenMetric.eigensolve("V", nodes, data);

    VertexData result = new VertexData(nodes);

    for (int i = 0; i < nodes; i++) {
      result.set(i, eigenMetric.evResult[nodes + i]);
    }

    target.setValue(result);
  }
}