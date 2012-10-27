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
 * Default implementation of Common API Key.
 *
 * @author Anton Kraievoy
 */
class KeyBase implements Key {
  /**
   * Numeric value for upper limit of numeric values <b>(exclusive)</b>.
   */
  public static final BigInteger UPPERLIMIT = BigInteger.ONE.shiftLeft(BITNESS);

  protected final BitSet keyData;
  protected final int hashCode;

  protected KeyBase next;
  protected KeyBase prev;

  protected KeyBase(byte[] digestData) {
    this(new BigInteger(digestData).abs());
  }

  /**
   * Constructs from a BigInteger.
   *
   * @param number must be positive
   */
  protected KeyBase(BigInteger number) {
    this(Generic.bigInt2BitSet(validateConstructorArg(number).mod(UPPERLIMIT)));
  }

  protected static BigInteger validateConstructorArg(BigInteger number) {
    Die.ifFalse("number >= 0", BigInteger.ZERO.compareTo(number) <= 0);

    return number;
  }

  protected KeyBase(BitSet newKeyData) {
    Die.ifFalse("newKeyData.length <= BITNESS", newKeyData.length() <= BITNESS);

    keyData = newKeyData;
    hashCode = newKeyData.hashCode();
  }

  public boolean get(int atBit) {
    return keyData.get(atBit);
  }

  public String toHexString() {
    return Generic.bitSet2HexString(keyData, BYTENESS << 1);
  }

  public String toBinaryString() {
    return Generic.bitSet2BinaryString(keyData, BITNESS);
  }

  public String toString() {
    return toHexString();
  }

  public int compareTo(algores.holonet.capi.Key o) {
    return Generic.compareBitSets(keyData, ((KeyBase) o).keyData);
  }

  public boolean equals(Object o) {
    return this == o || o instanceof KeyBase && keyData.equals(((KeyBase) o).keyData);
  }

  public int hashCode() {
    return hashCode;
  }

  public BigInteger toNumber() {
    return Generic.bitSet2BigInt(keyData);
  }

  @Override
  public long toLong() {
    if (BITNESS > 63) {
      throw new IllegalStateException(
          "unable to convert key to long as BITNESS > 63"
      );
    }
    return Generic.bitSet2long(keyData);
  }

  public KeyBase set(int atBit, boolean value) {
    Die.ifFalse("atBit < BITNESS", atBit < BITNESS);
    final BitSet newKeyData = new BitSet(keyData.length());
    newKeyData.or(keyData);
    newKeyData.set(atBit, value);

    return new KeyBase(newKeyData);
  }

  public KeyBase sub(int bits) {
    final BitSet newKeyData = new BitSet(keyData.length());
    newKeyData.or(keyData);
    newKeyData.clear(bits, newKeyData.length());

    return new KeyBase(newKeyData);
  }

  public KeyBase next(final int power) {
    final BitSet newKeyData = new BitSet(keyData.length());
    newKeyData.or(keyData);

    final int clearBit = newKeyData.nextClearBit(power);
    newKeyData.set(clearBit, true);
    newKeyData.set(power, clearBit, false);
    if (newKeyData.length() > BITNESS) {
      newKeyData.clear(BITNESS, newKeyData.length());
    }

    return new KeyBase(newKeyData);
  }

  public KeyBase next() {
    if (next == null) {
      next = next(0);
      next.prev = this;
    }

    return next;
  }

  public KeyBase prev(final int power) {
    final BitSet newKeyData = new BitSet(keyData.length());
    newKeyData.or(keyData);
    newKeyData.set(BITNESS, true);

    final int nextSetBit = newKeyData.nextSetBit(power);
    newKeyData.set(nextSetBit, false);
    newKeyData.set(power, nextSetBit, true);
    if (newKeyData.length() > BITNESS) {
      newKeyData.clear(BITNESS, newKeyData.length());
    }

    return new KeyBase(newKeyData);
  }

  public KeyBase prev() {
    if (prev == null) {
      prev = prev(0);
      prev.next = this;
    }

    return prev;
  }

  public BigInteger distance(Key target) {
    return UPPERLIMIT.add(target.toNumber()).subtract(toNumber()).mod(UPPERLIMIT);
  }

  public KeyBase getKey() {
    return this;
  }
}
