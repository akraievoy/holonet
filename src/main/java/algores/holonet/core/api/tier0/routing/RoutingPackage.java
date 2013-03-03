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

public class RoutingPackage {

  public static class Flavor implements Comparable<Flavor> {
    private final String name;
    private final boolean forceReflavor;

    public Flavor(String name) {
      this(name, false);
    }

    public Flavor(String name, final boolean forcesReflavor) {
      this.name = name;
      this.forceReflavor = forcesReflavor;
    }

    public boolean forceReflavor() {
      return forceReflavor;
    }

    public String name() {
      return name;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      final Flavor flavor = (Flavor) o;

      return name.equals(flavor.name);
    }

    @Override
    public int hashCode() {
      return name.hashCode();
    }

    @Override
    public int compareTo(Flavor o) {
      return name.compareTo(o.name());
    }
  }

  public static class RouteTable {
    private final SortedMap<String, TreeSet<Address>> flavorToAddresses =
        new TreeMap<String, TreeSet<Address>>();
    private final SortedMap<Address, Flavor> addressToFlavor =
        new TreeMap<Address, Flavor>();
    private final SortedMap<Address, RoutingEntry> addressToRoute =
        new TreeMap<Address, RoutingEntry>();

    public int count(final Flavor flavor) {
      final SortedSet<Address> addresses = flavorToAddresses.get(flavor.name);
      if (addresses == null) {
        return 0;
      }

      return addresses.size();
    }

    public Flavor flavor(final Address address) {
      return addressToFlavor.get(address);
    }

    public RoutingEntry route(final Address address) {
      return addressToRoute.get(address);
    }

    public boolean has(final Address address) {
      return addressToFlavor.containsKey(address);
    }

    public Collection<Address> adresses(final Flavor flavor) {
      final SortedSet<Address> addresses = flavorToAddresses.get(flavor.name);
      if (addresses == null) {
        return Collections.emptySet();
      }

      return Collections.unmodifiableCollection(addresses);
    }

    public int add(final Flavor flavor, final RoutingEntry route) {
      final Address address = route.getAddress();
      final Flavor prevFlavor = addressToFlavor.get(address);
      if (prevFlavor != null) {
        if (prevFlavor.equals(flavor)) {
          addressToRoute.put(address, route);
          return count(flavor);
        }
        remove(address);
      }

      addressToFlavor.put(address, flavor);
      addressToRoute.put(address, route);
      final TreeSet<Address> addresses = flavorToAddresses.get(flavor.name);
      if (addresses == null) {
        final TreeSet<Address> newAddresses = new TreeSet<Address>();
        newAddresses.add(address);
        flavorToAddresses.put(flavor.name, newAddresses);
        return newAddresses.size();
      }

      addresses.add(address);
      return addresses.size();
    }

    public int remove(final Address address) {
      final Flavor prevFlavor = addressToFlavor.remove(address);
      addressToRoute.remove(address);
      if (prevFlavor == null) {
        return -1;
      }
      final TreeSet<Address> addresses = flavorToAddresses.get(prevFlavor.name);
      if (addresses == null) {
        return 0;
      }
      addresses.remove(address);
      return addresses.size();
    }
  }
}
