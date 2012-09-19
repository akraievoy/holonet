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

import gnu.trove.TIntArrayList;
import org.akraievoy.base.soft.Soft;
import org.akraievoy.db.Streamable;
import org.codehaus.jackson.annotate.JsonIgnore;

/**
 * Sparse storage scheme (here we try to keep data closely packed and
 *  linearized to avoid fragmentation and extra GC overheads):
 *   leadLen: int[nodes][2] -> lead/len for each row
 *   intoIdx: int[2*nodes*avgOrder] -> column indexes
 *   storage: 2*nodes*avgOrder*slot -> handcrafted packed linear storage
 *      bit, byte, int, float, long, double
 * Total memory consumption o-function (in bytes) looks like so:
 *   8*nodes*(1 + avgOrder * (1 + slot / 4) )
 *
 * Dense storage scheme:
 *   storage: nodes*nodes*slotSize
 * Total memory consumption o-function (in bytes) looks like so:
 *   nodes * nodes * slot
 *
 * So to decide which storage is more efficient in runtime:
 *   nodes * nodes * slot > 8 * nodes * ( 1 + avgOrger * (1 + slot) / 4 )
 *   nodes * slot > 8 + 2 * avgOrder * ( 1 + slot )
 *   2 * avgOrder * (1 + slot) < nodes * slot - 8
 *
 * So, storage equilibrium is at
 *   avgOrder < ( nodes * slot / 2 - 4 ) / ( 1 + slot )
 *
 * Which effectively boils down to these statements:
 *   doubles should be stores sparsely if avgOrder < ( nodes * 4 - 4 ) / 9
 *     which is 454.6 links per node for 1024-node net, or 113 links for 256 nodes
 *   floats should be stores sparsely if avgOrder < ( nodes * 2 - 4 ) / 5
 *     which is 408 links for 1024 nodes, or 101 links for 256 nodes
 *   bytes should be stores sparsely if avgOrder < ( nodes / 2 - 4 ) / 2
 *     which is 254 links for 1024 nodes, or 62 links for 256 nodes
 *   bits should be stores sparsely if avgOrder < (nodes / 16 - 4) / ( 9/8 )
 *     which is 53.3 links for 1024 nodes , or 10.6 links for 256 nodes
 */
//  TODO tighten the exposed API area
//  FIXME most efficient serialization scheme <<< active
//  TODO compactify hook (compactify just before streaming down to DB?)
//  TODO routes now may be optimized
//  TODO wipe equals
public interface EdgeData extends Streamable {
  class Util {
    public static String dump(EdgeData data) {
      final StringBuilder res = new StringBuilder();

      for (int i = 0; i < data.getSize(); i++) {
        for (int j = 0; j < data.getSize(); j++) {
          double val = 0;
          final double linkVal = data.get(i, j);
          while (val < linkVal) {
            res.append("#");
            val += 1;
          }
          while (val < 1) {
            res.append("_");
            val += 1;
          }
          res.append(":");
        }
        res.append("\n");
      }
      return res.toString();
    }

    public static boolean eq(IteratorTuple t1, IteratorTuple t2) {
      if (t1.from() != t2.from()) {
        return false;
      }

      if (t1.into() != t2.into()) {
        return false;
      }

      final double t1value = t1.value();
      final double t2value = t2.value();
      return (
          Double.isNaN(t1value) && Double.isNaN(t2value) ||
              Soft.PICO.equal(t1value, t2value)
      );
    }
  }

  public int getSize();

  boolean isSymmetric();

  boolean isDef(double elem);

  double weight(double elem);

  double getDefElem();

  EdgeData proto(final int protoSize);

  double get(int from, int into);

  double set(int from, int into, double elem);

  //	TODO :: rename/clone to isConnected()
  boolean conn(int from, int into);

  TIntArrayList connVertexes(int index);

  TIntArrayList connVertexes(int index, TIntArrayList result);

  double weight(int from, int into);

  boolean isDef(int from, int into);

  double power(int index);

  double weight(Route route, double emptyWeight);

  double weight(TIntArrayList indexes, double emptyWeight);

  @JsonIgnore
  int getNonDefCount();

  void visitNonDef(EdgeVisitor visitor);

  ElemIterator nonDefIterator();

  double total();

  double similarity(EdgeData that);

  void clear();

  public static interface EdgeVisitor {
    void visit(int from, int into, double e);
  }

  public static interface IteratorTuple {
    int from();
    int into();
    double value();
  }

  public static interface ElemIterator {
    boolean hasNext();
    IteratorTuple next();
  }

}