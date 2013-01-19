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
import org.akraievoy.cnet.metrics.api.MetricRoutes;
import org.akraievoy.cnet.net.vo.EdgeData;
import org.akraievoy.cnet.net.vo.Routes;

import java.util.Arrays;

public class MetricRoutesFloydWarshall extends MetricRoutes {
  protected RefRO<EdgeData> distSource = new RefObject<EdgeData>();
  protected RefRO<EdgeData> source = new RefObject<EdgeData>();

  public String getName() {
    return "Floyd-Warshall Routes";
  }

  public void setDistSource(RefRO<EdgeData> distSource) {
    this.distSource = distSource;
  }

  public void setSource(RefRO<EdgeData> source) {
    this.source = source;
  }

  public void run() {
    final EdgeData eData = source.getValue();
    final EdgeData distEData = distSource.getValue();

    final int nodes = Math.max(eData.getSize(), distEData == null ? 0 : distEData.getSize());

    final double[] d = new double[nodes * nodes];
    final int[] p = new int[nodes * nodes];
    Arrays.fill(p, -1);

    //	LATER unroll this, iterate only by edges
    for (int i = 0; i < nodes; i++) {
      for (int j = 0; j < nodes; j++) {
        final int index = i * nodes + j;

        //	here we combine connectivity and weight data from different layers
        final boolean connIJ = eData.conn(i, j);
        if (i == j) {
          d[index] = 0;
        } else {
          if (connIJ) {
            d[index] = distEData == null ? eData.weight(i, j) : distEData.weight(i, j);
          } else {
            d[index] = Double.POSITIVE_INFINITY;
          }
        }

        if (i != j && connIJ) {
          p[index] = i;
        }
      }
    }

    floydWarshall(nodes, d, p);

    final Routes routes = new Routes(nodes);
    for (int i = 0; i < nodes; i++) {
      for (int j = 0; j < nodes; j++) {
        routes.set(i, j, new RouteFloydWarshall(nodes, i, j, d, p));
      }
    }

    target.setValue(routes);
  }

  protected void floydWarshall(int nodes, double[] d, int[] p) {
//		@debug
//		dump(-1, nodes, d, p);

    for (int k = 0; k < nodes; k++) {
      for (int i = 0; i < nodes; i++) {
        for (int j = 0; j < nodes; j++) {
          final int index = i * nodes + j;
          double dij = d[index];
          double dik = d[i * nodes + k];
          double dkj = d[k * nodes + j];

          if (dij > dik + dkj) {
            d[index] = dik + dkj;
            p[index] = p[k * nodes + j];
          }
        }
      }

//			@debug
//			dump(k, nodes, d, p);
    }
  }

  @SuppressWarnings({"UnusedDeclaration"})
  protected void dump(int k, int nodes, double[] d, int[] p) {
    System.out.println("[[ k = " + k + " ]] ");
    for (int i = 0; i < nodes; i++) {
      System.out.print("\t[");
      for (int j = 0; j < nodes; j++) {
        final double dij = d[(i * nodes + j)];
        final String dijStr = dij != Double.POSITIVE_INFINITY ? String.valueOf(dij) : "inf";

        System.out.print("\t" + dijStr);
      }
      System.out.println("\t]");
    }
    System.out.println("");
    for (int i = 0; i < nodes; i++) {
      System.out.print("\t[");
      for (int j = 0; j < nodes; j++) {
        final int pij = p[(i * nodes + j)];
        final String pijStr = pij >= 0 ? String.valueOf(pij) : "nil";

        System.out.print("\t" + pijStr);
      }
      System.out.println("\t]");
    }
    System.out.println("");
  }
}
