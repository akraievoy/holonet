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

package algores.holonet.testbench;

import algores.holonet.core.Network;
import algores.holonet.core.Node;
import algores.holonet.core.api.Address;
import algores.holonet.core.api.tier0.routing.RoutingService;
import org.akraievoy.cnet.metrics.api.Metric;
import org.akraievoy.cnet.metrics.domain.MetricScalarEigenGap;
import org.akraievoy.cnet.net.vo.EdgeData;
import org.akraievoy.cnet.net.vo.EdgeDataFactory;
import org.akraievoy.cnet.net.vo.EdgeDataSparse;
import org.akraievoy.holonet.exp.store.RefObject;
import org.akraievoy.holonet.exp.store.StoreLens;
import scala.Tuple4;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Metrics that make sense in static context.
 */
public class Snapshot {
  private Tuple4<Integer, Double, Double, Double> elemStats;
  private Tuple4<Integer, Double, Double, Double> rangeStats;
  private EdgeData linksAll;
  private EdgeData linksSeed;
  private EdgeData linksDht;
  private Double lambdaAll;
  private Double lambdaDht;
  private Double lambdaSeed;

  public static interface NodeFun{
    public double apply(final Node n);
  }

  public static NodeFun ELEMS = new NodeFun() {
    @Override
    public double apply(final Node n) {
      return n.getServices().getStorage().getDataEntries().size();
    }
  };

  public static NodeFun RANGE = new NodeFun() {
    @Override
    public double apply(final Node n) {
      return n.getServices().getRouting().ownRoute().getRange().width().doubleValue();
    }
  };

  private final String name;

  public Snapshot(String newName) {
    name = newName;
  }

  public String getName() {
    return name;
  }

  public void process(final Network network) {
    elemStats = processFun(network, ELEMS);
    rangeStats = processFun(network, RANGE);

    final Collection<Node> nodes = network.getAllNodes();
    int maxIndex = 0;
    for (Node node : nodes) {
      maxIndex = Math.max(maxIndex, network.getEnv().indexOf(node.getAddress()));
    }

    linksAll = EdgeDataFactory.sparse(false, maxIndex + 1);
    linksSeed = EdgeDataFactory.sparse(false, maxIndex + 1);
    linksDht = EdgeDataFactory.sparse(false, maxIndex + 1);
    //  assymetric, keeping node gaps
    for (Node nodeFrom : nodes) {
      final RoutingService fromRouting = nodeFrom.getServices().getRouting();
      final int fromIndex = network.getEnv().indexOf(nodeFrom.getAddress());

      for (Node nodeInto : nodes) {
        final Address intoAddr = nodeInto.getAddress();
        if (nodeFrom.getAddress().equals(intoAddr)) {
          continue;
        }

        final boolean linkSeed =
            fromRouting.hasRouteFor(intoAddr, false, true);
        final boolean linkDht =
            fromRouting.hasRouteFor(intoAddr, true, false);

        final int intoIndex = network.getEnv().indexOf(nodeInto.getAddress());

        if (linkSeed || linkDht) {
          linksAll.set(fromIndex, intoIndex, 1);
        }
        if (linkDht) {
          linksDht.set(fromIndex, intoIndex, 1);
        }
        if (linkSeed) {
          linksSeed.set(fromIndex, intoIndex, 1);
        }
      }
    }

    final List<Integer> addrIndexes = new ArrayList<Integer>(nodes.size());
    for (Node node : nodes) {
      addrIndexes.add(network.getEnv().indexOf(node.getAddress()));
    }
    Collections.sort(addrIndexes);

    final EdgeData lambdaLinksAll = EdgeDataFactory.sparse(true, addrIndexes.size());
    final EdgeData lambdaLinksSeed = EdgeDataFactory.sparse(true, addrIndexes.size());
    final EdgeData lambdaLinksDht = EdgeDataFactory.sparse(true, addrIndexes.size());
    //  symmetric, avoiding node gaps
    for (Node nodeFrom : nodes) {
      final RoutingService fromRouting = nodeFrom.getServices().getRouting();
      final int fromIndex = Collections.binarySearch(
          addrIndexes,
          network.getEnv().indexOf(nodeFrom.getAddress())
      );

      for (Node nodeInto : nodes) {
        final Address intoAddr = nodeInto.getAddress();
        if (nodeFrom.getAddress().equals(intoAddr)) {
          continue;
        }

        final boolean linkSeed =
            fromRouting.hasRouteFor(intoAddr, false, true);
        final boolean linkDht =
            fromRouting.hasRouteFor(intoAddr, true, false);

        final int intoIndex = Collections.binarySearch(
            addrIndexes,
            network.getEnv().indexOf(nodeInto.getAddress())
        );

        if (linkSeed || linkDht) {
          lambdaLinksAll.set(fromIndex, intoIndex, 1);
        }
        if (linkDht) {
          lambdaLinksDht.set(fromIndex, intoIndex, 1);
        }
        if (linkSeed) {
          lambdaLinksSeed.set(fromIndex, intoIndex, 1);
        }
      }
    }

    final MetricScalarEigenGap eigenGapMetric = new MetricScalarEigenGap();

    eigenGapMetric.setSource(new RefObject<EdgeData>(lambdaLinksAll));
    lambdaAll = Metric.fetch(eigenGapMetric);

    eigenGapMetric.setSource(new RefObject<EdgeData>(lambdaLinksDht));
    lambdaDht = Metric.fetch(eigenGapMetric);

    eigenGapMetric.setSource(new RefObject<EdgeData>(lambdaLinksSeed));
    lambdaSeed = Metric.fetch(eigenGapMetric);



  }

  public void store(StoreLens<Double> reportLens) {
    reportLens.forTypeName(Integer.class, name + "_nodeCount").set(elemStats._1());
    reportLens.forTypeName(Double.class, name + "_elemCount").set(elemStats._2());
    reportLens.forName(name + "_elemsPerNodeAvg").set(elemStats._3());
    reportLens.forName(name + "_elemsPerNodeDev").set(elemStats._4());
    reportLens.forName(name + "_rangePerNodeAvg").set(rangeStats._3());
    reportLens.forName(name + "_rangePerNodeDev").set(rangeStats._4());

    reportLens.forName(name + "_lambdaAll").set(lambdaAll);
    reportLens.forName(name + "_lambdaDht").set(lambdaDht);
    reportLens.forName(name + "_lambdaSeed").set(lambdaSeed);

    reportLens.forTypeName(EdgeDataSparse.class, name + "_linksAll").set((EdgeDataSparse) linksAll);
    reportLens.forTypeName(EdgeDataSparse.class, name + "_linksDht").set((EdgeDataSparse) linksDht);
    reportLens.forTypeName(EdgeDataSparse.class, name + "_linksSeed").set((EdgeDataSparse) linksSeed);
  }

  protected static Tuple4<Integer, Double, Double, Double> processFun(
      final Network network,
      final NodeFun nodeFun
  ) {
    final Collection<Node> nodes = network.getAllNodes();
    int nodeCount = nodes.size();
    final double[] funVals = new double[nodeCount];
    {
      int nodeIndex = 0;
      for (Node node : nodes) {
        funVals[nodeIndex] = nodeFun.apply(node);
        nodeIndex++;
      }
    }

    double total = 0;
    for (double funVal : funVals) {
      total += funVal;
    }

    final double mean = total / nodeCount;

    double deviationTotal = 0;
    for (double funVal : funVals) {
      final double entryDeviation = funVal / mean - 1;
      deviationTotal += Math.pow(entryDeviation, 2);
    }

    final double deviation = Math.pow(deviationTotal / nodeCount, .5);

    return new Tuple4<Integer, Double, Double, Double>(
        nodeCount, total, mean, deviation
    );
  }
}
