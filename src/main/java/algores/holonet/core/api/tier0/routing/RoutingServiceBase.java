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

/**
 * Implementation of basic methods.
 */
public abstract class RoutingServiceBase extends LocalServiceBase implements RoutingService {
  /**
   * golden ratio, we should trigger maintenance after some portion of redundant routes is gathered
   */
  public static final double MAINTENANCE_THRESHOLD = (1 + Math.sqrt(5)) / 2;

  protected List<RoutingEntry> routes = new ArrayList<RoutingEntry>();
  /**
   * essentially the same structure used to describe the owner node
   */
  protected RoutingEntry ownRoute = null;
  /**
   * collects all known entry flavors
   */
  protected final SortedMap<String, Integer> flavorToCount =
      new TreeMap<String, Integer>();
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

  public List<RoutingEntry> localLookup(Key key, int num, boolean safe) {
    Die.ifNull("ownRoute", ownRoute);
    final List<RoutingEntry> result = new ArrayList<RoutingEntry>();

    if (safe) {
      filterSafeRoutes(result);
    } else {
      result.addAll(routes);
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
        result.add(new RoutingEntry(seedAddress.getKey(), seedAddress));
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
    final byte livenessAvg = computeAverageLiveness();

    for (RoutingEntry re : routes) {
      if (re.getLiveness() >= livenessAvg) {
        result.add(re);
      }
    }
  }

  protected byte computeAverageLiveness() {
    int livenessTotal = 0;
    for (RoutingEntry re : routes) {
      livenessTotal += re.getLiveness();
    }

    final double avg = Math.floor((double) livenessTotal / routes.size());
    byte livenessAvg = (byte) Math.min(RoutingEntry.LIVENESS_DEFAULT, avg);

    return livenessAvg;
  }

  public List<RoutingEntry> neighborSet(int num) {
    Die.ifNull("ownRoute", ownRoute);

    final List<RoutingEntry> result = new ArrayList<RoutingEntry>();

    for (RoutingEntry re : routes) {
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
          new RoutingEntry(seedAddress.getKey(), seedAddress);
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
    Die.ifNull("ownRoute", ownRoute);

    final List<RoutingEntry> result = new ArrayList<RoutingEntry>();

    if (ownRoute.isReplicaFor(key, maxRank)) {
      result.add(ownRoute);
    }

    for (RoutingEntry re : routes) {
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
          new RoutingEntry(seedAddress.getKey(), seedAddress);
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
    update(handle, joined ? Event.JOINED : Event.LEFT);
  }

  @Deprecated
  public boolean range(
      RoutingEntry handle,
      byte rank,
      AtomicReference<Key> lKey,
      AtomicReference<Key> rKey
  ) {
    Die.ifNull("ownRoute", ownRoute);

    final RoutingEntry re = getEntry(handle);
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
    Die.ifNull("ownRoute", ownRoute);

    final RoutingEntry re = getEntry(handle);
    if (re == null) {
      return null;
    }

    final Range range = re.getRangeFor(lKey, rank);

    return range;
  }

  protected RoutingEntry getEntry(final NodeHandle handle) {
    return getEntry(handle.getAddress());
  }

  protected RoutingEntry getEntry(final Address address) {
    Die.ifNull("ownRoute", ownRoute);

    for (RoutingEntry re : routes) {
      if (re.getAddress().equals(address)) {
        return re;
      }
    }

    return null;
  }

  public void update(RoutingEntry[] entries, Event event) {
    Die.ifNull("ownRoute", ownRoute);

    FlavorTuple tuple = null;

    for (RoutingEntry upEntry : entries) {
      if (upEntry.getAddress().equals(ownRoute.getAddress())) {
        continue;
      }

      boolean found = false;
      for (RoutingEntry myRe : routes) {
        if (!myRe.getAddress().equals(upEntry.getAddress())) {
          continue;
        }

        if (myRe.getStamp() < upEntry.getStamp()) {
          myRe.update(upEntry.getStamp(), upEntry.getEntryCount(), upEntry.getRanges());
        }
        myRe.updateLiveness(event);

        found = true;
        break;
      }

      if (found) {
        continue;
      }

      final RoutingEntry newRe = upEntry.copy();
      newRe.updateLiveness(event);

      final FlavorTuple currentFlavor = flavorize(getOwnRoute(), newRe);
      final Integer oldCount = flavorToCount.get(currentFlavor.flavor);
      flavorToCount.put(currentFlavor.flavor, oldCount == null ? 1 : oldCount + 1);

      if (tuple == null || !tuple.requireFullReflavor) {
        tuple = currentFlavor;
      }
    }

    if (requiresCleanup() || tuple != null && tuple.requireFullReflavor) {
      fullReflavor();
    }
  }

  public void update(RoutingEntry upEntry, Event event) {
    Die.ifNull("ownRoute", ownRoute);

    if (upEntry.getAddress().equals(ownRoute.getAddress())) {
      return;
    }

    final float newLiveness = upEntry.computeNewLiveness(event);

    float minLiveness =
        RoutingEntry.LIVENESS_MIN * RoutingEntry.LIVENESS_COMM_FAIL_PENALTY;
    for (int i = 0, size = routes.size(); i < size; i++) {
      RoutingEntry myRe = routes.get(i);
      minLiveness = Math.min(minLiveness, myRe.getLiveness());
      if (!myRe.getAddress().equals(upEntry.getAddress())) {
        continue;
      }

      if (myRe.getStamp() < upEntry.getStamp()) {
        myRe.update(upEntry.getStamp(), upEntry.getEntryCount(), upEntry.getRanges());
      }
      if (newLiveness >= RoutingEntry.LIVENESS_MIN) {
        myRe.updateLiveness(event);
      } else {
        routes.remove(i);
        final FlavorTuple fTuple = flavorize(getOwnRoute(), upEntry);
        final Integer oldCount = flavorToCount.get(fTuple.flavor);
        if (oldCount != null) {
          if (oldCount > 1) {
            flavorToCount.put(fTuple.flavor, oldCount - 1);
          } else {
            flavorToCount.remove(fTuple.flavor);
          }
        }
      }

      return;
    }

    final FlavorTuple fTuple = flavorize(getOwnRoute(), upEntry);
    final Integer count = flavorToCount.get(fTuple.flavor);
    if (newLiveness < minLiveness || count != null && count > Math.floor(redundancy)) {
      return;
    }
    final RoutingEntry newRe = upEntry.copy();
    newRe.updateLiveness(event);
    routes.add(newRe);
    final Integer oldCount = flavorToCount.get(fTuple.flavor);
    flavorToCount.put(fTuple.flavor, oldCount == null ? 1 : oldCount + 1);

    if (fTuple.requireFullReflavor || requiresCleanup()) {
      fullReflavor();
    }
  }

  private Map<RoutingEntry, String> entryToFlavorCache;

  protected void fullReflavor() {
    //  allocate the map lazily, basing on actual load on the data structure
    if (entryToFlavorCache == null) {
      entryToFlavorCache =
          new HashMap<RoutingEntry, String>(routes.size() * 4, 0.25f);
    }

    flavorToCount.clear();
    for (RoutingEntry re : routes) {
      final FlavorTuple tuple = flavorize(ownRoute, re);
      entryToFlavorCache.put(re, tuple.flavor);
      final Integer countPrev = flavorToCount.get(tuple.flavor);
      flavorToCount.put(
          tuple.flavor,
          countPrev == null ? 1 : countPrev + 1
      );
    }

    final List<Address> seedAddresses =
        owner.getNetwork().getEnv().seedLinks(ownRoute.getAddress());
    for (Address seedAddress : seedAddresses) {
      final RoutingEntry seedEntry =
          new RoutingEntry(seedAddress.getKey(), seedAddress);
      final FlavorTuple tuple = flavorize(ownRoute, seedEntry);
      if (tuple.requireFullReflavor) {
        continue;
      }
      final Integer countPrev = flavorToCount.get(tuple.flavor);
      flavorToCount.put(
          tuple.flavor,
          countPrev == null ? 1 : countPrev + 1
      );
    }

    if (requiresCleanup()) {
      Collections.sort(routes, livenessOrder);

      final int totalMax = (int) Math.ceil(flavorToCount.size() * redundancy);
      final int redundancyMin = (int) Math.floor(redundancy);
      final int redundancyMax = (int) Math.ceil(redundancy);

      int keptTotal = routes.size();
      for (int curIndex = routes.size() - 1; curIndex >= 0; curIndex--) {
        final RoutingEntry re = routes.get(curIndex);
        final String flavor = entryToFlavorCache.get(re);
        final int count = flavorToCount.get(flavor);
        if (
            count > redundancyMin &&
            (count >= redundancyMax || keptTotal > totalMax)
        ) {
          routes.remove(curIndex);
          keptTotal--;
          flavorToCount.put(flavor, count - 1);
        }
      }
    }

    entryToFlavorCache.clear();
  }

  protected boolean requiresCleanup() {
    final double trigger = flavorToCount.size() * redundancy * MAINTENANCE_THRESHOLD;
    return routes.size() > trigger;
  }

  protected static class FlavorTuple {
    public final String flavor;
    public final boolean requireFullReflavor;

    public FlavorTuple(final String flavor) {
      this(flavor, false);
    }

    public FlavorTuple(final String flavor, final boolean fullReflavor1) {
      this.flavor = flavor;
      this.requireFullReflavor = fullReflavor1;
    }
  }

  protected abstract FlavorTuple flavorize(RoutingEntry owner, RoutingEntry created);

  public RoutingEntry getOwnRoute() {
    return ownRoute;
  }

  public void updateOwnRoute(RoutingEntry owner) {
    Die.ifNull("owner", owner);
    Die.ifNotEqual("owner.nodeId", this.ownRoute.getNodeId(), owner.getNodeId());

    this.ownRoute = owner;

    fullReflavor();
  }

  public abstract RoutingDistance getRoutingDistance();

  public List<RoutingEntry> getRoutes() {
    final ArrayList<RoutingEntry> routesRes = new ArrayList<RoutingEntry>(routes);

    if (!routes.contains(getOwnRoute())) {
      routesRes.add(getOwnRoute());
    }

    return routesRes;
  }

  @Override
  public RoutingStatsTuple getStats() {
    final int routeCount =
        routes.size() + (routes.contains(getOwnRoute()) ? 0 : 1);
    final float routeRedundancy =
        (float) routeCount / flavorToCount.size();
    return new RoutingStatsTuple(routeCount, routeRedundancy);
  }

  public void registerCommunicationFailure(Address calleeAddress) {
    //	TODO we don't really NEED to create an object just to do a query
    final NodeHandle dummyHandle = new NodeHandleBase(calleeAddress.getKey(), calleeAddress);
    final RoutingEntry re = getEntry(dummyHandle);

    //  this should also provoke eviction / reflavoring
    if (re != null) {
      re.updateLiveness(Event.CONNECTION_FAILED);
    } else {
//			System.out.println("stamped a foreign route entry");
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

  protected static class LivenessComparator implements Comparator<RoutingEntry> {
    public int compare(RoutingEntry o1, RoutingEntry o2) {
      final float l1 = o1.getLiveness();
      final float l2 = o2.getLiveness();
      return -Float.compare(l1, l2);
    }
  }
}
