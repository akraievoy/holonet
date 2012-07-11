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
import org.codehaus.jackson.annotate.JsonIgnore;

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

  public static EdgeData constant(final int size, final double value) {
    return new EdgeDataConstant(size, value);
  }

  public static EdgeData dense(boolean symmetric, double nullElement, int capacity) {
    final int bits = (int) Math.ceil(Math.log(capacity) / Math.log(2.0));
    final EdgeDataDense dense = new EdgeDataDense(symmetric, nullElement, bits);

    return dense;
  }

  /** FIXME the whole mess of EdgeDatas/VertexDatas begs for some hardcore optimization */
  public static class EdgeDataConstant implements EdgeData {
    private double value;
    private int size;

    public EdgeDataConstant() {
      this(0, 0);
    }

    public EdgeDataConstant(int size, double value) {
      this.value = value;
      this.size = size;
    }

    public double getValue() {
      return value;
    }

    public void setValue(double value) {
      this.value = value;
    }

    @JsonIgnore
    public boolean isSymmetric() {
      return true;
    }

    public boolean isNull(double elem) {
      return elem == getNullElement();
    }

    public double weight(double elem) {
      return elem;
    }

    @JsonIgnore
    public double getNullElement() {
      return 0.0;
    }

    public EdgeData proto() {
      throw new UnsupportedOperationException("constant EdgeData");
    }

    public double get(int from, int into) {
      return value;
    }

    public double set(int from, int into, double elem) {
      throw new UnsupportedOperationException("constant EdgeData");
    }

    public boolean conn(int from, int into) {
      return !isNull(value);
    }

    public TIntArrayList connVertexes(int index) {
      return connVertexes(index, new TIntArrayList());
    }

    public TIntArrayList connVertexes(int index, TIntArrayList result) {
      result.clear();
      if (isNull(value)) {
        return result;
      }
      for (int i = 0; i < size; i++) {
        result.add(i);
      }
      return result;
    }

    public double weight(int from, int into) {
      return value;
    }

    public boolean isNull(int from, int into) {
      return isNull(weight(from, into));
    }

    public double power(int index) {
      return size * value;
    }

    public double weight(Route route, double emptyWeight) {
      throw new UnsupportedOperationException("not implemented yet");
    }

    public double weight(TIntArrayList indexes, double emptyWeight) {
      throw new UnsupportedOperationException("not implemented yet");
    }

    @JsonIgnore
    public int getNotNullCount() {
      if (isNull(value)) {
        return 0;
      }
      return size * size;
    }

    public void visitNotNull(EdgeVisitor visitor) {

      for (int from = 0; from < size; from++) {
        for (int into = 0; into < size; into++) {
          visitor.visit(from, into, value);
        }
      }
    }

    public double total() {
      return value * size * size;
    }

    public double similarity(EdgeData that) {
      throw new UnsupportedOperationException("constant EdgeData");
    }

    public void clear() {
      throw new UnsupportedOperationException("constant EdgeData");
    }

    public void setSize(int size) {
      this.size = size;
    }

    public int getSize() {
      return size;
    }

    public void insert(int index) {
      throw new UnsupportedOperationException("constant EdgeData");
    }

    public void remove(int index) {
      throw new UnsupportedOperationException("constant EdgeData");
    }
  }
}
