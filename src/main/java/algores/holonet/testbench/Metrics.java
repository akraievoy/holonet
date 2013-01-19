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
import org.akraievoy.holonet.exp.store.StoreLens;

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

  public void store(StoreLens<Double> reportLens ) {
    reportLens.forTypeName(Integer.class, periodName + "_joinSuccessCount").set(getNodeArrivalSuccesses());
    reportLens.forTypeName(Integer.class, periodName + "_joinFailCount").set(getNodeArrivalFailures());
    reportLens.forName(periodName + "_joinSuccessRatio").set(getArrivalSuccessRatio());
    reportLens.forTypeName(Integer.class, periodName + "_leaveCount").set(getNodeDepartures());
    reportLens.forTypeName(Integer.class, periodName + "_failCount").set(getNodeFailures());

    for (LookupService.Mode mode : LookupService.Mode.values()) {
      final LookupMetrics lookups = modeToLookups(mode);
      final String prefix = "_lookup_" + mode.toString().toLowerCase();
      reportLens.forTypeName(Long.class, periodName + prefix + "_count").set(lookups.getLookupCount());
      reportLens.forName(periodName + prefix + "_hopAvg").set(lookups.getMeanPathLength());
      reportLens.forName(periodName + prefix + "_delayAvg").set(lookups.getMeanLatency());
      reportLens.forName(periodName + prefix + "_vsDirectRatioAvg").set(lookups.getLookupVsDirectRatioAvg());
      reportLens.forTypeName(Long.class, periodName + prefix + "_vsDirectCount").set(lookups.getLookupVsDirectCount());
      reportLens.forTypeName(Long.class, periodName + prefix + "_successes").set(lookups.getLookupSuccesses());
      reportLens.forTypeName(Long.class, periodName + prefix + "_failures").set(lookups.getLookupFailures());
      reportLens.forName(periodName + prefix + "_successRatio").set(lookups.getLookupSuccessRatio());
      reportLens.forName(periodName + prefix + "_successRatioTimesHopsAvg").set(lookups.getLookupSuccessRatio() * lookups.getMeanPathLength());
      reportLens.forName(periodName + prefix + "_correctRatio").set(lookups.getLookupConsistency());
      reportLens.forName(periodName + prefix + "_routingServiceRouteCountAvg").set(lookups.getRoutingServiceRouteCountAvg());
      reportLens.forName(periodName + prefix + "_routingServiceRedundancyAvg").set(lookups.getRoutingServiceRedundancyAvg());
      reportLens.forName(periodName + prefix + "_routingServiceRedundancyChangeAvg").set(lookups.getRoutingServiceRedundancyChangeAvg());
      reportLens.forName(periodName + prefix + "_routeRedundancyAvg").set(lookups.getRouteRedundancyAvg());
      reportLens.forName(periodName + prefix + "_routeExhaustionAvg").set(lookups.getRouteExhaustionAvg());
      reportLens.forName(periodName + prefix + "_routeRetractionAvg").set(lookups.getRouteRetractionAvg());
      reportLens.forName(periodName + prefix + "_routeRpcFailRatioAvg").set(lookups.getRouteRpcFailRatioAvg());
    }

    reportLens.forTypeName(Integer.class, periodName + "_rpcCount").set(getRpcCallsTotal());
    reportLens.forName(periodName + "_rpcSuccessRatio").set(getRpcSuccessRatio());
  }
}
