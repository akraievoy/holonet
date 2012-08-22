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

import com.google.common.io.ByteStreams;
import org.junit.Test;

import java.io.IOException;
import java.util.Random;

@SuppressWarnings({"ConstantConditions", "UnusedDeclaration"})
public class StoreTest {
  @Test
  public void testArray() {
    final StoreByte store = new StoreByte(4, (byte) 0);

    assert store.size() == 4;
    assert !store.get(0, false);
    assert !store.set(0, true);
    assert store.get(0, false);

    assert store.get(0, (byte) 0) == 1;
    assert store.get(1, (byte) 0) == 0;

    assert store.set(3, (byte) 3) == 0;
    assert store.get(3, (byte) 0) == 3;

    store.ins(0, 1, false);

    assert store.get(0, (byte) 0) == 0;
    assert store.get(1, (byte) 0) == 1;
    assert store.get(2, (byte) 0) == 0;
    assert store.get(3, (byte) 0) == 0;
    assert store.get(4, (byte) 0) == 3;

    store.ins(2, 3, false);

    assert store.get(0, (byte) 0) == 0;
    assert store.get(1, (byte) 0) == 1;
    assert store.get(2, (byte) 0) == 0;
    assert store.get(3, (byte) 0) == 0;
    assert store.get(4, (byte) 0) == 0;
    assert store.get(5, (byte) 0) == 3;

    store.ins(5, 6, false);

    assert store.get(0, (byte) 0) == 0;
    assert store.get(1, (byte) 0) == 1;
    assert store.get(2, (byte) 0) == 0;
    assert store.get(3, (byte) 0) == 0;
    assert store.get(4, (byte) 0) == 0;
    assert store.get(5, (byte) 0) == 0;
    assert store.get(6, (byte) 0) == 3;

    store.ins(7, 8, false);

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
    store.ins(4, 5000 + 5000 + 4, false);

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

  @Test
  public void testBit() {
    final StoreBit store = new StoreBit(4, false);

    assert store.size() == 4;
    assert !store.get(0, false);
    assert !store.set(0, true);
    assert store.get(0, false);

    assert store.get(0, false);
    assert !store.get(1, false);

    assert !store.set(3, true);
    assert store.get(3, false);

    store.ins(0, 1, false);

    assert !store.get(0, false);
    assert store.get(1, false);
    assert !store.get(2, false);
    assert !store.get(3, false);
    assert store.get(4, false);

    store.ins(2, 3, false);

    assert !store.get(0, false);
    assert store.get(1, false);
    assert !store.get(2, false);
    assert !store.get(3, false);
    assert !store.get(4, false);
    assert store.get(5, false);

    store.ins(5, 6, false);

    assert !store.get(0, false);
    assert store.get(1, false);
    assert !store.get(2, false);
    assert !store.get(3, false);
    assert !store.get(4, false);
    assert !store.get(5, false);
    assert store.get(6, false);

    store.ins(7, 8, false);

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
    store.ins(4, 5000 + 5000 + 4, false);

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

  @Test
  public void testBitStreaming() throws IOException {
    for (int size = 0; size < 1024; size++) {
      for (int def = 0; def < 2; def++) {
        for (int seed = 1001; seed < 1013; seed++) {
          StoreBit store = new StoreBit(size, def == 1);
          Random rand = new Random(seed);
          for (int pos = 0; pos < size; pos++) {
            store.set(pos, rand.nextBoolean());
          }

          final byte[] bytes = ByteStreams.toByteArray(store.toStream());
          final StoreBit storeStar = new StoreBit();
          storeStar.fromStream(ByteStreams.newInputStreamSupplier(bytes));

          final int storeSize = store.size();
          final int starSize = storeStar.size();
          if (storeSize != starSize) {
            throw new IllegalStateException(
                "store should have size = " + storeSize+ " but has size " + starSize
            );
          }
          for (int pos = 0; pos < size; pos++) {
            final boolean orig = store.get(pos, false);
            final boolean star = storeStar.get(pos, false);
            if (star != orig) {
              throw new IllegalStateException(
                  "pos = " + pos + " should be " + orig + " but is " + star
              );
            }
          }
        }
      }
    }

    System.out.println("bitset-based streaming MAY work");
  }

  @Test
  public void testByteStreaming() throws IOException {
    for (int size = 0; size < 1024; size++) {
      for (byte def = -1; def < 2; def++) {
        for (int seed = 1001; seed < 1013; seed++) {
          StoreByte store = new StoreByte(size, def);
          Random rand = new Random(seed);
          for (int pos = 0; pos < size; pos++) {
            if (rand.nextBoolean()) {
              store.set(pos, (byte) rand.nextInt());
            }
          }

          final byte[] bytes = ByteStreams.toByteArray(store.toStream());
          final StoreByte storeStar = new StoreByte();
          storeStar.fromStream(ByteStreams.newInputStreamSupplier(bytes));

          final int storeSize = store.size();
          final int starSize = storeStar.size();
          if (storeSize != starSize) {
            throw new IllegalStateException(
                "store should have size = " + storeSize+ " but has size " + starSize
            );
          }
          for (int pos = 0; pos < size; pos++) {
            final byte orig = store.get(pos, (byte) 0);
            final byte star = storeStar.get(pos, (byte) 0);
            if (star != orig) {
              throw new IllegalStateException(
                  "pos = " + pos + " should be " + orig + " but is " + star
              );
            }
          }
        }
      }
    }

    System.out.println("byte-based streaming MAY work");
  }

  @Test
  public void testDoubleStreaming() throws IOException {
    System.out.println("please wait: this test takes 20 secs on i7...");

    double[] doubleDefs = {
        0, -1, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NaN
    };

    for (int size = 0; size < 1024; size++) {
      for (double doubleDef : doubleDefs) {
        for (int seed = 1001; seed < 1013; seed++) {
          StoreDouble store = new StoreDouble(size, doubleDef);
          Random rand = new Random(seed);
          for (int pos = 0; pos < size; pos++) {
            if (rand.nextBoolean()) {
              store.set(pos, rand.nextDouble());
            }
          }

          final byte[] bytes = ByteStreams.toByteArray(store.toStream());
          final StoreDouble storeStar = new StoreDouble();
          storeStar.fromStream(ByteStreams.newInputStreamSupplier(bytes));

          final int storeSize = store.size();
          final int starSize = storeStar.size();
          if (storeSize != starSize) {
            throw new IllegalStateException(
                "store should have size = " + storeSize+ " but has size " + starSize
            );
          }
          for (int pos = 0; pos < size; pos++) {
            final double orig = store.get(pos, .0);
            final double star = storeStar.get(pos, .0);
            if (Double.compare(star, orig) != 0) {
              throw new IllegalStateException(
                  "pos = " + pos + " should be " + orig + " but is " + star
              );
            }
          }

        }
      }
    }

    System.out.println("double-based streaming MAY work");
  }
}
