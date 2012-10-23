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

package algores.holonet.protocols;

import algores.holonet.protocols.chord.ChordRoutingServiceImpl;
import algores.holonet.protocols.chord.ChordServiceBase;

public class ChordProtocolTestCase extends RingProtocolTestCase {
  @Override
  protected TextContextMeta createContextMeta() {
    return super.createContextMeta()
        .withRouting(new ChordRoutingServiceImpl())
        .withOverlay(new ChordServiceBase());
  }

  public void testHopCount() throws Throwable {
    testHopCount0(135930, 4);
  }
}
