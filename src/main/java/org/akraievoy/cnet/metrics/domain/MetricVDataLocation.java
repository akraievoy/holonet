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

import org.akraievoy.base.ref.Ref;
import org.akraievoy.holonet.exp.store.RefObject;
import org.akraievoy.cnet.gen.vo.EntropySource;
import org.akraievoy.cnet.gen.vo.LocationGenerator;
import org.akraievoy.cnet.gen.vo.Point;
import org.akraievoy.cnet.metrics.api.MetricVData;
import org.akraievoy.cnet.net.vo.VertexData;

public class MetricVDataLocation extends MetricVData {
  protected final EntropySource eSource;
  protected final LocationGenerator locationGenerator;

  protected Ref<VertexData> targetX = new RefObject<VertexData>();
  protected Ref<VertexData> targetY = new RefObject<VertexData>();

  protected int nodes = 120;

  public MetricVDataLocation(EntropySource eSource, LocationGenerator locationGenerator) {
    this.eSource = eSource;
    this.locationGenerator = locationGenerator;
  }

  public String getName() {
    return "Location";
  }

  public void setTargetY(Ref<VertexData> targetY) {
    this.targetY = targetY;
  }

  public void setTargetX(Ref<VertexData> targetX) {
    this.targetX = targetX;
  }

  public Ref<VertexData> getTargetX() {
    return targetX;
  }

  public Ref<VertexData> getTargetY() {
    return targetY;
  }

  public void setNodes(int nodes) {
    this.nodes = nodes;
  }

  public void run() {
    final VertexData locationsX = new VertexData(nodes);
    final VertexData locationsY = new VertexData(nodes);

    for (int i = 0; i < nodes; i++) {
      final Point location = locationGenerator.chooseLocation(eSource);
      locationsX.set(i, location.getX());
      locationsY.set(i, location.getY());
    }

    targetX.setValue(locationsX);
    targetY.setValue(locationsY);
  }
}