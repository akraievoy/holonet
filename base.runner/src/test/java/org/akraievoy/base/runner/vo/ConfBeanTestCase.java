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

package org.akraievoy.base.runner.vo;

import junit.framework.TestCase;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.io.StringWriter;

public class ConfBeanTestCase extends TestCase {
  public void testSerializeAndDeserialize() throws IOException {
    final ObjectMapper om = new ObjectMapper();
    final StringWriter serialized = new StringWriter();

    final ConfBean so = new ConfBean();
    so.setDesc("Nya");
    so.setExpUid(204);
    so.setUid(133);
    so.setName("C:/sdjhgf");

    om.writeValue(serialized, so);

    String jsonStr = serialized.toString();
    final ConfBean soRound2 = om.readValue(jsonStr, ConfBean.class);
    System.out.println("jsonStr = " + jsonStr);

    assertNotNull(soRound2);
    assertEquals(so.getDesc(), soRound2.getDesc());
    assertEquals(so.getUid(), soRound2.getUid());
    assertEquals(so.getName(), soRound2.getName());
  }
}
