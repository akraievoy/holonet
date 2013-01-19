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
import org.akraievoy.cnet.gen.vo.Point;
import org.akraievoy.cnet.metrics.api.MetricEData;
import org.akraievoy.cnet.net.vo.EdgeData;
import org.akraievoy.cnet.net.vo.EdgeDataFactory;
import org.akraievoy.cnet.net.vo.VertexData;

public class MetricEDataDistance extends MetricEData {
  protected final org.akraievoy.cnet.gen.vo.Metric metric;

  protected boolean symmetric = false;

  protected RefRO<VertexData> sourceX = new RefObject<VertexData>();
  protected RefRO<VertexData> sourceY = new RefObject<VertexData>();

  public MetricEDataDistance(final org.akraievoy.cnet.gen.vo.Metric metric) {
    this.metric = metric;
  }

  public void setSymmetric(boolean symmetric) {
    this.symmetric = symmetric;
  }

  public void setSourceX(RefRO<VertexData> sourceX) {
    this.sourceX = sourceX;
  }

  public void setSourceY(RefRO<VertexData> sourceY) {
    this.sourceY = sourceY;
  }

  public void run() {
    @SuppressWarnings({"unchecked"})
    final VertexData locationX = sourceX.getValue();
    final VertexData locationY = sourceY.getValue();

    final int size = Math.max(locationX.getSize(), locationY.getSize());

    final EdgeData data = EdgeDataFactory.dense(symmetric, Double.POSITIVE_INFINITY, size);

    for (int i = 0; i < size; i++) {
      final Point iPoint = new Point(locationX.get(i), locationY.get(i));

      for (int j = 0; j < i; j++) {
        final Point jPoint = new Point(locationX.get(j), locationY.get(j));

        data.set(i, j, metric.dist(iPoint, jPoint));
        if (!symmetric) {
          data.set(j, i, metric.dist(jPoint, iPoint));
        }
      }

      data.set(i, i, metric.dist(iPoint, iPoint));
    }

    target.setValue(data);
  }

  public String getName() {
    return "Distance";
  }
}