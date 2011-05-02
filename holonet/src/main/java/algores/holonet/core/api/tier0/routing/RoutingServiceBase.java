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
  protected List<String> flavors = new ArrayList<String>();
  /**
   * should we store duplicates of enries of the same flavor, this value might be fractional, like 1.75
   */
  protected double redundancy = 1.75;
  /**
   * Comparator defining which redundant entries would be be dropped: less means drop, greater means keep.
   * Default strategy is based on sole liveness value, which is itself adjusted by failures and usage frequence.
   */
  protected Comparator<RoutingEntry> redundancyComparator = new LivenessComparator();

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

    localLookupInternal(key, num, result);

    if (result.size() > num) {
      result.subList(num, result.size()).clear();
    }

    return result;
  }

  protected void localLookupInternal(Key key, int num, List<RoutingEntry> result) {
    final RoutingPreferenceBase pref = getRoutingPreference();
    final Comparator<Range> comparator = pref.createRangeComparator(key);
    final TreeMap<Range, RoutingEntry> rangeToEntry = new TreeMap<Range, RoutingEntry>(comparator);

    for (RoutingEntry re : result) {
      rangeToEntry.put(re.selectRange(key, pref), re);
    }

    result.clear();
    result.addAll(rangeToEntry.values());
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
        if (result.size() >= num) {
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

    Collections.sort(result, getRoutingPreference().createReComparator(key));

    return result;
  }

  @Deprecated
  public void update(RoutingEntry handle, boolean joined) {
    update(handle, joined ? Event.JOINED : Event.LEFT);
  }

  @Deprecated
  public boolean range(RoutingEntry handle, byte rank, AtomicReference<Key> lKey, AtomicReference<Key> rKey) {
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

    boolean added = false;

    for (NodeHandle upNh : entries) {
      if (upNh.getAddress().equals(ownRoute.getAddress())) {
        continue;
      }

      boolean found = false;
      for (RoutingEntry myRe : routes) {
        if (!myRe.getAddress().equals(upNh.getAddress())) {
          continue;
        }

        if (upNh instanceof RoutingEntry) {
          final RoutingEntry upRe = (RoutingEntry) upNh;
          if (myRe.getStamp() < upRe.getStamp()) {
            myRe.update(upRe.getStamp(), upRe.getEntryCount(), upRe.getRanges());
          }
        }
        myRe.updateLiveness(event);

        found = true;
        break;
      }

      if (found) {
        continue;
      }

      if (upNh instanceof RoutingEntry) {
        added = true;

        final RoutingEntry newRe = ((RoutingEntry) upNh).copy();
        newRe.updateLiveness(event);
      }
    }

    if (added) {
      maintainRedundancyRatio();
    }
  }

  public void update(RoutingEntry upNh, Event event) {
    Die.ifNull("ownRoute", ownRoute);

    if (upNh.getAddress().equals(ownRoute.getAddress())) {
      return;
    }

    for (RoutingEntry myRe : routes) {
      if (!myRe.getAddress().equals(upNh.getAddress())) {
        continue;
      }

      final RoutingEntry upRe = (RoutingEntry) upNh;
      if (myRe.getStamp() < upRe.getStamp()) {
        myRe.update(upRe.getStamp(), upRe.getEntryCount(), upRe.getRanges());
      }
      myRe.updateLiveness(event);

      return;
    }

    final RoutingEntry newRe = ((RoutingEntry) upNh).copy();
    storeFlavor(flavorize(ownRoute, newRe));
    newRe.updateLiveness(event);

    maintainRedundancyRatio();
  }

  protected void reflavor() {
    flavors.clear();

    for (RoutingEntry re : routes) {
      storeFlavor(flavorize(ownRoute, re));
    }
  }

  protected void maintainRedundancyRatio() {
    final double trigger = flavors.size() * redundancy * MAINTENANCE_THRESHOLD;
    if (routes.size() > trigger) {
      final Map<RoutingEntry, String> entryToFlavor = new HashMap<RoutingEntry, String>();
      for (RoutingEntry re : routes) {
        entryToFlavor.put(re, flavorize(ownRoute, re));
      }

      Collections.sort(routes, redundancyComparator);

      final int totalMax = (int) Math.ceil(flavors.size() * redundancy);
      final int redundancyMin = (int) Math.floor(redundancy);
      final int redundancyMax = (int) Math.ceil(redundancy);

      final TreeSet<String> flavorSet = new TreeSet<String>(entryToFlavor.values());
      final String[] flavorIndex = flavorSet.toArray(new String[flavorSet.size()]);

      int keptTotal = 0;
      final int[] keptCount = new int[flavorIndex.length];

      for (int curIndex = routes.size() - 1; curIndex >= 0; curIndex--) {
        final RoutingEntry re = routes.get(curIndex);
        final String flavor = entryToFlavor.get(re);
        final int flavorIdx = Arrays.binarySearch(flavorIndex, flavor);

        if (keptCount[flavorIdx] >= redundancyMax) {
          routes.remove(curIndex);
        } else if (keptCount[flavorIdx] >= redundancyMin && keptTotal > totalMax) {
          routes.remove(curIndex);
        } else {
          keptTotal++;
          keptCount[flavorIdx]++;
        }
      }
    }
  }

  protected abstract String flavorize(RoutingEntry owner, RoutingEntry created);

  protected void storeFlavor(String flavor) {
    final int insertionIndex = Collections.binarySearch(flavors, flavor);
    if (insertionIndex < 0) {
      flavors.add(-(insertionIndex + 1), flavor);
    }
  }

  public RoutingEntry getOwnRoute() {
    return ownRoute;
  }

  public void updateOwnRoute(RoutingEntry owner) {
    Die.ifNull("owner", owner);
    Die.ifNotEqual("owner.nodeId", this.ownRoute.getNodeId(), owner.getNodeId());

    this.ownRoute = owner;

    reflavor();
    maintainRedundancyRatio();
  }

  public abstract RoutingPreferenceBase getRoutingPreference();

  public List<RoutingEntry> getRoutes() {
    final ArrayList<RoutingEntry> routesRes = new ArrayList<RoutingEntry>(routes);

    if (!routes.contains(getOwnRoute())) {
      routesRes.add(getOwnRoute());
    }

    return routesRes;
  }

  public void registerCommunicationFailure(Address calleeAddress) {
    //	TODO fix this
    final NodeHandle dummyHandle = new NodeHandleBase(calleeAddress.getKey(), calleeAddress);
    final RoutingEntry re = getEntry(dummyHandle);

    if (re != null) {
      re.updateLiveness(Event.CONNECTION_FAILED);
    } else {
//			System.out.println("stamped a foreign route entry");
    }
  }

  protected static class LivenessComparator implements Comparator<RoutingEntry> {
    public int compare(RoutingEntry o1, RoutingEntry o2) {
      return o1.getLiveness() - o2.getLiveness();
    }
  }
}
