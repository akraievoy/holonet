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
import org.akraievoy.cnet.net.vo.Route;

public class RouteFloydWarshall extends Route {
  protected final int nodes;
  protected final int from;
  protected final int into;
  protected final double[] dist;
  protected final int[] prec;

  public RouteFloydWarshall(int nodes, int from, int into, double[] dist, int[] prec) {
    super();
    this.nodes = nodes;
    this.from = from;
    this.into = into;
    this.dist = dist;
    this.prec = prec;
  }

  public TIntArrayList getIndexes(final TIntArrayList indexes) {
    indexes.clear();
    if (prec[from * nodes + into] < 0) {
      return indexes;
    }

    indexes.add(into);
    int curInto = into;
    while ((curInto = prec[from * nodes + curInto]) >= 0) {
      indexes.insert(0, curInto);
    }

    return indexes;
  }

  public double doubleValue() {
    return dist[from * nodes + into];
  }
}
