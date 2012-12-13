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

package org.akraievoy.cnet.metrics.domain;

import junit.framework.TestCase;
import org.akraievoy.cnet.metrics.vo.Histogram;
import org.akraievoy.cnet.metrics.vo.Stat;
import org.akraievoy.cnet.net.ref.RefEdgeData;
import org.akraievoy.cnet.net.vo.EdgeData;
import org.akraievoy.cnet.net.vo.EdgeDataFactory;

/**
 * LATER add javadocs for a class created by anton
 */
public class MetricStatDegreesTest extends TestCase {
  public void testHistogram() {
    final int capacity = 32;
    final EdgeData edgeData = EdgeDataFactory.dense(true, 0, capacity);

    for (int from = 0; from < capacity; from++) {
      for (int into = from + 1; into < capacity - from; into++) {
        edgeData.set(from, into, 1);
      }
    }

    final MetricStatDegrees msd = new MetricStatDegrees();
    msd.setSource(new RefEdgeData(edgeData));

    msd.run();

    final Stat stat = (Stat) msd.getTarget().getValue();

    assertNotNull(stat);
    final double[] data = stat.getData();
    assertEquals(capacity, data.length);

    for (int i = 0; i < capacity; i++) {
      assertEquals("at index " + i, i + (i >= capacity / 2 ? 0.0 : 1.0), data[capacity - i - 1]);
    }

    final Histogram singleHist = stat.createHistogram(1, capacity - 2);
    assertEquals(1, singleHist.getLength());

    assertEquals(capacity + 0.0, singleHist.getValueAt(0));
    assertEquals(1.0, singleHist.getArgumentAt(0));
    assertEquals(capacity - 1.0, singleHist.getArgumentAt(1));

    final Histogram multiHist = stat.createHistogram(capacity - 2, 1);
    assertEquals(capacity - 2, multiHist.getLength());

    for (int i = 0; i < capacity - 2; i++) {
      assertEquals("arg min at " + i, i + 1.0, multiHist.getArgumentAt(i));
      assertEquals("arg max at " + i, i + 2.0, multiHist.getArgumentAt(i + 1));
      if (i == capacity / 2 - 1 || i == capacity - 3) {
        assertEquals("value at " + i, 2.0, multiHist.getValueAt(i));
      } else {
        assertEquals("value at " + i, 1.0, multiHist.getValueAt(i));
      }
    }

    final Histogram multiHist2 = stat.createHistogram(1, 1);
    assertEquals(capacity - 2, multiHist2.getLength());

    for (int i = 0; i < capacity - 2; i++) {
      assertEquals("arg min at " + i, i + 1.0, multiHist2.getArgumentAt(i));
      assertEquals("arg max at " + i, i + 2.0, multiHist2.getArgumentAt(i + 1));
      if (i == capacity / 2 - 1 || i == capacity - 3) {
        assertEquals("value at " + i, 2.0, multiHist2.getValueAt(i));
      } else {
        assertEquals("value at " + i, 1.0, multiHist2.getValueAt(i));
      }
    }
  }
}
