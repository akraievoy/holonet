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

import com.google.common.io.Closeables;
import com.google.common.io.InputSupplier;
import org.akraievoy.base.soft.Soft;
import org.akraievoy.db.Streamable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.BitSet;

import static org.akraievoy.cnet.net.vo.StoreUtils.*;

@SuppressWarnings("UnusedDeclaration")
interface Store extends Streamable {
  enum Width { BIT, BYTE, INT, LONG, FLOAT, DOUBLE }

  Width width();
  int size();
  void ins(int fromIncl, int uptoExcl, boolean def);
  void ins(int fromIncl, int uptoExcl, byte def);
  void ins(int fromIncl, int uptoExcl, int def);
  void ins(int fromIncl, int uptoExcl, long def);
  void ins(int fromIncl, int uptoExcl, float def);
  void ins(int fromIncl, int uptoExcl, double def);
  void del(int fromIncl, int uptoExcl);

  double get(int pos, double typeHint);
  double set(int pos, double val);
  float get(int pos, float typeHint);
  float set(int pos, float val);
  long get(int pos, long typeHint);
  long set(int pos, long val);
  int get(int pos, int typeHint);
  int set(int pos, int val);
  byte get(int pos, byte typeHint);
  byte set(int pos, byte val);
  boolean get(int pos, boolean typeHint);
  boolean set(int pos, boolean val);
}

@SuppressWarnings("UnusedDeclaration")
class StoreUtils {
  protected static void validateInsert(
      final int storeSize,
      final int fromIncl,
      final int uptoExcl
  ) {
    if (fromIncl > storeSize) {
      throw new IllegalArgumentException(
          "fromIncl(" + fromIncl + ") > size(" + storeSize + ")"
      );
    }

    if (uptoExcl < fromIncl) {
      throw new IllegalArgumentException(
          "uptoExcl(" + uptoExcl + ") < fromIncl(" + fromIncl + ")"
      );
    }

    if (fromIncl < 0) {
      throw new IllegalArgumentException(
          "uptoExcl(" + fromIncl + ") < 0"
      );
    }
  }

  protected static void validateDelete(
      final int storeSize,
      final int fromIncl,
      final int uptoExcl
  ) {
    if (uptoExcl > storeSize) {
      throw new IllegalArgumentException(
          "uptoExcl(" + uptoExcl + ") > size(" + storeSize + ")"
      );
    }

    if (fromIncl >= storeSize) {
      throw new IllegalArgumentException(
          "fromIncl(" + fromIncl + ") >= size(" + storeSize + ")"
      );
    }

    if (fromIncl > uptoExcl) {
      throw new IllegalArgumentException(
          "fromIncl(" + fromIncl + ") >= uptoExcl(" + uptoExcl + ")"
      );
    }

    if (fromIncl < 0) {
      throw new IllegalArgumentException(
          "uptoExcl(" + fromIncl + ") < 0"
      );
    }
  }

  protected static void validateAccess(
      final int pos,
      final int storeSize
  ) {
    if (pos >= storeSize) {
      throw new IllegalArgumentException(
          "pos("+pos + ") >= size(" + storeSize + ")"
      );
    }

    if (pos < 0) {
      throw new IllegalArgumentException(
          "pos("+pos + ") < " + 0
      );
    }
  }

  public static void intBits(final int val, final byte[] dest) {
    dest[3] = (byte) val;
    dest[2] = (byte) (val >> 8);
    dest[1] = (byte) (val >> 16);
    dest[0] = (byte) (val >> 24);
  }

  public static void longBits(final long val, final byte[] dest) {
    dest[7] = (byte) val;
    dest[6] = (byte) (val >> 8);
    dest[5] = (byte) (val >> 16);
    dest[4] = (byte) (val >> 24);
    dest[3] = (byte) (val >> 32);
    dest[2] = (byte) (val >> 40);
    dest[1] = (byte) (val >> 48);
    dest[0] = (byte) (val >> 56);
  }

  public static int escapeByte(final byte res) {
    return ((int) res) & 0xFF;
  }

  public static byte unescapeByte(final InputStream input) throws IOException {
    int readValue = input.read();

    if (readValue < 0) {
      throw new IOException("should have read a byte successfully");
    }

    return (byte) readValue;
  }

  public static int unescapeInt(InputStream input) throws IOException {
    return
        (unescapeByte(input) << 24) |
            ((unescapeByte(input) << 16) & 0xFF0000) |
            ((unescapeByte(input) << 8) & 0xFF00) |
            (unescapeByte(input) & 0xFF);
  }

  public static long unescapeLong(InputStream input) throws IOException {
    return
        ((long) unescapeInt(input) << 32) |
            (unescapeInt(input) & 0xFFFFFFFFL);
  }

  enum StreamState {SIZE, DATA, COMPLETE }
}

@SuppressWarnings("UnusedDeclaration")
class StoreBit implements Store {
  private BitSet bits;
  private int size;

  StoreBit() {
    this(0, false);
  }

  StoreBit(final int newSize, boolean newDef) {
    bits = new BitSet(newSize);
    size = newSize;

    bits.set(0, size, newDef);
  }

  public Width width() {
    return Width.BIT;
  }

  public int size() {
    return size;
  }

  public void ins(int fromIncl, int uptoExcl, boolean def) {
    StoreUtils.validateInsert(size, fromIncl, uptoExcl);

    int subSize = uptoExcl - fromIncl;

    //  shift
    for (int i = size - 1; i >= fromIncl; i--) {
      bits.set(i + subSize, bits.get(i));
    }

    //  def out new
    bits.set(fromIncl, uptoExcl, def);

    size += subSize;
  }

  public void ins(int fromIncl, int uptoExcl, byte def) {
    ins(fromIncl, uptoExcl, def > 0);
  }

  public void ins(int fromIncl, int uptoExcl, int def) {
    ins(fromIncl, uptoExcl, def > 0);
  }

  public void ins(int fromIncl, int uptoExcl, long def) {
    ins(fromIncl, uptoExcl, def > 0);
  }

  public void ins(int fromIncl, int uptoExcl, float def) {
    ins(fromIncl, uptoExcl, Soft.PICO.positive(def));
  }

  public void ins(int fromIncl, int uptoExcl, double def) {
    ins(fromIncl, uptoExcl, Soft.PICO.positive(def));
  }

  public void del(int fromIncl, int uptoExcl) {
    StoreUtils.validateDelete(size, fromIncl, uptoExcl);

    final int subSize = uptoExcl - fromIncl;

    //  shift
    for (int i = uptoExcl; i < size; i++) {
      bits.set(i - subSize, bits.get(i));
    }

    size -= subSize;
  }

  public double get(int pos, double typeHint) {
    return get(pos, false) ? 1.0 : 0.0;
  }

  public double set(int pos, double val) {
    return set(pos, Soft.PICO.positive(val)) ? 1.0 : 0.0;
  }

  public float get(int pos, float typeHint) {
    return get(pos, false) ? 1.0f : 0.0f;
  }

  public float set(int pos, float val) {
    //  LATER un-iceness: we're upcasting to double from float for simple comparison
    return set(pos, Soft.PICO.positive(val)) ? 1.0f : 0.0f;
  }

  public long get(int pos, long typeHint) {
    return get(pos, false) ? 1L : 0L;
  }

  public long set(int pos, long val) {
    return set(pos, val > 0) ? 1L : 0L;
  }

  public int get(int pos, int typeHint) {
    return get(pos, false) ? 1 : 0;
  }

  public int set(int pos, int val) {
    return set(pos, val > 0) ? 1 : 0;
  }

  public byte get(int pos, byte typeHint) {
    return get(pos, false) ? (byte) 1 : (byte) 0;
  }

  public byte set(int pos, byte val) {
    return set(pos, val > 0) ? (byte) 1 : (byte) 0;
  }

  public boolean get(int pos, boolean typeHint) {
    StoreUtils.validateAccess(pos, size);

    return bits.get(pos);
  }

  public boolean set(int pos, boolean val) {
    StoreUtils.validateAccess(pos, size);

    boolean oldVal = bits.get(pos);

    bits.set(pos, val);

    return oldVal;
  }

  public void fromStream(
      InputSupplier<? extends InputStream> inputSupplier
  ) throws IOException {
    InputStream input = null;
    try {
      input = inputSupplier.getInput();

      size = unescapeInt(input);

      byte buf = 0;
      for (int pos = 0; pos < size; pos++) {
        if (pos % 8 == 0) {
          buf = unescapeByte(input);
        }

        bits.set(pos, buf < 0);

        buf <<= 1;
      }
    } finally {
      Closeables.closeQuietly(input);
    }
  }

  public InputSupplier<InputStream> toStream() {
    return new InputSupplier<InputStream>() {
      private StreamState state = StreamState.SIZE;
      private int sizePos = 0;
      private byte[] sizeBits = new byte[4];
      private int pos = 0;

      public InputStream getInput() throws IOException {
        return new InputStream() {
          @Override
          public int read() throws IOException {
            if (state == StreamState.SIZE) {
              if (sizePos == 0) {
                intBits(size, sizeBits);
              }
              final byte res = sizeBits[sizePos++];
              if (sizePos == sizeBits.length) {
                state = size > 0 ? StreamState.DATA : StreamState.COMPLETE;
              }
              return escapeByte(res);
            } else if (state == StreamState.DATA) {
              byte res = 0;
              int posOld = pos;
              while (pos - posOld < 8) {
                res = (byte) ((res << 1) | (bits.get(pos) ? 1 : 0));
                pos ++ ;
              }
              if (pos >= size) {
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
    };
  }
}

@SuppressWarnings("UnusedDeclaration")
class StoreByte implements Store {
  private byte[] arr;
  private int size;

  StoreByte() {
    this(0, (byte) 0);
  }

  StoreByte(final int newSize, final byte newDef) {
    arr = new byte[newSize];
    size = newSize;

    Arrays.fill(arr, 0, size, newDef);
  }

  public Width width() {
    return Width.BYTE;
  }

  public int size() {
    return size;
  }

  public void ins(int fromIncl, int uptoExcl, byte def) {
    StoreUtils.validateInsert(size, fromIncl, uptoExcl);

    final int subSize = uptoExcl - fromIncl;

    //  expand storage
    int newLength = arr.length;
    while (newLength < size + subSize) {
      newLength *= 2;
    }

    if (newLength > arr.length) {
      final byte[] oldArr = arr;
      arr = new byte[newLength];
      System.arraycopy(oldArr, 0, arr, 0, oldArr.length);
    }

    //  shift
    System.arraycopy(arr, fromIncl, arr, fromIncl + subSize, size - fromIncl);

    //  zero out new
    Arrays.fill(arr, fromIncl, uptoExcl, def);

    size += subSize;
  }

  public void ins(int fromIncl, int uptoExcl, boolean def) {
    ins(fromIncl, uptoExcl, def ? (byte) 1 : (byte) 0);
  }

  public void ins(int fromIncl, int uptoExcl, int def) {
    ins(fromIncl, uptoExcl, (byte) def);
  }

  public void ins(int fromIncl, int uptoExcl, long def) {
    ins(fromIncl, uptoExcl, (byte) def);
  }

  public void ins(int fromIncl, int uptoExcl, float def) {
    ins(fromIncl, uptoExcl, (byte) def);
  }

  public void ins(int fromIncl, int uptoExcl, double def) {
    ins(fromIncl, uptoExcl, (byte) def);
  }

  public void del(int fromIncl, int uptoExcl) {
    StoreUtils.validateDelete(size, fromIncl, uptoExcl);

    final int subSize = uptoExcl - fromIncl;

    //  shift
    System.arraycopy(arr, uptoExcl, arr, fromIncl, size - uptoExcl);

    if (size * 4 < arr.length) {
      final byte[] oldArr = arr;
      arr = new byte[size * 2];
      System.arraycopy(oldArr, 0, arr, 0, size);
    }

    size -= subSize;
  }

  public double get(int pos, double typeHint) {
    return get(pos, (byte) typeHint);
  }

  public double set(int pos, double val) {
    return set(pos, (byte) val);
  }

  public float get(int pos, float typeHint) {
    return get(pos, (byte) typeHint);
  }

  public float set(int pos, float val) {
    return set(pos, (byte) val);
  }

  public long get(int pos, long typeHint) {
    return get(pos, (byte) typeHint);
  }

  public long set(int pos, long val) {
    return set(pos, (byte) val);
  }

  public int get(int pos, int typeHint) {
    return get(pos, (byte) typeHint);
  }

  public int set(int pos, int val) {
    return set(pos, (byte) val);
  }

  public byte get(int pos, byte typeHint) {
    StoreUtils.validateAccess(pos, size);

    return arr[pos];
  }

  public byte set(int pos, byte val) {
    StoreUtils.validateAccess(pos, size);

    final byte oldByte = arr[pos];

    arr[pos] = val;

    return oldByte;
  }

  public boolean get(int pos, boolean typeHint) {
    return get(pos, (byte) 0) != 0;
  }

  public boolean set(int pos, boolean val) {
    return set(pos, val ? (byte) 1 : (byte) 0) != 0;
  }

  public void fromStream(
      InputSupplier<? extends InputStream> inputSupplier
  ) throws IOException {
    InputStream input = null;
    try {
      input = inputSupplier.getInput();

      size = unescapeInt(input);
      arr = new byte[size];
      for (int pos = 0; pos < size; pos++) {
        arr[pos] = unescapeByte(input);
      }
    } finally {
      Closeables.closeQuietly(input);
    }
  }

  public InputSupplier<InputStream> toStream() {
    return new InputSupplier<InputStream>() {
      private StreamState state = StreamState.SIZE;
      private int sizePos = 0;
      private byte[] sizeBits = new byte[4];
      private int pos = 0;

      public InputStream getInput() throws IOException {
        return new InputStream() {
          @Override
          public int read() throws IOException {
            if (state == StreamState.SIZE) {
              if (sizePos == 0) {
                intBits(size, sizeBits);
              }
              final byte res = sizeBits[sizePos++];
              if (sizePos == sizeBits.length) {
                state = size > 0 ? StreamState.DATA : StreamState.COMPLETE;
              }
              return escapeByte(res);
            } else if (state == StreamState.DATA) {
              byte res = arr[pos++];
              if (pos == size) {
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
    };
  }
}

@SuppressWarnings("UnusedDeclaration")
class StoreInt implements Store {
  private int[] arr;
  private int size;

  StoreInt() {
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
      newLength *= 2;
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

  public void del(int fromIncl, int uptoExcl) {
    StoreUtils.validateDelete(size, fromIncl, uptoExcl);

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

  public void fromStream(
      InputSupplier<? extends InputStream> inputSupplier
  ) throws IOException {
    InputStream input = null;
    try {
      input = inputSupplier.getInput();

      size = unescapeInt(input);
      arr = new int[size];
      for (int pos = 0; pos < size; pos++) {
        arr[pos] = unescapeInt(input);
      }
    } finally {
      Closeables.closeQuietly(input);
    }
  }

  public InputSupplier<InputStream> toStream() {
    return new InputSupplier<InputStream>() {
      private StreamState state = StreamState.SIZE;
      private int sizePos = 0;
      private byte[] sizeBits = new byte[4];
      private int pos = 0;
      private int byteBufPos = 0;
      private byte[] byteBuf = new byte[4];

      public InputStream getInput() throws IOException {
        return new InputStream() {
          @Override
          public int read() throws IOException {
            if (state == StreamState.SIZE) {
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
    };
  }
}

@SuppressWarnings("UnusedDeclaration")
class StoreLong implements Store {
  private long[] arr;
  private int size;

  StoreLong() {
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
      newLength *= 2;
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

  public void del(int fromIncl, int uptoExcl) {
    StoreUtils.validateDelete(size, fromIncl, uptoExcl);

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

  public void fromStream(
      InputSupplier<? extends InputStream> inputSupplier
  ) throws IOException {
    InputStream input = null;
    try {
      input = inputSupplier.getInput();

      size = unescapeInt(input);
      arr = new long[size];
      for (int pos = 0; pos < size; pos++) {
        arr[pos] = unescapeLong(input);
      }
    } finally {
      Closeables.closeQuietly(input);
    }
  }

  public InputSupplier<InputStream> toStream() {
    return new InputSupplier<InputStream>() {
      private StreamState state = StreamState.SIZE;
      private int sizePos = 0;
      private byte[] sizeBits = new byte[4];
      private int pos = 0;
      private int byteBufPos = 0;
      private byte[] byteBuf = new byte[8];

      public InputStream getInput() throws IOException {
        return new InputStream() {
          @Override
          public int read() throws IOException {
            if (state == StreamState.SIZE) {
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
    };
  }
}

@SuppressWarnings("UnusedDeclaration")
class StoreFloat implements Store {
  private float[] arr;
  private int size;

  StoreFloat() {
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
      newLength *= 2;
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
    ins(fromIncl,uptoExcl,(float) def);
  }

  public void ins(int fromIncl, int uptoExcl, int def) {
    ins(fromIncl,uptoExcl,(float) def);
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

  public void del(int fromIncl, int uptoExcl) {
    StoreUtils.validateDelete(size, fromIncl, uptoExcl);

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

  public void fromStream(
      InputSupplier<? extends InputStream> inputSupplier
  ) throws IOException {
    InputStream input = null;
    try {
      input = inputSupplier.getInput();
      size = unescapeInt(input);
      arr = new float[size];
      for (int pos = 0; pos < size; pos++) {
        arr[pos] = Float.intBitsToFloat(unescapeInt(input));
      }
    } finally {
      Closeables.closeQuietly(input);
    }
  }

  public InputSupplier<InputStream> toStream() {
    return new InputSupplier<InputStream>() {
      private StreamState state = StreamState.SIZE;
      private int sizePos = 0;
      private byte[] sizeBits = new byte[4];
      private int pos = 0;
      private int byteBufPos = 0;
      private byte[] byteBuf = new byte[4];

      public InputStream getInput() throws IOException {
        return new InputStream() {
          @Override
          public int read() throws IOException {
            if (state == StreamState.SIZE) {
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
    };
  }
}

@SuppressWarnings("UnusedDeclaration")
class StoreDouble implements Store {
  private double[] arr;
  private int size;

  StoreDouble() {
    this(0, 0);
  }

  StoreDouble(final int newSize, final double newDef) {
    arr = new double[newSize];
    size = newSize;

    Arrays.fill(arr, 0, size, newDef);
  }

  public Width width() {
    return Width.DOUBLE;
  }

  public int size() {
    return size;
  }

  public void ins(int fromIncl, int uptoExcl, double def) {
    StoreUtils.validateInsert(size, fromIncl, uptoExcl);

    final int subSize = uptoExcl - fromIncl;

    //  expand storage
    int newLength = arr.length;
    while (newLength < size + subSize) {
      newLength *= 2;
    }

    if (newLength > arr.length) {
      final double[] oldArr = arr;
      arr = new double[newLength];
      System.arraycopy(oldArr, 0, arr, 0, oldArr.length);
    }

    //  shift
    System.arraycopy(arr, fromIncl, arr, fromIncl + subSize, size - fromIncl);

    //  zero out new
    Arrays.fill(arr, fromIncl, uptoExcl, def);

    size += subSize;
  }

  public void ins(int fromIncl, int uptoExcl, long def) {
    ins(fromIncl, uptoExcl, (double) def);
  }

  public void ins(int fromIncl, int uptoExcl, int def) {
    ins(fromIncl, uptoExcl, (double) def);
  }

  public void ins(int fromIncl, int uptoExcl, float def) {
    ins(fromIncl, uptoExcl, (double) def);
  }

  public void ins(int fromIncl, int uptoExcl, byte def) {
    ins(fromIncl, uptoExcl, (double) def);
  }

  public void ins(int fromIncl, int uptoExcl, boolean def) {
    ins(fromIncl, uptoExcl, def ? 1.0 : .0);
  }

  public void del(int fromIncl, int uptoExcl) {
    StoreUtils.validateDelete(size, fromIncl, uptoExcl);

    final int subSize = uptoExcl - fromIncl;

    //  shift
    System.arraycopy(arr, uptoExcl, arr, fromIncl, size - uptoExcl);

    if (size * 4 < arr.length) {
      final double[] oldArr = arr;
      arr = new double[size * 2];
      System.arraycopy(oldArr, 0, arr, 0, size);
    }

    size -= subSize;
  }

  public double get(int pos, double typeHint) {
    StoreUtils.validateAccess(pos, size);

    return arr[pos];
  }

  public double set(int pos, double val) {
    StoreUtils.validateAccess(pos, size);

    final double oldDouble = arr[pos];

    arr[pos] = val;

    return oldDouble;
  }

  public float get(int pos, float typeHint) {
    return (float) get(pos, (double) typeHint);
  }

  public float set(int pos, float val) {
    return (float) set(pos, (double) val);
  }

  public long get(int pos, long typeHint) {
    return (long) get(pos, (double) typeHint);
  }

  public long set(int pos, long val) {
    return (long) set(pos, (double) val);
  }

  public int get(int pos, int typeHint) {
    return (int) get(pos, (double) typeHint);
  }

  public int set(int pos, int val) {
    return (int) set(pos, (double) val);
  }

  public byte get(int pos, byte typeHint) {
    return (byte) get(pos, (double) typeHint);
  }

  public byte set(int pos, byte val) {
    return (byte) set(pos, (double) val);
  }

  public boolean get(int pos, boolean typeHint) {
    return Double.compare(get(pos, (double) 0), .0) != 0;
  }

  public boolean set(int pos, boolean val) {
    return Double.compare(set(pos, val ? (double) 1 : (double) 0), .0) != 0;
  }

    public void fromStream(
        InputSupplier<? extends InputStream> inputSupplier
    ) throws IOException {
    InputStream input = null;
    try {
      input = inputSupplier.getInput();

      size = unescapeInt(input);
      arr = new double[size];
      for (int pos = 0; pos < size; pos++) {
        arr[pos] = Double.longBitsToDouble(unescapeLong(input));
      }
    } finally {
      Closeables.closeQuietly(input);
    }
  }

  public InputSupplier<InputStream> toStream() {
    return new InputSupplier<InputStream>() {
      private StreamState state = StreamState.SIZE;
      private int sizePos = 0;
      private byte[] sizeBits = new byte[4];
      private int pos = 0;
      private int byteBufPos = 0;
      private byte[] byteBuf = new byte[8];

      public InputStream getInput() throws IOException {
        return new InputStream() {
          @Override
          public int read() throws IOException {
            if (state == StreamState.SIZE) {
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
                longBits(Double.doubleToLongBits(arr[pos++]), byteBuf);
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
    };
  }
}