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

package algores.holonet.core.api.tier1.overlay;

import algores.holonet.core.CommunicationException;
import algores.holonet.core.SimulatorException;
import algores.holonet.core.api.Address;

/**
 * The interface for protocol. Each RingNode is supposed to run an instance of DhtProtocol.
 * <p/>
 * Further subinterfacing/subclassing will be required to implement protocol-specific features.
 */
public interface OverlayService {
  /**
   * Address of old node is provided to join the DHT.
   * The protocol is running on owner node, which should have been set before.
   *
   * @param old a random node in the old DHT. <code>null</code> if ownerNode is the first node in the DHT.
   */
  void join(Address old) throws SimulatorException;

  /**
   * Is called before owner node shutdown.
   * <p/>
   * The goal is to let the node leave the ring <b>gracefully</b>.
   */
  void leave() throws CommunicationException;

  /**
   * For maintaining finger table, etc.
   */
  void stabilize() throws CommunicationException;
}
