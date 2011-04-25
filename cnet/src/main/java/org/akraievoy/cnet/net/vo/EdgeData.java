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

import gnu.trove.TDoubleArrayList;
import gnu.trove.TIntArrayList;
import org.codehaus.jackson.annotate.JsonIgnore;

public interface EdgeData extends Resizable {
  class Util {
    public static String dump(EdgeData data) {
      final StringBuilder res = new StringBuilder();

      for (int i = 0; i < data.getSize(); i++) {
        for (int j = 0; j < data.getSize(); j++) {
          double val = 0;
          final double linkVal = data.get(i, j);
          while (val < linkVal) {
            res.append("#");
            val += 0.1;
          }
          while (val < 1) {
            res.append(" ");
            val += 0.1;
          }
          res.append("\t");
        }
        res.append("\n");
      }
      return res.toString();
    }
  }

  boolean isSymmetric();

  boolean isNull(double elem);

  double weight(double elem);

  double getNullElement();

  EdgeData proto();

  double get(int from, int into);

  double set(int from, int into, double elem);

  //	TODO :: rename/clone to isConnected()
  boolean conn(int from, int into);

  TIntArrayList outVertexes(int from);

  TIntArrayList outVertexes(int from, TIntArrayList result);

  TIntArrayList inVertexes(int into);

  TIntArrayList inVertexes(int into, TIntArrayList result);

  TIntArrayList connVertexes(int index);

  TIntArrayList connVertexes(int index, TIntArrayList result);

  TDoubleArrayList outElements(int from);

  TDoubleArrayList outElements(int from, TDoubleArrayList result);

  TDoubleArrayList inElements(int into);

  TDoubleArrayList inElements(int into, TDoubleArrayList result);

  TDoubleArrayList connElements(int index);

  TDoubleArrayList connElements(int index, TDoubleArrayList result);

  double weight(int from, int into);

  boolean isNull(int from, int into);

  double power(int index);

  double powerOut(int index);

  double powerIn(int index);

  double weight(Route route, double emptyWeight);

  double weight(TIntArrayList indexes, double emptyWeight);

  double diameter(int actualSize, boolean refrective);

  @JsonIgnore
  int getNotNullCount();

  void visitNotNull(EdgeVisitor visitor);

  double similarity(EdgeData that);

  void clear();

  public static interface EdgeVisitor {
    void visit(int from, int into, double e);
  }
}
