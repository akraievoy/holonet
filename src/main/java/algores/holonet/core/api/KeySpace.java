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
import java.util.BitSet;

/**
 * Generic key space logic.
 *
 * @author Anton Kraievoy
 */
public class KeySpace {
  private KeySpace() {
    //	sealed
  }

  public static boolean isInRange(KeySource min, boolean includeMin, KeySource max, boolean includeMax, KeySource key) {
    if (key == null) {
      throw new IllegalArgumentException("key should be NOT null");
    }

    //check the bound condition first
    if (sameKey(min, key)) {
      return includeMin;
    }

    if (sameKey(max, key)) {
      return includeMax;
    }

    final boolean left = min.getKey().compareTo(key.getKey()) < 0;
    final boolean right = max.getKey().compareTo(key.getKey()) > 0;

    return isWrapped(min, max) ? left || right : left && right;
  }

  public static boolean isWrapped(final KeySource min, final KeySource max) {
    return max.getKey().compareTo(min.getKey()) <= 0;
  }

  public static boolean sameKey(final KeySource keyA, KeySource keyB) {
    return keyA.getKey().equals(keyB.getKey());
  }

  public static boolean sameKey(KeySource a, KeySource b, int bits) {
    final Key aKey = a.getKey();
    final Key bKey = b.getKey();

    if ((aKey instanceof KeyBase) && (bKey instanceof KeyBase)) {
      final BitSet aData = ((KeyBase) aKey).keyData;
      final BitSet bData = ((KeyBase) bKey).keyData;

      return Generic.compareBitSubsets(aData, bData, bits) == 0;
    }

    final BigInteger aBits = aKey.toNumber().shiftRight(Key.BITNESS - bits);
    final BigInteger bBits = bKey.toNumber().shiftRight(Key.BITNESS - bits);

    return aBits.equals(bBits);
  }

  /**
   * Checks if given <code>keyToCheck</code> belongs to range with excluded boundaries.
   *
   * @param min        range minimum, exclusive
   * @param max        range maximum, exclusive
   * @param keyToCheck key to check
   * @return true if key present
   */
  public static boolean isInOpenRange(KeySource min, KeySource max, KeySource keyToCheck) {
    validateNullReferences(min, max, keyToCheck);
    return isInRange(min, false, max, false, keyToCheck);
  }

  /**
   * Checks if given <code>keyToCheck</code> belongs to range with included boundaries.
   *
   * @param min        range minimum, inclusive
   * @param max        range maximum, inclusive
   * @param keyToCheck key to check
   * @return true if key present
   */
  public static boolean isInClosedRange(KeySource min, KeySource max, KeySource keyToCheck) {
    validateNullReferences(min, max, keyToCheck);
    return isInRange(min, true, max, true, keyToCheck);
  }

  /**
   * Checks if given <code>keyToCheck</code> belongs to range with excluded left boundary, and included right boundary.
   *
   * @param min        range minimum, exclusive
   * @param max        range maximum, inclusive
   * @param keyToCheck key to check
   * @return true if key present
   */
  public static boolean isInOpenLeftRange(KeySource min, KeySource max, KeySource keyToCheck) {
    validateNullReferences(min, max, keyToCheck);
    return isInRange(min, false, max, true, keyToCheck);
  }

  /**
   * Checks if given <code>keyToCheck</code> belongs to range with excluded right boundary, and included left boundary.
   *
   * @param min        range minimum, inclusive
   * @param max        range maximum, exclusive
   * @param keyToCheck key to check
   * @return true if key present
   */
  public static boolean isInOpenRightRange(KeySource min, KeySource max, KeySource keyToCheck) {
    validateNullReferences(min, max, keyToCheck);
    return isInRange(min, true, max, false, keyToCheck);
  }

  public static void validateNullReferences(KeySource min, KeySource max, KeySource keyToCheck) {
    Die.ifNull("min", min);
    Die.ifNull("max", max);
    Die.ifNull("keyToCheck", keyToCheck);
  }

  public static int getCommonPrefixLen(final Key a, final Key b, final int maxBits) {
    int commonBits = 0;
    while (commonBits < maxBits && a.get(prefix2regular(commonBits)) == b.get(prefix2regular(commonBits))) {
      commonBits++;
    }

    return commonBits;
  }

  public static int prefix2regular(final int bitIndex) {
    return Key.BITNESS - bitIndex - 1;
  }
}
