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
import org.akraievoy.holonet.exp.store.StoreLens;
import scala.Tuple4;

import java.util.Collection;

/**
 * Metrics that make sense in static context.
 */
public class Snapshot {
  private Tuple4<Integer, Double, Double, Double> elemStats;
  private Tuple4<Integer, Double, Double, Double> rangeStats;

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
      return n.getServices().getRouting().getOwnRoute().getRange().width().doubleValue();
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
  }

  public double getKeyRangePerNodeAvg() {
    return rangeStats._3();
  }

  public double getKeyRangePerNodeDev() {
    return rangeStats._4();
  }

  public double getElemPerNodeAvg() {
    return elemStats._3();
  }

  public double getElemPerNodeDev() {
    return elemStats._4();
  }

  public int getNodeCount() {
    return elemStats._1();
  }

  public double getElemCount() {
    return elemStats._2();
  }

  public void store(StoreLens<Double> reportLens) {
    reportLens.forTypeName(Integer.class, name + "_nodeCount").set(elemStats._1());
    reportLens.forTypeName(Double.class, name + "_elemCount").set(elemStats._2());
    reportLens.forName(name + "_elemsPerNodeAvg").set(elemStats._3());
    reportLens.forName(name + "_elemsPerNodeDev").set(elemStats._4());
    reportLens.forName(name + "_rangePerNodeAvg").set(rangeStats._3());
    reportLens.forName(name + "_rangePerNodeDev").set(rangeStats._4());
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
