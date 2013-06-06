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

import com.google.common.io.ByteStreams;
import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class EdgeDataSparseTest extends TestCase {
  final EdgeData eData = EdgeDataFactory.sparse(true, 0.0, 5);  //  note: this might not be enough for some tests

  public void setUp() {
    eData.set(3, 4, 12.0);
    eData.set(4, 2, 11.0);
  }

  public void testNonDefCount() {
    assertEquals(4, eData.getNonDefCount());
  }

  public void testGet() throws Exception {
    assertEquals(4, eData.getNonDefCount());

    assertEquals(12.0, eData.get(3, 4));
    assertEquals(11.0, eData.get(2, 4));
    assertEquals(0.0, eData.get(1, 3));

    assertEquals(12.0, eData.get(4, 3));
    assertEquals(11.0, eData.get(4, 2));
    assertEquals(0.0, eData.get(3, 1));

    assertEquals(0.0, eData.get(2, 2));
    assertEquals(0.0, eData.get(2, 1));
    assertEquals(0.0, eData.get(1, 2));

    eData.set(1, 3, 13.0);

    assertEquals(6, eData.getNonDefCount());

    assertEquals(12.0, eData.get(3, 4));
    assertEquals(11.0, eData.get(2, 4));
    assertEquals(13.0, eData.get(1, 3));

    assertEquals(12.0, eData.get(4, 3));
    assertEquals(11.0, eData.get(4, 2));
    assertEquals(13.0, eData.get(3, 1));

    assertEquals(0.0, eData.get(2, 2));
    assertEquals(0.0, eData.get(2, 1));
    assertEquals(0.0, eData.get(1, 2));

    final EdgeData.ElemIterator ndi = eData.nonDefIterator();

    checkTuple(ndi, 1, 3, 13.0);
    checkTuple(ndi, 2, 4, 11.0);
    checkTuple(ndi, 3, 1, 13.0);
    checkTuple(ndi, 3, 4, 12.0);
    checkTuple(ndi, 4, 2, 11.0);
    checkTuple(ndi, 4, 3, 12.0);
    assertFalse(ndi.hasNext());

    assertEquals(13.0, eData.set(1, 3, 0.0));
    assertEquals(0.0, eData.get(1, 3));

    assertEquals(4, eData.getNonDefCount());
  }

  private static void checkTuple(EdgeData.ElemIterator ndi, final int from, final int into, final double value) {
    assertTrue(ndi.hasNext());
    final EdgeData.IteratorTuple tuple = ndi.next();
    assertEquals(from, tuple.from());
    assertEquals(into, tuple.into());
    assertEquals(value, tuple.value());
  }

  public void testGetSize() {
    eData.set(1, 3, 13.0);
    assertEquals(5, eData.getSize());
  }

  public void testPower() {
    assertEquals(23.0, eData.power(4));
    assertEquals(11.0, eData.power(2));
    assertEquals(12.0, eData.power(3));
    assertEquals(0.0, eData.power(1));
    assertEquals(0.0, eData.power(0));

    eData.set(3, 1, 2.0);

    assertEquals(6, eData.getNonDefCount());

    assertEquals(23.0, eData.power(4));
    assertEquals(11.0, eData.power(2));
    assertEquals(14.0, eData.power(3));
    assertEquals(2.0, eData.power(1));
    assertEquals(0.0, eData.power(0));

    eData.set(0, 0, 1.0);

    assertEquals(7, eData.getNonDefCount());

    assertEquals(23.0, eData.power(4));
    assertEquals(11.0, eData.power(2));
    assertEquals(14.0, eData.power(3));
    assertEquals(2.0, eData.power(1));
    assertEquals(1.0, eData.power(0));

    eData.set(4, 4, 3.0);

    assertEquals(8, eData.getNonDefCount());

    assertEquals(26.0, eData.power(4));
    assertEquals(11.0, eData.power(2));
    assertEquals(14.0, eData.power(3));
    assertEquals(2.0, eData.power(1));
    assertEquals(1.0, eData.power(0));
  }

  public void testSetAndClear() {
    final Random random = new Random(0xDEADBEEF);
    final int size = 128;
    final EdgeData d = EdgeDataFactory.sparse(true, 0.0, size);
    final int ops = size * size / 3;
    final int[] froms = new int[ops];
    final int[] intos = new int[ops];

    for (int op = 0; op < ops; op++) {
      d.set(
          froms[op] = random.nextInt(size),
          intos[op] = random.nextInt(size),
          1.0
      );
    }

    assertTrue(d.getNonDefCount() > 0);

    for (int op = 0; op < ops; op++) {
      //  yup: the data is symmetric so it's akay to swap the indices
      d.set(
          intos[op],
          froms[op],
          0.0
      );
    }

    for (int from = 0; from < size; from ++ ) {
      for (int into = 0; into < size; into ++ ) {
        assertEquals(0.0, d.get(from, into));
      }
    }

    assertEquals(0, d.getNonDefCount());
  }

  public void testJsonSerialization() throws IOException {
    EdgeData edd = EdgeDataFactory.sparse(false, Double.POSITIVE_INFINITY, 4);

    edd.set(1, 1, 0.0);
    edd.set(0, 1, 1.0);
    edd.set(1, 2, 2.0);
    edd.set(3, 2, 3.0);

    final byte[] bytes = ByteStreams.toByteArray(edd.createStream());

    final EdgeData res = new EdgeDataSparse().fromStream(new ByteArrayInputStream(bytes));

    final byte[] bytes2 = ByteStreams.toByteArray(res.createStream());

    assertTrue(Arrays.equals(bytes, bytes2));

    final EdgeData res2 = new EdgeDataSparse().fromStream(new ByteArrayInputStream(bytes2));

    assertEquals(1.0, res2.get(0, 1));
    assertEquals(Double.POSITIVE_INFINITY, res2.get(1, 0));

    assertEquals(2.0, res2.get(1, 2));
    assertEquals(Double.POSITIVE_INFINITY, res2.get(2, 1));

    assertEquals(Double.POSITIVE_INFINITY, res2.get(2, 3));
    assertEquals(3.0, res2.get(3, 2));

    assertEquals(Double.POSITIVE_INFINITY, res2.get(3, 3));
    assertEquals(Double.POSITIVE_INFINITY, res2.get(2, 2));
    assertEquals(0.0, res2.get(1, 1));
    assertEquals(Double.POSITIVE_INFINITY, res2.get(0, 0));

    assertEquals(Double.POSITIVE_INFINITY, res2.get(0, 3));
    assertEquals(Double.POSITIVE_INFINITY, res2.get(3, 0));
    assertEquals(Double.POSITIVE_INFINITY, res2.get(0, 2));
    assertEquals(Double.POSITIVE_INFINITY, res2.get(2, 0));
  }

  public void testJsonSerializationRandom() throws IOException {
    for (long seed = 0; seed < 100000; seed++) {
      testJsonSerializationRandom(26, true, Double.POSITIVE_INFINITY, 1234567L + seed, 0.5, 8);
    }
  }

  private void testJsonSerializationRandom(
      final int size0,
      final boolean symmetric0,
      final double defElem0,
      final long seed0,
      final double density0,
      final int passes0) throws IOException {
    final Random random = new Random(seed0);

    final EdgeData ori = EdgeDataFactory.sparse(symmetric0, defElem0, size0);

    for (int pass = 0; pass < passes0; pass++) {
      for (int from = 0; from < size0; from++) {
        for (int into = 0; into < size0; into++) {
          if (random.nextDouble() * passes0 < density0) {
            ori.set(from, into, 0);
          }
        }
      }
    }

    final byte[] bytes = ByteStreams.toByteArray(ori.createStream());

    final EdgeData res = new EdgeDataSparse().fromStream(new ByteArrayInputStream(bytes));

    final byte[] bytes2 = ByteStreams.toByteArray(res.createStream());

    assertTrue(Arrays.equals(bytes, bytes2));

    final EdgeData res2 = new EdgeDataSparse().fromStream(new ByteArrayInputStream(bytes2));

    assertEquals(ori.getSize(), res2.getSize());
    assertEquals(ori.isSymmetric(), res2.isSymmetric());
    assertEquals(ori.hashCode(), res2.hashCode());
    assertEquals(ori.getDefElem(), res2.getDefElem());

    for (int from = 0; from < size0; from++) {
      for (int into = 0; into < size0; into++) {
        assertEquals(ori.get(from, into), res2.get(from, into));
      }
    }

    for (int from = 0; from < size0; from++) {
      for (int into = 0; into < size0; into++) {
        assertEquals(ori.isDef(from, into), res2.isDef(from, into));
      }
    }

    final List<Object[]> visitNonDefRes = new ArrayList<Object[]>();
    ori.visitNonDef(new EdgeData.EdgeVisitor() {
      @Override
      public void visit(final int from, final int into, final double e) {
        visitNonDefRes.add(new Object[]{ from, into, e });
      }
    });

    final int[] resPos = { 0 };
    res2.visitNonDef(new EdgeData.EdgeVisitor() {
      @Override
      public void visit(final int from, final int into, final double e) {
        final Object[] expected = visitNonDefRes.get(resPos[0]);
        assertEquals(((Number) expected[0]).intValue(), from);
        assertEquals(((Number) expected[1]).intValue(), into);
        assertEquals(((Number) expected[2]).doubleValue(), e);
        resPos[0] += 1;
      }
    });
  }
}
