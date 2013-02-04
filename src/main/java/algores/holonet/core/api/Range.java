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

import java.math.BigInteger;
import java.util.Set;

/**
 * Essentially a pair of keys which define a contiguous segment in the key space.
 * <p>Is defined in form of [lKey, rKey), as this seems to involve less computing overhead while performing basic operations.</p>
 * <p>While used for building routing tables, the rank field also may carry some extra info.</p>
 *
 * @author Anton Kraievoy
 */
public interface Range extends KeySource {
  /**
   * Less boundary, inclusive.
   *
   * @return less boundary, inclusive.
   */
  Key getLKey();

  /**
   * Greater boundary, exclusive.
   *
   * @return greater boundary, exclusive.
   */
  Key getRKey();

  /**
   * Rank, optional property.
   *
   * @return rank value, non-negative.
   */
  byte getRank();

  boolean contains(Key key);

  //	TODO take the method over to here, don't envy, anyway wrapped depends on range semantics

  boolean isWrapped();

  boolean isSame(Range otherPath);

  boolean isPrefixFor(Range otherPath, boolean inclusive);

  Range expandWithComplement(Range otherPath);

  Range append(boolean newBit);

  int getBits();

  int count(Set<Key> pKeys);

  Range getCommonPrefixPath(Range path);

  int getCommonPrefixLen(Range otherPath, int maxBits);

  int getCommonPrefixLen(Range path);

  BigInteger width();
}
