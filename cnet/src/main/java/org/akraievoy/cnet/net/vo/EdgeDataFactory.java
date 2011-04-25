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

package org.akraievoy.cnet.net.vo;

public class EdgeDataFactory {
  public static EdgeData sparse(final boolean symmetric) {
    return sparse(symmetric, 0.0);
  }

  public static EdgeData sparse(boolean symmetric, double nullElement) {
    return new EdgeDataSparse(symmetric, nullElement);
  }

  public static EdgeData dense(boolean symmetric) {
    return dense(symmetric, 0.0);
  }

  public static EdgeData dense(boolean symmetric, double nullElement) {
    return dense(symmetric, nullElement, 32);
  }

  public static EdgeData dense(boolean symmetric, double nullElement, int capacity) {
    final int bits = (int) Math.ceil(Math.log(capacity) / Math.log(2.0));
    final EdgeDataDense dense = new EdgeDataDense(symmetric, nullElement, bits);

    return dense;
  }
}
