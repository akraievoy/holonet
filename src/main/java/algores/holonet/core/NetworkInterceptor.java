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

package algores.holonet.core;

import algores.holonet.core.api.Address;
import algores.holonet.core.api.tier1.delivery.LookupService;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Unifies hooks for intercepting various network events.
 */
public interface NetworkInterceptor {
  //	---------------------------
  //	hopcount/latency aggregates
  //	---------------------------

  //	----------------------------
  //	lookup failure/success ratio
  //	----------------------------

  void reportInconsistentLookup(LookupService.Mode get);

  //	----------------------------
  //	RPC failure/success ratio
  //	----------------------------

  void registerRpcCallResult(Address address, Address target, boolean successful);

  //	-------------------------------------
  //	Network structure dynamics aggregates
  //	-------------------------------------

  void registerNodeArrivals(final int nodeCount, boolean successful);

  void registerNodeFailure(Address address, double rangeWidth);

  void registerNodeDeparture(Address address, double rangeWidth);

  LookupMetrics modeToLookups(LookupService.Mode mode);

  public static interface Counters<T> {
    public T proto();

    double[] posts();

    double[] averages(double unusedvalue);

    double[] totals();
  }

  public static class WeightedCounters implements Counters<WeightedCounters> {
    private double[] data = new double[0];

    public void post(int pos, double weight) {
      int pos0 = pos * 2;
      if (data.length <= pos0) {
        final double[] dataOld = data;
        data = new double[pos0 + 2];
        System.arraycopy(dataOld, 0, data, 0, dataOld.length);
      }
      data[pos0] += 1;
      data[pos0 + 1] += weight;
    }

    @Override
    public double[] averages(double unusedvalue) {
      double[] averages = new double[data.length / 2];
      for (int i = 0; i < averages.length; i++) {
        double postCount = data[i * 2];
        averages[i] = postCount > 0 ? data[i*2 + 1] / postCount : unusedvalue;
      }
      return averages;
    }

    @Override
    public double[] posts() {
      double[] totals = new double[data.length / 2];
      for (int i = 0; i < totals.length; i++) {
        totals[i] = data[i*2];
      }
      return totals;
    }

    @Override
    public double[] totals() {
      double[] totals = new double[data.length / 2];
      for (int i = 0; i < totals.length; i++) {
        totals[i] = data[i*2 + 1];
      }
      return totals;
    }

    @Override
    public WeightedCounters proto() {
      return new WeightedCounters();
    }
  }

  public static class LazyCounters<T extends Counters<T>> {
    private Map<Integer, T> indexToCounters =
        new HashMap<Integer, T>(256, 0.25f);

    private final T t;

    public LazyCounters(T t) {
      this.t = t;
    }

    public T at(int index) {
      final T stored = indexToCounters.get(index);
      if (stored != null) {
        return stored;
      }

      final T created = t.proto();
      indexToCounters.put(index, created);
      return created;
    }

    public Map<Integer, T> getIndexToCounters() {
      return Collections.unmodifiableMap(indexToCounters);
    }
  }

  //	----------------------------------------------------------
  //	hopcount/latency aggregates + lookup failure/success ratio
  //	----------------------------------------------------------
  public static class LookupMetrics {
    private final Network network;

    public LookupMetrics(Network network) {
      this.network = network;
    }

    private LazyCounters<WeightedCounters> lookupFailureCounters =
        new LazyCounters<WeightedCounters>(new WeightedCounters());
    public LazyCounters<WeightedCounters> getLookupFailureCounters() {
      return lookupFailureCounters;
    }

    private long totalHopCount;
    private long lookupCount;
    private double routeRedundancyTotal;
    private double routeRetractionTotal;
    private double routeExhaustionTotal;
    private double routeRpcFailRatioTotal;
    private double totalLatency;
    private double lookupVsDirectTotal;
    private long lookupVsDirectCount;
    private long lookupSuccesses;
    private long lookupFailures;
    private long inconsistentLookups;
    private double routingServiceRouteCountTotal;
    private double routingServiceRedundancyTotal;
    private double routingServiceRedundancyChangeTotal;
    private long routingServiceSnapshotCount;

    public double getMeanLatency() {
      return totalLatency / lookupSuccesses;
    }

    public double getMeanPathLength() {
      return (double) totalHopCount / lookupSuccesses;
    }

    public long getLookupCount() {
      return lookupCount;
    }

    public double getLookupVsDirectRatioAvg() {
      return lookupVsDirectTotal / lookupVsDirectCount;
    }

    public void registerLookup(
        final Address source,
        final Address target,
        final double latency,
        final long hopCount,
        final double routeRedundancy,
        final double routeRetraction,
        final double routeExhaustion,
        final double routeRpcFailRatio,
        final double directLatency,
        final boolean success
    ) {
      final double failureWeight;
      if (success) {
        totalLatency += latency;
        totalHopCount += hopCount;
        if (hopCount > 0 && directLatency > 1.0e-3) {
          lookupVsDirectTotal += latency / directLatency;
          lookupVsDirectCount++;
        }
        lookupSuccesses += 1.0;
        failureWeight = 0;
      } else {
        lookupFailures += 1.0;
        failureWeight = 1;
      }
      routeRedundancyTotal += routeRedundancy;
      routeRetractionTotal += routeRetraction;
      routeExhaustionTotal += routeExhaustion;
      routeRpcFailRatioTotal += routeRpcFailRatio;
      lookupCount++;

      if (network != null) {
        final int sourceIndex = network.getEnv().indexOf(source);
        final int targetIndex = network.getEnv().indexOf(target);
        lookupFailureCounters.at(sourceIndex).post(targetIndex, failureWeight);
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

    public void registerRoutingStats(
        final int routeCount,
        final float routeRedundancy,
        final float redundancyChangeRate) {
      routingServiceSnapshotCount += 1;
      routingServiceRouteCountTotal += routeCount;
      routingServiceRedundancyTotal += routeRedundancy;
      routingServiceRedundancyChangeTotal += redundancyChangeRate;
    }

    public double getRoutingServiceRouteCountAvg() {
      return routingServiceRouteCountTotal / routingServiceSnapshotCount;
    }

    public double getRoutingServiceRedundancyAvg() {
      return routingServiceRedundancyTotal / routingServiceSnapshotCount;
    }

    public double getRoutingServiceRedundancyChangeAvg() {
      return routingServiceRedundancyChangeTotal / routingServiceSnapshotCount;
    }

    public double getRouteExhaustionAvg() {
      return routeExhaustionTotal / lookupCount;
    }

    public double getRouteRedundancyAvg() {
      return routeRedundancyTotal / lookupCount;
    }

    public double getRouteRetractionAvg() {
      return routeRetractionTotal / lookupCount;
    }

    public double getRouteRpcFailRatioAvg() {
      return routeRpcFailRatioTotal / lookupCount;
    }
  }

  NetworkInterceptor NOOP = new NetworkInterceptor() {
    private final LookupMetrics mockLookups = new LookupMetrics(null);

    public void reportInconsistentLookup(LookupService.Mode get) {
      //  nothing to do
    }

    public void registerNodeArrivals(int nodeCount, boolean successful) {
      //  nothing to do
    }

    public void registerNodeFailure(Address address, double rangeWidth) {
      //  nothing to do
    }

    public void registerNodeDeparture(Address address, double rangeWidth) {
      //  nothing to do
    }

    public void registerRpcCallResult(Address source, Address target, boolean successful) {
      //  nothing to do
    }

    @Override
    public LookupMetrics modeToLookups(LookupService.Mode mode) {
      return mockLookups;
    }
  };

}
