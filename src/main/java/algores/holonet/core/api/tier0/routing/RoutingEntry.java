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

  public static final int RANGE_COUNT_DEFAULT = 1;

  //	TODO later change this to model time
  protected static final AtomicLong vmStamp = new AtomicLong(1);

  protected final RangeBase nodeIdRange;
  protected final List<Range> ranges = new ArrayList<Range>(RANGE_COUNT_DEFAULT);
  protected int entryCount;
  protected float liveness = LIVENESS_DEFAULT;

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
    nodeIdRange = new RangeBase(getNodeId(), getNodeId().next());
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

  public float getLiveness() {
    return liveness;
  }

  public void setLiveness(float liveness) {
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
    liveness = computeNewLiveness(event);
  }

  public float computeNewLiveness(Event event) {
    final float newLiveness;
    if (event == Event.DISCOVERED) {
      //	recover/improve linearly
      newLiveness = liveness + 1;
    } else if (event == Event.CONNECTION_FAILED) {
      //	penalize exponentially
      newLiveness = liveness * LIVENESS_COMM_FAIL_PENALTY;
    } else if (event == Event.JOINED) {
      //	reset
      newLiveness = LIVENESS_DEFAULT;
    } else if (event == Event.LEFT) {
      //	jolt to minimum
      newLiveness = 1;
    } else if (event == Event.HEART_BEAT) {
      //	improve exponentially
      newLiveness = liveness * LIVENESS_COMM_SUCCESS_REWARD;
    } else {
      throw Die.unexpected("event", event);
    }
    return newLiveness;
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

    return nodeIdRange;
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
