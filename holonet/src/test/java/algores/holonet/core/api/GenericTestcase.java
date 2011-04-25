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
import java.util.BitSet;

public class GenericTestcase extends TestCase {
  public void testBitSet2BigInt() {
    final BitSet test = new BitSet();
    test.set(0);
    test.set(3);
    test.set(4);
    test.set(5);
    test.set(7);
    test.set(9);
    test.set(11);

    assertEquals(new BigInteger("AB9", 16), Generic.bitSet2BigInt(test));
  }
}
