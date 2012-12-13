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
import java.util.BitSet;

/**
 * Some generic util code extracted from here and there.
 *
 * @author Anton Kraievoy
 */
class Generic {
  private Generic() {
    //	sealed
  }

  protected static BitSet bigInt2BitSet(BigInteger number) {
    final BitSet numAsBits = new BitSet();

    int atBit = 0;
    BigInteger curNumber = number;
    while (curNumber.compareTo(BigInteger.ZERO) > 0) {
      final int lowestSetBit = curNumber.getLowestSetBit();
      final int bitsProcessed = lowestSetBit + 1;

      numAsBits.set(atBit + lowestSetBit, true);
      atBit += bitsProcessed;
      curNumber = curNumber.shiftRight(bitsProcessed);
    }

    return numAsBits;
  }

  @SuppressWarnings({"ConstantConditions"})
  protected static String bitSet2HexString(BitSet bits, int padding) {
    final StringBuffer result = new StringBuffer();

    int atBit = 0;
    while (atBit < bits.length()) {
      char nextDigit;
      if (bits.get(atBit)) {
        if (bits.get(atBit + 1)) {
          if (bits.get(atBit + 2)) {
            if (bits.get(atBit + 3)) {
              nextDigit = 'f';
            } else {
              nextDigit = '7';
            }
          } else {
            if (bits.get(atBit + 3)) {
              nextDigit = 'b';
            } else {
              nextDigit = '3';
            }
          }
        } else {
          if (bits.get(atBit + 2)) {
            if (bits.get(atBit + 3)) {
              nextDigit = 'd';
            } else {
              nextDigit = '5';
            }
          } else {
            if (bits.get(atBit + 3)) {
              nextDigit = '9';
            } else {
              nextDigit = '1';
            }
          }
        }
      } else {
        if (bits.get(atBit + 1)) {
          if (bits.get(atBit + 2)) {
            if (bits.get(atBit + 3)) {
              nextDigit = 'e';
            } else {
              nextDigit = '6';
            }
          } else {
            if (bits.get(atBit + 3)) {
              nextDigit = 'a';
            } else {
              nextDigit = '2';
            }
          }
        } else {
          if (bits.get(atBit + 2)) {
            if (bits.get(atBit + 3)) {
              nextDigit = 'c';
            } else {
              nextDigit = '4';
            }
          } else {
            if (bits.get(atBit + 3)) {
              nextDigit = '8';
            } else {
              nextDigit = '0';
            }
          }
        }
      }

      atBit += 4;
      result.insert(0, nextDigit);
    }

    while (result.length() < padding) {
      result.insert(0, '0');
    }

    return result.toString();
  }

  protected static String bitSet2BinaryString(BitSet bits, int padding) {
    final StringBuffer result = new StringBuffer();

    int atBit = 0;
    while (atBit < bits.length()) {
      char nextDigit = bits.get(atBit) ? '1' : '0';
      atBit += 1;
      result.insert(0, nextDigit);
    }

    while (result.length() < padding) {
      result.insert(0, '0');
    }

    return result.toString();
  }

  protected static int compareBitSets(BitSet thisKeyData, BitSet thatKeyData) {
    int thatLen = thatKeyData.length();
    int thisLen = thisKeyData.length();
    if (thatLen != thisLen) {
      return thisLen < thatLen ? -1 : 1;
    }

    return compareBitSubsets(thisKeyData, thatKeyData, thisLen);
  }

  protected static int compareBitSubsets(BitSet thisKeyData, BitSet thatKeyData, int maxBits) {
    for (int atBit = maxBits - 1; atBit >= 0; atBit--) {
      final boolean thisBit = thisKeyData.get(atBit);
      final boolean thatBit = thatKeyData.get(atBit);
      if (thisBit ^ thatBit) {
        return thisBit ? 1 : -1;
      }
    }

    return 0;
  }

  private static BigInteger[] masksBigInt = createMasksBigInt();
  private static long[] masksLong = createMasksLong();

  private static long[] createMasksLong() {
    final long[] masks = new long[63];
    for (int i = 0; i < masks.length; i++) {
      masks[i] = i == 0 ? 1 : masks[i-1] << 1;
    }
    return masks;
  }

  private static BigInteger[] createMasksBigInt() {
    final BigInteger[] masks = new BigInteger[Key.BITNESS];
    for (int i = 0; i < masks.length; i++) {
      masks[i] = i == 0 ? BigInteger.ONE : masks[i-1].shiftLeft(1);
    }
    return masks;
  }

  protected static BigInteger bitSet2BigInt(final BitSet bits) {
    BigInteger result = BigInteger.ZERO;

    int atBit = 0;
    while (atBit < bits.length()) {
      final int lowestSetBit = bits.nextSetBit(atBit);
      result = result.add(masksBigInt[lowestSetBit]);

      atBit = lowestSetBit + 1;
    }

    return result;
  }

  protected static long bitSet2long(final BitSet bits) {
    long result = 0;

    int atBit = 0;
    while (atBit < bits.length()) {
      final int lowestSetBit = bits.nextSetBit(atBit);
      result = result + masksLong[lowestSetBit];

      atBit = lowestSetBit + 1;
    }

    return result;
  }
}
