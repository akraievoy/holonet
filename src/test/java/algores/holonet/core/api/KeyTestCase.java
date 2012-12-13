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

import junit.framework.TestCase;

import java.math.BigInteger;

public class KeyTestCase extends TestCase {
  public void testGet() throws Exception {
    KeyBase key1 = new KeyBase(new BigInteger("11110000111100001111000011110000", 2));
    //                                         31282724232019161512110807040300

    assertTrue(key1.get(31));
    assertTrue(key1.get(30));
    assertTrue(key1.get(29));
    assertTrue(key1.get(28));
    assertFalse(key1.get(27));
    assertFalse(key1.get(26));
    assertFalse(key1.get(25));
    assertFalse(key1.get(24));

    assertTrue(key1.get(23));
    assertTrue(key1.get(20));
    assertFalse(key1.get(19));
    assertFalse(key1.get(16));
    assertTrue(key1.get(15));
    assertTrue(key1.get(12));
    assertFalse(key1.get(11));
    assertFalse(key1.get(8));

    assertTrue(key1.get(7));
    assertTrue(key1.get(6));
    assertTrue(key1.get(5));
    assertTrue(key1.get(4));
    assertFalse(key1.get(3));
    assertFalse(key1.get(2));
    assertFalse(key1.get(1));
    assertFalse(key1.get(0));
  }

  public void testSub() throws Exception {
    KeyBase key1 = new KeyBase(new BigInteger("11110000111100001111000011110000", 2));
    //                                         31282724232019161512110807040300

    assertEquals(BigInteger.ZERO, key1.sub(0).toNumber());
    assertEquals(BigInteger.ZERO, key1.sub(1).toNumber());
    assertEquals(BigInteger.ZERO, key1.sub(3).toNumber());
    assertEquals(BigInteger.ZERO, key1.sub(4).toNumber());
    assertEquals(new BigInteger("10000", 2), key1.sub(5).toNumber());
    assertEquals(new BigInteger("1110000", 2), key1.sub(7).toNumber());
    assertEquals(new BigInteger("11110000", 2), key1.sub(8).toNumber());
    assertEquals(new BigInteger("11110000", 2), key1.sub(9).toNumber());
    assertEquals(new BigInteger("11110000", 2), key1.sub(12).toNumber());
    assertEquals(new BigInteger("1000011110000", 2), key1.sub(13).toNumber());
  }

  public void testSet() throws Exception {
    KeyBase key1 = new KeyBase(new BigInteger("11110000111100001111000011110000", 2));
    //                                         31282724232019161512110807040300

    assertEquals(new BigInteger("11110000111100001111000011110000", 2), key1.set(0, false).toNumber());
    assertEquals(new BigInteger("11110000111100001111000011110001", 2), key1.set(0, true).toNumber());
    assertEquals(new BigInteger("11110000111100001111000011111000", 2), key1.set(3, true).toNumber());
    assertEquals(new BigInteger("11110000111100001111000011100000", 2), key1.set(4, false).toNumber());
    assertEquals(new BigInteger("11110000111100001111000001110000", 2), key1.set(7, false).toNumber());
    assertEquals(new BigInteger("11110000111100001111010011110000", 2), key1.set(10, true).toNumber());
    assertEquals(new BigInteger("11110000111100101111000011110000", 2), key1.set(17, true).toNumber());
    assertEquals(new BigInteger("11110000111100001111000011110000", 2), key1.set(17, false).toNumber());
    assertEquals(new BigInteger("11110000111100001111000011110000", 2), key1.set(31, true).toNumber());
    assertEquals(new BigInteger("11110000111000001111000011110000", 2), key1.set(20, false).toNumber());
    assertEquals(new BigInteger("11100000111100001111000011110000", 2), key1.set(28, false).toNumber());
    assertEquals(new BigInteger("10110000111100001111000011110000", 2), key1.set(30, false).toNumber());
    assertEquals(new BigInteger("01110000111100001111000011110000", 2), key1.set(31, false).toNumber());
  }
}

