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

public class MetricVDataPowers extends MetricVData {
  protected RefRO<EdgeData> source = new RefObject<EdgeData>();

  public String getName() {
    return "Degrees";
  }

  public void setSource(RefRO<EdgeData> source) {
    this.source = source;
  }

  public void run() {
    final EdgeData eData = source.getValue();

    final int nodes = eData.getSize();
    final VertexData result = new VertexData(nodes);

    for (int i = 0; i < nodes; i++) {
      result.set(i, eData.power(i));
    }

    target.setValue(result);
  }
}
