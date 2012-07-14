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

import org.akraievoy.base.soft.Soft;

import java.util.Arrays;
import java.util.BitSet;

interface Store {
  double def();
  int size();
  void ins(int fromIncl, int uptoExcl);
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

class StoreUtils {
  protected static void validateInsert(final int storeSize, int fromIncl, int uptoExcl) {
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

  protected static void validateDelete(final int storeSize, int fromIncl, int uptoExcl) {
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

  protected static void validateAccess(int pos, final int storeSize) {
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
}

class StoreBit implements Store {
  private BitSet bits;
  private int size;
  private boolean def;

  StoreBit(final int newSize, final boolean newDef) {
    bits = new BitSet(newSize);
    size = newSize;
    def = newDef;

    bits.set(0, size, def);
  }

  public double def() {
    return def ? 1 : 0;
  }

  public int size() {
    return size;
  }

  public void ins(int fromIncl, int uptoExcl) {
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
}

class StoreByte implements Store {
  private byte[] arr;
  private int size;
  private byte def;

  StoreByte(final int newSize, final byte newDef) {
    arr = new byte[newSize];
    size = newSize;
    def = newDef;

    Arrays.fill(arr, 0, size, def);
  }

  public double def() {
    return def;
  }

  public int size() {
    return size;
  }

  public void ins(int fromIncl, int uptoExcl) {
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
    return get(pos, (byte) 0) != def;
  }

  public boolean set(int pos, boolean val) {
    return set(pos, val ? (byte) 1 : (byte) 0) != def;
  }
}

class StoreInt implements Store {
  private int[] arr;
  private int size;
  private int def;

  StoreInt(final int newSize, final int newDef) {
    arr = new int[newSize];
    size = newSize;
    def = newDef;

    Arrays.fill(arr, 0, size, def);
  }

  public double def() {
    return def;
  }

  public int size() {
    return size;
  }

  public void ins(int fromIncl, int uptoExcl) {
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
    return get(pos, (int) 0) != def;
  }

  public boolean set(int pos, boolean val) {
    return set(pos, val ? (int) 1 : (int) 0) != def;
  }
}

class StoreLong implements Store {
  private long[] arr;
  private int size;
  private long def;

  StoreLong(final int newSize, final long newDef) {
    arr = new long[newSize];
    size = newSize;
    def = newDef;

    Arrays.fill(arr, 0, size, def);
  }

  public double def() {
    return def;
  }

  public int size() {
    return size;
  }

  public void ins(int fromIncl, int uptoExcl) {
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
    return get(pos, (long) 0) != def;
  }

  public boolean set(int pos, boolean val) {
    return set(pos, val ? (long) 1 : (long) 0) != def ;
  }
}

class StoreFloat implements Store {
  private float[] arr;
  private int size;
  private float def;

  StoreFloat(final int newSize, final float newDef) {
    arr = new float[newSize];
    size = newSize;
    def = newDef;

    Arrays.fill(arr, 0, size, def);
  }

  public double def() {
    return def;
  }

  public int size() {
    return size;
  }

  public void ins(int fromIncl, int uptoExcl) {
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
    return Float.compare(get(pos, (float) 0), def) != 0;
  }

  public boolean set(int pos, boolean val) {
    return Float.compare(set(pos, val ? (float) 1 : (float) 0), def) != 0 ;
  }
}

class StoreDouble implements Store {
  private double[] arr;
  private int size;
  private double def;

  StoreDouble(final int newSize, final double newDef) {
    arr = new double[newSize];
    size = newSize;
    def = newDef;

    Arrays.fill(arr, 0, size, def);
  }

  public double def() {
    return def;
  }

  public int size() {
    return size;
  }

  public void ins(int fromIncl, int uptoExcl) {
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
    return Double.compare(get(pos, (double) 0), def) != 0;
  }

  public boolean set(int pos, boolean val) {
    return Double.compare(set(pos, val ? (double) 1 : (double) 0), def) != 0;
  }
}

public class CNetLocal {
  protected static void testArray() {
    final StoreByte store = new StoreByte(4, (byte) 0);

    assert store.size() == 4;
    assert !store.get(0, false);
    assert !store.set(0, true);
    assert store.get(0, false);

    assert store.get(0, (byte) 0) == 1;
    assert store.get(1, (byte) 0) == 0;

    assert store.set(3, (byte) 3) == 0;
    assert store.get(3, (byte) 0) == 3;

    store.ins(0, 1);

    assert store.get(0, (byte) 0) == 0;
    assert store.get(1, (byte) 0) == 1;
    assert store.get(2, (byte) 0) == 0;
    assert store.get(3, (byte) 0) == 0;
    assert store.get(4, (byte) 0) == 3;

    store.ins(2, 3);

    assert store.get(0, (byte) 0) == 0;
    assert store.get(1, (byte) 0) == 1;
    assert store.get(2, (byte) 0) == 0;
    assert store.get(3, (byte) 0) == 0;
    assert store.get(4, (byte) 0) == 0;
    assert store.get(5, (byte) 0) == 3;

    store.ins(5, 6);

    assert store.get(0, (byte) 0) == 0;
    assert store.get(1, (byte) 0) == 1;
    assert store.get(2, (byte) 0) == 0;
    assert store.get(3, (byte) 0) == 0;
    assert store.get(4, (byte) 0) == 0;
    assert store.get(5, (byte) 0) == 0;
    assert store.get(6, (byte) 0) == 3;

    store.ins(7, 8);

    assert store.get(0, (byte) 0) == 0;
    assert store.get(1, (byte) 0) == 1;
    assert store.get(2, (byte) 0) == 0;
    assert store.get(3, (byte) 0) == 0;
    assert store.get(4, (byte) 0) == 0;
    assert store.get(5, (byte) 0) == 0;
    assert store.get(6, (byte) 0) == 3;
    assert store.get(7, (byte) 0) == 0;

    store.set(3, (byte) 23);
    store.set(4, (byte) 32);
    store.ins(4, 5000 + 5000 + 4);

    assert store.size() == 5000 + 5000 + 8;
    assert store.get(1, (byte) 0) == 1;
    assert store.get(3, (byte) 0) == 23;
    assert store.get(5000 + 5000 + 4, (byte) 0) == 32;
    assert store.get(5000 + 5000 + 6, (byte) 0) == 3;

    assert store.set(4 + 5000 - 1, (byte) 34) == 0;
    assert store.set(4 + 5000, (byte) 43) == 0;

    store.del(4, 4 + 5000 - 1);

    assert store.get(3, (byte) 0) == 23;
    assert store.get(4, (byte) 0) == 34;
    assert store.get(5, (byte) 0) == 43;

    store.del(6, 6 + 5000 - 1);

    assert store.get(3, (byte) 0) == 23;
    assert store.get(4, (byte) 0) == 34;
    assert store.get(5, (byte) 0) == 43;
    assert store.get(6, (byte) 0) == 32;

    store.del(9, 10);
    store.del(0, 1);

    assert store.get(0, (byte) 0) == 1;
    assert store.get(2, (byte) 0) == 23;
    assert store.get(3, (byte) 0) == 34;
    assert store.get(4, (byte) 0) == 43;
    assert store.get(5, (byte) 0) == 32;
    assert store.get(7, (byte) 0) == 3;

    store.del(6, 7);
    store.del(1, 2);

    assert store.get(0, (byte) 0) == 1;
    assert store.get(1, (byte) 0) == 23;
    assert store.get(2, (byte) 0) == 34;
    assert store.get(3, (byte) 0) == 43;
    assert store.get(4, (byte) 0) == 32;
    assert store.get(5, (byte) 0) == 3;

    assert store.size() == 6;

    System.out.println("array-based storage MAY work");
  }

  protected static void testBit() {
    final StoreBit store = new StoreBit(4, false);

    assert store.size() == 4;
    assert !store.get(0, false);
    assert !store.set(0, true);
    assert store.get(0, false);

    assert store.get(0, false);
    assert !store.get(1, false);

    assert !store.set(3, true);
    assert store.get(3, false);

    store.ins(0, 1);

    assert !store.get(0, false);
    assert store.get(1, false);
    assert !store.get(2, false);
    assert !store.get(3, false);
    assert store.get(4, false);

    store.ins(2, 3);

    assert !store.get(0, false);
    assert store.get(1, false);
    assert !store.get(2, false);
    assert !store.get(3, false);
    assert !store.get(4, false);
    assert store.get(5, false);

    store.ins(5, 6);

    assert !store.get(0, false);
    assert store.get(1, false);
    assert !store.get(2, false);
    assert !store.get(3, false);
    assert !store.get(4, false);
    assert !store.get(5, false);
    assert store.get(6, false);

    store.ins(7, 8);

    assert !store.get(0, false);
    assert store.get(1, false);
    assert !store.get(2, false);
    assert !store.get(3, false);
    assert !store.get(4, false);
    assert !store.get(5, false);
    assert store.get(6, false);
    assert !store.get(7, false);

    store.set(3, true);
    store.set(4, true);
    store.ins(4, 5000 + 5000 + 4);

    assert store.size() == 5000 + 5000 + 8;
    assert store.get(1, false);
    assert store.get(3, false);
    assert store.get(5000 + 5000 + 4, false);
    assert store.get(5000 + 5000 + 6, false);

    assert !store.set(4 + 5000 - 1, true);
    assert !store.set(4 + 5000, true);

    store.del(4, 4 + 5000 - 1);

    assert store.get(3, false);
    assert store.get(4, false);
    assert store.get(5, false);

    store.del(6, 6 + 5000 - 1);

    assert store.get(3, false);
    assert store.get(4, false);
    assert store.get(5, false);
    assert store.get(6, false);

    store.del(9, 10);
    store.del(0, 1);

    assert store.get(0, false);
    assert !store.get(1, false);
    assert store.get(2, false);
    assert store.get(3, false);
    assert store.get(4, false);
    assert store.get(5, false);
    assert !store.get(6, false);
    assert store.get(7, false);

    store.del(6, 7);
    store.del(1, 2);

    assert store.get(0, false);
    assert store.get(1, false);
    assert store.get(2, false);
    assert store.get(3, false);
    assert store.get(4, false);
    assert store.get(5, false);

    assert store.size() == 6;

    System.out.println("bitset-based storage MAY work");
  }

  public static void main(String[] args) {
    testArray();
    testBit();
  }
}