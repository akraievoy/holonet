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
import org.akraievoy.base.runner.api.Context;

/**
 * Metrics that make sense in static context.
 */
public class Snapshot {
  private final String name;

  public Snapshot(String newName) {
    name = newName;
  }

  public String getName() {
    return name;
  }

  public void process(final Network network) {
    nodeCount = network.getAllNodes().size();
    processMean(network);
    processDeviation(network);
  }

  private void processMean(final Network network) {
    totalMappings = 0;
    for (Node node : network.getAllNodes()) {
      totalMappings += node.getServices().getStorage().getDataEntries().size();
    }
  }

  /**
   * Actually this is not a classic deviation (I think). It is normalized against
   * mean^-2 to make this metric consistent with situations with different mean value.
   */
  private void processDeviation(final Network network) {
    final double mean = getMappingsAverage();
    double deviationTotal = 0;
    for (Node node : network.getAllNodes()) {
      final double entryDeviation = 1 - node.getServices().getStorage().getDataEntries().size() / mean;
      deviationTotal += Math.pow(entryDeviation, 2);
    }

    deviation = deviationTotal / nodeCount;
  }

  private int nodeCount;
  private int totalMappings;
  private double deviation;

  public double getMappingsAverage() {
    return (double) totalMappings / nodeCount;
  }

  public int getNodeCount() {
    return nodeCount;
  }

  public int getTotalMappings() {
    return totalMappings;
  }

  public void store(Context ctx) {
    ctx.put(name + "_nodeCount", getNodeCount());
    ctx.put(name + "_elemCount", getTotalMappings());
    ctx.put(name + "_elemPerNodeAvg", getMappingsAverage());
    ctx.put(name + "_elemPerNodeDev", deviation);
  }
}
