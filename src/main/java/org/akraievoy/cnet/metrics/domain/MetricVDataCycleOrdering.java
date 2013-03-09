/*
 Copyright 2013 Anton Kraievoy akraievoy@gmail.com
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
import org.akraievoy.base.ref.RefRO;
import org.akraievoy.cnet.metrics.api.MetricVData;
import org.akraievoy.cnet.net.vo.EdgeData;
import org.akraievoy.cnet.net.vo.VertexData;
import org.akraievoy.holonet.exp.store.RefObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class MetricVDataCycleOrdering extends MetricVData {
  private static final Logger log = LoggerFactory.getLogger(EigenMetric.class);
  protected RefRO<? extends EdgeData> source = new RefObject<EdgeData>();

  public MetricVDataCycleOrdering() {
    //
  }

  public MetricVDataCycleOrdering(
      RefRO<? extends EdgeData> source
  ) {
    this.source = source;
  }

  public MetricVDataCycleOrdering(
      RefRO<? extends EdgeData> source,
      Ref<VertexData> target
  ) {
    this.source = source;
    this.target = target;
  }

  public String getName() {
    return "Cycle Ordering";
  }

  public void setSource(RefRO<? extends EdgeData> source) {
    this.source = source;
  }

  public void run() {
    final EdgeData eData = source.getValue();

    final int n = eData.getSize();
    final int[] indexing = new int[n];
    final boolean[] visited = new boolean[n];
    final int[] powers = new int[n];
    //  most simple heuristic:
    //    next node always has least power within non-visited network
    for (int indexingLen = 0; indexingLen < n; indexingLen++) {
      Arrays.fill(powers, 0);
      eData.visitNonDef(new EdgeData.EdgeVisitor() {
        @Override
        public void visit(int from, int into, double e) {
          if (!visited[from] && !visited[into]) {
            powers[from] += 1;
            powers[into] += 1;
          }
        }
      });

      int minPow = -1;
      int minPowIndex = -1;

      //  what is best of non-visited nodes reachable from last node?
      if (indexingLen > 0) {
        for (int i = 0; i < n; i++) {
          if (visited[i] || !eData.conn(indexing[indexingLen - 1], i)) {
            continue;
          }

          if (minPowIndex < 0 || powers[i] < minPow) {
            minPowIndex = i;
            minPow = powers[i];
          }
        }
      }
      //  if nothing is found, then select weakest non-visited node as next
      if (minPowIndex < 0) {
        for (int i = 0; i < n; i++) {
          if (visited[i]) {
            continue;
          }

          if (
              minPowIndex < 0 || powers[i] < minPow
          ) {
            minPowIndex = i;
            minPow = powers[i];
          }
        }
      }

      visited[minPowIndex] = true;
      indexing[indexingLen] = minPowIndex;
    }

    int nonConnectedCount = 0;
    final VertexData result = new VertexData(n);
    for (int i = 0; i < n; i++) {
      if (!eData.conn(indexing[i], indexing[(i + 1) % n])) {
        nonConnectedCount++;
      }
      result.set(indexing[i], i);
    }

    log.debug("nonConnectedCount = " + nonConnectedCount);

    target.setValue(result);
  }
}
