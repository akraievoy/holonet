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
import algores.holonet.core.api.Range;
import org.akraievoy.cnet.gen.vo.EntropySource;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class EnvSimple implements Env {
  protected final Map<Address, Node> addressToNode = new HashMap<Address, Node>();
  protected final EnvMappings mappings = new EnvMappings();

  public Address createNetworkAddress(EntropySource eSource) {
    final Address address = new PlanarAddress();

    address.init(eSource);

    return address;
  }

  public Node getNode(Address address) {
    return addressToNode.get(address);
  }

  public void putNode(Node newNode, Address address) {
    addressToNode.put(address, newNode);
  }

  public void removeNode(Address address) {
    addressToNode.remove(address);
  }

  public Collection<Node> getAllNodes() {
    return addressToNode.values();
  }

  public void initialize() {
    // nothing to do
  }

  public void init() {
    //	nothing to do
  }

  @Override
  public double apply(
      Address localAddress, Key target,
      Address curAddress, Range curRange
  ) {
    return 0;
  }

  public EnvMappings getMappings() {
    return mappings;
  }
}
