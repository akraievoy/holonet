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

import junit.framework.TestCase;
import org.akraievoy.cnet.metrics.api.Metric;
import org.akraievoy.holonet.exp.store.RefObject;
import org.akraievoy.cnet.net.vo.EdgeData;
import org.akraievoy.cnet.net.vo.EdgeDataFactory;

public class ScalarEigenGapTest extends TestCase {
  public void testGetMetric() throws Exception {
    EdgeData netEData = EdgeDataFactory.sparse(true, 5);

    netEData.set(0, 4, 1.0);
    netEData.set(1, 4, 1.0);
    netEData.set(2, 4, 1.0);
    netEData.set(3, 4, 1.0);

    netEData.set(3, 0, 1.0);
    netEData.set(3, 1, 1.0);
    netEData.set(3, 2, 1.0);

    netEData.set(0, 2, 1.0);
    netEData.set(1, 2, 1.0);

    netEData.set(1, 0, 1.0);

    final MetricScalarEigenGap metric = new MetricScalarEigenGap();
    metric.setSource(new RefObject<EdgeData>(netEData));

    final double v = Metric.fetch(metric);

    assertEquals(1.25, v, 1e-12);
  }

  public void testGetMetric_forPath() throws Exception {
    EdgeData netEData = EdgeDataFactory.sparse(true, 5);

    netEData.set(0, 1, 1.0);
    netEData.set(1, 2, 1.0);
    netEData.set(2, 3, 1.0);
    netEData.set(3, 4, 1.0);

    final MetricScalarEigenGap metric = new MetricScalarEigenGap();
    metric.setSource(new RefObject<EdgeData>(netEData));

    final double v = Metric.fetch(metric);

    assertEquals(1 - Math.cos(Math.PI / 4), v, 1e-12);
  }
}
