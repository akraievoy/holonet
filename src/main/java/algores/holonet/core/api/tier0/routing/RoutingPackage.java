/*
 Copyright 2013 Anton Kraievoy akraievoy@gmail.com
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
import java.util.concurrent.atomic.AtomicLong;

public class RoutingPackage {

  public static class Flavor implements Comparable<Flavor> {
    private final String name;
    private final boolean forceReflavor;

    public Flavor(String name) {
      this(name, false);
    }

    public Flavor(String name, final boolean forcesReflavor) {
      this.name = name;
      this.forceReflavor = forcesReflavor;
    }

    public boolean forceReflavor() {
      return forceReflavor;
    }

    public String name() {
      return name;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      final Flavor flavor = (Flavor) o;

      return name.equals(flavor.name);
    }

    @Override
    public int hashCode() {
      return name.hashCode();
    }

    @Override
    public int compareTo(Flavor o) {
      return name.compareTo(o.name());
    }

    @Override
    public String toString() {
      return name + (forceReflavor ? "!" : "");
    }
  }

  /**
   * Typical entry from a routing table.
   * Tracks entry count, node segments (with ranks), morphs to a NodeHandle as needed.
   *
   * @author Anton Kraievoy
   */
  public static class RoutingEntry extends NodeHandleBase {
    public static final int LIVENESS_DEFAULT = 55;
    public static final int RANGE_COUNT_DEFAULT = 1;

    public static final float LIVENESS_COMM_FAIL_PENALTY =
        (float) Math.pow((1 + Math.sqrt(5)) / 2, -0.25);
    public static final float LIVENESS_COMM_SUCCESS_REWARD =
        (float) Math.pow(LIVENESS_COMM_FAIL_PENALTY, -0.25);
    public static final float LIVENESS_MIN =
        (float) (LIVENESS_DEFAULT * Math.pow(LIVENESS_COMM_FAIL_PENALTY, 3));

    static {
      if (LIVENESS_COMM_FAIL_PENALTY >= 1) {
        throw new IllegalStateException(
            String.format(
                "LIVENESS_COMM_FAIL_PENALTY(%g) >= 1",
                LIVENESS_COMM_FAIL_PENALTY
            )
        );
      }
      if (LIVENESS_COMM_SUCCESS_REWARD <= 1) {
        throw new IllegalStateException(
            String.format(
                "LIVENESS_COMM_SUCCESS_REWARD(%g) <= 1",
                LIVENESS_COMM_SUCCESS_REWARD
            )
        );
      }
    }

    //	LATER change this to model time
    protected static final AtomicLong vmStamp = new AtomicLong(1);

    protected final RangeBase nodeIdRange;
    protected final List<Range> ranges;
    protected final int entryCount;
    protected float liveness;
    /**
     * Essentially this field allows for controlling stale routing data.
     * This works when all new records with incremented stamp for any nodeId are created on that node.
     */
    protected long stamp;  //	TODO find a way to enforce this stamping rule

    public static RoutingEntry stub(Key nodeId, Address address) {
      return new RoutingEntry(nodeId, address);
    }

    private RoutingEntry(Key nodeId, Address address) {
      this(nodeId, address, 0, Collections.<Range>emptyList(), LIVENESS_DEFAULT, -1);
    }

    public static RoutingEntry own(Key nodeId, Address address, int entries, Range... ranges) {
      return new RoutingEntry(nodeId, address, entries, ranges);
    }

    private RoutingEntry(Key nodeId, Address address, int entries, Range... ranges) {
      this(nodeId, address, entries, Arrays.asList(ranges), LIVENESS_DEFAULT, stampNext());
    }

    protected RoutingEntry(
        Key nodeId,
        Address address,
        int entries,
        List<Range> rangeList,
        float liveness,
        long stamp
    ) {
      super(nodeId, address);
      this.nodeIdRange = new RangeBase(nodeId, nodeId.next());
      this.ranges = new ArrayList<Range>(rangeList);
      this.entryCount = entries;
      this.liveness = liveness;
      this.stamp = stamp;
    }

    public long stamp() {
      return stamp;
    }

    public float liveness() {
      return liveness;
    }

    public int entryCount() {
      return entryCount;
    }

    public List<Range> ranges() {
      return ranges;
    }

    public String toString() {
      return "[" + nodeId + "@" + address + ":" + liveness + " " + ranges.toString() + "]";
    }

    public RoutingEntry entryCount(final int newEntryCount) {
      return new RoutingEntry(
          nodeId, address, newEntryCount, ranges, liveness, stampUpdate()
      );
    }

    public RoutingEntry liveness(Event event) {
      return new RoutingEntry(
          nodeId, address, entryCount, ranges, nextLiveness(liveness, event), stampUpdate()
      );
    }

    public RoutingEntry update(RoutingEntry otherVersion) {
      if (!nodeId.equals(otherVersion.nodeId)) {
        throw new IllegalStateException("updating from entry with other nodeId");
      }

      if (stamp <= otherVersion.stamp) {
        return new RoutingEntry(
            nodeId,
            address,
            otherVersion.entryCount,
            otherVersion.ranges,
            liveness,
            otherVersion.stamp
        );
      }

      return this;
    }

    public RoutingEntry ranges(final Range singleRange) {
      return ranges(Collections.singletonList(singleRange));
    }

    public RoutingEntry ranges(final List<Range> ranges) {
      return new RoutingEntry(
          nodeId, address, entryCount, ranges, liveness, stampUpdate()
      );
    }

    private long stampUpdate() { return stamp > 0 ? stampNext() : stamp; }
    private static long stampNext() {return vmStamp.getAndIncrement();}

    public boolean isReplicaFor(Key key, byte maxRank) {
      for (Range r : ranges) {
        if (r.getRank() <= maxRank && r.contains(key)) {
          return true;
        }
      }

      return false;
    }

    public Range getRangeFor(Key lKey, byte maxRank) {
      Range bestRange = null;

      for (int i = 0; i < ranges.size() && (bestRange == null || !bestRange.contains(lKey)); i++) {
        Range r = ranges.get(i);
        if (r.getRank() > maxRank) {
          continue;
        }

        if (bestRange == null) {
          bestRange = r;
          continue;
        }

        if (r.contains(lKey) || KeySpace.isInOpenRightRange(lKey, bestRange.getLKey(), r.getLKey())) {
          bestRange = r;
        }
      }

      return bestRange;
    }

    public static float nextLiveness(float curLiveness, Event event) {
      final float newLiveness;
      if (event == Event.DISCOVERED) {
        //	recover/improve linearly
        newLiveness = curLiveness + 1;
      } else if (event == Event.CONNECTION_FAILED) {
        //	penalize exponentially
        newLiveness = curLiveness * LIVENESS_COMM_FAIL_PENALTY;
      } else if (event == Event.JOINED) {
        //	reset
        newLiveness = LIVENESS_DEFAULT;
      } else if (event == Event.LEFT) {
        //	jolt to minimum
        newLiveness = 1;
      } else if (event == Event.HEART_BEAT) {
        //	improve exponentially
        newLiveness = curLiveness * LIVENESS_COMM_SUCCESS_REWARD;
      } else {
        throw Die.unexpected("event", event);
      }
      return newLiveness;
    }

    public Range selectRange(
        Address localAddress, Key target, final RoutingDistance routingDistance
    ) {
      Range bestRange = nodeIdRange;
      double bestDist =
          routingDistance.apply(localAddress, target, address, bestRange);

      for (Range r : ranges) {
        final double curDist =
            routingDistance.apply(localAddress, target, address, r);

        if (curDist < bestDist) {
          bestRange = r;
          bestDist = curDist;
        }
      }

      return bestRange;
    }

    public Range getRange() {
      return getRange((byte) 0);
    }

    public Range getRange(byte rank) {
      for (Range r : ranges) {
        if (r.getRank() == rank) {
          return r;
        }
      }

      return nodeIdRange;
    }
  }

  public static class RouteTable {
    private final SortedMap<String, TreeSet<Address>> flavorToAddresses =
        new TreeMap<String, TreeSet<Address>>();
    private final SortedMap<Address, Flavor> addressToFlavor =
        new TreeMap<Address, Flavor>();
    private final SortedMap<Address, RoutingEntry> addressToRoute =
        new TreeMap<Address, RoutingEntry>();

    protected RouteTable() {
      //  sealed for foreigners
    }

    public int size() {
      return addressToRoute.size();
    }

    public int size(final Flavor flavor) {
      final SortedSet<Address> addresses = flavorToAddresses.get(flavor.name);
      if (addresses == null) {
        return 0;
      }

      return addresses.size();
    }

    public int flavorCount(boolean includeEmpty) {
      if (includeEmpty) {
        return flavorToAddresses.size();
      }

      int nonEmptyFlavorCount = 0;

      for (Map.Entry<String, TreeSet<Address>> e : flavorToAddresses.entrySet()) {
        if (!e.getValue().isEmpty()) {
          nonEmptyFlavorCount++;
        }
      }

      return nonEmptyFlavorCount;
    }

    public Flavor flavor(final Address address) {
      return addressToFlavor.get(address);
    }

    public boolean has(final Address address) {
      return addressToFlavor.containsKey(address);
    }

    public RoutingEntry route(final Address address) {
      return addressToRoute.get(address);
    }

    public Collection<RoutingEntry> routes() {
      return Collections.unmodifiableCollection(addressToRoute.values());
    }

    public Collection<Address> adresses() {
      return Collections.unmodifiableCollection(addressToRoute.keySet());
    }

    public Collection<Address> adresses(final Flavor flavor) {
      final SortedSet<Address> addresses = flavorToAddresses.get(flavor.name);
      if (addresses == null) {
        return Collections.emptySet();
      }

      return Collections.unmodifiableCollection(addresses);
    }

    public float minLiveness() {
      float minLiveness =
          RoutingEntry.LIVENESS_MIN * RoutingEntry.LIVENESS_COMM_FAIL_PENALTY;
      for (RoutingEntry myRe : routes()) {
        minLiveness = Math.min(minLiveness, myRe.liveness());
      }
      return minLiveness;
    }

    public int add(final Flavor flavor, final RoutingEntry route) {
      final Address address = route.getAddress();
      final Flavor prevFlavor = addressToFlavor.get(address);
      if (prevFlavor != null) {
        if (prevFlavor.equals(flavor)) {
          addressToRoute.put(address, route);
          return size(flavor);
        }
        remove(address);
      }

      addressToFlavor.put(address, flavor);
      addressToRoute.put(address, route);
      final TreeSet<Address> addresses = flavorToAddresses.get(flavor.name);
      if (addresses == null) {
        final TreeSet<Address> newAddresses = new TreeSet<Address>();
        newAddresses.add(address);
        flavorToAddresses.put(flavor.name, newAddresses);
        return newAddresses.size();
      }

      addresses.add(address);
      return addresses.size();
    }

    public void update(final RoutingEntry route) {
      final Address address = route.getAddress();
      final Flavor prevFlavor = addressToFlavor.get(address);

      if (prevFlavor == null) {
        throw new IllegalStateException("route not previously stored");
      }

      addressToRoute.put(address, route);
    }

    public int remove(final Address address) {
      final Flavor prevFlavor = addressToFlavor.remove(address);
      if (prevFlavor == null) {
        return -1;
      }

      addressToRoute.remove(address);
      final TreeSet<Address> addresses = flavorToAddresses.get(prevFlavor.name);
      if (addresses == null) {
        return 0;
      }
      addresses.remove(address);
      return addresses.size();
    }
  }
}
