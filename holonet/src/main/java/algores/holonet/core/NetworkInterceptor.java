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

import algores.holonet.core.api.tier1.delivery.LookupService;

/**
 * Unifies hooks for intercepting various network events.
 */
public interface NetworkInterceptor {
  //	---------------------------
  //	hopcount/latency aggregates
  //	---------------------------

  /**
   * Registers a successful lookup
   *
   * @param mode          which operation current lookup supported
   * @param latency       time elapsed on this lookup.
   * @param hopCount      network nodes transferred before lookup completed.
   * @param directLatency between source and nextHandler nodes.
   * @param success       either true or false
   */
  void registerLookup(LookupService.Mode mode, double latency, long hopCount, double directLatency, boolean success);

  //	----------------------------
  //	lookup failure/success ratio
  //	----------------------------

  void reportInconsistentLookup(LookupService.Mode get);

  //	----------------------------
  //	RPC failure/success ratio
  //	----------------------------

  void registerRpcCallResult(boolean successful);

  //	-------------------------------------
  //	Network structure dynamics aggregates
  //	-------------------------------------

  void registerNodeArrivals(final int nodeCount, boolean successful);

  void registerNodeFailures(final int nodeCount);

  void registerNodeDepartures(final int nodeCount);

  NetworkInterceptor NOOP = new NetworkInterceptor() {
    private final LookupMetrics mockLookups = new LookupMetrics();

    public void registerLookup(LookupService.Mode mode, double latency, long hopCount, double directLatency, boolean success) {
    }

    public void reportInconsistentLookup(LookupService.Mode get) {
    }

    public void registerNodeArrivals(int nodeCount, boolean successful) {
    }

    public void registerNodeFailures(int nodeCount) {
    }

    public void registerNodeDepartures(int nodeCount) {
    }

    public void registerRpcCallResult(boolean successful) {
    }

    @Override
    public LookupMetrics modeToLookups(LookupService.Mode mode) {
      return mockLookups;
    }
  };

  LookupMetrics modeToLookups(LookupService.Mode mode);

  //	----------------------------------------------------------
  //	hopcount/latency aggregates + lookup failure/success ratio
  //	----------------------------------------------------------
  public static class LookupMetrics {
    private long totalHopCount;
    private long lookupCount;
    private double totalLatency;
    private double lookupVsDirectTotal;
    private long lookupVsDirectCount;
    private long lookupSuccesses;
    private long lookupFailures;
    private long inconsistentLookups;

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

    public long getLookupVsDirectCount() {
      return lookupVsDirectCount;
    }

    public void registerLookup(
        final double latency,
        final long hopCount,
        final double directLatency,
        boolean success
    ) {
      if (success) {
        totalLatency += latency;
        totalHopCount += hopCount;
        if (hopCount > 0 && directLatency > 1.0e-3) {
          lookupVsDirectTotal += latency / directLatency;
          lookupVsDirectCount++;
        }
        lookupSuccesses += 1.0;
      } else {
        lookupFailures += 1.0;
      }
      lookupCount++;
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

    public long getLookupSuccesses() {
      return lookupSuccesses;
    }

    public long getLookupFailures() {
      return lookupFailures;
    }
  }
}
