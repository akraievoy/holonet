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

import java.util.ArrayList;
import java.util.List;

/**
 * Assymetric case:
 *   store all from->into mappings
 * Symmetric case:
 *   store only from->into mappings where from <= into
 */
@JsonPropertyOrder({"nullElement", "symmetric", "fiEdges"})
public class EdgeDataSparse implements EdgeData {
  protected double nullElement;

  protected Edges fiEdges;

  protected boolean symmetric;

  @Deprecated
  @SuppressWarnings("UnusedDeclaration")
  public EdgeDataSparse() {
    this(true, 0.0, 0);
  }

  protected EdgeDataSparse(boolean symmetric, double nullElement, final int size) {
    this.fiEdges = new Edges(size);

    this.nullElement = nullElement;
    this.symmetric = symmetric;
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
  public Edges getFiEdges() {
    return fiEdges;
  }

  @SuppressWarnings({"UnusedDeclaration"})
  @Deprecated
  public void setFiEdges(Edges fiEdges) {
    this.fiEdges = fiEdges;
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

  public EdgeData proto(final int protoSize) {
    return EdgeDataFactory.sparse(isSymmetric(), nullElement, protoSize);
  }

  public double get(int from, int into) {
    if (from >= fiEdges.getSize()) {
      throw new IllegalArgumentException(
          "from(" + from + ") >= size(" + fiEdges.getSize() + ")"
      );
    }
    if (into >= fiEdges.getSize()) {
      throw new IllegalArgumentException(
        "into(" + into + ") >= size(" + fiEdges.getSize() + ")"
      );
    }

    if (isSymmetric() && from > into) {
      return fiEdges.get(into, from, this);
    }

    return fiEdges.get(from, into, this);
  }

  public double set(int from, int into, double elem) {
    if (from >= fiEdges.getSize()) {
      throw new IllegalArgumentException(
          "from(" + from + ") >= size(" + fiEdges.getSize() + ")"
      );
    }
    if (into >= fiEdges.getSize()) {
      throw new IllegalArgumentException(
        "into(" + into + ") >= size(" + fiEdges.getSize() + ")"
      );
    }

    if (isSymmetric() && from > into) {
      return fiEdges.set(into, from, elem, this);
    }

    return fiEdges.set(from, into, elem, this);
  }

  @JsonIgnore
  public int getSize() {
    if (fiEdges.isEmpty()) {
      return 0;
    }

    return fiEdges.getSize();
  }

  public boolean conn(int from, int into) {
    return !isNull(from, into);
  }

  public TIntArrayList connVertexes(int index) {
    return connVertexes(index, new TIntArrayList());
  }

  public TIntArrayList connVertexes(int index, final TIntArrayList result) {
    fiEdges.vertexes(index, result, false);
    if (symmetric) {
      for (int from = 0; from < index; from++) {
        if (conn(from, index)) {
          result.add(from);
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
    double power = fiEdges.power(index, false, this);
    if (symmetric) {
      for (int from = 0; from < index; from++) {
        power += weight(get(from, index));
      }
    }
    return power;
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
  public int getNotNullCount() {
    return fiEdges.getNotNullCount();
  }

  public void visitNotNull(EdgeVisitor visitor) {
    fiEdges.visit(visitor);
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof EdgeData)) return false;

    final EdgeDataSparse edgeData = (EdgeDataSparse) o;

    if (Double.compare(edgeData.nullElement, nullElement) != 0) return false;
    if (symmetric != edgeData.symmetric) return false;

    final int linkCount = edgeData.getNotNullCount();
    if (getNotNullCount() != linkCount) {
      return false;
    }

    //noinspection SimplifiableIfStatement
    if (edgeData.getSize() != edgeData.getSize()) {
      return false;
    }

    return fiEdges.index.equals(edgeData.fiEdges.index) && fiEdges.elems.equals(edgeData.fiEdges.elems);
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

  //	LATER add a generic similarity measure
  public double similarity(EdgeData that) {
    final List<TIntArrayList> thisIndex = fiEdges.index;
    final List<TIntArrayList> thatIndex = ((EdgeDataSparse) that).fiEdges.index;


    double sameLinkCount = 0.0;
    for (int lead = 0, maxLead = Math.min(thisIndex.size(), thatIndex.size()); lead < maxLead; lead++) {
      final TIntArrayList thisRange = thisIndex.get(lead);
      final TIntArrayList thatRange = thatIndex.get(lead);
      final int thisSize = thisRange.size();
      final int thatSize = thatRange.size();

      int thisPos = 0;
      int thatPos = 0;
      while (thisPos < thisSize && thatPos < thatSize) {
        final int thisIdx = thisRange.get(thisPos);
        final int thatIdx = thatRange.get(thatPos);

        if (thisIdx == thatIdx) {
          //	LATER: the data might be different too
          sameLinkCount += 1;
          thisPos++;
          thatPos++;
        } else if (thisIdx > thatIdx) {
          thatPos++;
        } else {
          thisPos++;
        }
      }
    }


    return sameLinkCount / fiEdges.getNotNullCount();
  }

  public void clear() {
    fiEdges.clear();
  }

  public double total() {
    final double[] totalConnectivity = new double[] {0.0};

    fiEdges.visit(new EdgeVisitor() {
      public void visit(int from, int into, double e) {
        totalConnectivity[0] += e;
      }
    });

    return totalConnectivity[0];
  }

  public static class Edges {
    protected final List<TIntArrayList> index = new ArrayList<TIntArrayList>();
    protected final List<TDoubleArrayList> elems = new ArrayList<TDoubleArrayList>();

    protected int capacity = 4;

    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    public Edges() {
    }

    public Edges(int size) {
      while (index.size() < size) {
        index.add(new TIntArrayList(capacity));
        elems.add(new TDoubleArrayList(capacity));
      }
    }

    @SuppressWarnings({"UnusedDeclaration"})
    @Deprecated
    public byte[] getIndex() {
      return G4Trove.intsListToBinary(index);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    @Deprecated
    public void setIndex(byte[] indexBinary) {
      G4Trove.binaryToIntsList(indexBinary, index);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    @Deprecated
    public byte[] getElems() {
      return G4Trove.doublesListToBinary(elems);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    @Deprecated
    public void setElems(byte[] elemsBinary) {
      G4Trove.binaryToDoublesList(elemsBinary, elems);
    }

    public double get(int lead, int tail, final EdgeData weightOperator) {
      final int i = index.get(lead).binarySearch(tail);

      if (i < 0) {
        return weightOperator.getNullElement();
      }

      return elems.get(lead).get(i);
    }

    public double set(int lead, int tail, double elem, final EdgeData weightOperator) {
      final int i = index.get(lead).binarySearch(tail);
      final boolean isNull = weightOperator != null && weightOperator.isNull(elem);

      if (i < 0 && !isNull) {
        int insertionIndex = -(i + 1);
        index.get(lead).insert(insertionIndex, tail);
        elems.get(lead).insert(insertionIndex, elem);
        return weightOperator != null ? weightOperator.getNullElement() : 0;
      }

      if (isNull) {
        if (i >= 0) {
          index.get(lead).remove(i);
          return elems.get(lead).remove(i);
        } else {
          return weightOperator.getNullElement();
        }
      } else {
        return elems.get(lead).getSet(i, elem);
      }
    }

    @JsonIgnore
    public boolean isEmpty() {
      return index.isEmpty();
    }

    public TIntArrayList vertexes(int leadIdx, final TIntArrayList result, final boolean ignoreReflective) {
      final TIntArrayList range = index.get(leadIdx);
      result.add(range.toNativeArray());

      if (ignoreReflective) {
        final int leadPos = range.binarySearch(leadIdx);
        if (leadPos >= 0) {
          result.remove(result.size() - range.size() + leadPos);
        }
      }

      return result;
    }

    public double power(int leadIdx, final boolean ignoreReflective, EdgeData weightOperator) {
      final TIntArrayList range = index.get(leadIdx);
      final TDoubleArrayList elm = elems.get(leadIdx);

      double result = 0;
      for (int i = 0; i < range.size(); i++) {
        if (ignoreReflective) {
          if (range.get(i) == leadIdx) {
            continue;
          }
        }

        result += weightOperator.weight(elm.get(i));
      }

      return result;
    }

    @JsonIgnore
    protected int getSize() {
      return index.size();
    }

    @JsonIgnore
    public int getNotNullCount() {
      int notNullCount = 0;

      for (TDoubleArrayList elem : elems) {
        notNullCount += elem.size();
      }

      return notNullCount;
    }

    public void visit(final EdgeVisitor visitor) {
      for (int lead = 0; lead < index.size(); lead++) {
        final TIntArrayList range = index.get(lead);
        for (int pos = 0; pos < range.size(); pos++) {
          final int tail = range.get(pos);
          final double elm = elems.get(lead).get(pos);

          visitor.visit(lead, tail, elm);
        }
      }
    }

    protected void clear() {
      index.clear();
      elems.clear();
    }
  }

  public String toString() {
    final int notNull = getNotNullCount();
    final int size = getSize();
    final double density = (double) notNull / size / size;

    return "EdgeDataSparse[" + notNull + "/" + size + "^2 = " + density + "]";
  }
}