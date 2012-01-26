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
import algores.holonet.core.api.AddressSource;
import algores.holonet.core.api.Key;
import algores.holonet.core.api.KeySource;
import algores.holonet.core.api.tier0.rpc.ServiceRegistry;
import algores.holonet.core.api.tier0.rpc.ServiceRegistryBase;

/**
 * Local hash table storage, has the node address information.
 * <p/>
 * Relies on implementation of Protocol class.
 */

public class Node implements KeySource, AddressSource, Comparable<Node> {
  protected Network network;
  protected Address address;
  protected ServiceRegistryBase serviceRegistry;

  protected void init(final Network parentNetwork, final ServiceFactory factory, final Address randomAddress) {
    network = parentNetwork;
    address = randomAddress;

    serviceRegistry = new ServiceRegistryBase();
    serviceRegistry.setFactory(factory);
    serviceRegistry.init(Node.this);
  }

  public Key getKey() {
    return address.getKey();
  }

  public Address getAddress() {
    return address;
  }

  public String toString() {
    return getAddress().toString() + " " + getServices().getOverlay().toString();
  }

  public Network getNetwork() {
    return network;
  }

  public ServiceRegistry getServices() {
    return serviceRegistry;
  }

  public int compareTo(Node o) {
    return address.compareTo(o.address);
  }
}
