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

import java.io.IOException;
import java.io.InputStream;
import java.util.BitSet;

import static org.akraievoy.cnet.net.vo.StoreUtils.*;

@SuppressWarnings("UnusedDeclaration")
public class StoreBit implements Store {
  private BitSet bits;
  private int size;

  public StoreBit() {
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

  public void fill(int fromIncl, int uptoExcl, boolean def) {
    StoreUtils.validateAccess(size, fromIncl, uptoExcl);
    bits.set(fromIncl, uptoExcl, def);
  }

  public void fill(int fromIncl, int uptoExcl, byte def) {
    fill(fromIncl, uptoExcl, def > 0);
  }

  public void fill(int fromIncl, int uptoExcl, int def) {
    fill(fromIncl, uptoExcl, def > 0);
  }

  public void fill(int fromIncl, int uptoExcl, long def) {
    fill(fromIncl, uptoExcl, def > 0);
  }

  public void fill(int fromIncl, int uptoExcl, float def) {
    fill(fromIncl, uptoExcl, Soft.PICO.positive(def));
  }

  public void fill(int fromIncl, int uptoExcl, double def) {
    fill(fromIncl, uptoExcl, Soft.PICO.positive(def));
  }

  public int bSearch(int fromIncl, int uptoExcl, byte search) {
    throw new IllegalStateException("no binary search for StoreBit");
  }

  public int bSearch(int fromIncl, int uptoExcl, int search) {
    throw new IllegalStateException("no binary search for StoreBit");
  }

  public int bSearch(int fromIncl, int uptoExcl, long search) {
    throw new IllegalStateException("no binary search for StoreBit");
  }

  public int bSearch(int fromIncl, int uptoExcl, float search) {
    throw new IllegalStateException("no binary search for StoreBit");
  }

  public int bSearch(int fromIncl, int uptoExcl, double search) {
    throw new IllegalStateException("no binary search for StoreBit");
  }

  public void rotDown(int fromIncl, int uptoExcl) {
    validateAccess(fromIncl, uptoExcl);
    final boolean saved = bits.get(fromIncl);
    for (int pos = fromIncl; pos < uptoExcl - 1; pos++) {
      bits.set(pos, pos + 1);
    }
    bits.set(uptoExcl - 1, saved);
  }

  public void rotUp(int fromIncl, int uptoExcl) {
    validateAccess(fromIncl, uptoExcl);
    final boolean saved = bits.get(uptoExcl - 1);
    for (int pos = uptoExcl - 1; pos > fromIncl; pos--) {
      bits.set(pos, pos - 1);
    }
    bits.set(fromIncl, saved);
  }

  public void del(int fromIncl, int uptoExcl) {
    StoreUtils.validateAccess(size, fromIncl, uptoExcl);

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

  public StoreBit fromStream(
      InputStream in
  ) throws IOException {
    size = unescapeInt(in);

    byte buf = 0;
    for (int pos = 0; pos < size; pos++) {
      if (pos % 8 == 0) {
        buf = unescapeByte(in);
      }

      bits.set(pos, buf < 0);

      buf <<= 1;
    }

    return this;
  }

  public InputStream createStream() {
    return new InputStream() {
      private StoreUtils.StreamState state = StoreUtils.StreamState.SIZE;
      private int sizePos = 0;
      private byte[] sizeBits = new byte[4];
      private int pos = 0;

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
}
