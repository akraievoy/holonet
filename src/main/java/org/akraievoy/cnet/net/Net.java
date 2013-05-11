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

package org.akraievoy.cnet.net;

import org.akraievoy.cnet.net.vo.EdgeData;
import org.akraievoy.cnet.net.vo.EdgeDataFactory;

public class Net {
  private static final int POW = 14;
  private static final int MASK = (1 << POW) - 1;

  private Net() { /* sealed */ }

  public static int toId(final int from, final int into) {
    return (from << POW) + into;
  }

  public static int toInto(final int id) {
    return id & MASK;
  }

  public static int toFrom(final int id) {
    return id >> POW;
  }

  public static double[] eMinMax(final EdgeData edgeData) {
    final double[] minMax = {Double.NaN, Double.NaN};

    edgeData.visitNonDef(new EdgeData.EdgeVisitor() {
      @Override
      public void visit(final int from, final int into, final double e) {
        if (Double.isNaN(minMax[0]) || minMax[0] < e) {
          minMax[0] = e;
        }
        if (Double.isNaN(minMax[1]) || minMax[1] > e) {
          minMax[1] = e;
        }
      }
    });

    if (Double.isNaN(minMax[0]) || Double.isNaN(minMax[1])) {
      throw new IllegalStateException("edgeData has no non-default elements?");
    }

    return minMax;
  }

  public static EdgeData eClone(EdgeData eData) {
    final EdgeData eClone = eData.proto(eData.getSize());
    eData.visitNonDef(
        new EdgeData.EdgeVisitor() {
          @Override
          public void visit(int from, int into, double e) {
            eClone.set(from, into, e);
          }
        }
    );
    return eClone;
  }

  public static EdgeData eSparseSymSum(EdgeData eData) {
    //  LATER allow flipping symmetric flag in EdgeData.proto()
    final EdgeData reqClone = EdgeDataFactory.sparse(true, 0, eData.getSize());
    eData.visitNonDef(
        new EdgeData.EdgeVisitor() {
          @Override
          public void visit(int from, int into, double e) {
            reqClone.set(from, into, reqClone.get(from, into) + e);
          }
        }
    );
    return reqClone;
  }
}
