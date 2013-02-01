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

package org.akraievoy.util;

import junit.framework.TestCase;

public class InterpolateTest extends TestCase {
  public void testLogistic() throws Exception {
    Interpolate.Fun fun = Interpolate.norm(2, 8, 3, 7, Interpolate.LOGISTIC);
    assertEquals(3.000, fun.apply(0), 0.001);
    assertEquals(3.001, fun.apply(1), 0.001);
    assertEquals(3.010, fun.apply(2), 0.001);
    assertEquals(3.072, fun.apply(3), 0.001);
    assertEquals(3.477, fun.apply(4), 0.001);
    assertEquals(5.000, fun.apply(5), 0.001);
    assertEquals(6.523, fun.apply(6), 0.001);
    assertEquals(6.928, fun.apply(7), 0.001);
    assertEquals(6.990, fun.apply(8), 0.001);
    assertEquals(6.999, fun.apply(9), 0.001);
    assertEquals(6.999, fun.apply(10), 0.001);

    fun = Interpolate.norm(8, 2, 3, 7, Interpolate.LOGISTIC);
    assertEquals(6.999, fun.apply(0), 0.001);
    assertEquals(6.999, fun.apply(1), 0.001);
    assertEquals(6.990, fun.apply(2), 0.001);
    assertEquals(6.928, fun.apply(3), 0.001);
    assertEquals(6.523, fun.apply(4), 0.001);
    assertEquals(5.000, fun.apply(5), 0.001);
    assertEquals(3.477, fun.apply(6), 0.001);
    assertEquals(3.072, fun.apply(7), 0.001);
    assertEquals(3.010, fun.apply(8), 0.001);
    assertEquals(3.001, fun.apply(9), 0.001);
    assertEquals(3.000, fun.apply(10), 0.001);

    fun = Interpolate.norm(8, 2, 7, 3, Interpolate.LOGISTIC);
    assertEquals(3.000, fun.apply(0), 0.001);
    assertEquals(3.001, fun.apply(1), 0.001);
    assertEquals(3.010, fun.apply(2), 0.001);
    assertEquals(3.072, fun.apply(3), 0.001);
    assertEquals(3.477, fun.apply(4), 0.001);
    assertEquals(5.000, fun.apply(5), 0.001);
    assertEquals(6.523, fun.apply(6), 0.001);
    assertEquals(6.928, fun.apply(7), 0.001);
    assertEquals(6.990, fun.apply(8), 0.001);
    assertEquals(6.999, fun.apply(9), 0.001);
    assertEquals(6.999, fun.apply(10), 0.001);
  }
}
