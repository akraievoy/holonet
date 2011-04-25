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

import org.akraievoy.cnet.metrics.api.MetricEData;
import org.akraievoy.cnet.metrics.api.MetricResultFetcher;
import org.akraievoy.cnet.metrics.api.MetricRoutes;
import org.akraievoy.cnet.net.vo.EdgeData;
import org.akraievoy.cnet.net.vo.EdgeDataFactory;
import org.akraievoy.cnet.net.vo.Routes;

public class MetricEDataRouteLen extends MetricEData {
  protected final MetricRoutes routes;
  protected boolean symmetric = false;

  public MetricEDataRouteLen(MetricRoutes routes) {
    this.routes = routes;
  }

  public void setSymmetric(boolean symmetric) {
    this.symmetric = symmetric;
  }

  public MetricRoutes getRoutes() {
    return routes;
  }

  public void run() {
    final Routes routesObj = (Routes) MetricResultFetcher.fetch(routes);

    final int size = routesObj.getSize();
    //	LATER actually we should check whether all routes lengths are symmetric
    //	routes are not symmetric as i -> j indexes are inverted from j -> i (at best)
    final EdgeData data = EdgeDataFactory.dense(symmetric, Double.POSITIVE_INFINITY, size);

    for (int i = 0; i < size; i++) {
      for (int j = 0; j <= i; j++) {
        data.set(i, j, routesObj.get(i, j).doubleValue());
      }

      if (data.isSymmetric()) {
        continue;
      }

      for (int j = i + 1; j < size; j++) {
        data.set(i, j, routesObj.get(i, j).doubleValue());
      }
    }

    target.setValue(data);
  }

  public String getName() {
    return "Route Lengths";
  }
}
