/*
 Copyright 2013 Anton Kraievoy akraievoy@gmail.com
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

package algores.holonet.core;

import junit.framework.TestCase;

import java.math.BigInteger;

public class EnvCNetTest extends TestCase {
  public void testGenerateKey() {
    assertEquals(
        "fcafe7",
        EnvCNet.generateKey(256, 0 ,new BigInteger("fffcafe7", 16)).toNumber().toString(16)
    );
    assertEquals(
        "4fcafe7",
        EnvCNet.generateKey(256, 4 ,new BigInteger("fffcafe7", 16)).toNumber().toString(16)
    );
    assertEquals(
        "4fcafe7",
        EnvCNet.generateKey(256, 4 ,new BigInteger("00fcafe7", 16)).toNumber().toString(16)
    );
    assertEquals(
        "fffcafe7",
        EnvCNet.generateKey(256, 255 ,new BigInteger("00fcafe7", 16)).toNumber().toString(16)
    );
  }
}
