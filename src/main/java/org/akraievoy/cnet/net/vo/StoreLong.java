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
public class StoreLong implements Store {
  private long[] arr;
  private int size;

  public StoreLong() {
    this(0, 0);
  }

  StoreLong(final int newSize, final long newDef) {
    arr = new long[newSize];
    size = newSize;

    Arrays.fill(arr, 0, size, newDef);
  }

  public Width width() {
    return Width.LONG;
  }

  public int size() {
    return size;
  }

  public void ins(int fromIncl, int uptoExcl, long def) {
    StoreUtils.validateInsert(size, fromIncl, uptoExcl);

    final int subSize = uptoExcl - fromIncl;

    //  expand storage
    int newLength = arr.length;
    while (newLength < size + subSize) {
      newLength += newLength == 0 ? subSize * 2 : newLength;
    }

    if (newLength > arr.length) {
      final long[] oldArr = arr;
      arr = new long[newLength];
      System.arraycopy(oldArr, 0, arr, 0, oldArr.length);
    }

    //  shift
    System.arraycopy(arr, fromIncl, arr, fromIncl + subSize, size - fromIncl);

    //  zero out new
    Arrays.fill(arr, fromIncl, uptoExcl, def);

    size += subSize;
  }

  public void ins(int fromIncl, int uptoExcl, int def) {
    ins(fromIncl, uptoExcl, (long) def);
  }

  public void ins(int fromIncl, int uptoExcl, float def) {
    ins(fromIncl, uptoExcl, (long) def);
  }

  public void ins(int fromIncl, int uptoExcl, double def) {
    ins(fromIncl, uptoExcl, (long) def);
  }

  public void ins(int fromIncl, int uptoExcl, byte def) {
    ins(fromIncl, uptoExcl, (long) def);
  }

  public void ins(int fromIncl, int uptoExcl, boolean def) {
    ins(fromIncl, uptoExcl, def ? 1L : 0L);
  }

  public void fill(int fromIncl, int uptoExcl, boolean def) {
    fill(fromIncl, uptoExcl, def ? 1L : 0L);
  }

  public void fill(int fromIncl, int uptoExcl, byte def) {
    fill(fromIncl, uptoExcl, (long) def);
  }

  public void fill(int fromIncl, int uptoExcl, int def) {
    fill(fromIncl, uptoExcl, (long) def);
  }

  public void fill(int fromIncl, int uptoExcl, long def) {
    validateAccess(size, fromIncl, uptoExcl);
    Arrays.fill(arr, fromIncl, uptoExcl, def);
  }

  public void fill(int fromIncl, int uptoExcl, float def) {
    fill(fromIncl, uptoExcl, (long) def);
  }

  public void fill(int fromIncl, int uptoExcl, double def) {
    fill(fromIncl, uptoExcl, (long) def);
  }

  public int bSearch(int fromIncl, int uptoExcl, byte search) {
    return bSearch(fromIncl, uptoExcl, (long) search);
  }

  public int bSearch(int fromIncl, int uptoExcl, int search) {
    return bSearch(fromIncl, uptoExcl, (long) search);
  }

  public int bSearch(int fromIncl, int uptoExcl, long search) {
    return Arrays.binarySearch(arr, fromIncl, uptoExcl, search);
  }

  public int bSearch(int fromIncl, int uptoExcl, float search) {
    return bSearch(fromIncl, uptoExcl, (long) search);
  }

  public int bSearch(int fromIncl, int uptoExcl, double search) {
    return bSearch(fromIncl, uptoExcl, (long) search);
  }

  public void rotUp(int fromIncl, int uptoExcl) {
    validateAccess(fromIncl, uptoExcl);
    final long saved = arr[uptoExcl - 1];
    System.arraycopy(arr, fromIncl, arr, fromIncl + 1, uptoExcl - fromIncl - 1);
    arr[fromIncl] = saved;
  }

  public void rotDown(int fromIncl, int uptoExcl) {
    validateAccess(fromIncl, uptoExcl);
    final long saved = arr[fromIncl];
    System.arraycopy(arr, fromIncl + 1, arr, fromIncl, uptoExcl - fromIncl - 1);
    arr[uptoExcl - 1] = saved;
  }

  public void del(int fromIncl, int uptoExcl) {
    StoreUtils.validateAccess(size, fromIncl, uptoExcl);

    final int subSize = uptoExcl - fromIncl;

    //  shift
    System.arraycopy(arr, uptoExcl, arr, fromIncl, size - uptoExcl);

    if (size * 4 < arr.length) {
      final long[] oldArr = arr;
      arr = new long[size * 2];
      System.arraycopy(oldArr, 0, arr, 0, size);
    }

    size -= subSize;
  }

  public double get(int pos, double typeHint) {
    return get(pos, (long) typeHint);
  }

  public double set(int pos, double val) {
    return set(pos, (long) val);
  }

  public float get(int pos, float typeHint) {
    return get(pos, (long) typeHint);
  }

  public float set(int pos, float val) {
    return set(pos, (long) val);
  }

  public long get(int pos, long typeHint) {
    StoreUtils.validateAccess(pos, size);

    return arr[pos];
  }

  public long set(int pos, long val) {
    StoreUtils.validateAccess(pos, size);

    final long oldLong = arr[pos];

    arr[pos] = val;

    return oldLong;
  }

  public int get(int pos, int typeHint) {
    return (int) get(pos, (long) typeHint);
  }

  public int set(int pos, int val) {
    return (int) set(pos, (long) val);
  }

  public byte get(int pos, byte typeHint) {
    return (byte) get(pos, (long) typeHint);
  }

  public byte set(int pos, byte val) {
    return (byte) set(pos, (long) val);
  }

  public boolean get(int pos, boolean typeHint) {
    return get(pos, (long) 0) != 0;
  }

  public boolean set(int pos, boolean val) {
    return set(pos, val ? (long) 1 : (long) 0) != 0;
  }

  public StoreLong fromStream(
      InputStream in
  ) throws IOException {
    size = unescapeInt(in);
    arr = new long[size];
    for (int pos = 0; pos < size; pos++) {
      arr[pos] = unescapeLong(in);
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
      private byte[] byteBuf = new byte[8];

      @Override
      public int read() throws IOException {
        if (state == StoreUtils.StreamState.SIZE) {
          if (sizePos == 0) {
            intBits(size, sizeBits);
          }
          final byte res = sizeBits[sizePos++];
          if (sizePos == sizeBits.length) {
            state = size > 0 ? StreamState.DATA : StreamState.COMPLETE;
          }
          return escapeByte(res);
        } else if (state == StreamState.DATA) {
          if (byteBufPos == 0) {
            longBits(arr[pos++], byteBuf);
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
