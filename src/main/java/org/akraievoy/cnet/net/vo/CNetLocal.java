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

  protected static void validateAccess(
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
          "fromIncl(" + fromIncl + ") > uptoExcl(" + uptoExcl + ")"
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