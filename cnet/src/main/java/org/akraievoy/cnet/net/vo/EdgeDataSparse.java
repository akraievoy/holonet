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

@JsonPropertyOrder({"size", "notNullCount", "nullElement", "symmetric", "fiEdges", "ifEdges"})
public class EdgeDataSparse implements EdgeData {
  protected double nullElement;

  protected Edges fiEdges;
  protected Edges ifEdges;

  protected boolean symmetric;

  @Deprecated
  public EdgeDataSparse() {
    this(true, 0.0);
  }

  protected EdgeDataSparse(boolean symmetric, double nullElement) {
    this.fiEdges = new Edges();
    this.ifEdges = new Edges();

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

  @Deprecated
  @SuppressWarnings({"UnusedDeclaration"})
  public Edges getIfEdges() {
    return ifEdges;
  }

  @Deprecated
  @SuppressWarnings({"UnusedDeclaration"})
  public void setIfEdges(Edges ifEdges) {
    this.ifEdges = ifEdges;
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
    return EdgeDataFactory.sparse(isSymmetric(), nullElement);
  }

  public double get(int from, int into) {
    if (isSymmetric() && from > into) {
      return fiEdges.get(into, from, this);
    }

    return fiEdges.get(from, into, this);
  }

  public double set(int from, int into, double elem) {
    if (isSymmetric() && from > into) {
      ifEdges.set(from, into, elem, this);
      return fiEdges.set(into, from, elem, this);
    }

    ifEdges.set(into, from, elem, this);
    return fiEdges.set(from, into, elem, this);
  }

  public void setSize(int size) {
    ifEdges.setSize(size);
    fiEdges.setSize(size);
  }

  public int getSize() {
    if (fiEdges.isEmpty()) {
      return 0;
    }

    final int maxFrom = fiEdges.getMaxLeading();
    final int maxInto = ifEdges.getMaxLeading();

    return Math.max(maxFrom, maxInto);
  }

  public void remove(int index) {
    fiEdges.remove(index);
    ifEdges.remove(index);
  }

  public void insert(final int index) {
    final boolean last = index == getSize();
    if (last) {
      return;
    }

    fiEdges.insert(index);
    ifEdges.insert(index);
  }

  public boolean conn(int from, int into) {
    return !isNull(from, into);
  }

  public TIntArrayList outVertexes(int from) {
    return outVertexes(from, new TIntArrayList());
  }

  public TIntArrayList outVertexes(int from, final TIntArrayList result) {
    if (isSymmetric()) {
      return connVertexes(from, result);
    }

    return fiEdges.vertexes(from, result, false);
  }

  public TIntArrayList inVertexes(int into) {
    return inVertexes(into, new TIntArrayList());
  }

  public TIntArrayList inVertexes(int into, final TIntArrayList result) {
    if (isSymmetric()) {
      return connVertexes(into, result);
    }

    return ifEdges.vertexes(into, result, false);
  }

  public TIntArrayList connVertexes(int index) {
    return connVertexes(index, new TIntArrayList());
  }

  public TIntArrayList connVertexes(int index, final TIntArrayList result) {
    ifEdges.vertexes(index, result, false);
    fiEdges.vertexes(index, result, true);

    return result;
  }

  public TDoubleArrayList outElements(int from) {
    return outElements(from, new TDoubleArrayList());
  }

  public TDoubleArrayList outElements(int from, final TDoubleArrayList result) {
    if (isSymmetric()) {
      return connElements(from, result);
    }

    return fiEdges.elements(from, result, false);
  }

  public TDoubleArrayList inElements(int into) {
    return inElements(into, new TDoubleArrayList());
  }

  public TDoubleArrayList inElements(int into, final TDoubleArrayList result) {
    if (isSymmetric()) {
      return connElements(into, result);
    }

    return ifEdges.elements(into, result, false);
  }

  public TDoubleArrayList connElements(int index) {
    return connElements(index, new TDoubleArrayList());
  }

  public TDoubleArrayList connElements(int index, final TDoubleArrayList result) {
    fiEdges.elements(index, result, false);
    ifEdges.elements(index, result, true);

    return result;
  }

  public double weight(int from, int into) {
    return weight(get(from, into));
  }

  public boolean isNull(int from, int into) {
    return isNull(get(from, into));
  }

  public double power(final int index) {
    return ifEdges.power(index, false, this) + fiEdges.power(index, true, this);
  }

  public double powerOut(final int index) {
    if (isSymmetric()) {
      return power(index);
    }

    return fiEdges.power(index, false, this);
  }

  public double powerIn(final int index) {
    if (isSymmetric()) {
      return power(index);
    }

    return ifEdges.power(index, false, this);
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
    ifEdges.clear();
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

    @Deprecated
    public Edges() {
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
      while (index.size() <= lead) {
        index.add(new TIntArrayList(capacity));
        elems.add(new TDoubleArrayList(capacity));
      }

      final int i = index.get(lead).binarySearch(tail);

      if (i < 0) {
        return weightOperator.getNullElement();
      }

      return elems.get(lead).get(i);
    }

    public double set(int lead, int tail, double elem, final EdgeData weightOperator) {
      while (index.size() <= lead) {
        index.add(new TIntArrayList(capacity));
        elems.add(new TDoubleArrayList(capacity));
      }

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

    public void setSize(int size) {
      if (index.size() > size) {
        index.subList(size, index.size()).clear();
        elems.subList(size, elems.size()).clear();
      }

      for (int i = index.size() - 1; i >= 0; i--) {
        final TIntArrayList idx = index.get(i);

        final int sizePos = idx.binarySearch(size);
        final int cutoff = sizePos < 0 ? -(sizePos + 1) : sizePos;
        final int cutlen = idx.size() - cutoff;

        idx.remove(cutoff, cutlen);
        elems.get(i).remove(cutoff, cutlen);
      }
    }

    @JsonIgnore
    public boolean isEmpty() {
      return index.isEmpty();
    }

    public void remove(int remIdx) {
      if (index.size() > remIdx) {
        index.remove(remIdx);
        elems.remove(remIdx);
      }

      for (int i = index.size() - 1; i >= 0; i--) {
        final TIntArrayList idx = index.get(i);

        final int remPos = idx.binarySearch(remIdx);

        if (remPos >= 0) {
          idx.remove(remPos);
          elems.get(i).remove(remPos);
        }

        final int decStart = remPos < 0 ? -(remPos + 1) : remPos;
        for (int insI = decStart; insI < idx.size(); insI++) {
          idx.set(insI, idx.get(insI) - 1);
        }
      }
    }

    public void insert(int insIdx) {
      while (index.size() < insIdx) {
        index.add(new TIntArrayList(capacity));
        elems.add(new TDoubleArrayList(capacity));
      }
      index.add(insIdx, new TIntArrayList(capacity));
      elems.add(insIdx, new TDoubleArrayList(capacity));

      for (int i = index.size() - 1; i >= 0; i--) {
        final TIntArrayList idx = index.get(i);

        final int insPos = idx.binarySearch(insIdx);
        final int incStart = insPos < 0 ? -(insPos + 1) : insPos;

        for (int insI = incStart; insI < idx.size(); insI++) {
          idx.set(insI, 1 + idx.get(insI));
        }
      }
    }

    public TIntArrayList vertexes(int leadIdx, final TIntArrayList result, final boolean ignoreReflective) {
      while (index.size() <= leadIdx) {
        index.add(new TIntArrayList(capacity));
        elems.add(new TDoubleArrayList(capacity));
      }

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

    public TDoubleArrayList elements(int leadIdx, final TDoubleArrayList result, final boolean ignoreReflective) {
      while (index.size() <= leadIdx) {
        index.add(new TIntArrayList(capacity));
        elems.add(new TDoubleArrayList(capacity));
      }

      final TIntArrayList range = index.get(leadIdx);
      final TDoubleArrayList elm = elems.get(leadIdx);
      result.add(elm.toNativeArray());

      if (ignoreReflective) {
        final int leadPos = range.binarySearch(leadIdx);
        if (leadPos >= 0) {
          result.remove(result.size() - range.size() + leadPos);
        }
      }

      return result;
    }

    public double power(int leadIdx, final boolean ignoreReflective, EdgeData weightOperator) {
      while (index.size() <= leadIdx) {
        index.add(new TIntArrayList(capacity));
        elems.add(new TDoubleArrayList(capacity));
      }

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
    protected int getMaxLeading() {
      return index.size();
    }

    @JsonIgnore
    public int getNotNullCount() {
      int notNullCount = 0;

      for (int i = 0, size = elems.size(); i < size; i++) {
        notNullCount += elems.get(i).size();
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