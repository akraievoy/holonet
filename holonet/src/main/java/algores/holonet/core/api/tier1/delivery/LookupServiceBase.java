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

package algores.holonet.core.api.tier1.delivery;

import algores.holonet.core.CommunicationException;
import algores.holonet.core.api.Address;
import algores.holonet.core.api.Key;
import algores.holonet.core.api.LocalServiceBase;
import algores.holonet.core.api.tier0.routing.RoutingEntry;
import algores.holonet.core.api.tier0.routing.RoutingService;
import org.akraievoy.base.Die;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class LookupServiceBase extends LocalServiceBase implements LookupService {
  public LookupServiceBase() {
    //	default
  }

  public LookupServiceBase copy() {
    return new LookupServiceBase();
  }

  /**
   * An iterative lookup algorithm, starting from startingAddr.
   *
   * @param key       to be looked up.
   * @param mustExist would not search the replicas if current node is responsible for the key and does not have it
   * @return the final address that is responsible for the key.
   * @throws algores.holonet.core.CommunicationException
   *          propagated
   */
  public Address lookup(Key key, boolean mustExist) throws CommunicationException {
    double lookupStartTime = getOwner().getNetwork().getElapsedTime();
    final Stack<Address> route = new Stack<Address>();

    try {
      final Address recursiveLookupResult = recursiveLookup(key, mustExist, route, null);

      if (recursiveLookupResult != null) {
        getOwner().getNetwork().registerSuccess(lookupStartTime, route);
        return recursiveLookupResult;
      }

    } catch (CommunicationException nfe) {
      getOwner().getNetwork().getInterceptor().registerLookupSuccess(false);
      throw nfe;
    }

    getOwner().getNetwork().getInterceptor().registerLookupSuccess(false);
    throw new CommunicationException("No route for '" + key + "', " + route.size() + " nodes traversed");
  }

  public Address recursiveLookup(Key key, boolean mustExist, Stack<Address> traversed, List<RoutingEntry> callerPending) throws CommunicationException {
    if (traversed.size() >= HOP_LIMIT) {
      throw new RoutingException("Hop limit exceeded");
    }

    final Address ownerAddress = getOwner().getAddress();
    traversed.push(ownerAddress);

    if (getOwner().getServices().getStorage().get(key) != null) {
      return ownerAddress;
    }
    if (!mustExist && owner.getServices().getRouting().getOwnRoute().isReplicaFor(key, (byte) 0)) {
      return ownerAddress;
    }

    final List<RoutingEntry> routes = lookupRoutes(key);
    Die.ifNull("routes", routes);

    final List<RoutingEntry> pending = callerPending != null ? callerPending : new ArrayList<RoutingEntry>();
    int addedCount = addNewRoutes(pending, routes);

    if (callerPending != null && addedCount * 2 <= pending.size()) {
      //	this effectively returns extra local routes to the caller
      //	here aliasing allows to keep code cleaner (but not simpler of course)
      //	so we switch from growing the simulator call stack by recursion to iterative lookup version
      //	LATER here we have a nice injection point for lots of strategies
      return null;
    }

    CommunicationException nfe = null;
    while (!pending.isEmpty()) {
      final RoutingEntry route = pending.remove(0);
      if (traversed.contains(route.getAddress())) {
        continue;
      }

      try {
        final LookupService remoteLookup = getOwner().getServices().getRpc().rpcTo(route, LookupService.class);
        final Address address = remoteLookup.recursiveLookup(key, mustExist, traversed, pending);

        if (address != null) {
          return address;
        }
      } catch (CommunicationException myNfe) {
        nfe = myNfe;
      } catch (StackOverflowError soe) {
        System.err.println(traversed.toString());
        throw new RuntimeException(soe);
      }
    }

    if (nfe != null) {
      throw nfe;
    }
    return null;
  }

  protected static int addNewRoutes(List<RoutingEntry> pending, List<RoutingEntry> routingEntries) {
    routingEntries.removeAll(pending);
    int addedCount = routingEntries.size();
    pending.addAll(routingEntries);

    return addedCount;
  }

  public List<RoutingEntry> lookupRoutes(Key key) throws CommunicationException {
    final List<RoutingEntry> replicas = getRouting().replicaSet(key, Byte.MAX_VALUE);
    final List<RoutingEntry> nodeHandles = getRouting().localLookup(key, 5, true);

    replicas.addAll(nodeHandles);

    return replicas;
  }

  protected RoutingService getRouting() {
    return getOwner().getServices().getRouting();
  }
}