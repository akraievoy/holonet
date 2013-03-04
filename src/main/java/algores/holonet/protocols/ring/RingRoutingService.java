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

package algores.holonet.protocols.ring;

import algores.holonet.core.CommunicationException;
import algores.holonet.core.api.tier0.routing.RoutingService;

import algores.holonet.core.api.tier0.routing.RoutingPackage.*;

/**
 * Some extra operations RingRoutingService should provide.
 */
public interface RingRoutingService extends RoutingService {
  Flavor FLAVOR_SUCCESSOR = new Flavor("successor", true);
  Flavor FLAVOR_PREDECESSOR = new Flavor("predecessor", true);
  Flavor FLAVOR_EXTRA = new Flavor("extra");

  RoutingEntry getSuccessor() throws CommunicationException;

  void setSuccessor(RoutingEntry successor) throws CommunicationException;

  RoutingEntry getPredecessor() throws CommunicationException;

  RoutingEntry setPredecessor(RoutingEntry predecessor) throws CommunicationException;

  RoutingEntry getPredecessorSafe() throws CommunicationException;
}
