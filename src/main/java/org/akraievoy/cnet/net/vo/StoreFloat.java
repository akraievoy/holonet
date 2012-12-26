/*
 Copyright 2012 Anton Kraievoy akraievoy@gmail.com
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

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import static org.akraievoy.cnet.net.vo.StoreUtils.*;

@SuppressWarnings("UnusedDeclaration")
public class StoreFloat implements Store {
  private float[] arr;
  private int size;

  public StoreFloat() {
    this(0, 0);
  }

  StoreFloat(final int newSize, final float newDef) {
    arr = new float[newSize];
    size = newSize;

    Arrays.fill(arr, 0, size, newDef);
  }

  public Width width() {
    return Width.FLOAT;
  }

  public int size() {
    return size;
  }

  public void ins(int fromIncl, int uptoExcl, float def) {
    StoreUtils.validateInsert(size, fromIncl, uptoExcl);

    final int subSize = uptoExcl - fromIncl;

    //  expand storage
    int newLength = arr.length;
    while (newLength < size + subSize) {
      newLength += newLength == 0 ? subSize * 2 : newLength;
    }

    if (newLength > arr.length) {
      final float[] oldArr = arr;
      arr = new float[newLength];
      System.arraycopy(oldArr, 0, arr, 0, oldArr.length);
    }

    //  shift
    System.arraycopy(arr, fromIncl, arr, fromIncl + subSize, size - fromIncl);

    //  zero out new
    Arrays.fill(arr, fromIncl, uptoExcl, def);

    size += subSize;
  }

  public void ins(int fromIncl, int uptoExcl, long def) {
    ins(fromIncl, uptoExcl, (float) def);
  }

  public void ins(int fromIncl, int uptoExcl, int def) {
    ins(fromIncl, uptoExcl, (float) def);
  }

  public void ins(int fromIncl, int uptoExcl, double def) {
    ins(fromIncl,uptoExcl,(float) def);
  }

  public void ins(int fromIncl, int uptoExcl, byte def) {
    ins(fromIncl,uptoExcl,(float) def);
  }

  public void ins(int fromIncl, int uptoExcl, boolean def) {
    ins(fromIncl,uptoExcl, def ? 1f : 0f);
  }

  public void fill(int fromIncl, int uptoExcl, boolean def) {
    fill(fromIncl, uptoExcl, def ? 1.0f : 0.0f);
  }

  public void fill(int fromIncl, int uptoExcl, byte def) {
    fill(fromIncl, uptoExcl, (float) def);
  }

  public void fill(int fromIncl, int uptoExcl, int def) {
    fill(fromIncl, uptoExcl, (float) def);
  }

  public void fill(int fromIncl, int uptoExcl, long def) {
    fill(fromIncl, uptoExcl, (float) def);
  }

  public void fill(int fromIncl, int uptoExcl, float def) {
    validateAccess(size, fromIncl, uptoExcl);
    Arrays.fill(arr, fromIncl, uptoExcl, def);
  }

  public void fill(int fromIncl, int uptoExcl, double def) {
    fill(fromIncl, uptoExcl, (float) def);
  }

  public int bSearch(int fromIncl, int uptoExcl, byte search) {
    return bSearch(fromIncl, uptoExcl, (float) search);
  }

  public int bSearch(int fromIncl, int uptoExcl, int search) {
    return bSearch(fromIncl, uptoExcl, (float) search);
  }

  public int bSearch(int fromIncl, int uptoExcl, long search) {
    return bSearch(fromIncl, uptoExcl, (float) search);
  }

  public int bSearch(int fromIncl, int uptoExcl, float search) {
    return Arrays.binarySearch(arr, fromIncl, uptoExcl, search);
  }

  public int bSearch(int fromIncl, int uptoExcl, double search) {
    return bSearch(fromIncl, uptoExcl, (float) search);
  }

  public void rotUp(int fromIncl, int uptoExcl) {
    validateAccess(fromIncl, uptoExcl);
    final float saved = arr[uptoExcl - 1];
    System.arraycopy(arr, fromIncl, arr, fromIncl + 1, uptoExcl - fromIncl - 1);
    arr[fromIncl] = saved;
  }

  public void rotDown(int fromIncl, int uptoExcl) {
    validateAccess(fromIncl, uptoExcl);
    final float saved = arr[fromIncl];
    System.arraycopy(arr, fromIncl + 1, arr, fromIncl, uptoExcl - fromIncl - 1);
    arr[uptoExcl - 1] = saved;
  }

  public void del(int fromIncl, int uptoExcl) {
    StoreUtils.validateAccess(size, fromIncl, uptoExcl);

    final int subSize = uptoExcl - fromIncl;

    //  shift
    System.arraycopy(arr, uptoExcl, arr, fromIncl, size - uptoExcl);

    if (size * 4 < arr.length) {
      final float[] oldArr = arr;
      arr = new float[size * 2];
      System.arraycopy(oldArr, 0, arr, 0, size);
    }

    size -= subSize;
  }

  public double get(int pos, double typeHint) {
    return get(pos, (float) typeHint);
  }

  public double set(int pos, double val) {
    return set(pos, (float) val);
  }

  public float get(int pos, float typeHint) {
    StoreUtils.validateAccess(pos, size);

    return arr[pos];
  }

  public float set(int pos, float val) {
    StoreUtils.validateAccess(pos, size);

    final float oldFloat = arr[pos];

    arr[pos] = val;

    return oldFloat;
  }

  public long get(int pos, long typeHint) {
    return (long) get(pos, (float) typeHint);
  }

  public long set(int pos, long val) {
    return (long) set(pos, (float) val);
  }

  public int get(int pos, int typeHint) {
    return (int) get(pos, (float) typeHint);
  }

  public int set(int pos, int val) {
    return (int) set(pos, (float) val);
  }

  public byte get(int pos, byte typeHint) {
    return (byte) get(pos, (float) typeHint);
  }

  public byte set(int pos, byte val) {
    return (byte) set(pos, (float) val);
  }

  public boolean get(int pos, boolean typeHint) {
    return Float.compare(get(pos, (float) 0), .0f) != 0;
  }

  public boolean set(int pos, boolean val) {
    return Float.compare(set(pos, val ? (float) 1 : (float) 0), .0f) != 0 ;
  }

  public StoreFloat fromStream(
      InputStream in
  ) throws IOException {
    size = unescapeInt(in);
    arr = new float[size];
    for (int pos = 0; pos < size; pos++) {
      arr[pos] = Float.intBitsToFloat(unescapeInt(in));
    }

    return this;
  }

  public InputStream createStream() {
    return new InputStream() {
      private StoreUtils.StreamState state = StoreUtils.StreamState.SIZE;
      private int sizePos = 0;
      private byte[] sizeBits = new byte[4];
      private int pos = 0;
      private int byteBufPos = 0;
      private byte[] byteBuf = new byte[4];

      @Override
      public int read() throws IOException {
        if (state == StoreUtils.StreamState.SIZE) {
          if (sizePos == 0) {
            intBits(size, sizeBits);
          }
          final byte res = sizeBits[sizePos++];
          if (sizePos == sizeBits.length) {
            state = size > 0 ? StoreUtils.StreamState.DATA : StoreUtils.StreamState.COMPLETE;
          }
          return escapeByte(res);
        } else if (state == StreamState.DATA) {
          if (byteBufPos == 0) {
            intBits(Float.floatToIntBits(arr[pos++]), byteBuf);
          }
          byte res = byteBuf[byteBufPos++];
          if (byteBufPos == byteBuf.length) {
            byteBufPos = 0;
          }
          if (pos == size && byteBufPos == 0) {
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
}
