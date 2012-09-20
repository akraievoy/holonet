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
import org.akraievoy.db.Streamable;

import java.io.IOException;
import java.io.InputStream;

import static org.akraievoy.cnet.net.vo.StoreUtils.*;
import static org.akraievoy.cnet.net.vo.StoreUtils.escapeByte;
import static org.akraievoy.cnet.net.vo.StoreUtils.longBits;

public class VertexData implements Streamable {
  protected Store data = new StoreDouble();
  protected double nullElement;
  protected boolean readonly;

  @Deprecated
  public VertexData() {
    this(0);
  }

  public VertexData(int nodes) {
    this(0.0, nodes);
  }

  //  FIXME rename nullElement to defElem
  public VertexData(double nullElement, int size) {
    this.nullElement = nullElement;
    data.ins(0, size, nullElement);
  }

  public double[] getData() {
    final double[] nativeArr = new double[data.size()];
    for (int pos = 0; pos < data.size(); pos++) {
      nativeArr[pos] = data.get(pos, .0);
    }
    return nativeArr;
  }

  static enum StreamState {DEF, WIDTH, DATA, COMPLETE}

  public InputStream createStream() {
    return new InputStream() {
      StreamState state = StreamState.DEF;
      int defPos = 0;
      byte[] defBits = new byte[8];
      InputStream edgeStoreIn = null;

      @Override
      public int read() throws IOException {
        switch (state) {
          case DEF: {
            if (defPos == 0) {
              longBits(Double.doubleToLongBits(nullElement), defBits);
            }
            final byte res = defBits[defPos++];
            if (defPos == defBits.length) {
              state = StreamState.WIDTH;
            }
            return res;
          }
          case WIDTH:
            state = StreamState.DATA;
            return escapeByte((byte) data.width().ordinal());
          case DATA: {
            if (edgeStoreIn == null) {
              edgeStoreIn = data.createStream();
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

  public VertexData fromStream(InputStream in) throws IOException {
    nullElement = Double.longBitsToDouble(unescapeLong(in));
    final Store.Width width = Store.Width.values()[unescapeByte(in)];
    data = width.create().fromStream(in);
    readonly = true;
    return this;
  }

  public double getNullElement() {
    return nullElement;
  }

  public VertexData proto(final int size) {
    return new VertexData(nullElement, size);
  }

  public boolean isNull(int index) {
    return Double.compare(getNullElement(), get(index)) == 0;
  }

  public int getSize() {
    return data.size();
  }

  public double get(int index) {
    if (index < 0 || index >= data.size()) {
      return getNullElement();
    }

    return data.get(index, .0);
  }

  public double set(int index, double elem) {
    if (readonly) {
      throw new IllegalStateException("read-only mode");
    }
    return data.set(index, elem);
  }

  public String toString() {
    return "VertexData[" + getSize() + "]";
  }
}
