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
import org.codehaus.jackson.map.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;

public class VertexDataDoubleTest extends TestCase {
  public void testJsonSerialization() throws IOException {
    final VertexData vdd = new VertexData(3);

    vdd.set(0, 1.0);
    vdd.set(1, 2.0);
    vdd.set(2, 3.0);

    final byte[] bytes = ByteStreams.toByteArray(vdd.createStream());

    final VertexData vertexData = new VertexData().fromStream(new ByteArrayInputStream(bytes));

    assertNotNull(vertexData);
    assertEquals(3, vertexData.getSize());
    assertEquals(2.0, vertexData.get(1));
    assertEquals(3.0, vertexData.get(2));
  }
}
