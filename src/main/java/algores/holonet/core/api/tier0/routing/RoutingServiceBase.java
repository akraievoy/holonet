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
import algores.holonet.core.api.tier0.rpc.RpcService;
import algores.holonet.protocols.ring.RingRoutingService;
import algores.holonet.protocols.ring.RingRoutingServiceImpl;
import com.google.common.base.Optional;
import org.akraievoy.base.Die;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static algores.holonet.core.api.tier0.routing.Routing.*;

/**
 * Implementation of basic methods.
 */
public abstract class RoutingServiceBase extends LocalServiceBase implements RoutingService {
  /**
   * trigger maintenance after some portion of redundant routes is gathered
   */
  public static final double MAINTENANCE_THRESHOLD = 1.2;

  public static final Event[] EVENT_PROCESS_PRIRORITY =
      new Event[]{
          Event.CONNECTION_FAILED,
          Event.LEFT,
          Event.HEART_BEAT,
          Event.JOINED,
          Event.DISCOVERED
      };

  protected static final Logger log = LoggerFactory.getLogger(RingRoutingServiceImpl.class);

  protected final RouteTable routes = new RouteTable();

  /**
   * should we store duplicates of enries of the same flavor, this value might be fractional, like 1.75
   */
  protected double redundancy = 3.25;
  protected int maxFingerFlavorNum = Key.BITNESS;

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

  public int getMaxFingerFlavorNum() {
    return maxFingerFlavorNum;
  }

  @Override
  public void setMaxFingerFlavorNum(int maxFingerFlavorNum) {
    this.maxFingerFlavorNum = maxFingerFlavorNum;
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
      throw new IllegalStateException("routes do not contain entry for ownRoute");
    }
    return null;
  }

  public List<RoutingEntry> localLookup(Key key, int num, boolean safe) {
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

    Collections.sort(result, preferenceComparator(key));

    return result;
  }

  @Deprecated
  public void update(RoutingEntry handle, boolean joined) {
    update(eventToRoute(joined ? Event.JOINED : Event.LEFT, handle));
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

  public void update(Map<Event, Iterable<RoutingEntry>> eventToRoutes) {
    byte updateStatus = UPDATE_NOOP;
    for (Event e : EVENT_PROCESS_PRIRORITY) {
      final Iterable<RoutingEntry> routes = eventToRoutes.get(e);
      if (routes == null) {
        continue;
      }
      for (RoutingEntry entry : routes) {
        updateStatus |= update0(entry, e);
      }
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

    byte result = UPDATE_NOOP;
    final Flavor flavor = flavorize(foreign);
    final RoutingEntry prev = routes.route(foreign.getAddress());
    final Flavor oldFlavor;
    if (prev != null) {
      oldFlavor = routes.flavor(prev.getAddress());
      if (flavor.equals(oldFlavor)) {
        final RoutingEntry next = prev.update(foreign).liveness(event);
        if (next.liveness >= RoutingEntry.LIVENESS_MIN) {
          routes.update(next);
          return UPDATE_CHANGED;
        } else {
          if (oldFlavor.structural || flavor.structural) {
/*
            System.out.printf(
                "HOUSTON: DROP DEAD ROUTE: %s -> %s (%s) %n",
                ownRoute.getAddress().getKey(),
                foreign.getAddress().getKey(),
                flavor
            );
*/
          }
          routes.remove(foreign.getAddress());
          return updateOf(flavor);
        }
      } else {
        routes.remove(foreign.getAddress());
      }
    } else {
      oldFlavor = null;
    }

    final boolean isSeed = owner.getNetwork().getEnv().seedLink(ownRoute.getAddress(), foreign.getAddress());
    final RoutingEntry next = foreign.liveness(event);
    if (
        //  filtering out redundant non-seed fingers
        !flavor.structural && !isSeed && requiresCleanup(flavor) ||
        //  or any stale
        next.liveness() < routes.minLiveness()
    ) {
      if (oldFlavor != null && oldFlavor.structural || flavor.structural) {
/*
        System.out.printf(
            "HOUSTON: DROP ROUTE ON FLAVOR CHANGE: %s -> %s (%s => %s) %n",
            ownRoute.getAddress().getKey(),
            foreign.getAddress().getKey(),
            oldFlavor,
            flavor
        );
*/
      }
      return result;
    }
    routes.add(flavor, next);

    return (byte) (result | updateOf(flavor));
  }

  private static byte updateOf(final Flavor ... flavors) {
    boolean structural = false;
    for (int i = 0, length = flavors.length; !structural && i < length; i++) {
      structural = flavors[i].structural;
    }
    return structural ? UPDATE_REFLAVOR | UPDATE_CHANGED : UPDATE_CHANGED;
  }

  @Override
  public void fullReflavor() {
    final RoutingEntry ownRoute = ownRoute();

    final List<RoutingEntry> prevRoutes = new ArrayList<RoutingEntry>(routes.routes());
    for (RoutingEntry re : prevRoutes) {
      routes.add(flavorize(re),re);
    }

    if (requiresCleanup()) {
      Collections.sort(prevRoutes, livenessOrder);

      final int redundancyMin = (int) Math.floor(redundancy);
      final int redundancyMax = (int) Math.ceil(redundancy);

      int keptTotal = prevRoutes.size();
      for (RoutingEntry re : prevRoutes) {
        final int totalMax = (int) Math.ceil(routes.flavorCount() * redundancy);
        final Flavor flavor = routes.flavor(re.getAddress());
        final int count = routes.size(flavor);
        final boolean isSeed = owner.getNetwork().getEnv().seedLink(ownRoute.getAddress(), re.getAddress());

        if (
            !isSeed && !flavor.structural && routes.flavorCount(true, true, false) > maxFingerFlavorNum ||
            count > redundancyMin && (count >= redundancyMax || keptTotal > totalMax)
        ) {
          routes.remove(re.getAddress());
          keptTotal--;
        }
      }
    }
  }

  protected boolean requiresCleanup(final Flavor addedFlavor) {
    final int sameFlavorRoutes = routes.size(addedFlavor);
    if (sameFlavorRoutes > Math.floor(redundancy)) {
      return true;
    }

    final boolean newFlavor = sameFlavorRoutes == 0;
    if (routes.flavorCount(true, true, false) + (newFlavor && !addedFlavor.structural ? 1 : 0) > maxFingerFlavorNum) {
      return true;
    }

    final double trigger = (routes.flavorCount() + (newFlavor ? 1 : 0)) * redundancy * MAINTENANCE_THRESHOLD;
    return (routes.size() + 1) > trigger;
  }

  protected boolean requiresCleanup() {
    if (routes.flavorCount(true, true, false) > maxFingerFlavorNum) {
      return true;
    }

    final double trigger = routes.flavorCount() * redundancy * MAINTENANCE_THRESHOLD;
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
        (float) routeCount / routes.flavorCount();
    return new RoutingStatsTuple(routeCount, routeRedundancy);
  }

  public Optional<RoutingEntry> registerCommunicationFailure(Address calleeAddress, final boolean recover) {
    final RoutingEntry route = routes.route(calleeAddress);
    if (route == null) {
      final Flavor flavorDropped = flavorize(RoutingEntry.stub(calleeAddress.getKey(), calleeAddress));
      if (flavorDropped.structural) {
        return recoverRef(flavorDropped, calleeAddress.getKey());
      }
      return Optional.absent();
    }

    final Flavor flavor = flavorize(route);
    if (!flavor.structural) {
      //  LATER decrease liveness before removal (tolerate transient link failures)
      routes.remove(route.getAddress());
    } else {
      routes.update(route.liveness(Event.CONNECTION_FAILED));
      if (recover) {
        return recoverRef(flavor, route.getAddress().getKey());
      } else {
/*
        System.out.printf(
            "HOUSTON: NOT RECOVERING ROUTE: %s -> %s (%s) %n",
            ownRoute().getAddress().getKey(),
            calleeAddress.getKey(),
            flavor
        );
*/
      }
    }
    return Optional.absent();
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

  public Optional<RoutingEntry> recoverRef(
      final Flavor refFlavor,
      final Key targetKey
  ) {
    final RpcService rpc = owner.getServices().getRpc();

    final List<RoutingEntry> routes = new ArrayList<RoutingEntry>(routes().routes());
    Collections.sort(routes, distanceOrder(targetKey));
    int triedRoutes = 0;
    for (RoutingEntry route : routes) {
      if (route.equals(ownRoute())) {
        continue;
      }

      triedRoutes++;
      final Optional<RingRoutingService> routingOpt =
          rpc.rpcTo(route.getAddress(), RingRoutingService.class);

      if (routingOpt.isPresent()) {
        final RoutingEntry recoveredRoute = routingOpt.get().ownRoute(true);
/*
        System.out.printf(
            "HOUSTON: RECOVER ROUTE ON COMMFAIL: %s -> (%s=>%s) (%s) SUCCESS %n",
            ownRoute().getAddress().getKey(),
            targetKey,
            recoveredRoute.getAddress().getKey(),
            refFlavor
        );
*/
        updateOnRecover(refFlavor, recoveredRoute);
        return Optional.of(recoveredRoute);
      }

      registerCommunicationFailure(route.getAddress(), false);
    }

    log.debug(
        "unable to recover any {} after peer failure, tried {} routes",
        refFlavor,
        triedRoutes
    );

/*
    System.out.printf(
        "HOUSTON: RECOVER ROUTE ON COMMFAIL: %s -> %s (%s) FAILED %n",
        ownRoute().getAddress().getKey(),
        targetKey,
        refFlavor
    );
*/

    return Optional.absent();
  }

  protected abstract void updateOnRecover(Flavor refFlavor, RoutingEntry recoveredRoute);

  protected static class LivenessComparator implements Comparator<RoutingEntry> {
    public int compare(RoutingEntry o1, RoutingEntry o2) {
      final float l1 = o1.liveness();
      final float l2 = o2.liveness();
      return -Float.compare(l1, l2);
    }
  }
}

