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

package org.akraievoy.cnet.gen.domain;

import junit.framework.TestCase;

public class StructuralNetFactoryTest extends TestCase {
  public void testIsPrime() {
    assertTrue(MetricEDataGenStructural.isPrime(1));
    assertTrue(MetricEDataGenStructural.isPrime(2));
    assertTrue(MetricEDataGenStructural.isPrime(3));
    assertFalse(MetricEDataGenStructural.isPrime(4));
    assertTrue(MetricEDataGenStructural.isPrime(5));
    assertFalse(MetricEDataGenStructural.isPrime(6));
    assertTrue(MetricEDataGenStructural.isPrime(7));
    assertFalse(MetricEDataGenStructural.isPrime(8));
    assertFalse(MetricEDataGenStructural.isPrime(9));
    assertFalse(MetricEDataGenStructural.isPrime(10));
    assertTrue(MetricEDataGenStructural.isPrime(11));
    assertFalse(MetricEDataGenStructural.isPrime(12));
    assertTrue(MetricEDataGenStructural.isPrime(13));
    assertFalse(MetricEDataGenStructural.isPrime(14));
    assertFalse(MetricEDataGenStructural.isPrime(15));
    assertFalse(MetricEDataGenStructural.isPrime(16));
    assertTrue(MetricEDataGenStructural.isPrime(17));
    assertFalse(MetricEDataGenStructural.isPrime(18));
    assertTrue(MetricEDataGenStructural.isPrime(19));
    assertFalse(MetricEDataGenStructural.isPrime(20));
    assertFalse(MetricEDataGenStructural.isPrime(21));
    assertFalse(MetricEDataGenStructural.isPrime(22));
    assertTrue(MetricEDataGenStructural.isPrime(23));
    assertFalse(MetricEDataGenStructural.isPrime(24));
    assertFalse(MetricEDataGenStructural.isPrime(25));
    assertFalse(MetricEDataGenStructural.isPrime(26));
    assertFalse(MetricEDataGenStructural.isPrime(27));
    assertFalse(MetricEDataGenStructural.isPrime(28));
    assertTrue(MetricEDataGenStructural.isPrime(29));
    assertFalse(MetricEDataGenStructural.isPrime(30));
    assertTrue(MetricEDataGenStructural.isPrime(31));
    assertFalse(MetricEDataGenStructural.isPrime(32));
    assertFalse(MetricEDataGenStructural.isPrime(33));
    assertFalse(MetricEDataGenStructural.isPrime(34));
    assertFalse(MetricEDataGenStructural.isPrime(35));
  }
}
