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
import org.akraievoy.cnet.gen.vo.LocationGenerator;
import org.akraievoy.cnet.gen.vo.Point;
import org.akraievoy.cnet.metrics.api.MetricVData;
import org.akraievoy.cnet.net.ref.RefVertexData;
import org.akraievoy.cnet.net.vo.RefKeys;
import org.akraievoy.cnet.net.vo.VertexData;

public class MetricVDataDensity extends MetricVData {
  protected final LocationGenerator locationGen;

  protected RefRO<VertexData> sourceX = RefVertexData.forPath(RefKeys.LAYER_LOCATION + ".x");
  protected RefRO<VertexData> sourceY = RefVertexData.forPath(RefKeys.LAYER_LOCATION + ".y");

  public MetricVDataDensity(LocationGenerator locationGen) {
    this.locationGen = locationGen;
  }

  public String getName() {
    return "Location Probability Density";
  }

  public void setSourceX(RefRO<VertexData> sourceX) {
    this.sourceX = sourceX;
  }

  public void setSourceY(RefRO<VertexData> sourceY) {
    this.sourceY = sourceY;
  }

  public void run() {
    final VertexData layerSpatialX = sourceX.getValue();
    final VertexData layerSpatialY = sourceY.getValue();

    final int nodes = Math.max(layerSpatialX.getSize(), layerSpatialY.getSize());

    VertexData result = new VertexData(nodes);

    for (int i = 0; i < nodes; i++) {
      final Point iPoint = new Point(
          layerSpatialX.get(i),
          layerSpatialY.get(i)
      );

      result.set(i, locationGen.getDensity(iPoint));
    }

    target.setValue(result);
  }
}