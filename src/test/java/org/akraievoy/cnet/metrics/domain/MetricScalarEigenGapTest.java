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
import org.akraievoy.holonet.exp.store.RefObject;
import org.akraievoy.cnet.metrics.api.MetricResultFetcher;
import org.akraievoy.cnet.net.vo.EdgeData;
import org.akraievoy.cnet.net.vo.EdgeDataFactory;
import org.akraievoy.cnet.net.vo.VertexData;

import java.util.Arrays;

public class MetricScalarEigenGapTest extends TestCase {
  public void testStructures() {
    final EdgeData path = EdgeDataFactory.sparse(true, 0, 6);

    path.set(0, 1, 1.0);
    path.set(1, 2, 1.0);
    path.set(2, 3, 1.0);
    path.set(3, 4, 1.0);
    path.set(4, 5, 1.0);

    final EdgeData star = EdgeDataFactory.sparse(true, 0, 6);

    star.set(0, 1, 1.0);
    star.set(0, 2, 1.0);
    star.set(0, 3, 1.0);
    star.set(0, 4, 1.0);
    star.set(0, 5, 1.0);

    final EdgeData cycle = EdgeDataFactory.sparse(true, 0, 6);

    cycle.set(0, 1, 1.0);
    cycle.set(1, 2, 1.0);
    cycle.set(2, 3, 1.0);
    cycle.set(3, 4, 1.0);
    cycle.set(4, 5, 1.0);
    cycle.set(5, 0, 1.0);

    final EdgeData star2 = EdgeDataFactory.sparse(true, 0, 6);

    star2.set(0, 1, 1.0);
    star2.set(1, 2, 1.0);
    star2.set(2, 0, 1.0);
    star2.set(0, 3, 1.0);
    star2.set(1, 4, 1.0);
    star2.set(2, 5, 1.0);

    final MetricScalarEigenGap metric = new MetricScalarEigenGap();
    metric.setSource(new RefObject<EdgeData>(path));
    final MetricVDataEigenGap vdata = new MetricVDataEigenGap();
    vdata.setSource(new RefObject<EdgeData>(path));

    final Double pathGap = (Double) MetricResultFetcher.fetch(metric);
		System.out.println("path = " + pathGap);
		System.out.println("path = " + Arrays.toString(((VertexData) MetricResultFetcher.fetch(vdata)).getData()));

    metric.setSource(new RefObject<EdgeData>(star));
    vdata.setSource(new RefObject<EdgeData>(star));

    final Double starGap = (Double) MetricResultFetcher.fetch(metric);
		System.out.println("star = " + starGap);
		System.out.println("star = " + Arrays.toString(((VertexData) MetricResultFetcher.fetch(vdata)).getData()));
    assertTrue(starGap > pathGap);

    metric.setSource(new RefObject<EdgeData>(cycle));
    vdata.setSource(new RefObject<EdgeData>(cycle));

    final Double cycleGap = (Double) MetricResultFetcher.fetch(metric);
		System.out.println("cycle = " + cycleGap);
		System.out.println("cycle = " + Arrays.toString(((VertexData) MetricResultFetcher.fetch(vdata)).getData()));

    metric.setSource(new RefObject<EdgeData>(star2));
    vdata.setSource(new RefObject<EdgeData>(star2));

    final Double star2Gap = (Double) MetricResultFetcher.fetch(metric);
		System.out.println("star2 = " + star2Gap);
		System.out.println("star2 = " + Arrays.toString(((VertexData) MetricResultFetcher.fetch(vdata)).getData()));
    assertTrue(star2Gap > cycleGap);
  }
}
