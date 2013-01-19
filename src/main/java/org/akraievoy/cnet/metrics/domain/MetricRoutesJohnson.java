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

import gnu.trove.TDoubleArrayList;
import gnu.trove.TIntArrayList;
import org.akraievoy.base.ref.RefRO;
import org.akraievoy.holonet.exp.store.RefObject;
import org.akraievoy.cnet.metrics.api.MetricRoutes;
import org.akraievoy.cnet.net.vo.EdgeData;
import org.akraievoy.cnet.net.vo.Route;
import org.akraievoy.cnet.net.vo.Routes;

import java.util.Arrays;
import java.util.BitSet;

public class MetricRoutesJohnson extends MetricRoutes {
  protected RefRO<EdgeData> distSource = new RefObject<EdgeData>();
  protected RefRO<EdgeData> source = new RefObject<EdgeData>();

  protected final BitSet was = new BitSet();

  protected final int length = 0;

  public String getName() {
    return "Johnson Routes";
  }

  public void setDistSource(RefRO<EdgeData> distSource) {
    this.distSource = distSource;
  }

  public void setSource(RefRO<EdgeData> source) {
    this.source = source;
  }

  public void run() {
    final EdgeData struct = source.getValue();
    final EdgeData dist = distSource.getValue() == null ? struct : distSource.getValue();

    final int nodes = Math.max(struct.getSize(), dist == null ? 0 : dist.getSize());

    final Routes routes = new Routes(nodes);
    final double[] length = new double[nodes];
    final TIntArrayList[] intos = new TIntArrayList[nodes];
    for (int from = 0; from < nodes; from++) {
      intos[from] = struct.connVertexes(from);
    }

    final TDoubleArrayList qLen = new TDoubleArrayList();
    final TIntArrayList qIdx = new TIntArrayList();
    for (int from = 0; from < nodes; from++) {
      dijkstra(from, intos, dist, nodes, length, qLen, qIdx);
      for (int into = 0; into < nodes; into++) {
        routes.set(from, into, new RouteJohnson(length[into]));
      }
    }

    target.setValue(routes);
  }

  public void dijkstra(int from, TIntArrayList[] intos, EdgeData dist, int nodes, double[] length, final TDoubleArrayList qLen, final TIntArrayList qIdx) {
    was.clear();
    Arrays.fill(length, Double.POSITIVE_INFINITY);
    length[from] = 0;

    qLen.clear();
    qIdx.clear();
    qLen.add(0);
    qIdx.add(from);

    while (!qLen.isEmpty() && was.cardinality() < nodes) {
      int newNode = qIdx.remove(0);
      qLen.remove(0);
/*
			System.err.println(newNode + " > " + ObjArrays.toString(length) + " > " + was.toString());
*/
      final TIntArrayList newIntos = intos[newNode];
      for (int intoI = 0; intoI < newIntos.size(); intoI++) {
        int into = newIntos.get(intoI);
        if (was.get(into)) {
          continue;
        }

        final double newDist = length[newNode] + dist.get(newNode, into);
        final double prevDist = length[into];

        if (prevDist > newDist) {
          length[into] = newDist;

          //	find and remove previously queued entry
          if (!Double.isInfinite(prevDist)) {
            int prevPos = qLen.binarySearch(prevDist);
            if (prevPos >= 0) {
              while (qLen.get(prevPos) == prevDist) {
                if (qIdx.get(prevPos) == into) {
                  qIdx.remove(prevPos);
                  qLen.remove(prevPos);
                  break;
                }
                prevPos++;
              }
            }
          }

          int newPos = qLen.binarySearch(newDist);
          final int newInsPos = newPos >= 0 ? newPos : -(newPos + 1);
          qLen.insert(newInsPos, newDist);
          qIdx.insert(newInsPos, into);
        }
      }

      was.set(newNode, true);
    }
  }

  protected static class RouteJohnson extends Route {
    protected double length;

    public RouteJohnson(double length) {
      this.length = length;
    }

    public TIntArrayList getIndexes(TIntArrayList indexes) {
      return null;
    }

    public double doubleValue() {
      return this.length;
    }
  }
}
