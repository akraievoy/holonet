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

public class IdNameBeanTestCase extends TestCase {
  public void testSerializeAndDeserialize() throws IOException {
    final ObjectMapper om = new ObjectMapper();
    final StringWriter serialized = new StringWriter();

    final IdNameBean so = new IdNameBean();
    so.setName("i");
    so.setId("987654321987654321987654321");

    om.writeValue(serialized, so);

    String jsonStr = serialized.toString();
    final IdNameBean soRound2 = om.readValue(jsonStr, IdNameBean.class);
    System.out.println("jsonStr = " + jsonStr);
    assertNotNull(soRound2);
    assertEquals(so.getName(), soRound2.getName());
    assertEquals(so.getId(), soRound2.getId());
  }
}
