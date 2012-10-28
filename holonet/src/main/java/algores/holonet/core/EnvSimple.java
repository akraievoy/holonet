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
import algores.holonet.core.api.AddressMeta;
import algores.holonet.core.api.Key;
import algores.holonet.core.api.Range;
import org.akraievoy.cnet.gen.vo.EntropySource;

import java.util.*;

public class EnvSimple implements Env {
  protected final Map<Address, Node> addressToNode =
      new HashMap<Address, algores.holonet.core.Node>(128, 0.25f);
  protected final EnvMappings mappings = new EnvMappings();
  protected final AddressMeta addressMeta = new PlanarAddressMeta();

  public Address createNetworkAddress(EntropySource eSource) {
    return addressMeta.create(eSource);
  }

  @Override
  public SortedMap<Key, Node> keyToNode() {
    final TreeMap<Key, Node> keyToNode = new TreeMap<Key, Node>();
    for (Map.Entry<Address, Node> addrToNode : this.addressToNode.entrySet()) {
      keyToNode.put(
          addrToNode.getValue().getKey(),
          addrToNode.getValue()
      );
    }
    return keyToNode;
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
//    return 1 + (localAddress.getKey().toLong() % 3 + curAddress.getKey().toLong() % 5 + 1) * 32;
    return 1;
  }

  public EnvMappings getMappings() {
    return mappings;
  }

  @Override
  public RequestPair generateRequestPair(EntropySource entropy) {
    final Collection<Node> allNodes = getAllNodes();

    return new RequestPair(
      entropy.randomElement(allNodes),
      entropy.randomElement(allNodes)
    );
  }
}
