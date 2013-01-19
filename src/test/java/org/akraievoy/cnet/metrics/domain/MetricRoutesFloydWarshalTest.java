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
import junit.framework.TestCase;
import org.akraievoy.holonet.exp.store.RefObject;
import org.akraievoy.cnet.metrics.api.MetricResultFetcher;
import org.akraievoy.cnet.metrics.api.MetricRoutes;
import org.akraievoy.cnet.net.vo.EdgeData;
import org.akraievoy.cnet.net.vo.EdgeDataFactory;
import org.akraievoy.cnet.net.vo.Route;
import org.akraievoy.cnet.net.vo.Routes;

public class MetricRoutesFloydWarshalTest extends TestCase {
  public void testFloyfWarshall() {
    final MetricRoutesFloydWarshall metricShortestRoutes = new MetricRoutesFloydWarshall();

    final double inf = Double.POSITIVE_INFINITY;
    final double[] d = {
        0, 3, 8, inf, -4,
        inf, 0, inf, 1, 7,
        inf, 4, 0, inf, inf,
        2, inf, -5, 0, inf,
        inf, inf, inf, 6, 0
    };
    final int nil = -1;
    final int[] p = {
        nil, 0, 0, nil, 0,
        nil, nil, nil, 1, 1,
        nil, 2, nil, nil, nil,
        3, nil, 3, nil, nil,
        nil, nil, nil, 4, nil
    };

    metricShortestRoutes.floydWarshall(5, d, p);

    assertEquals(4, p[3]);
    assertEquals(8.0, d[20]);
  }

  /*

 Checking this simple topology to see how FW behaves in general
                0
             /    \
            [1]     [1]
           /          \
   1 --- [3] --- 2 --- [3] --- 4
    \          /
     [1]    [1]
     \   /
       3
   */
  public void testRoutes() {
    EdgeData eData = EdgeDataFactory.sparse(true, Double.POSITIVE_INFINITY, 5);

    eData.set(1, 2, 3.0);
    eData.set(1, 3, 1.0);
    eData.set(3, 2, 1.0);

    eData.set(2, 0, 1.0);
    eData.set(0, 4, 1.0);
    eData.set(2, 4, 3.0);

    final MetricRoutes metric = new MetricRoutesFloydWarshall();
    metric.setSource(new RefObject<EdgeData>(eData));

    final Routes routeData = (Routes) MetricResultFetcher.fetch(metric);

    final Route route = routeData.get(1, 4);

    assertEquals(4.0, route.doubleValue());
    final TIntArrayList indexes = route.getIndexes();

    assertNotNull(indexes);
    assertEquals(5, indexes.size());
    assertEquals(1, indexes.get(0));
    assertEquals(3, indexes.get(1));
    assertEquals(2, indexes.get(2));
    assertEquals(0, indexes.get(3));
    assertEquals(4, indexes.get(4));

    assertEquals(eData.weight(route, 0.0), route.doubleValue());
  }
}
