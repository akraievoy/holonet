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

@JsonPropertyOrder({"bits", "size", "notNullCount", "nullElement", "symmetric", "edges"})
public class EdgeDataDense implements EdgeData {
  protected double nullElement;

  protected final TDoubleArrayList edges;
  protected int size = 0;
  protected int bits;

  protected boolean symmetric;

  @Deprecated
  public EdgeDataDense() {
    this(true, 0.0, 6);
  }

  protected EdgeDataDense(boolean symmetric, double nullElement, final int bits) {
    this.edges = new TDoubleArrayList();

    this.nullElement = nullElement;
    this.symmetric = symmetric;

    this.bits = bits;

    int capacity = computeCapacity();
    this.edges.fill(0, capacity * capacity, nullElement);
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

  public boolean isNull(double elem) {
    return Double.compare(elem, nullElement) == 0;
  }

  public double weight(double elem) {
    return elem;
  }

  public double getNullElement() {
    return nullElement;
  }

  @SuppressWarnings({"UnusedDeclaration"})
  @Deprecated
  public void setNullElement(double nullElement) {
    this.nullElement = nullElement;
  }

  public EdgeData proto() {
    return EdgeDataFactory.dense(isSymmetric(), nullElement, size);
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
      Arrays.fill(filler, nullElement);

      for (int from = 0; from < capacity; from++) {
        edges.insert((2 * from + 1) * capacity, filler);
      }

      edges.fill(2 * capacity * capacity, 4 * capacity * capacity, nullElement);

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
  public void setSize(int size) {
    ensureCapacity(size - 1);

    this.size = size;
  }

  @JsonIgnore
  public int getSize() {
    return size;
  }

  public void remove(int index) {
    Die.ifFalse("index < size", index < size);
    Die.ifFalse("size > 0", size > 0);

    final double[] edges = G4Trove.elements(this.edges);
    final int capacity = computeCapacity();
    for (int i = 0; i < size - 1; i++) {
      final int row = capacity * i;
      if (i < index) {
        System.arraycopy(edges, row + index + 1, edges, row + index, size - index - 1);
        edges[row + size - 1] = nullElement;
      } else {
        System.arraycopy(edges, row + capacity, edges, row, index);
        System.arraycopy(edges, row + capacity + index + 1, edges, row + index, size - index - 1);
        edges[row + size - 1] = nullElement;
      }
    }
    Arrays.fill(edges, (size - 1) * capacity, size * capacity, nullElement);

    size--;
  }

  public void insert(final int index) {
    if (index >= size) {
      ensureCapacity(index);
      return;
    }

    ensureCapacity(size);

    final int capacity = computeCapacity();
    final double[] edges = G4Trove.elements(this.edges);
    for (int i = size - 1; i >= 0; i--) {
      final int row = capacity * i;

      if (i > index) {
        System.arraycopy(edges, row - capacity, edges, row, index);
        System.arraycopy(edges, row - capacity + index, edges, row + index + 1, size - index - 1);
        edges[row + index] = nullElement;
      } else if (i == index) {
        Arrays.fill(edges, row, row + capacity, nullElement);
      } else {
        System.arraycopy(edges, row + index, edges, row + index + 1, size - index - 1);
        edges[row + index] = nullElement;
      }
    }
  }

  public boolean conn(int from, int into) {
    return !isNull(from, into);
  }

  public TIntArrayList outVertexes(int from) {
    return outVertexes(from, new TIntArrayList());
  }

  public TIntArrayList outVertexes(final int from, final TIntArrayList result) {
    if (isSymmetric()) {
      return connVertexes(from, result);
    }

    for (int into = 0; into < size; into++) {
      if (conn(from, into)) {
        result.add(into);
      }
    }

    return result;
  }

  public TIntArrayList inVertexes(int into) {
    return inVertexes(into, new TIntArrayList());
  }

  public TIntArrayList inVertexes(final int into, final TIntArrayList result) {
    if (isSymmetric()) {
      return connVertexes(into, result);
    }

    for (int from = 0; from < size; from++) {
      if (conn(from, into)) {
        result.add(from);
      }
    }

    return result;
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

  public TDoubleArrayList outElements(int from) {
    return outElements(from, new TDoubleArrayList());
  }

  public TDoubleArrayList outElements(final int from, final TDoubleArrayList result) {
    if (isSymmetric()) {
      return connElements(from, result);
    }

    for (int into = 0; into < size; into++) {
      final double elem = get(from, into);
      if (!isNull(elem)) {
        result.add(elem);
      }
    }

    return result;
  }

  public TDoubleArrayList inElements(int into) {
    return inElements(into, new TDoubleArrayList());
  }

  public TDoubleArrayList inElements(final int into, final TDoubleArrayList result) {
    if (isSymmetric()) {
      return connElements(into, result);
    }

    for (int from = 0; from < size; from++) {
      final double elem = get(from, into);
      if (!isNull(elem)) {
        result.add(elem);
      }
    }

    return result;
  }

  public TDoubleArrayList connElements(int index) {
    return connElements(index, new TDoubleArrayList());
  }

  public TDoubleArrayList connElements(int index, final TDoubleArrayList result) {
    for (int jndex = 0; jndex < size; jndex++) {
      final double ijElem = get(index, jndex);
      if (!isNull(ijElem)) {
        result.add(ijElem);
      } else {
        final double jiElem = get(jndex, index);
        if (!isNull(jiElem)) {
          result.add(jiElem);
        }
      }
    }

    return result;
  }

  public double weight(int from, int into) {
    return weight(get(from, into));
  }

  public boolean isNull(int from, int into) {
    return isNull(get(from, into));
  }

  public double power(final int index) {
    double result = 0;
    for (int jndex = 0; jndex < size; jndex++) {
      final double ijElem = get(index, jndex);
      if (!isNull(ijElem)) {
        result += ijElem;
      } else {
        final double jiElem = get(jndex, index);
        if (!isNull(jiElem)) {
          result += jiElem;
        }
      }
    }

    return result;
  }

  public double powerOut(final int from) {
    if (isSymmetric()) {
      return power(from);
    }

    double result = 0;
    for (int into = 0; into < size; into++) {
      final double elem = get(from, into);
      if (!isNull(elem)) {
        result += elem;
      }
    }

    return result;
  }

  public double powerIn(final int into) {
    if (isSymmetric()) {
      return power(into);
    }

    double result = 0;
    for (int from = 0; from < size; from++) {
      final double elem = get(from, into);
      if (!isNull(elem)) {
        result += elem;
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

  public double diameter(final int actualSize, final boolean refrective) {
    double diameter = 0;

    for (int from = 0; diameter < Double.POSITIVE_INFINITY && from < actualSize; from++) {
      for (int into = isSymmetric() ? from : 0; diameter < Double.POSITIVE_INFINITY && into < actualSize; into++) {
        if (!refrective && from == into) {
          continue;
        }

        diameter = Math.max(diameter, weight(from, into));
      }
    }

    return diameter;
  }

  @JsonIgnore
  public int getNotNullCount() {
    int notNullCount = 0;
    for (int from = 0; from < size; from++) {
      for (int into = 0; into < size; into++) {
        if (!isNull(from, into)) {
          notNullCount += 1;
        }
      }
    }
    return notNullCount;
  }

  public void visitNotNull(EdgeVisitor visitor) {
    for (int from = 0; from < size; from++) {
      for (int into = 0; into < size; into++) {
        final double value = get(from, into);
        if (!isNull(value)) {
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

    if (Double.compare(edgeData.nullElement, nullElement) != 0) return false;
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
    temp = nullElement != +0.0d ? Double.doubleToLongBits(nullElement) : 0L;
    result = (int) (temp ^ (temp >>> 32));
    result = 31 * result + (symmetric ? 1 : 0);
    result = 13 * result + (getNotNullCount());

    return result;
  }

  public double similarity(EdgeData that) {
    int thisNotNull = 0;
    int thatNotNull = 0;
    int similar = 0;
    for (int from = 0; from < size; from++) {
      for (int into = 0; into < size; into++) {
        final double valueThis = get(from, into);
        final double valueThat = that.get(from, into);

        final boolean isNullThis = isNull(valueThis);
        final boolean isNullThat = that.isNull(valueThat);
        if (isNullThis && isNullThat) {
          continue;
        }

        if (!isNullThat) {
          thatNotNull++;
        }

        if (!isNullThis) {
          thisNotNull++;
        }

        if (!isNullThis && !isNullThat) {
          similar++;
        }
      }
    }

    return similar / (double) Math.max(thisNotNull, thatNotNull);
  }

  public void clear() {
    final int capacity = computeCapacity();
    this.edges.fill(0, capacity * capacity, nullElement);
  }

  public String toString() {
    final int size = getSize();

    return "EdgeDataDense[" + size + "]";
  }

}