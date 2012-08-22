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
import org.akraievoy.base.Die;
import org.akraievoy.gear.G4Trove;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonPropertyOrder;

import java.util.Arrays;

@JsonPropertyOrder({"bits", "size", "nonDefCount", "defElem", "symmetric", "edges"})
public class EdgeDataDense implements EdgeData {
  protected double defElem;

  protected final TDoubleArrayList edges;
  protected int size = 0;
  protected int bits;

  protected boolean symmetric;

  @Deprecated
  public EdgeDataDense() {
    this(true, 0.0, 6);
  }

  protected EdgeDataDense(boolean symmetric, double defElem, final int bits) {
    this.edges = new TDoubleArrayList();

    this.defElem = defElem;
    this.symmetric = symmetric;

    this.bits = bits;

    int capacity = computeCapacity();
    this.edges.fill(0, capacity * capacity, defElem);
  }

  protected int computeCapacity() {
    return (int) Math.pow(2, this.bits);
  }

  public boolean isSymmetric() {
    return symmetric;
  }

  @Deprecated
  @SuppressWarnings({"UnusedDeclaration"})
  public void setSymmetric(boolean symmetric) {
    this.symmetric = symmetric;
  }

  @SuppressWarnings({"UnusedDeclaration"})
  @Deprecated
  public int getBits() {
    return bits;
  }

  @SuppressWarnings({"UnusedDeclaration"})
  @Deprecated
  public void setBits(int bits) {
    this.bits = bits;
  }

  @SuppressWarnings({"UnusedDeclaration"})
  @Deprecated
  public byte[] getEdges() {
    return G4Trove.doublesToBinary(edges);
  }

  @SuppressWarnings({"UnusedDeclaration"})
  @Deprecated
  public void setEdges(byte[] edgesBinary) {
    G4Trove.binaryToDoubles(edgesBinary, edges);
  }

  public boolean isDef(double elem) {
    return Double.compare(elem, defElem) == 0;
  }

  public double weight(double elem) {
    return elem;
  }

  public double getDefElem() {
    return defElem;
  }

  @SuppressWarnings({"UnusedDeclaration"})
  @Deprecated
  public void setDefElem(double defElem) {
    this.defElem = defElem;
  }

  public EdgeData proto(final int protoSize) {
    return EdgeDataFactory.dense(isSymmetric(), defElem, protoSize);
  }

  public double get(int from, int into) {
    ensureCapacity(Math.max(from, into));

    final int index;
    if (symmetric && from > into) {
      index = getIndex(into, from);
    } else {
      index = getIndex(from, into);
    }

    return edges.get(index);
  }

  protected boolean ensureCapacity(int vertex) {
    if (size > vertex) {
      return false;
    }

    size = vertex + 1;

    int capacity;
    while ((capacity = computeCapacity()) < size) {
      double[] filler = new double[capacity];
      Arrays.fill(filler, defElem);

      for (int from = 0; from < capacity; from++) {
        edges.insert((2 * from + 1) * capacity, filler);
      }

      edges.fill(2 * capacity * capacity, 4 * capacity * capacity, defElem);

      bits += 1;
    }

    return true;
  }

  protected int getIndex(int from, int into) {
    return (from << bits) + into;
  }

  public double set(int from, int into, double elem) {
    ensureCapacity(Math.max(from, into));

    final int index;
    if (isSymmetric() && from > into) {
      index = getIndex(into, from);
    } else {
      index = getIndex(from, into);
    }

    final double prevElem = edges.get(index);

    edges.set(index, elem);

    return prevElem;
  }

  @Deprecated
  @SuppressWarnings({"UnusedDeclaration"})
  public void setSizeJson(int size) {
    this.size = size;
  }

  @Deprecated
  @SuppressWarnings({"UnusedDeclaration"})
  public int getSizeJson() {
    return this.size;
  }

  @JsonIgnore
  public int getSize() {
    return size;
  }

  public boolean conn(int from, int into) {
    return !isDef(from, into);
  }

  public TIntArrayList connVertexes(int index) {
    return connVertexes(index, new TIntArrayList());
  }

  public TIntArrayList connVertexes(final int index, final TIntArrayList result) {
    for (int jndex = 0; jndex < size; jndex++) {
      if (conn(index, jndex)) {
        result.add(jndex);
      } else if (conn(jndex, index)) {
        result.add(jndex);
      }
    }

    return result;
  }

  public double weight(int from, int into) {
    return weight(get(from, into));
  }

  public boolean isDef(int from, int into) {
    return isDef(get(from, into));
  }

  public double power(final int index) {
    double result = 0;
    for (int jndex = 0; jndex < size; jndex++) {
      final double ijElem = get(index, jndex);
      if (!isDef(ijElem)) {
        result += ijElem;
      } else {
        final double jiElem = get(jndex, index);
        if (!isDef(jiElem)) {
          result += jiElem;
        }
      }
    }

    return result;
  }

  public double weight(Route route, final double emptyWeight) {
    return weight(route.getIndexes(), emptyWeight);
  }

  public double weight(TIntArrayList indexes, double emptyWeight) {
    if (indexes.isEmpty()) {
      return emptyWeight;
    }
    Die.ifTrue("indexes.size == 1", indexes.size() == 1);

    double weight = 0;
    for (int i = 0; i < indexes.size() - 1; i++) {
      weight += weight(indexes.get(i), indexes.get(i + 1));
    }

    return weight;
  }

  @JsonIgnore
  public int getNonDefCount() {
    int nonDefCount = 0;
    for (int from = 0; from < size; from++) {
      for (int into = 0; into < size; into++) {
        if (!isDef(from, into)) {
          nonDefCount += 1;
        }
      }
    }
    return nonDefCount;
  }

  public void visitNonDef(EdgeVisitor visitor) {
    for (int from = 0; from < size; from++) {
      for (int into = 0; into < size; into++) {
        final double value = get(from, into);
        if (!isDef(value)) {
          visitor.visit(from, into, value);
        }
      }
    }
  }

  public double total() {
    double totalConnectivity = 0;
    for (int from = 0; from < size; from++) {
      for (int into = 0; into < size; into++) {
        totalConnectivity += get(from, into);
      }
    }

    return totalConnectivity;
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof EdgeData)) return false;

    final EdgeDataDense edgeData = (EdgeDataDense) o;

    if (Double.compare(edgeData.defElem, defElem) != 0) return false;
    if (symmetric != edgeData.symmetric) return false;

    for (int from = 0; from < size; from++) {
      for (int into = 0; into < size; into++) {
        final double valueThis = get(from, into);
        final double valueThat = edgeData.get(from, into);
        if (valueThat != valueThis) {
          return false;
        }
      }
    }

    return true;
  }

  public int hashCode() {
    int result;
    long temp;
    temp = defElem != +0.0d ? Double.doubleToLongBits(defElem) : 0L;
    result = (int) (temp ^ (temp >>> 32));
    result = 31 * result + (symmetric ? 1 : 0);
    result = 13 * result + (getNonDefCount());

    return result;
  }

  public double similarity(EdgeData that) {
    int thisNonDef = 0;
    int thatNonDef = 0;
    int similar = 0;
    for (int from = 0; from < size; from++) {
      for (int into = 0; into < size; into++) {
        final double valueThis = get(from, into);
        final double valueThat = that.get(from, into);

        final boolean isDefThis = isDef(valueThis);
        final boolean isDefThat = that.isDef(valueThat);
        if (isDefThis && isDefThat) {
          continue;
        }

        if (!isDefThat) {
          thatNonDef++;
        }

        if (!isDefThis) {
          thisNonDef++;
        }

        if (!isDefThis && !isDefThat) {
          similar++;
        }
      }
    }

    return similar / (double) Math.max(thisNonDef, thatNonDef);
  }

  public void clear() {
    final int capacity = computeCapacity();
    this.edges.fill(0, capacity * capacity, defElem);
  }

  public String toString() {
    final int size = getSize();

    return "EdgeDataDense[" + size + "]";
  }

}