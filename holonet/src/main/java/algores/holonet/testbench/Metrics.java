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
import algores.holonet.core.api.tier1.delivery.LookupService;
import org.akraievoy.base.runner.api.Context;

import java.util.HashMap;
import java.util.Map;

/**
 * Bean that stores different network statistical metrics.
 * Convenience dynamic update methods are provided for gathering the metrics.
 */
public class Metrics implements NetworkInterceptor {
  private final String periodName;

  private Metrics(String newPeriodName) {
    periodName = newPeriodName;
    for (LookupService.Mode mode : LookupService.Mode.values()) {
      modeToLookups.put(mode, new LookupMetrics());
    }
  }

  public String getPeriodName() {
    return periodName;
  }

  public void registerLookup(
      final LookupService.Mode mode,
      final double latency,
      final long hopCount,
      final double directLatency,
      boolean success
  ) {
    modeToLookups(mode).registerLookup(
        latency, hopCount, directLatency, success
    );
  }

  @Override
  public LookupMetrics modeToLookups(LookupService.Mode mode) {
    return modeToLookups.get(mode);
  }

  public void reportInconsistentLookup(LookupService.Mode mode) {
    modeToLookups(mode).reportInconsistentLookup();
  }

  private Map<LookupService.Mode, LookupMetrics> modeToLookups =
    new HashMap<LookupService.Mode, LookupMetrics>(
        2*LookupService.Mode.values().length
    );

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
    return (double) rpcCallsSuccesses / (rpcCallsSuccesses + rpcCallsFailures);
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
    ctx.put(periodName + "_joinSuccessCount", getNodeArrivalSuccesses());
    ctx.put(periodName + "_joinFailCount", getNodeArrivalFailures());
    ctx.put(periodName + "_joinSuccessRatio", getArrivalSuccessRatio());
    ctx.put(periodName + "_leaveCount", getNodeDepartures());
    ctx.put(periodName + "_failCount", getNodeFailures());

    for (LookupService.Mode mode : LookupService.Mode.values()) {
      final LookupMetrics lookups = modeToLookups(mode);
      final String prefix = "_lookup_" + mode.toString().toLowerCase();
      ctx.put(
          periodName + prefix + "_count",
          lookups.getLookupCount()
      );
      ctx.put(
          periodName + prefix + "_hopAvg",
          lookups.getMeanPathLength()
      );
      ctx.put(
          periodName + prefix + "_delayAvg",
          lookups.getMeanLatency()
      );
      ctx.put(
          periodName + prefix + "_vsDirectRatioAvg",
          lookups.getLookupVsDirectRatioAvg()
      );
      ctx.put(
          periodName + prefix + "_vsDirectCount",
          lookups.getLookupVsDirectCount()
      );
      ctx.put(
          periodName + prefix + "_successes",
          lookups.getLookupSuccesses()
      );
      ctx.put(
          periodName + prefix + "_failures",
          lookups.getLookupFailures()
      );
      ctx.put(
          periodName + prefix + "_successRatio",
          lookups.getLookupSuccessRatio()
      );
      ctx.put(
          periodName + prefix + "_successRatioTimesHopsAvg",
          lookups.getLookupSuccessRatio() * lookups.getMeanPathLength()
      );
      ctx.put(
          periodName + prefix + "_correctRatio",
          lookups.getLookupConsistency()
      );
    }

    ctx.put(periodName + "_rpcCount", getRpcCallsTotal());
    ctx.put(periodName + "_rpcSuccessRatio", getRpcSuccessRatio());
  }
}
