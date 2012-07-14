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
import org.akraievoy.base.ref.Ref;
import org.akraievoy.base.runner.api.RefObject;
import org.akraievoy.cnet.gen.domain.LocationGeneratorRecursive;
import org.akraievoy.cnet.gen.domain.MetricEDataGenStructural;
import org.akraievoy.cnet.gen.vo.ConnPreferenceYookJeongBarabasi;
import org.akraievoy.cnet.gen.vo.EntropySourceRandom;
import org.akraievoy.cnet.gen.vo.MetricEuclidean;
import org.akraievoy.cnet.gen.vo.Point;
import org.akraievoy.cnet.metrics.api.MetricResultFetcher;
import org.akraievoy.cnet.metrics.api.MetricRoutes;
import org.akraievoy.cnet.net.ref.RefEdgeData;
import org.akraievoy.cnet.net.ref.RefVertexData;
import org.akraievoy.cnet.net.vo.EdgeData;
import org.akraievoy.cnet.net.vo.EdgeDataFactory;
import org.akraievoy.cnet.net.vo.Route;
import org.akraievoy.cnet.net.vo.Routes;

public class MetricRoutesJohnsonTest extends TestCase {
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

    final MetricRoutes metric = new MetricRoutesJohnson();
    metric.setSource(new RefEdgeData(eData));

    final Routes routeData = (Routes) MetricResultFetcher.fetch(metric);

    final Route route = routeData.get(1, 4);

    assertEquals(4.0, route.doubleValue());
/*		final TIntArrayList indexes = route.getIndexes();

		assertNotNull(indexes);
		assertEquals(5, indexes.size());
		assertEquals(1, indexes.get(0));
		assertEquals(3, indexes.get(1));
		assertEquals(2, indexes.get(2));
		assertEquals(0, indexes.get(3));
		assertEquals(4, indexes.get(4));

		assertEquals(eData.weight(route, 0.0), route.doubleValue());*/
  }

  public void testRoutes_for01() {

    EdgeData eData = EdgeDataFactory.sparse(true, Double.POSITIVE_INFINITY, 9);

    eData.set(0, 1, 1.0);
    eData.set(1, 2, 1.0);
    eData.set(2, 3, 3.0);
    eData.set(2, 4, 2.0);
    eData.set(4, 5, 1.0);
    eData.set(5, 6, 1.0);
    eData.set(5, 8, 1.0);
    eData.set(6, 7, 1.0);
    final MetricRoutes metric = new MetricRoutesJohnson();
    metric.setSource(new RefEdgeData(eData));

    final Routes routeData = (Routes) MetricResultFetcher.fetch(metric);
    final Route route = routeData.get(1, 4);

    assertEquals(3.0, route.doubleValue());


  }

  public void testRoutes_for02() {

    EdgeData eData = EdgeDataFactory.sparse(true, Double.POSITIVE_INFINITY, 8);

    eData.set(0, 1, 3.0);
    eData.set(1, 2, 1.0);
    eData.set(2, 4, 1.0);
    eData.set(2, 5, 1.0);
    eData.set(3, 4, 2.0);
    eData.set(5, 6, 1.0);
    eData.set(6, 7, 2.0);
    final MetricRoutes metric = new MetricRoutesJohnson();
    metric.setSource(new RefEdgeData(eData));
    final Routes routeData = (Routes) MetricResultFetcher.fetch(metric);

    Route route = routeData.get(1, 4);
    assertEquals(2.0, route.doubleValue());

    route = routeData.get(1, 5);
    assertEquals(2.0, route.doubleValue());

    route = routeData.get(1, 6);
    assertEquals(3.0, route.doubleValue());


  }

  public void testRoutes_for03() {

    EdgeData eData = EdgeDataFactory.sparse(true, Double.POSITIVE_INFINITY, 8);


    eData.set(0, 1, 1.0);
    eData.set(1, 2, 1.0);
    eData.set(2, 3, 1.0);
    eData.set(3, 4, 1.0);
    eData.set(3, 5, 1.0);
    eData.set(4, 7, 4.0);
    eData.set(5, 6, 3.0);

    final MetricRoutes metric = new MetricRoutesJohnson();

    metric.setSource(new RefEdgeData(eData));

    final Routes routeData = (Routes) MetricResultFetcher.fetch(metric);

    final Route route = routeData.get(2, 5);

    assertEquals(2.0, route.doubleValue());
  }


  public void testRoutes_for04() {

    EdgeData eData = EdgeDataFactory.sparse(true, Double.POSITIVE_INFINITY, 6);

    eData.set(1, 3, 4.0);
    eData.set(1, 4, 3.0);
    eData.set(2, 4, 3.0);
    eData.set(2, 5, 3.0);
    eData.set(3, 5, 4.0);

    final MetricRoutes metric = new MetricRoutesJohnson();

    metric.setSource(new RefEdgeData(eData));

    final Routes routeData = (Routes) MetricResultFetcher.fetch(metric);

    final Route route = routeData.get(1, 3);

    assertEquals(4.0, route.doubleValue());
  }

  public void testRoutes_for05() {

    EdgeData eData = EdgeDataFactory.sparse(true, Double.POSITIVE_INFINITY, 6);

    eData.set(1, 2, 3.0);
    eData.set(1, 5, 3.0);
    eData.set(2, 3, 4.0);
    eData.set(2, 4, 5.0);
    eData.set(2, 5, 3.0);
    eData.set(3, 4, 3.0);
    eData.set(3, 5, 4.0);
    eData.set(4, 5, 4.0);
    final MetricRoutes metric = new MetricRoutesJohnson();

    metric.setSource(new RefEdgeData(eData));

    final Routes routeData = (Routes) MetricResultFetcher.fetch(metric);

    final Route route = routeData.get(2, 4);

    assertEquals(5.0, route.doubleValue());
  }

  public void testRoutes_forFW_512() {
    validateAgainstFW(512, 134534, 539450, 59403123, 39509843);
  }

  public void testRoutes_forFW_256() {
    validateAgainstFW(256, 450394, 494805, 23049804, 39058409);
  }

  public void testRoutes_forFW_128() {
    validateAgainstFW(128, 348954, 969058, 93948490, 73948579);
  }

  public void testRoutes_forFW_64() {
    validateAgainstFW(64, 149854, 985043, 87093843, 79384570);
  }

  public void testRoutes_forFW_32() {
    validateAgainstFW(32, 243504, 109345, 12398343, 93049834);
  }

  protected void validateAgainstFW(final int size, final int locationProbSeed, final int locationSeed, final int distSeed, final int structSeed) {
    final EntropySourceRandom locationProbGen = new EntropySourceRandom();
    locationProbGen.setSeed(locationProbSeed);
    final EntropySourceRandom locationGen = new EntropySourceRandom();
    locationGen.setSeed(locationSeed);
    final EntropySourceRandom distGen = new EntropySourceRandom();
    distGen.setSeed(distSeed);
    final EntropySourceRandom structGen = new EntropySourceRandom();
    structGen.setSeed(structSeed);

    final RefVertexData refX = new RefVertexData();
    final RefVertexData refY = new RefVertexData();
    final RefEdgeData refDist = new RefEdgeData();
    final RefEdgeData refStruct = new RefEdgeData();

    final LocationGeneratorRecursive generator = new LocationGeneratorRecursive(locationProbGen);
    final MetricVDataLocation metricLocation = new MetricVDataLocation(locationGen, generator);

    metricLocation.setNodes(size);
    metricLocation.setTargetX(refX);
    metricLocation.setTargetY(refY);

    MetricEDataDistance metricDist = new MetricEDataDistance(new MetricEuclidean() {
      public double dist(Point iPoint, Point jPoint) {
        return 2 * distGen.nextDouble() * super.dist(iPoint, jPoint);
      }
    });
    metricDist.setSourceX(refX);
    metricDist.setSourceY(refY);
    metricDist.setTarget(refDist);
    metricDist.setSymmetric(true);

    final ConnPreferenceYookJeongBarabasi cpYJB = new ConnPreferenceYookJeongBarabasi();
    cpYJB.setAlpha(1);
    cpYJB.setBeta(1);

    final MetricEDataGenStructural metricStructGen = new MetricEDataGenStructural();
    metricStructGen.setTarget(refStruct);
    metricStructGen.setNetNodeNum(3);
    metricStructGen.setType(MetricEDataGenStructural.TYPE_PATH);

    final MetricEDataStructure metricStructure = new MetricEDataStructure(cpYJB, structGen);
    metricStructure.setBaseDegree(2);
    metricStructure.setDistSource(refDist);
    metricStructure.setStructureSource(refStruct);
    metricStructure.setTarget(refStruct);

    @SuppressWarnings({"unchecked"})
    final Ref<Routes> refRoutesFW = (Ref) new RefObject();

    final MetricRoutesFloydWarshall metricRoutesFW = new MetricRoutesFloydWarshall();
    metricRoutesFW.setSource(refStruct);
    metricRoutesFW.setDistSource(refDist);
    metricRoutesFW.setTarget(refRoutesFW);


    @SuppressWarnings({"unchecked"})
    final Ref<Routes> refRoutesJ = (Ref) new RefObject();

    final MetricRoutesJohnson metricRoutesJ = new MetricRoutesJohnson();
    metricRoutesJ.setSource(refStruct);
    metricRoutesJ.setDistSource(refDist);
    metricRoutesJ.setTarget(refRoutesJ);

    generator.run();
    metricLocation.run();
    metricDist.run();
    metricStructGen.run();
    metricStructure.run();
    metricRoutesFW.run();
    metricRoutesJ.run();

    final Routes routesFW = (Routes) refRoutesFW.getValue();
    final Routes routesJ = (Routes) refRoutesJ.getValue();

    for (int from = 0; from < size; from++) {
      for (int into = 0; into < size; into++) {
        final Route routeFW = routesFW.get(from, into);
        final Route routeJ = routesJ.get(from, into);

        assertEquals(
            from + "->" + into,
            routeFW.doubleValue(),
            routeJ.doubleValue(),
            1e-12
        );
      }
    }
  }

}
