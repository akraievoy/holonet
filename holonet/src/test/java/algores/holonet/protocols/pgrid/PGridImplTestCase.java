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

package algores.holonet.protocols.pgrid;

import algores.holonet.core.Network;
import algores.holonet.core.ServiceFactorySpring;
import algores.holonet.core.SimulatorException;
import algores.holonet.protocols.RingProtocolTestCase;

public class PGridImplTestCase extends RingProtocolTestCase {
  public void setUp() {
    super.setUp();
    final Network network = new Network();

    final ServiceFactorySpring factory = new ServiceFactorySpring();

    factory.setOverlay(new PGridImpl());
    factory.setRouting(new PGridRouting());

    network.setFactory(factory);

    setNetwork(network);

    dataEntryLeaksOnLeavePermitted = true;
  }

  public void testGeneric() throws Throwable {
    if (System.getProperty("pgrid.test") != null) {
      super.testGeneric();
    }
  }

  @Override
  public void testJoin() throws SimulatorException {
    if (System.getProperty("pgrid.test") != null) {
      super.testJoin();
    }
  }

  @Override
  public void testLeave() throws SimulatorException {
    if (System.getProperty("pgrid.test") != null) {
      super.testLeave();
    }
  }

  @Override
  public void testStabilize() throws SimulatorException {
    if (System.getProperty("pgrid.test") != null) {
      super.testStabilize();
    }
  }
}
