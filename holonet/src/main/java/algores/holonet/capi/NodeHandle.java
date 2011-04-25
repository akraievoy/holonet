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

package algores.holonet.capi;

import algores.holonet.core.api.Address;

/**
 * A nodehandle encapsulates the transport address and nodeId of a node in the system.
 * The nodeId is of type key; the transport address might be, for example, an IP address and port.
 *
 * @author Frank Dabek
 * @author Ben Zhao
 * @author Peter Druschel
 * @author John Kubiatowicz
 * @author Ion Stoica
 */
public interface NodeHandle {
  Key getNodeId();

  Address getAddress();
}
