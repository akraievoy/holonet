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
import algores.holonet.core.NetworkInterceptor;
import algores.holonet.core.Node;
import algores.holonet.core.api.Address;
import algores.holonet.core.api.tier1.delivery.LookupService;
import org.akraievoy.base.ref.Ref;
import org.akraievoy.cnet.net.vo.EdgeData;
import org.akraievoy.cnet.net.vo.EdgeDataFactory;
import org.akraievoy.cnet.net.vo.VertexData;
import org.akraievoy.holonet.exp.store.StoreLens;

import java.util.HashMap;
import java.util.Map;

/**
 * Bean that stores different network statistical metrics.
 * Convenience dynamic update methods are provided for gathering the metrics.
 */
public class Metrics implements NetworkInterceptor {
  private final Network network;
  private final String periodName;

  private Metrics(Network network, String newPeriodName) {
    this.network = network;
    periodName = newPeriodName;
    for (LookupService.Mode mode : LookupService.Mode.values()) {
      modeToLookups.put(mode, new LookupMetrics(network));
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
  private LazyCounters<WeightedCounters> rpcFailureCounters =
      new LazyCounters<WeightedCounters>(new WeightedCounters());
  private int rpcCallsSuccesses;
  private int rpcCallsFailures;

  public void registerRpcCallResult(
      final Address source,
      final Address target,
      final boolean successful
  ) {
    final double failureWeight;
    if (successful) {
      rpcCallsSuccesses++;
      failureWeight = 0;
    } else {
      rpcCallsFailures++;
      failureWeight = 1;
    }

    final int sourceIndex = network.getEnv().indexOf(source);
    final int targetIndex = network.getEnv().indexOf(target);
    rpcFailureCounters.at(sourceIndex).post(targetIndex, failureWeight);
  }

  public int getRpcCallsTotal() {
    return rpcCallsFailures + rpcCallsSuccesses;
  }

  public double getRpcSuccessRatio() {
    return (double) rpcCallsSuccesses / (rpcCallsSuccesses + rpcCallsFailures);
  }

  //	-------------------------------------
  //	Network structure dynamics aggregates
  //	-------------------------------------
  private WeightedCounters rangeSizeCounters = new WeightedCounters();
  private int nodeDepartures;
  private int nodeFailures;
  private int nodeArrivalSuccesses;
  private int nodeArrivalFailures;

  public int getNodeJoins() {
    return nodeArrivalSuccesses + nodeArrivalFailures;
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

  public void registerNodeFailure(Address address, double rangeWidth) {
    nodeFailures += 1;
    rangeSizeCounters.post(
        network.getEnv().indexOf(address),
        rangeWidth
    );
  }

  public void registerNodeDeparture(Address address, double rangeWidth) {
    nodeDepartures += 1;
    rangeSizeCounters.post(
        network.getEnv().indexOf(address),
        rangeWidth
    );
  }

  public static Metrics createInstance(Network network, String newPeriodName) {
    return new Metrics(network, newPeriodName);
  }

  public void store(StoreLens<Double> reportLens ) {
    reportLens.forTypeName(Integer.class, periodName + "Joins").set(getNodeJoins());
    reportLens.forName(periodName + "JoinScsRatio").set(getArrivalSuccessRatio());
    reportLens.forTypeName(Integer.class, periodName + "LeaveCount").set(getNodeDepartures());
    reportLens.forTypeName(Integer.class, periodName + "FailCount").set(getNodeFailures());

    for (LookupService.Mode mode : LookupService.Mode.values()) {
      final LookupMetrics lookups = modeToLookups(mode);
      final String prefix = capitalize(mode.toString().toLowerCase());
      reportLens.forTypeName(Long.class, periodName + prefix + "Count").set(lookups.getLookupCount());
      reportLens.forName(periodName + prefix + "HopAvg").set(lookups.getMeanPathLength());
      reportLens.forName(periodName + prefix + "DelayAvg").set(lookups.getMeanLatency());
      reportLens.forName(periodName + prefix + "VsDirectRatioAvg").set(lookups.getLookupVsDirectRatioAvg());
      reportLens.forName(periodName + prefix + "ScsRatio").set(lookups.getLookupSuccessRatio());
      reportLens.forName(periodName + prefix + "CorrRatio").set(lookups.getLookupConsistency());
      reportLens.forName(periodName + prefix + "RoutingServiceRouteCountAvg").set(lookups.getRoutingServiceRouteCountAvg());
      reportLens.forName(periodName + prefix + "RoutingServiceRedundancyAvg").set(lookups.getRoutingServiceRedundancyAvg());
      reportLens.forName(periodName + prefix + "RoutingServiceRedundancyChangeAvg").set(lookups.getRoutingServiceRedundancyChangeAvg());
      reportLens.forName(periodName + prefix + "RouteRedundancyAvg").set(lookups.getRouteRedundancyAvg());
      reportLens.forName(periodName + prefix + "RouteExhaustionAvg").set(lookups.getRouteExhaustionAvg());
      reportLens.forName(periodName + prefix + "RouteRetractionAvg").set(lookups.getRouteRetractionAvg());
      reportLens.forName(periodName + prefix + "RouteRpcFailRatioAvg").set(lookups.getRouteRpcFailRatioAvg());
    }

    reportLens.forTypeName(Integer.class, periodName + "RpcCount").set(getRpcCallsTotal());
    reportLens.forName(periodName + "RpcScsRatio").set(getRpcSuccessRatio());
  }

  public String capitalize(String s) {
    return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
  }

  public void storeStats(
      Ref<VertexData> rangeSizesRef,
      Ref<EdgeData> rpcCountsRef,
      Ref<EdgeData> rpcFailuresRef,
      Ref<EdgeData> lookupCountsRef,
      Ref<EdgeData> lookupFailuresRef
  ) {
    for (Node node : network.getAllNodes()) {
      this.rangeSizeCounters.post(
          network.getEnv().indexOf(node.getAddress()),
          node.getServices().getRouting().getOwnRoute().getRange().width().doubleValue()
      );
    }
    double[] rangeSizeAverages = rangeSizeCounters.averages(0);
    int usedAddressCount = rangeSizeAverages.length;
    VertexData rangeSizesValue = new VertexData(usedAddressCount);
    for (int i = 0; i < usedAddressCount; i++) {
      rangeSizesValue.set(i, rangeSizeAverages[i]);
    }
    rangeSizesRef.setValue(rangeSizesValue);

    final EdgeData rpcCounts = EdgeDataFactory.dense(false, 0, usedAddressCount);
    final EdgeData rpcFailures = EdgeDataFactory.dense(false, 0, usedAddressCount);
    for (int from = 0; from < usedAddressCount; from++) {
      final double[] posts = rpcFailureCounters.at(from).posts();
      final double[] averages = rpcFailureCounters.at(from).averages(0);

      for (int into = 0; into < posts.length; into++) {
        rpcCounts.set(from, into, posts[into]);
        rpcFailures.set(from, into, averages[into]);
      }
    }
    rpcCountsRef.setValue(rpcCounts);
    rpcFailuresRef.setValue(rpcFailures);

    final LazyCounters<WeightedCounters> getFailureCounters =
        modeToLookups(LookupService.Mode.GET).getLookupFailureCounters();

    final EdgeData lookupCounts = EdgeDataFactory.dense(false, 0, usedAddressCount);
    final EdgeData lookupFailures = EdgeDataFactory.dense(false, 0, usedAddressCount);
    for (int from = 0; from < usedAddressCount; from++) {
      final double[] posts = getFailureCounters.at(from).posts();
      final double[] averages = getFailureCounters.at(from).averages(0);

      for (int into = 0; into < posts.length; into++) {
        lookupCounts.set(from, into, posts[into]);
        lookupFailures.set(from, into, averages[into]);
      }
    }
    lookupCountsRef.setValue(lookupCounts);
    lookupFailuresRef.setValue(lookupFailures);
  }
}
