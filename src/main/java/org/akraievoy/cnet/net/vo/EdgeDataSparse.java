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

import com.google.common.io.Closeables;
import gnu.trove.TIntArrayList;
import org.akraievoy.base.Die;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

import static org.akraievoy.cnet.net.vo.StoreUtils.*;

/**
 * Assymetric case:
 *   store all from->into mappings
 * Symmetric case:
 *   store only from->into mappings where from <= into
 */
public class EdgeDataSparse implements EdgeData {
  private static final Logger log = LoggerFactory.getLogger(EdgeDataSparse.class);

  protected boolean symmetric;
  protected double defElem;
  protected int nonDefElems;
  protected int[][] leads;
  protected Store trails;
  protected Store data;
  protected boolean readonly;

  public EdgeDataSparse() {
    this(true, 0.0, 0);
  }

  protected EdgeDataSparse(boolean symmetric, double defElem, final int size) {
    this.symmetric = symmetric;
    this.defElem = defElem;
    this.nonDefElems = 0;
    this.leads = new int[size][2];
    this.trails = new StoreInt();
    this.data = new StoreDouble();
  }

  static enum StreamState {SYMM, SIZE, DEF, NONDEF_ELEMS, LEADS, TRAILS, WIDTH, DATA, COMPLETE}

  public EdgeDataSparse fromStream(InputStream in) throws IOException {
    symmetric = unescapeByte(in) > 0;
    int size = unescapeInt(in);
    defElem = Double.longBitsToDouble(unescapeLong(in));
    nonDefElems = unescapeInt(in);
    leads=new int[size][2];
    for (int lead = 0; lead < leads.length; lead++) {
      leads[lead][0] = unescapeInt(in);
      leads[lead][1] = unescapeInt(in);
    }
    trails.fromStream(in);
    Store.Width width = Store.Width.values()[unescapeByte(in)];
    data = width.create().fromStream(in);
    readonly = true;
    return this;
  }

  public InputStream createStream() {
    //  compactify
    int offset = 0;
    for (int lead = 0; lead < leads.length; lead++) {
      leads[lead][0] -= offset;
      leads[lead][1] -= offset;

      final int uptoExcl = leads[lead][1];

      final int capacityUptoExcl =
          lead + 1 < leads.length ? leads[lead + 1][0] - offset : trails.size();
      final int freeCapacity = capacityUptoExcl - uptoExcl;

      if (freeCapacity == 0) {
        continue;
      }
      trails.del(uptoExcl, capacityUptoExcl);
      data.del(uptoExcl, capacityUptoExcl);

      offset += freeCapacity;
    }
    //  while we may have some extra cells allocated in the stores,
    //    they won't get serialized anyway, so...

    return new InputStream() {
      StreamState state = StreamState.SYMM;
      int sizePos = 0;
      byte[] sizeBits = new byte[4];
      int nonDefElemPos = 0;
      byte[] nonDefElemBits = new byte[4];
      int defPos = 0;
      byte[] defBits = new byte[8];
      int leadIdx = 0;
      int leadPos = 0;
      byte[] leadBits = new byte[4];
      InputStream trailsStoreIn = null;
      InputStream dataStoreIn = null;

      @Override
      public int read() throws IOException {
        switch (state) {
          case SYMM: {
            state = StreamState.SIZE;
            return escapeByte(symmetric ? (byte) 1 : (byte) 0);
          }
          case SIZE: {
            if (sizePos == 0) {
              intBits(leads.length, sizeBits);
            }
            final byte res = sizeBits[sizePos++];
            if (sizePos == sizeBits.length) {
              state = StreamState.DEF;
            }
            return res;
          }
          case DEF: {
            if (defPos == 0) {
              longBits(Double.doubleToLongBits(defElem), defBits);
            }
            final byte res = defBits[defPos++];
            if (defPos == defBits.length) {
              state = StreamState.NONDEF_ELEMS;
            }
            return res;
          }
          case NONDEF_ELEMS: {
            if (nonDefElemPos == 0) {
              intBits(nonDefElems, nonDefElemBits);
            }
            final byte res = nonDefElemBits[nonDefElemPos++];
            if (nonDefElemPos == nonDefElemBits.length) {
              state = StreamState.LEADS;
            }
            return res;
          }
          case LEADS: {
            if (leadPos == 0) {
              intBits(leads[leadIdx / 2][leadIdx % 2], leadBits);
            }
            final byte res = leadBits[leadPos++];
            if (leadPos == leadBits.length) {
              leadIdx += 1;
              leadPos = 0;
              if (leadIdx / 2 == leads.length) {
                state = StreamState.TRAILS;
              }
            }
            return escapeByte(res);
          }
          case TRAILS: {
            if (trailsStoreIn == null) {
              trailsStoreIn = trails.createStream();
            }
            final int res = trailsStoreIn.read();
            if (res < 0) {
              state = StreamState.WIDTH;
              final int widthRes = escapeByte((byte) data.width().ordinal());
              state = StreamState.DATA;
              return widthRes;
            } else {
              return res;
            }
          }
          case WIDTH:
            throw new IllegalStateException("WIDTH state was be not reachable");
          case DATA: {
            if (dataStoreIn == null) {
              dataStoreIn = data.createStream();
            }
            int res = dataStoreIn.read();

            if (res < 0) {
              state = StreamState.COMPLETE;
            }

            return res;
          }
          case COMPLETE:
            return -1;
          default:
            throw new IllegalStateException(
                "implement handling state " + state
            );
        }
      }

      @Override
      public void close() throws IOException {
        Closeables.closeQuietly(trailsStoreIn);
        Closeables.closeQuietly(dataStoreIn);
      }
    };
  }
  public boolean isSymmetric() {
    return symmetric;
  }

  public double getDefElem() {
    return defElem;
  }

  public EdgeData proto(final int protoSize) {
    return EdgeDataFactory.sparse(isSymmetric(), defElem, protoSize);
  }

  public boolean isDef(double elem) {
    return Double.compare(elem, defElem) == 0;
  }

  public double weight(double elem) {
    return elem;
  }

  public double get(int from, int into) {
    validateAccess(from, into);

    final int lead = from;
    final int trail = into;

    final int fromIncl = leads[lead][0];
    final int uptoExcl = leads[lead][1];

    if (fromIncl == uptoExcl) {
      return defElem;
    }

    final int dataIdx = trails.bSearch(fromIncl, uptoExcl, trail);
    if (dataIdx >= 0) {
      return data.get(dataIdx, .0);
    }

    return defElem;
  }

  protected int validateAccess(int from, int into) {
    if (from < 0) {
      throw new IllegalArgumentException(
          "from(" + from + ") < 0"
      );
    }
    if (into < 0) {
      throw new IllegalArgumentException(
          "into(" + into + ") < 0"
      );
    }

    final int size = leads.length;
    if (from >= size) {
      throw new IllegalArgumentException(
          "from(" + from + ") >= size(" + size + ")"
      );
    }
    if (into >= size) {
      throw new IllegalArgumentException(
          "into(" + into + ") >= size(" + size + ")"
      );
    }

    return size;
  }

  public double set(int from, int into, double elem) {
    if (readonly) {
      throw new IllegalStateException("read-only mode");
    }
    validateAccess(from, into);

    //  it's better to degrade writes linearly, and
    //    store twice as much data, but
    //    keep reads and queries efficient
    if (symmetric && from != into) {
      set0(into, from, elem, true);
      if (defElem == 0.0 && Math.IEEEremainder(elem, 1.0) == 0.0 && total() != nonDefElems) {
        log.warn("non-def elems skewed");
      }
    }

    final double origVal = set0(from, into, elem, true);
    if (defElem == 0.0 && Math.IEEEremainder(elem, 1.0) == 0.0 && total() != nonDefElems) {
      log.warn("non-def elems skewed");
    }
    return origVal;
  }

  protected double set0(int from, int into, double elem, final boolean count) {
    final int lead = from;
    final int trail = into;
    final int fromIncl = leads[lead][0];
    final int uptoExcl = leads[lead][1];

    final int size = uptoExcl - fromIncl;
    final int capacityUptoExcl =
        lead + 1 < leads.length ? leads[lead + 1][0] : trails.size();
    final int capacity = capacityUptoExcl - fromIncl;

    final int elemPos = trails.bSearch(fromIncl, uptoExcl, trail);

    if (elemPos < 0) { //  such position was not stored before
      if (Double.compare(elem, defElem) == 0) {
        return defElem; //  nothing to do
      }

      if (uptoExcl >= capacityUptoExcl) { //  expand storage
        final int increment = size == 0 ? 2 : size;
        trails.ins(uptoExcl, uptoExcl + increment, -1);
        data.ins(uptoExcl, uptoExcl + increment, defElem);
        for (int leadPos = lead + 1; leadPos < leads.length; leadPos++) {
          leads[leadPos][0] += increment;
          leads[leadPos][1] += increment;
        }
      }

      //  insert element
      int insertIdx = -(elemPos + 1);
      trails.rotUp(insertIdx, uptoExcl + 1);
      data.rotUp(insertIdx, uptoExcl + 1);
      trails.set(insertIdx, trail);
      data.set(insertIdx, elem);
      leads[lead][1] += 1;
      if (count) {
        nonDefElems += 1;
      }

      return defElem;
    }

    //  such position stored
    if (Double.compare(elem, defElem) == 0) { //  remove element
      trails.rotDown(elemPos, uptoExcl);
      data.rotDown(elemPos, uptoExcl);
      trails.set(uptoExcl - 1, -1);
      final double ori = data.set(uptoExcl - 1, defElem);
      leads[lead][1] -= 1;
      if (count) {
        nonDefElems -= 1;
      }

      if (size * 4 < capacity) { //  leave at most size*2 capacity
        int delFromIncl = uptoExcl + size;
        trails.del(delFromIncl, capacityUptoExcl);
        data.del(delFromIncl, capacityUptoExcl);
        final int decrement = capacityUptoExcl - delFromIncl;
        for (int leadPos = lead + 1; leadPos < leads.length; leadPos++) {
          leads[leadPos][0] -= decrement;
          leads[leadPos][1] -= decrement;
        }
      }

      return ori;
    }

    return data.set(elemPos, elem); //  overwrite existing position
  }

  public int getSize() {
    return leads.length;
  }

  public boolean conn(int from, int into) {
    return !isDef(from, into);
  }

  public TIntArrayList connVertexes(int index) {
    return connVertexes(index, new TIntArrayList());
  }

  public TIntArrayList connVertexes(int index, final TIntArrayList result) {
    final int fromIncl = leads[index][0];
    final int uptoExcl = leads[index][1];

    if (fromIncl == uptoExcl) { //  nothing to add
      return result;
    }

    for (int trailPos = fromIncl; trailPos < uptoExcl; trailPos++) {
      result.add(trails.get(trailPos, 0));
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
    final int fromIncl = leads[index][0];
    final int uptoExcl = leads[index][1];

    if (fromIncl == uptoExcl) { //  nothing to add
      return 0;
    }

    double power = 0;
    for (int trailPos = fromIncl; trailPos < uptoExcl; trailPos++) {
      power += weight(data.get(trailPos, .0));
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

  public int getNonDefCount() {
    return nonDefElems;
  }

  public void visitNonDef(EdgeVisitor visitor) {
    for (int lead = 0; lead < leads.length; lead++) {
      final int fromIncl = leads[lead][0];
      final int uptoExcl = leads[lead][1];
      for (int pos = fromIncl; pos < uptoExcl; pos++) {
        final int trail = trails.get(pos, 0);
        final double elem = data.get(pos, .0);

        visitor.visit(lead, trail, elem);
      }
    }
  }

  public ElemIterator nonDefIterator() {
    return new SparseElemIterator();
  }

  class SparseElemIterator implements ElemIterator, IteratorTuple {
    int from = -1;
    int fromIncl = -1;
    int uptoExcl = -1;
    int posTuple = -1;
    int pos = -1;

    public boolean hasNext() {
      while (pos >= uptoExcl && from < leads.length) {
        from += 1; // outer loop for from
        if (from < leads.length) {
          pos = fromIncl = leads[from][0];
          uptoExcl = leads[from][1];
        } else {
          pos = leads.length;
          uptoExcl = leads.length;
        }
      }

      return from() < leads.length;
    }

    public IteratorTuple next() {
      posTuple = pos;
      pos++;
      return this;
    }

    public int from() {
      return from;
    }

    public int into() {
      return trails.get(posTuple, 0);
    }

    public double value() {
      return data.get(posTuple, .0);
    }
  }


  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof EdgeData)) return false;

    final EdgeDataSparse edgeData = (EdgeDataSparse) o;

    if (Double.compare(edgeData.defElem, defElem) != 0) return false;
    if (symmetric != edgeData.symmetric) return false;

    final int linkCount = edgeData.getNonDefCount();
    if (getNonDefCount() != linkCount) {
      return false;
    }

    final int size = leads.length;
    if (size != edgeData.getSize()) {
      return false;
    }

    final ElemIterator thisNDI = nonDefIterator();
    final ElemIterator thatNDI = edgeData.nonDefIterator();

    while (thisNDI.hasNext()) {
      if (!thatNDI.hasNext()) {
        throw new IllegalStateException(
            "same link count, but `that` iterator exhausted?"
        );
      }

      if (!Util.eq(thisNDI.next(), thatNDI.next())) {
        return false;
      }
    }
    if (thatNDI.hasNext()) {
      throw new IllegalStateException(
          "same link count, but `that` iterator NOT exhausted?"
      );
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
    final int[] resultRef = {result};
    visitNonDef(new EdgeVisitor() {
      @Override
      public void visit(int from, int into, double e) {
        resultRef[0] = 101 * resultRef[0] + from;
        resultRef[0] = 107 * resultRef[0] + into;
      }
    });

    return resultRef[0];
  }

  //	TODO add a generic similarity measure with an Edge Iterator
  public double similarity(EdgeData that) {
    final int[][] thisLeads = leads;
    final int[][] thatLeads = ((EdgeDataSparse) that).leads;
    final Store thisTrails = trails;
    final Store thatTrails = ((EdgeDataSparse) that).trails;


    double sameLinkCount = 0.0;
    for (int lead = 0, maxLead = Math.min(thisLeads.length, thatLeads.length); lead < maxLead; lead++) {
      final int thisFromIncl = thisLeads[lead][0];
      final int thisUptoExcl = thisLeads[lead][1];
      final int thatFromIncl = thatLeads[lead][0];
      final int thatUptoExcl = thatLeads[lead][1];

      int thisPos = thisFromIncl;
      int thatPos = thatFromIncl;
      while (thisPos < thisUptoExcl && thatPos < thatUptoExcl) {
        final int thisIdx = thisTrails.get(thisPos, 0);
        final int thatIdx = thatTrails.get(thatPos, 0);

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

    return sameLinkCount / nonDefElems;
  }

  public void clear() {
    for (int i = 0; i < leads.length; i++) {
      leads[i][0] = leads[i][1] = 0;
      trails.del(0, trails.size());
      data.del(0, data.size());
    }
  }

  public double total() {
    final double[] totalConnectivity = new double[] {0.0};

    visitNonDef(new EdgeVisitor() {
      public void visit(int from, int into, double e) {
        totalConnectivity[0] += e;
      }
    });

    return totalConnectivity[0];
  }

  public String toString() {
    final int nonDefCount = getNonDefCount();
    final int size = getSize();
    final double density = (double) nonDefCount / size / size;

    return "EdgeDataSparse[" + nonDefCount + "/" + size + "^2 = " + density + "]";
  }
}