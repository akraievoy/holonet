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
import org.akraievoy.db.Streamable;

import static org.akraievoy.cnet.net.vo.StoreUtils.*;

import java.io.IOException;
import java.io.InputStream;

public class EdgeDataDense implements EdgeData {
  protected boolean readonly = false;
  protected boolean symmetric;
  protected int size;
  protected double defElem;
  protected Store edgeStore;  //  LATER rename to data

  @Deprecated
  public EdgeDataDense() {
    this(true, 0.0, 32);
  }

  protected EdgeDataDense(boolean symmetric, double defElem, final int size) {
    this.defElem = defElem;
    this.symmetric = symmetric;
    this.size = size;

    if (symmetric) {
      //  column-based packed storage for the upper triangular matrix
      //    please see the links for more details on this
      //      http://software.intel.com/sites/products/documentation/hpc/mkl/mklman/GUID-7B11079E-CB74-4A5F-AEEA-D6C9B7181C42.htm#PACKED
      //      http://www.netlib.org/lapack/lug/node123.html
      //      http://wwwasdoc.web.cern.ch/wwwasdoc/shortwrupsdir/f112/top.html
      this.edgeStore = new StoreDouble(size * (size + 1) / 2, defElem);
    } else {
      this.edgeStore = new StoreDouble(size * size, defElem);
    }
  }

  static enum StreamState {SYMM, SIZE, DEF, WIDTH, DATA, COMPLETE}

  public Streamable fromStream(InputStream in) throws IOException {
    symmetric = unescapeByte(in) > 0;
    size = unescapeInt(in);
    defElem = Double.longBitsToDouble(unescapeLong(in));
    Store.Width width = Store.Width.values()[unescapeByte(in)];
    edgeStore = width.create().fromStream(in);
    readonly = true;
    return this;
  }

  public InputStream createStream() {
    return new InputStream() {
      StreamState state = StreamState.SYMM;
      int sizePos = 0;
      byte[] sizeBits = new byte[4];
      int defPos = 0;
      byte[] defBits = new byte[8];
      InputStream edgeStoreIn = null;

      @Override
      public int read() throws IOException {
        switch (state) {
          case SYMM:
            state = StreamState.SIZE;
            return escapeByte(symmetric ? (byte) 1 : (byte) 0);
          case SIZE: {
            if (sizePos == 0) {
              intBits(size, sizeBits);
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
              state = StreamState.WIDTH;
            }
            return res;
          }
          case WIDTH:
            state = StreamState.DATA;
            return escapeByte((byte) edgeStore.width().ordinal());
          case DATA: {
            if (edgeStoreIn == null) {
              edgeStoreIn = edgeStore.createStream();
            }
            int res = edgeStoreIn.read();

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
        Closeables.closeQuietly(edgeStoreIn);
      }
    };
  }

  public boolean isSymmetric() {
    return symmetric;
  }

  public double getDefElem() {
    return defElem;
  }

  public int getSize() {
    return size;
  }

  public boolean isDef(double elem) {
    return Double.compare(elem, defElem) == 0;
  }

  public double weight(double elem) {
    return elem;
  }

  public EdgeData proto(final int protoSize) {
    return EdgeDataFactory.dense(isSymmetric(), defElem, protoSize);
  }

  public double get(int from, int into) {
    final int index = getIndex(into, from);

    return edgeStore.get(index, .0);
  }

  protected int getIndex(int from, int into) {
    if (symmetric) {
      if (from > into) {
        return into + from * (from + 1) / 2;
      } else {
        return from + into * (into + 1) / 2;
      }
    }

    return from * size + into;
  }

  public double set(int from, int into, double elem) {
    if (readonly) {
      throw new IllegalStateException("read-only mode");
    }

    final int index = getIndex(into, from);
    final double prevElem = edgeStore.get(index, .0);

    edgeStore.set(index, elem);

    return prevElem;
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
    if (indexes.size() == 1) {
      throw new IllegalStateException("indexes.size == 1");
    }

    double weight = 0;
    for (int i = 0; i < indexes.size() - 1; i++) {
      weight += weight(indexes.get(i), indexes.get(i + 1));
    }

    return weight;
  }

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

  public ElemIterator nonDefIterator() {
    return new ElemIterator();
  }

  class ElemIterator implements EdgeData.ElemIterator, IteratorTuple{
    int from = -1;
    int into = size;
    int fromTuple = -1;
    int intoTuple = -1;

    public boolean hasNext() {
      while (from < size && (into >= size || isDef(get(from, into)))) {
        if (into >= size) {
          from++;
          into = 0;
        } else {
          into++;
        }
      }

      return from < size;
    }

    public IteratorTuple next() {
      fromTuple = from;
      intoTuple = into;
      into++;
      return this;
    }

    public int from() {
      return fromTuple;
    }

    public int into() {
      return intoTuple;
    }

    public double value() {
      return get(fromTuple, intoTuple);
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
    edgeStore.fill(0, edgeStore.size(), defElem);
  }

  public String toString() {
    final int size = getSize();

    return "EdgeDataDense[" + size + "]";
  }
}