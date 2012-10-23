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

/**
 * A key is a 160-bit string.
 *
 * @author Anton Kraievoy
 */
public interface Key extends KeySource, algores.holonet.capi.Key {
  /**
   * Number of significant bits in the key.
   */
  int BITNESS = 32;
  /**
   * Number of bytes in the key.
   */
  int BYTENESS = (BITNESS + 7) / 8;

  BigInteger toNumber();

  long toLong();

  boolean get(int atBit);

  Key set(int atBit, boolean value);

  /**
   * Leaves out only <code>bits</code> less significant bits, clearing all others.
   *
   * @param bits to leave intact
   * @return the cleared version of the Key
   */
  Key sub(int bits);

  /**
   * Adds 2 ^ <code>power</code> to the given key.
   *
   * @param power to use
   * @return the shifted version of the Key
   */
  Key next(int power);

  /**
   * Returns the next adjanced key.
   *
   * @return next adjanced key.
   */
  Key next();

  Key prev(int power);

  Key prev();

  BigInteger distance(Key target);
}
