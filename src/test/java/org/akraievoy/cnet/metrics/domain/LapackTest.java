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

package org.akraievoy.cnet.metrics.domain;

import junit.framework.TestCase;
import org.akraievoy.base.Die;
import org.netlib.lapack.LAPACK;
import org.netlib.util.intW;

public class LapackTest extends TestCase {

  private LAPACK lapackInstance;

  public void setUp() throws Exception {
    super.setUp();
    lapackInstance = LAPACK.getInstance();
  }

  public double[] getSpectre(int N, final double[] matrix) {
    Die.ifFalse("matrix.length >= N*N", matrix.length >= N * N);

    final double[] result = new double[N];
    final double[] work = new double[3 * N];
    intW status = new intW(0);

    lapackInstance.dsyev(
        "N", "U", N, matrix, N,
        result, work, 3 * N, status
    );

    return result;
  }

  public void testGetSpectre() throws Exception {
    final double[] spectre = getSpectre(
        5,
        new double[]{
            1, -0.25, -0.25, -0.25, -0.25,
            -0.25, 1, -0.25, -0.25, -0.25,
            -0.25, -0.25, 1, -0.25, -0.25,
            -0.25, -0.25, -0.25, 1, -0.25,
            -0.25, -0.25, -0.25, -0.25, 1
        }
    );

    assertEquals(0.0, spectre[0]);
    assertEquals(1.25, spectre[1]);
    assertEquals(1.25, spectre[2]);
    assertEquals(1.25, spectre[3]);
    assertEquals(1.25, spectre[4]);
  }
}
