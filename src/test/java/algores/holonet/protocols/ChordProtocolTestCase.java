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

public class ChordProtocolTestCase extends DhtProtocolTestCase {
  @Override
  protected ContextMeta createContextMeta() {
    return new ContextMeta()
        .withRouting(new ChordRoutingServiceImpl())
        .withOverlay(new ChordServiceBase());
  }

  public void testHopCount() throws Throwable {
    testHopCount0(135930, 4, 2);
    testHopCount0(388934, 4, 2);
    testHopCount0(909845, 4, 2);
    testHopCount0(135930, 16, 4);
    testHopCount0(230474, 16, 4);
    testHopCount0(847598, 16, 4);
    testHopCount0(135930, 32, 5);
    testHopCount0(874934, 32, 5);
    testHopCount0(129874, 32, 5);
    testHopCount0(830388, 64, 6);
    testHopCount0(135930, 256, 8);
    testHopCount0(238479, 256, 8);
    testHopCount0(983430, 512, 9);
  }

  public void testJoinLeave() throws Throwable {
/*
    if (true) {
      final int width = 100000;
      final Progress probing =
          ProgressMeta.DEFAULT.progress("probing seeds", width).start();
      for (int seedOffs = 0; seedOffs < width; seedOffs++) {
        try {
          testJoinLeave0(136279 + seedOffs, 5);
          probing.iter(seedOffs);
        } catch (Throwable t) {
          System.err.println("seed = " + (seedOffs + 136279) + ": " + t.getMessage());
          probing.iter(seedOffs, false);
        }
      }
      probing.stop();
    }
*/

    testJoinLeave0(145391, 5);
    testJoinLeave0(137073, 5);
    testJoinLeave0(138657, 5);
    testJoinLeave0(135930, 4);
    testJoinLeave0(388934, 4);
    testJoinLeave0(909845, 4);
    testJoinLeave0(136279, 5);
    testJoinLeave0(135930, 16);
    testJoinLeave0(230474, 16);
    testJoinLeave0(847598, 16);
    testJoinLeave0(135930, 32);
    testJoinLeave0(874934, 32);
    testJoinLeave0(129874, 32);
    testJoinLeave0(830388, 64);
    testJoinLeave0(135930, 256);
    testJoinLeave0(238479, 256);
    testJoinLeave0(983430, 512);
  }
}
