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
public class StoreInt implements Store {
  private int[] arr;
  private int size;

  public StoreInt() {
    this(0, 0);
  }

  StoreInt(final int newSize, final int newDef) {
    arr = new int[newSize];
    size = newSize;

    Arrays.fill(arr, 0, size, newDef);
  }

  public Width width() {
    return Width.INT;
  }

  public int size() {
    return size;
  }

  public void ins(int fromIncl, int uptoExcl, int def) {
    StoreUtils.validateInsert(size, fromIncl, uptoExcl);

    final int subSize = uptoExcl - fromIncl;

    //  expand storage
    int newLength = arr.length;
    while (newLength < size + subSize) {
      newLength += newLength == 0 ? subSize * 2 : newLength;
    }

    if (newLength > arr.length) {
      final int[] oldArr = arr;
      arr = new int[newLength];
      System.arraycopy(oldArr, 0, arr, 0, oldArr.length);
    }

    //  shift
    System.arraycopy(arr, fromIncl, arr, fromIncl + subSize, size - fromIncl);

    //  zero out new
    Arrays.fill(arr, fromIncl, uptoExcl, def);

    size += subSize;
  }

  public void ins(int fromIncl, int uptoExcl, long def) {
    ins(fromIncl, uptoExcl, (int) def);
  }

  public void ins(int fromIncl, int uptoExcl, float def) {
    ins(fromIncl, uptoExcl, (int) def);
  }

  public void ins(int fromIncl, int uptoExcl, double def) {
    ins(fromIncl, uptoExcl, (int) def);
  }

  public void ins(int fromIncl, int uptoExcl, byte def) {
    ins(fromIncl, uptoExcl, (int) def);
  }

  public void ins(int fromIncl, int uptoExcl, boolean def) {
    ins(fromIncl, uptoExcl, def ? 1 : 0);
  }

  public void fill(int fromIncl, int uptoExcl, boolean def) {
    fill(fromIncl, uptoExcl, def ? 1 : 0);
  }

  public void fill(int fromIncl, int uptoExcl, byte def) {
    fill(fromIncl, uptoExcl, (int) def);
  }

  public void fill(int fromIncl, int uptoExcl, int def) {
    validateAccess(size, fromIncl, uptoExcl);
    Arrays.fill(arr, fromIncl, uptoExcl, def);
  }

  public void fill(int fromIncl, int uptoExcl, long def) {
    fill(fromIncl, uptoExcl, (int) def);
  }

  public void fill(int fromIncl, int uptoExcl, float def) {
    fill(fromIncl, uptoExcl, (int) def);
  }

  public void fill(int fromIncl, int uptoExcl, double def) {
    fill(fromIncl, uptoExcl, (int) def);
  }

  public int bSearch(int fromIncl, int uptoExcl, byte search) {
    return bSearch(fromIncl, uptoExcl, (int) search);
  }

  public int bSearch(int fromIncl, int uptoExcl, int search) {
    return Arrays.binarySearch(arr, fromIncl, uptoExcl, search);
  }

  public int bSearch(int fromIncl, int uptoExcl, long search) {
    return bSearch(fromIncl, uptoExcl, (int) search);
  }

  public int bSearch(int fromIncl, int uptoExcl, float search) {
    return bSearch(fromIncl, uptoExcl, (int) search);
  }

  public int bSearch(int fromIncl, int uptoExcl, double search) {
    return bSearch(fromIncl, uptoExcl, (int) search);
  }

  public void rotUp(int fromIncl, int uptoExcl) {
    validateAccess(fromIncl, uptoExcl);
    final int saved = arr[uptoExcl - 1];
    System.arraycopy(arr, fromIncl, arr, fromIncl + 1, uptoExcl - fromIncl - 1);
    arr[fromIncl] = saved;
  }

  public void rotDown(int fromIncl, int uptoExcl) {
    validateAccess(fromIncl, uptoExcl);
    final int saved = arr[fromIncl];
    System.arraycopy(arr, fromIncl + 1, arr, fromIncl, uptoExcl - fromIncl - 1);
    arr[uptoExcl - 1] = saved;
  }

  public void del(int fromIncl, int uptoExcl) {
    StoreUtils.validateAccess(size, fromIncl, uptoExcl);

    final int subSize = uptoExcl - fromIncl;

    //  shift
    System.arraycopy(arr, uptoExcl, arr, fromIncl, size - uptoExcl);

    if (size * 4 < arr.length) {
      final int[] oldArr = arr;
      arr = new int[size * 2];
      System.arraycopy(oldArr, 0, arr, 0, size);
    }

    size -= subSize;
  }

  public double get(int pos, double typeHint) {
    return get(pos, (int) typeHint);
  }

  public double set(int pos, double val) {
    return set(pos, (int) val);
  }

  public float get(int pos, float typeHint) {
    return get(pos, (int) typeHint);
  }

  public float set(int pos, float val) {
    return set(pos, (int) val);
  }

  public long get(int pos, long typeHint) {
    return get(pos, (int) typeHint);
  }

  public long set(int pos, long val) {
    return set(pos, (int) val);
  }

  public int get(int pos, int typeHint) {
    StoreUtils.validateAccess(pos, size);

    return arr[pos];
  }

  public int set(int pos, int val) {
    StoreUtils.validateAccess(pos, size);

    final int oldInt = arr[pos];

    arr[pos] = val;

    return oldInt;
  }

  public byte get(int pos, byte typeHint) {
    return (byte) get(pos, (int) typeHint);
  }

  public byte set(int pos, byte val) {
    return (byte) set(pos, (int) val);
  }

  public boolean get(int pos, boolean typeHint) {
    return get(pos, 0) != 0;
  }

  public boolean set(int pos, boolean val) {
    return set(pos, val ? 1 : 0) != 0;
  }

  public StoreInt fromStream(
      InputStream in
  ) throws IOException {
    size = unescapeInt(in);
    arr = new int[size];
    for (int pos = 0; pos < size; pos++) {
      try {
        arr[pos] = unescapeInt(in);
      } catch (IOException e) {
        throw new IOException("failed at pos " + pos + " of " + size, e);
      }
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
            intBits(arr[pos++], byteBuf);
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
