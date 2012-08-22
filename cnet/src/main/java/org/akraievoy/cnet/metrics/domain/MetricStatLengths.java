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
import org.akraievoy.cnet.metrics.api.MetricStat;
import org.akraievoy.cnet.metrics.vo.Stat;
import org.akraievoy.cnet.metrics.vo.StatImpl;
import org.akraievoy.cnet.net.ref.RefEdgeData;
import org.akraievoy.cnet.net.vo.EdgeData;
import org.akraievoy.cnet.net.vo.RefKeys;

public class MetricStatLengths extends MetricStat {
  protected RefRO<EdgeData> source = RefEdgeData.forPath(RefKeys.LAYER_STRUCTURE);
  protected RefRO<EdgeData> distSource = RefEdgeData.forPath(RefKeys.LAYER_STRUCTURE);

  public String getName() {
    return "Degree Distribution";
  }

  public void setSource(RefRO<EdgeData> source) {
    this.source = source;
  }

  public void setDistSource(RefRO<EdgeData> distSource) {
    this.distSource = distSource;
  }

  public void run() {
    final EdgeData eData = source.getValue();
    final EdgeData distEData = distSource.getValue();

    final int nodes = eData.getSize();
    final Stat lengths = new StatImpl(nodes);

    eData.visitNonDef(new EdgeData.EdgeVisitor() {
      public void visit(int from, int into, double e) {
        lengths.put(distEData.get(from, into));
      }
    });

    target.setValue(lengths);
  }
}
