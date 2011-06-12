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

import gnu.trove.TIntArrayList;
import org.akraievoy.base.Die;
import org.akraievoy.cnet.net.vo.EdgeData;
import org.netlib.lapack.LAPACK;
import org.netlib.util.intW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

class EigenMetric {
  private static final Logger log = LoggerFactory.getLogger(EigenMetric.class);

  protected int cachedNodes;
  protected double[] powers;
  protected double[] work;
  protected double[] result;
  protected double[] evResult;
  protected double[] laplacian;
  protected int[] evWork;
  protected int[] evStatus;

  protected void init(int nodes) {
    if (nodes > cachedNodes) {
      laplacian = new double[nodes * nodes];
      powers = new double[nodes];
      evWork = new int[5 * nodes];
      work = new double[8 * nodes];
      result = new double[nodes];
      evResult = new double[nodes * 2];
      evStatus = new int[nodes];

      cachedNodes = nodes;
    } else {
      Arrays.fill(laplacian, 0);
      Arrays.fill(powers, 0);
      //	not required, but let it be
      Arrays.fill(evWork, 0);
      Arrays.fill(work, 0);
      Arrays.fill(result, 0);
      Arrays.fill(evResult, 0);
      Arrays.fill(evStatus, 0);
    }
  }

  protected void eigensolve(final String mode, final int nodes, final EdgeData edgeData) {
    Die.ifFalse("nodes > 1", nodes > 1);

    init(nodes);

    for (int i = 0; i < nodes; i++) {
      powers[i] = edgeData.power(i);
    }

    final TIntArrayList vertexes = new TIntArrayList();
    for (int from = 0; from < nodes; from++) {
      vertexes.clear();
      edgeData.connVertexes(from, vertexes);

      for (int intoPos = 0, size = vertexes.size(); intoPos < size; intoPos++) {
        int into = vertexes.get(intoPos);
        if (from >= into) {
          continue;
        }
        final int index = into * nodes + from; //	fortran-style memory addressing
        laplacian[index] = -edgeData.get(from, into) * Math.pow(powers[from] * powers[into], -0.5);
      }

      final int index = from * nodes + from; //	fortran-style memory addressing
      laplacian[index] = powers[from] > 0 ? 1 - edgeData.get(from, from) / powers[from] : 0;
    }

    final intW lapackStatus = new intW(0);
    final intW foundCount = new intW(0);

    LAPACK.getInstance().dsyevx(
        mode, "I", "U", nodes, laplacian, nodes,
        0.0, 0.0, 1, 2, 2 * LAPACK.getInstance().dlamch("S"),
        foundCount, result, evResult, nodes,
        work, 8 * nodes, evWork, evStatus, lapackStatus
    );

    if (lapackStatus.val != 0) {
      log.warn(
          "lapackStatus = {}, mode={}, foundCount = {}, evStatus={}",
          new Object[]{
              lapackStatus.val,
              mode,
              foundCount.val,
              Arrays.toString(evStatus)
          }
      );
    }

    Die.ifFalse("lapackStatus == 0", lapackStatus.val == 0);
  }
}
