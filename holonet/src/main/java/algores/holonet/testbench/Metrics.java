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

import algores.holonet.core.NetworkInterceptor;
import org.akraievoy.base.runner.api.Context;

/**
 * Bean that stores different network statistical metrics.
 * Convenience dynamic update methods are provided for gathering the metrics.
 */
public class Metrics implements NetworkInterceptor {
  private final String periodName;

  private Metrics(String newPeriodName) {
    periodName = newPeriodName;
  }

  public String getPeriodName() {
    return periodName;
  }

  //	---------------------------
  //	hopcount/latency aggregates
  //	---------------------------
  private long totalHopCount;
  private long totalRequests;
  private double totalLatency;
  private double totalLatencyRatios;

  public double getMeanLatency() {
    return totalLatency / totalRequests;
  }

  public double getMeanPathLength() {
    return (double) totalHopCount / totalRequests;
  }

  public long getTotalRequests() {
    return totalRequests;
  }

  public double getMeanLatencyRatio() {
    return totalLatencyRatios;
  }

  public void registerLookup(final double latency, final long hopCount, final double directLatency) {
    totalLatency += latency;
    totalHopCount += hopCount;
    totalLatencyRatios = directLatency > 1.0e-9 ? latency / directLatency : 1;
    totalRequests++;
  }

  //	----------------------------
  //	lookup failure/success ratio
  //	----------------------------
  private long lookupSuccesses;
  private long lookupFailures;
  private long inconsistentLookups;

  public void registerLookupSuccess(final boolean successfull) {
    if (successfull) {
      lookupSuccesses += 1.0;
    } else {
      lookupFailures += 1.0;
    }
  }

  public void reportInconsistentLookup() {
    inconsistentLookups += 1;
  }

  public double getLookupConsistency() {
    return 1 - (double) inconsistentLookups / lookupSuccesses;
  }

  public double getLookupSuccessRatio() {
    return (double) lookupSuccesses / (lookupSuccesses + lookupFailures);
  }

  public double getLookupFailureRatio() {
    return (double) lookupFailures / (lookupSuccesses + lookupFailures);
  }

  //	----------------------------
  //	RPC failure/success ratio
  //	----------------------------
  private int rpcCallsSuccesses;
  private int rpcCallsFailures;

  public void registerRpcCallResult(boolean successful) {
    if (successful) {
      rpcCallsSuccesses++;
    } else {
      rpcCallsFailures++;
    }
  }

  public int getRpcCallsTotal() {
    return rpcCallsFailures + rpcCallsSuccesses;
  }

  public double getRpcFailureRatio() {
    return (double) rpcCallsFailures / (rpcCallsSuccesses + rpcCallsFailures);
  }

  public double getRpcSuccessRatio() {
    return (double) rpcCallsFailures / (rpcCallsSuccesses + rpcCallsFailures);
  }

  //	-------------------------------------
  //	Network structure dynamics aggregates
  //	-------------------------------------
  private int nodeDepartures;
  private int nodeFailures;
  private int nodeArrivalSuccesses;
  private int nodeArrivalFailures;

  public int getNodeArrivalSuccesses() {
    return nodeArrivalSuccesses;
  }

  public int getNodeDepartures() {
    return nodeDepartures;
  }

  public int getNodeFailures() {
    return nodeFailures;
  }

  public void registerNodeArrivals(final int nodeCount, boolean successful) {
    if (successful) {
      nodeArrivalSuccesses += nodeCount;
    } else {
      nodeArrivalFailures += nodeCount;
    }
  }

  public double getArrivalSuccessRatio() {
    return (double) nodeArrivalSuccesses / (nodeArrivalFailures + nodeArrivalSuccesses);
  }

  public int getNodeArrivalFailures() {
    return nodeArrivalFailures;
  }

  public void registerNodeFailures(final int nodeCount) {
    nodeFailures += nodeCount;
  }

  public void registerNodeDepartures(final int nodeCount) {
    nodeDepartures += nodeCount;
  }

  public static Metrics createInstance(String newPeriodName) {
    return new Metrics(newPeriodName);
  }

  public void store(Context ctx) {
    ctx.put(periodName + "_joins", getNodeArrivalSuccesses());
    ctx.put(periodName + "_joinFails", getNodeArrivalFailures());
    ctx.put(periodName + "_joinRatio", getArrivalSuccessRatio());
    ctx.put(periodName + "_leaves", getNodeDepartures());
    ctx.put(periodName + "_fails", getNodeFailures());

    ctx.put(periodName + "_lookups", getTotalRequests());
    ctx.put(periodName + "_hops", getMeanPathLength());
    ctx.put(periodName + "_delay", getMeanLatency());
    ctx.put(periodName + "_delayEff", getMeanLatencyRatio());

    ctx.put(periodName + "_lookupRatio", getLookupSuccessRatio());
    ctx.put(periodName + "_lookupCorr", getLookupConsistency());

    ctx.put(periodName + "_rpcs", getRpcCallsTotal());
    ctx.put(periodName + "_rpcRatio", getRpcSuccessRatio());
  }
}
