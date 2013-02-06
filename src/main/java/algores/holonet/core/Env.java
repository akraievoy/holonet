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

package algores.holonet.core;

import algores.holonet.core.api.Address;
import algores.holonet.core.api.Key;
import algores.holonet.core.api.tier0.routing.RoutingDistance;
import algores.holonet.core.api.tier0.routing.RoutingSeeder;
import com.google.common.base.Optional;
import org.akraievoy.cnet.gen.vo.EntropySource;

import java.util.Collection;
import java.util.SortedMap;

/**
 * Network environment.
 */
public interface Env extends RoutingDistance, RoutingSeeder {
  Address createNetworkAddress(EntropySource eSource);

  Node getNode(Address address);

  void putNode(Node newNode, Address address);

  void removeNode(Address address);

  Collection<Node> getAllNodes();

  /**
   * Beware: init checks lots of context injectables, which are not set up if Spring calls it as a start-method.
   */
  void init();

  EnvMappings getMappings();

  Optional<RequestPair> generateRequestPair(EntropySource entropy);

  SortedMap<Key, Node> keyToNode();
}
