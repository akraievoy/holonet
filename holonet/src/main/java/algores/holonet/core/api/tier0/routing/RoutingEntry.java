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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Typical entry from a routing table.
 * Tracks entry count, node segments (with ranks), morphs to a NodeHandle as needed.
 *
 * @author Anton Kraievoy
 */
public class RoutingEntry extends NodeHandleBase {
  /**
   * yep, that's a Fibonachi number here
   */
  public static final int LIVENESS_DEFAULT = 55;
  public static final int RANGE_COUNT_DEFAULT = 1;

  /**
   * the golden ratio.
   */
  public final double phi = (1 + Math.sqrt(5)) / 2;
  /**
   * the inverted golden ratio.
   */
  public final double PHI = 2 / (1 + Math.sqrt(5));

  //	TODO later change this to model time
  protected static final AtomicLong vmStamp = new AtomicLong(1);

  protected final List<Range> ranges = new ArrayList<Range>(RANGE_COUNT_DEFAULT);
  protected int entryCount;
  protected byte liveness = LIVENESS_DEFAULT;

  /**
   * Essentially this field allows for controlling stale routing data.
   * This works when all new records with incremented stamp for any nodeId are created on that node.
   */
  protected long stamp;  //	TODO find a way to enforce this stamping rule

  public RoutingEntry(Key nodeId, Address address, Range range) {
    this(nodeId, address);

    ranges.add(range);
  }

  public RoutingEntry(Key nodeId, Address address) {
    super(nodeId, address);
    updateStamp();
  }

  public RoutingEntry(Key nodeId, Address address, Range path, int entries) {
    this(nodeId, address, path);
    entryCount = entries;
  }

  /**
   * careful: called from constructor
   */
  protected void updateStamp() {
    this.stamp = vmStamp.getAndIncrement();
  }

  public long getStamp() {
    return stamp;
  }

  public byte getLiveness() {
    return liveness;
  }

  public void setLiveness(byte liveness) {
    this.liveness = liveness;
  }

  public int getEntryCount() {
    return entryCount;
  }

  public RoutingEntry updateEntryCount(final int newEntryCount) {
    if (this.entryCount != newEntryCount) {
      this.entryCount = newEntryCount;
      this.stamp = getNextStamp();
    }

    return this;
  }

  public List<Range> getRanges() {
    return ranges;
  }

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

  public void updateLiveness(Event event) {
    if (event == Event.DISCOVERED) {
      //	recover/improve linearly
      liveness = liveness < 127 ? (byte) (liveness + 0x01) : 127;
    } else if (event == Event.CONNECTION_FAILED) {
      //	penalize exponentially
      liveness = renormLiveness(PHI * liveness);
    } else if (event == Event.JOINED) {
      //	reset
      liveness = LIVENESS_DEFAULT;
    } else if (event == Event.LEFT) {
      //	jolt to minimum
      liveness = 1;
    } else if (event == Event.HEART_BEAT) {
      //	improve exponentially
      liveness = renormLiveness(phi * liveness);
    } else {
      throw Die.unexpected("event", event);
    }
  }

  protected byte renormLiveness(double value) {
    if (value <= 1) {
      return 1;
    }

    if (value >= 127) {
      return 127;
    }

    return (byte) Math.round(value);
  }

  public boolean update(long newStamp, int newEntryCount, List<Range> newRanges) {
    if (stamp <= newStamp) {

      entryCount = newEntryCount;
      ranges.clear();
      ranges.addAll(newRanges);
      stamp = newStamp;

      return true;
    }

    return false;
  }

  public Range selectRange(Key target, final RoutingPreference routingPreference) {
    Range bestRange = getNodeIdRange();

    for (Range r : ranges) {
      if (routingPreference.isPreferred(target, r, bestRange)) {
        bestRange = r;
      }
    }

    return bestRange;
  }

  public RangeBase getNodeIdRange() {
    return new RangeBase(getNodeId(), getNodeId().next());
  }

  public String toString() {
    return "[" + nodeId + "@" + address + ":" + liveness + " " + ranges.toString() + "]";
  }

  public static long getNextStamp() {
    return RoutingEntry.vmStamp.getAndIncrement();
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

    return getNodeIdRange();
  }

  public RoutingEntry updateRanges(final Range[] ranges) {
    this.ranges.clear();
    this.ranges.addAll(Arrays.asList(ranges));
    this.stamp = getNextStamp();

    return this;
  }

  public RoutingEntry updateRanges(final Range singleRange) {
    this.ranges.clear();
    this.ranges.add(singleRange);
    this.stamp = getNextStamp();

    return this;
  }

  public RoutingEntry updateRanges(final List<Range> ranges) {
    this.ranges.clear();
    this.ranges.addAll(ranges);
    this.stamp = getNextStamp();

    return this;
  }

  public RoutingEntry copy() {
    final RoutingEntry copy = new RoutingEntry(nodeId, address);

    copy.entryCount = this.entryCount;
    copy.stamp = this.stamp;
    copy.ranges.addAll(this.ranges);

    return copy;
  }
}
