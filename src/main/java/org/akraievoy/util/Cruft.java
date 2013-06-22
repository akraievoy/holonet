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

/**
 * Pieces that JRE should have provided 15 years ago.
 * <p/>
 * <code>(crap+stuff+sh*t)/3 = cruft</code>
 */
public class Cruft {
  private Cruft() { /* sealed */ }

  public static boolean nonNegative(int value, final String negativeMessage) {
    if (value >= 0) {
      return true;
    }
    throw new IllegalStateException(negativeMessage);
  }

  public static int min(final int[] values) {
    if (values == null) {
      throw new IllegalArgumentException("values == null");
    }

    final int valueNum = values.length;
    if (valueNum == 0) {
      throw new IllegalArgumentException("values.length == 0");
    }

    int min = values[0];

    for (int i = 1; i < valueNum; i++) {
      final int value = values[i];
      if (value < min) {
        min = value;
      }
    }

    return min;
  }

  public static int[] min2(final int[] values) {
    if (values == null) {
      throw new IllegalArgumentException("values == null");
    }

    final int valueNum = values.length;
    if (valueNum < 2) {
      throw new IllegalArgumentException("values.length < 2");
    }

    int[] min =
        values[0] <= values[1] ?
            new int[] { values[0], values[1] } :
            new int[] { values[1], values[0] };

    for (int i = 2; i < valueNum; i++) {
      final int value = values[i];
      if (value <= min[0]) {
        min[1] = min[0];
        min[0] = value;
      } else if (value < min[1]) {
        min[1] = value;
      }
    }

    return min;
  }

  public static int intCompare(int a, int b) {
    return a < b ? -1 : (a == b ? 0 : 1);
  }

  public static interface Fun0<V> {
    public V apply();
  }

  public static interface Fun1<A0, V> {
    public V apply(A0 a);
  }
}
