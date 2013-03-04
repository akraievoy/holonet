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

package algores.holonet.core.api.tier0.routing;

import algores.holonet.capi.Event;
import algores.holonet.core.Env;
import algores.holonet.core.api.*;
import org.akraievoy.base.Die;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static algores.holonet.core.api.tier0.routing.RoutingPackage.*;

/**
 * Implementation of basic methods.
 */
public abstract class RoutingServiceBase extends LocalServiceBase implements RoutingService {
  /**
   * golden ratio, we should trigger maintenance after some portion of redundant routes is gathered
   */
  public static final double MAINTENANCE_THRESHOLD = (1 + Math.sqrt(5)) / 2;

  protected final RouteTable routes = new RouteTable();

  /**
   * should we store duplicates of enries of the same flavor, this value might be fractional, like 1.75
   */
  protected double redundancy = 3.25;
  /**
   * Comparator defining which redundant entries would be dropped: less means drop, greater means keep.
   * Default strategy is based on sole liveness value, which is itself adjusted by failures and usage frequency.
   */
  protected Comparator<RoutingEntry> livenessOrder = new LivenessComparator();

  protected RoutingServiceBase() {
  }

  public double getRedundancy() {
    return redundancy;
  }

  public void setRedundancy(double redundancy) {
    this.redundancy = redundancy;
  }

  @Override
  public RoutingEntry ownRoute() {
    return ownRoute(true);
  }

  @Override
  public RouteTable routes() {
    return routes;
  }

  @Override
  public RoutingEntry ownRoute(boolean safe) {
    if (owner == null) {
      if (safe) {
        throw new IllegalStateException("owner not set");
      }
      return null;
    }

    final RoutingEntry ownRoute = routes.route(owner.getAddress());
    if (ownRoute != null) {
      return ownRoute;
    }
    if (safe) {
      throw new IllegalStateException("routez do not contain entry for ownRoute");
    }
    return null;
  }

  public List<RoutingEntry> localLookup(Key key, int num, boolean safe) {
    final RoutingEntry ownRoute = ownRoute();
    final List<RoutingEntry> result = new ArrayList<RoutingEntry>();

    if (safe) {
      filterSafeRoutes(result);
    } else {
      result.addAll(routes.routes());
    }

    localLookupInternal(key, result);

    if (num > 0 && result.size() > num) {
      result.subList(num, result.size()).clear();
    }

    //  seed addresses are not masked by num limit
    final List<Address> seedAddresses =
        owner.getNetwork().getEnv().seedLinks(ownRoute.getAddress());
    for (Address seedAddress : seedAddresses) {
      boolean found = false;
      for (RoutingEntry routingEntry : result) {
        if (routingEntry.getAddress().equals(seedAddress)) {
          found = true;
          break;
        }
      }
      if (!found) {
        result.add(RoutingEntry.stub(seedAddress.getKey(), seedAddress));
      }
    }

    return result;
  }

  protected void localLookupInternal(Key key, List<RoutingEntry> result) {
    final Comparator<RoutingEntry> distanceOrder = distanceOrder(key);

    Collections.sort(
        result,
        distanceOrder
    );
  }

  @Override
  public Comparator<RoutingEntry> distanceOrder(Key key) {
    return preferenceComparator(key);
  }

  @Override
  public Comparator<RoutingEntry> getLivenessOrder() {
    return livenessOrder;
  }

  protected void filterSafeRoutes(List<RoutingEntry> result) {
    final float livenessAvg = computeAverageLiveness();

    for (RoutingEntry re : routes.routes()) {
      if (re.liveness() >= livenessAvg) {
        result.add(re);
      }
    }
  }

  protected float computeAverageLiveness() {
    float livenessTotal = 0;
    for (RoutingEntry re : routes.routes()) {
      livenessTotal += re.liveness();
    }

    final double avg = Math.floor((double) livenessTotal / routes.size());
    byte livenessAvg = (byte) Math.min(RoutingEntry.LIVENESS_DEFAULT, avg);

    return livenessAvg;
  }

  public List<RoutingEntry> neighborSet(int num) {
    final RoutingEntry ownRoute = ownRoute();

    final List<RoutingEntry> result = new ArrayList<RoutingEntry>();

    for (RoutingEntry re : routes.routes()) {
      if (isNeighbor(ownRoute, re)) {
        result.add(re);
        if (num > 0 && result.size() >= num) {
          break;
        }
      }
    }

    //  seed addresses are not masked by num limit
    final List<Address> seedAddresses =
        owner.getNetwork().getEnv().seedLinks(ownRoute.getAddress());
    for (Address seedAddress : seedAddresses) {
      final RoutingEntry seedEntry =
          RoutingEntry.stub(seedAddress.getKey(), seedAddress);
      if (!isNeighbor(ownRoute, seedEntry)) {
        continue;
      }
      boolean found = false;
      for (RoutingEntry routingEntry : result) {
        if (routingEntry.getAddress().equals(seedAddress)) {
          found = true;
          break;
        }
      }
      if (!found) {
        result.add(seedEntry);
      }
    }

    return result;
  }

  protected boolean isNeighbor(RoutingEntry ownerEntry, RoutingEntry routingEntry) {
    final Range range = ownerEntry.getRangeFor(ownerEntry.getKey(), Byte.MAX_VALUE);

    if (range == null) {
      return false;
    }

    final Key nextLKey = range.getRKey().next();
    final Key prevRKey = range.getLKey().prev();

    return routingEntry.isReplicaFor(nextLKey, Byte.MAX_VALUE) || routingEntry.isReplicaFor(prevRKey, Byte.MAX_VALUE);
  }

  public List<RoutingEntry> replicaSet(Key key, byte maxRank) {
    final RoutingEntry ownRoute = ownRoute();

    final List<RoutingEntry> result = new ArrayList<RoutingEntry>();

    if (ownRoute.isReplicaFor(key, maxRank)) {
      result.add(ownRoute);
    }

    for (RoutingEntry re : routes.routes()) {
      //	in some rare cases node would add itself to its own routing table
      //	for example - ring or chord node when it is the only one in the net
      if (!ownRoute.equals(re) && re.isReplicaFor(key, maxRank)) {
        result.add(re);
      }
    }

    //  seed addresses are not masked by num limit
    final List<Address> seedAddresses =
        owner.getNetwork().getEnv().seedLinks(ownRoute.getAddress());
    for (Address seedAddress : seedAddresses) {
      final RoutingEntry seedEntry =
          RoutingEntry.stub(seedAddress.getKey(), seedAddress);
      if (!seedEntry.isReplicaFor(key, maxRank)) {
        continue;
      }
      boolean found = false;
      for (RoutingEntry routingEntry : result) {
        if (routingEntry.getAddress().equals(seedAddress)) {
          found = true;
          break;
        }
      }
      if (!found) {
        result.add(seedEntry);
      }
    }

    Collections.sort(result, preferenceComparator(key));

    return result;
  }

  @Deprecated
  public void update(RoutingEntry handle, boolean joined) {
    update(joined ? Event.JOINED : Event.LEFT, handle);
  }

  @Deprecated
  public boolean range(
      RoutingEntry handle,
      byte rank,
      AtomicReference<Key> lKey,
      AtomicReference<Key> rKey
  ) {
    final RoutingEntry re = routes.route(handle.getAddress());
    if (re == null) {
      return false;
    }

    final Range range = re.getRangeFor(lKey.get(), rank);
    if (range != null) {
      lKey.set(range.getLKey());
      rKey.set(range.getRKey().prev());
    }

    return range != null;
  }

  public Range getRange(NodeHandle handle, byte rank, Key lKey) {
    final RoutingEntry re = routes.route(handle.getAddress());
    if (re == null) {
      return null;
    }

    final Range range = re.getRangeFor(lKey, rank);

    return range;
  }

  public void update(Event event, RoutingEntry... entries) {
    byte updateStatus = UPDATE_NOOP;
    for (RoutingEntry entry : entries) {
      updateStatus |= update0(entry, event);
    }

    if ((updateStatus & UPDATE_REFLAVOR) > 0 || requiresCleanup()) {
      fullReflavor();
    }
  }

  protected final static byte UPDATE_NOOP = 0;
  protected final static byte UPDATE_CHANGED = 1;
  protected final static byte UPDATE_REFLAVOR = 2;

  protected byte update0(final RoutingEntry foreign, final Event event) {
    final RoutingEntry ownRoute = ownRoute();

    if (foreign.getAddress().equals(ownRoute.getAddress())) {
      return UPDATE_NOOP;
    }

    final Flavor flavor = flavorize(foreign);
    final RoutingEntry prev = routes.route(foreign.getAddress());
    if (prev != null) {
      if (flavor.equals(routes.flavor(prev.getAddress()))) {
        final RoutingEntry next = prev.update(foreign).liveness(event);
        if (next.liveness >= RoutingEntry.LIVENESS_MIN) {
          routes.update(next);
        } else {
          routes.remove(foreign.getAddress());
        }
        return UPDATE_CHANGED;
      } else {
        routes.remove(foreign.getAddress());
      }
    }

    final boolean isSeed = owner.getNetwork().getEnv().seedLink(ownRoute.getAddress(), foreign.getAddress());
    final RoutingEntry next = foreign.liveness(event);
    if (routes.size(flavor) > Math.floor(redundancy) || next.liveness() < routes.minLiveness()) {
      return UPDATE_NOOP;
    }
    routes.add(flavor, next);

    return flavor.forceReflavor() ? UPDATE_REFLAVOR | UPDATE_CHANGED : UPDATE_CHANGED;
  }

  protected void fullReflavor() {
    final RoutingEntry ownRoute = ownRoute();

/*    final List<Address> seedAddresses =
        owner.getNetwork().getEnv().seedLinks(ownRoute.getAddress());
    for (Address seedAddress : seedAddresses) {
      final boolean foundInRoutes = routes.has(seedAddress);
      if (foundInRoutes) {
        continue;
      }
      final RoutingEntry seedEntry =
          new RoutingEntry(seedAddress.getKey(), seedAddress);

      final Flavor tuple = flavorize(seedEntry);
      if (tuple.forceReflavor()) {
        continue;
      }
    }*/

    final List<RoutingEntry> prevRoutes = new ArrayList<RoutingEntry>(routes.routes());
    if (requiresCleanup()) {
      Collections.sort(prevRoutes, livenessOrder);

      final int totalMax = (int) Math.ceil(routes.flavorCount(false) * redundancy);
      final int redundancyMin = (int) Math.floor(redundancy);
      final int redundancyMax = (int) Math.ceil(redundancy);

      int keptTotal = prevRoutes.size();
      for (RoutingEntry re : prevRoutes) {
        final int count = routes.size(routes.flavor(re.getAddress()));
        if (
            count > redundancyMin &&
            (count >= redundancyMax || keptTotal > totalMax)
        ) {
          routes.remove(re.getAddress());
          keptTotal--;
        }
      }
    }
  }

  protected boolean requiresCleanup() {
    final double trigger = routes.flavorCount(false) * redundancy * MAINTENANCE_THRESHOLD;
    return routes.size() > trigger;
  }

  protected abstract Flavor flavorize(RoutingEntry entry);

  public void updateOwnRoute(RoutingEntry owner) {
    Die.ifNull("owner", owner);
    if (!getOwner().getAddress().equals(owner.getAddress())) {
      throw new IllegalArgumentException("setting owner route with invalid address");
    }

    routes.update(owner);

    fullReflavor();
  }

  public abstract RoutingDistance getRoutingDistance();

  @Override
  public RoutingStatsTuple getStats() {
    final int routeCount =
        routes().size();
    final float routeRedundancy =
        (float) routeCount / routes.flavorCount(false);
    return new RoutingStatsTuple(routeCount, routeRedundancy);
  }

  public void registerCommunicationFailure(Address calleeAddress) {
    //  FIXME successor/predecessor may fail and should be failed-over accordingly
    final RoutingEntry route = routes.route(calleeAddress);
    if (route == null) {
      return;
    }

    final RoutingEntry next = route.liveness(Event.CONNECTION_FAILED);
    if (next.liveness() < RoutingEntry.LIVENESS_MIN) {
      routes.remove(next.getAddress());
    } else {
      routes.update(next);
    }
  }

  protected Comparator<RoutingEntry> preferenceComparator(final Key key) {
    return new Comparator<RoutingEntry>() {
      public int compare(RoutingEntry r1, RoutingEntry r2) {
        final double r1dist = routingDistance(r1, key);
        final double r2dist = routingDistance(r2, key);
        return Double.compare(r1dist, r2dist);
      }
    };
  }

  @Override
  public double routingDistance(RoutingEntry r, Key key) {
    final Address localAddress = owner.getAddress();
    final Env env = owner.getNetwork().getEnv();
    final RoutingDistance dist = getRoutingDistance();
    //  LATER why we select range by routing metric only?
    final Range bestRange = r.selectRange(localAddress, key, dist);
    final double routingDist =
        dist.apply(localAddress, key, r.getAddress(), bestRange);
    final double envDist =
        env.apply(localAddress, key, r.getAddress(), bestRange);
    return routingDist * Math.pow(2, envDist);
  }

  @Override
  public boolean hasRouteFor(
      Address address,
      boolean includeStoredRoutes,
      boolean includeSeedRoutes
  ) {
    final RoutingEntry ownRoute = ownRoute();
    if (ownRoute.getAddress().equals(address)) {
      return false;
    }

    if (includeStoredRoutes) {
      if (routes.has(address)) {
        return true;
      }
    }

    if (includeSeedRoutes) {
      final List<Address> seedAddresses =
        owner.getNetwork().getEnv().seedLinks(ownRoute.getAddress());
      for (Address seedAddress : seedAddresses) {
        if (address.equals(seedAddress)) {
          return true;
        }
      }
    }

    return false;
  }

  protected static class LivenessComparator implements Comparator<RoutingEntry> {
    public int compare(RoutingEntry o1, RoutingEntry o2) {
      final float l1 = o1.liveness();
      final float l2 = o2.liveness();
      return -Float.compare(l1, l2);
    }
  }
}

