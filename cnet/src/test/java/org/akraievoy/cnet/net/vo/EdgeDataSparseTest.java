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

import junit.framework.TestCase;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.io.StringWriter;

public class EdgeDataSparseTest extends TestCase {
  final EdgeData eData = EdgeDataFactory.sparse(true, 0.0);

  public void setUp() {
    eData.set(3, 4, 12.0);
    eData.set(4, 2, 11.0);
  }

  public void testGet() throws Exception {
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

    assertEquals(12.0, eData.get(3, 4));
    assertEquals(11.0, eData.get(2, 4));
    assertEquals(13.0, eData.get(1, 3));

    assertEquals(12.0, eData.get(4, 3));
    assertEquals(11.0, eData.get(4, 2));
    assertEquals(13.0, eData.get(3, 1));

    assertEquals(0.0, eData.get(2, 2));
    assertEquals(0.0, eData.get(2, 1));
    assertEquals(0.0, eData.get(1, 2));
  }

  public void testGetSize() {
    eData.set(1, 3, 13.0);
    assertEquals(5, eData.getSize());
  }

  public void testRemove() {
    eData.remove(2);

    assertEquals(4, eData.getSize());

    assertEquals(12.0, eData.get(2, 3));
    assertEquals(0.0, eData.get(2, 4));
    assertEquals(0.0, eData.get(3, 4));
    assertEquals(0.0, eData.get(1, 3));
  }


  public void testInsert() {
    eData.set(3, 4, 12.0);
    eData.set(4, 2, 11.0);

    eData.insert(4);

    assertEquals(6, eData.getSize());

    assertEquals(12.0, eData.get(5, 3));
    assertEquals(12.0, eData.get(3, 5));
    assertEquals(11.0, eData.get(2, 5));
    assertEquals(11.0, eData.get(5, 2));
    assertEquals(0.0, eData.get(3, 4));
    assertEquals(0.0, eData.get(4, 3));
    assertEquals(0.0, eData.get(2, 4));
    assertEquals(0.0, eData.get(4, 2));
    assertEquals(0.0, eData.get(4, 5));
    assertEquals(0.0, eData.get(5, 4));
  }

  public void testPower() {
    assertEquals(23.0, eData.power(4));
    assertEquals(11.0, eData.power(2));
    assertEquals(12.0, eData.power(3));
    assertEquals(0.0, eData.power(1));
    assertEquals(0.0, eData.power(0));

    eData.set(3, 1, 2.0);

    assertEquals(23.0, eData.power(4));
    assertEquals(11.0, eData.power(2));
    assertEquals(14.0, eData.power(3));
    assertEquals(2.0, eData.power(1));
    assertEquals(0.0, eData.power(0));

    eData.set(0, 0, 1.0);

    assertEquals(23.0, eData.power(4));
    assertEquals(11.0, eData.power(2));
    assertEquals(14.0, eData.power(3));
    assertEquals(2.0, eData.power(1));
    assertEquals(1.0, eData.power(0));

    eData.set(4, 4, 3.0);

    assertEquals(26.0, eData.power(4));
    assertEquals(11.0, eData.power(2));
    assertEquals(14.0, eData.power(3));
    assertEquals(2.0, eData.power(1));
    assertEquals(1.0, eData.power(0));
  }

  public void testJsonSerialization() throws IOException {
    EdgeData edd = EdgeDataFactory.sparse(false, Double.POSITIVE_INFINITY);

    edd.set(1, 1, 0.0);
    edd.set(0, 1, 1.0);
    edd.set(1, 2, 2.0);
    edd.set(3, 2, 3.0);

    ObjectMapper om = new ObjectMapper();
    final StringWriter sw = new StringWriter();

    om.writeValue(sw, edd);

/*
		System.out.println(sw.toString());
*/

    final EdgeData res = om.readValue(sw.toString(), EdgeDataSparse.class);

    StringWriter sww = new StringWriter();

    om.writeValue(sww, res);

    assertEquals(sw.toString(), sww.toString());

    final EdgeData res2 = om.readValue(sww.toString(), EdgeDataSparse.class);

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
    assertEquals(Double.POSITIVE_INFINITY, res2.get(4, 4));

    assertEquals(Double.POSITIVE_INFINITY, res2.get(0, 3));
    assertEquals(Double.POSITIVE_INFINITY, res2.get(3, 0));
    assertEquals(Double.POSITIVE_INFINITY, res2.get(0, 2));
    assertEquals(Double.POSITIVE_INFINITY, res2.get(2, 0));
    assertEquals(Double.POSITIVE_INFINITY, res2.get(4, 0));
    assertEquals(Double.POSITIVE_INFINITY, res2.get(0, 4));
    assertEquals(Double.POSITIVE_INFINITY, res2.get(4, 1));
    assertEquals(Double.POSITIVE_INFINITY, res2.get(1, 4));
  }
}
