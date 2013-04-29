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

package algores.holonet.core.api;

import org.akraievoy.base.Die;

import java.math.BigInteger;
import java.util.Set;

/**
 * Essentially a pair of keys which define a contiguous segment in the key space.
 * <p>Is defined in form of [lKey, rKey), as this seems to involve less computing overhead while performing basic operations.</p>
 * <p>While used for building routing tables, the rank field also may carry some extra info.</p>
 *
 * @author Anton Kraievoy
 */
public class RangeBase implements Range {
  protected Key lKey;
  protected Key rKey;
  protected byte rank;
  private BigInteger width;

  public RangeBase(Key lKey, Key rKey) {
    this(lKey, rKey, (byte) 0);
  }

  public RangeBase(Key lKey, Key rKey, byte rank) {
    Die.ifNull("lKey", lKey);
    Die.ifNull("rKey", rKey);
    Die.ifTrue("rank < 0", rank < 0);

    this.lKey = lKey;
    this.rKey = rKey;
    this.rank = rank;
  }

  public RangeBase(Key lKey, int prefixBits) {
    final BigInteger mask = BigInteger.ONE.shiftLeft(KeySpace.prefix2regular(prefixBits) + 1).subtract(BigInteger.ONE);
    this.lKey = new KeyBase(lKey.toNumber().andNot(mask));
    this.rKey = this.lKey.next(Key.BITNESS - prefixBits);
  }

  /**
   * Less boundary, inclusive.
   *
   * @return less boundary, inclusive.
   */
  public Key getLKey() {
    return lKey;
  }

  /**
   * Less boundary, inclusive.
   *
   * @param lKey less boundary, inclusive.
   */
  public void setLKey(Key lKey) {
    Die.ifNull("lKey", lKey);

    this.lKey = lKey;
  }

  /**
   * Greater boundary, exclusive.
   *
   * @return greater boundary, exclusive.
   */
  public Key getRKey() {
    return rKey;
  }

  /**
   * Greater boundary, exclusive.
   *
   * @param rKey greater boundary, exclusive.
   */
  public void setRKey(Key rKey) {
    Die.ifNull("rKey", rKey);

    this.rKey = rKey;
  }

  /**
   * Rank, optional property.
   *
   * @return rank value, non-negative.
   */
  public byte getRank() {
    return rank;
  }

  /**
   * Rank, optional property.
   *
   * @param rank, non-negative.
   */
  public void setRank(byte rank) {
    Die.ifTrue("rank < 0", rank < 0);

    this.rank = rank;
  }

  public boolean contains(Key key) {
    return KeySpace.isInOpenRightRange(lKey, rKey, key);
  }

  public boolean isWrapped() {
    return KeySpace.isWrapped(lKey, rKey);
  }

  public String toString() {
    return "[" + lKey + ":" + rKey + ")";
  }

  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof RangeBase)) {
      return false;
    }

    final Range thatRange = (Range) o;
    return lKey.equals(thatRange.getLKey()) && rKey.equals(thatRange.getRKey());
  }

  public int hashCode() {
    int result = lKey.hashCode();
    result = 31 * result + rKey.hashCode();
    return result;
  }

  public boolean isSame(Range otherRange) {
    return getLKey().equals(otherRange.getLKey()) && getRKey().equals(otherRange.getRKey());
  }

  public boolean isPrefixFor(Range otherPath, boolean inclusive) {
    int bits = getBits();

    if (inclusive && isSame(otherPath)) {
      return true;
    }

    final boolean prefixFor = bits < otherPath.getBits() && KeySpace.sameKey(lKey, otherPath.getLKey(), bits);

    return prefixFor;
  }

  public Range expandWithComplement(Range otherPath) {
    Die.ifNull("otherPath", otherPath);

    final int bits = getBits();
    final int prefixBit = KeySpace.prefix2regular(bits);
    final Key expKey = getLKey().set(prefixBit, !otherPath.getKey().get(prefixBit));
    final Range expPath = new RangeBase(expKey, bits + 1);

    return expPath;
  }

  public Range append(boolean newBit) {
    final int bits = getBits();
    final Key newKey = getLKey().set(KeySpace.prefix2regular(bits), newBit);
    final Range appendedPath = new RangeBase(newKey, bits + 1);

    return appendedPath;
  }

  public Key getKey() {
    return getLKey();
  }

  public int getBits() {
    //	this is a special case when range covers all the address space
    if (getLKey().equals(getRKey())) {
      return 0;
    }

    return KeySpace.getCommonPrefixLen(getLKey(), getRKey().prev(), Key.BITNESS);
  }

  public int count(Set<Key> pKeys) {
    int result = 0;

    for (Key key : pKeys) {
      if (contains(key)) {
        result++;
      }
    }

    return result;
  }

  public Range getCommonPrefixPath(Range path) {
    return new RangeBase(getLKey(), getCommonPrefixLen(path, Key.BITNESS));
  }

  public int getCommonPrefixLen(Range otherPath, int maxBits) {
    final Key myKey = getLKey();
    final Key otherKey = otherPath.getLKey();

    return KeySpace.getCommonPrefixLen(myKey, otherKey, maxBits);
  }

  public int getCommonPrefixLen(Range path) {
    return getCommonPrefixLen(path, Math.min(getBits(), path.getBits()));
  }

  @Override
  public synchronized BigInteger width() {
    if (width == null) {
      width = lKey.distance(rKey).abs();
    }
    return width;
  }
}
