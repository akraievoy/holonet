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
   * @param latency       time elapsed on this lookup.
   * @param hopCount      network nodes transferred before lookup completed.
   * @param directLatency between source and nextHandler nodes.
   */
  void registerLookup(double latency, long hopCount, double directLatency);

  //	----------------------------
  //	lookup failure/success ratio
  //	----------------------------

  void registerLookupSuccess(boolean successfull);

  void reportInconsistentLookup();

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
    public void registerLookup(double latency, long hopCount, double directLatency) {
    }

    public void reportInconsistentLookup() {
    }

    public void registerLookupSuccess(boolean successfull) {
    }

    public void registerNodeArrivals(int nodeCount, boolean successful) {
    }

    public void registerNodeFailures(int nodeCount) {
    }

    public void registerNodeDepartures(int nodeCount) {
    }

    public void registerRpcCallResult(boolean successful) {
    }
  };
}
