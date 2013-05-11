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

import java.io.IOException;
import java.io.InputStream;

import static org.akraievoy.cnet.net.vo.StoreUtils.*;

public class EdgeDataFactory {
  public static EdgeData sparse(final boolean symmetric, final int size) {
    return sparse(symmetric, 0.0, size);
  }

  public static EdgeData sparse(boolean symmetric, double nullElement, final int size) {
    return new EdgeDataSparse(symmetric, nullElement, size);
  }

  public static EdgeData constant(final int size, final double value) {
    return new EdgeDataConstant(size, value);
  }

  public static EdgeData dense(boolean symmetric, double nullElement, int size) {
    final EdgeDataDense dense = new EdgeDataDense(symmetric, nullElement, size);

    return dense;
  }

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

    public boolean isSymmetric() {
      return true;
    }

    public boolean isDef(double elem) {
      return elem == getDefElem();
    }

    public double weight(double elem) {
      return elem;
    }

    public double getDefElem() {
      return 0.0;
    }

    public EdgeData proto(final int protoSize) {
      throw new UnsupportedOperationException("constant EdgeData");
    }

    public double get(int from, int into) {
      return value;
    }

    public double set(int from, int into, double elem) {
      throw new UnsupportedOperationException("constant EdgeData");
    }

    public boolean conn(int from, int into) {
      return !isDef(value);
    }

    public TIntArrayList connVertexes(int index) {
      return connVertexes(index, new TIntArrayList());
    }

    public TIntArrayList connVertexes(int index, TIntArrayList result) {
      result.clear();
      if (isDef(value)) {
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

    public boolean isDef(int from, int into) {
      return isDef(weight(from, into));
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

    public int getNonDefCount() {
      if (isDef(value)) {
        return 0;
      }
      return size * size;
    }

    public void visitNonDef(EdgeVisitor visitor) {
      for (int from = 0; from < size; from++) {
        for (int into = 0; into < size; into++) {
          visitor.visit(from, into, value);
        }
      }
    }

    public ElemIterator nonDefIterator() {
      return new ElemIterator();
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

    public int getSize() {
      return size;
    }

    static enum StreamState {SIZE, VALUE, COMPLETE}

    public InputStream createStream() {
      return new InputStream() {
        StreamState state = StreamState.SIZE;
        int sizePos = 0;
        byte[] sizeBits = new byte[4];
        int valuePos = 0;
        byte[] valueBits = new byte[8];

        @Override
        public int read() throws IOException {
          if (state == StreamState.SIZE) {
            if (sizePos == 0) {
              intBits(size, sizeBits);
            }
            final byte res = sizeBits[sizePos++];
            if (sizePos == sizeBits.length) {
              state = StreamState.VALUE;
            }
            return escapeByte(res);
          } else if (state == StreamState.VALUE) {
            if (valuePos == 0) {
              longBits(Double.doubleToLongBits(value), valueBits);
            }
            final byte res = valueBits[sizePos++];
            if (valuePos == valueBits.length) {
              state = StreamState.COMPLETE;
            }
            return escapeByte(res);
          } else if (state == StreamState.COMPLETE) {
            return -1;
          } else {
            throw new IllegalStateException(
                "implement handling state " + state
            );
          }
        }
      };
    }

    public EdgeDataConstant fromStream(InputStream in) throws IOException {
      size = unescapeInt(in);
      value = Double.longBitsToDouble(unescapeLong(in));
      return this;
    }

    class ElemIterator implements EdgeData.ElemIterator, IteratorTuple{
      final int len = size * size;
      int pos = 0;
      int posTuple = -1;

      public boolean hasNext() {
        return pos < len;
      }

      public IteratorTuple next() {
        posTuple = pos;
        pos++;
        return this;
      }

      public int from() {
        return posTuple / size;
      }

      public int into() {
        return posTuple % size;
      }

      public double value() {
        return value;
      }
    }
  }
}
