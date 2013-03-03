/*
 Copyright 2013 Anton Kraievoy akraievoy@gmail.com
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

package algores.holonet.core.api.tier0.routing;

import algores.holonet.core.api.Address;

import java.util.*;

public class Routing {

  public static class RouteTable {
    private final SortedMap<String, TreeSet<Address>> flavorToAddresses =
        new TreeMap<String, TreeSet<Address>>();
    private final SortedMap<Address, String> addressToFlavor =
        new TreeMap<Address, String>();
    private final SortedMap<Address, RoutingEntry> addressToRoute =
        new TreeMap<Address, RoutingEntry>();

    public int count(final String flavor) {
      final SortedSet<Address> addresses = flavorToAddresses.get(flavor);
      if (addresses == null) {
        return 0;
      }

      return addresses.size();
    }

    public String flavor(final Address address) {
      return addressToFlavor.get(address);
    }

    public RoutingEntry route(final Address address) {
      return addressToRoute.get(address);
    }

    public boolean has(final Address address) {
      return addressToFlavor.containsKey(address);
    }

    public Collection<Address> adresses(final String flavor) {
      final SortedSet<Address> addresses = flavorToAddresses.get(flavor);
      if (addresses == null) {
        return Collections.emptySet();
      }

      return Collections.unmodifiableCollection(addresses);
    }

    public int add(final String flavor, final RoutingEntry route) {
      final Address address = route.getAddress();
      final String prevFlavor = addressToFlavor.get(address);
      if (prevFlavor != null) {
        if (prevFlavor.equals(flavor)) {
          addressToRoute.put(address, route);
          return count(flavor);
        }
        remove(prevFlavor, address);
      }

      addressToFlavor.put(address, flavor);
      addressToRoute.put(address, route);
      final TreeSet<Address> addresses = flavorToAddresses.get(flavor);
      if (addresses == null) {
        final TreeSet<Address> newAddresses = new TreeSet<Address>();
        newAddresses.add(address);
        flavorToAddresses.put(flavor, newAddresses);
        return newAddresses.size();
      }

      addresses.add(address);
      return addresses.size();
    }

    public int remove(final String flavor, final Address address) {
      addressToFlavor.remove(address);
      addressToRoute.remove(address);
      final TreeSet<Address> addresses = flavorToAddresses.get(flavor);
      if (addresses == null) {
        return 0;
      }
      addresses.remove(address);
      return addresses.size();
    }
  }
}
