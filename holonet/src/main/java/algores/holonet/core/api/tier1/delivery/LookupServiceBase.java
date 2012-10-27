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

import algores.holonet.capi.Event;
import algores.holonet.core.CommunicationException;
import algores.holonet.core.api.Address;
import algores.holonet.core.api.Key;
import algores.holonet.core.api.LocalServiceBase;
import algores.holonet.core.api.tier0.routing.RoutingEntry;
import algores.holonet.core.api.tier0.routing.RoutingService;
import com.google.common.base.Optional;
import org.akraievoy.base.Die;

import java.util.*;

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
   *
   * @param key       to be looked up.
   * @param mustExist would not search the replicas if current node is responsible for the key and does not have it
   * @param mode which operation this lookup supports
   * @return the final address that is responsible for the key.
   * @throws algores.holonet.core.CommunicationException
   *          propagated
   */
  public Address lookup(Key key, boolean mustExist, Mode mode) throws CommunicationException {
    double lookupStartTime = getOwner().getNetwork().getElapsedTime();
    final Stack<Address> route = new Stack<Address>();

    try {
      final Address recursiveLookupResult = recursiveLookup(key, mustExist, route, null);

      if (recursiveLookupResult != null) {
        getOwner().getNetwork().registerLookupSuccess(mode, lookupStartTime, route, true);
        return recursiveLookupResult;
      }

    } catch (CommunicationException nfe) {
      getOwner().getNetwork().registerLookupSuccess(mode, lookupStartTime, route, false);
      throw nfe;
    }

    getOwner().getNetwork().registerLookupSuccess(mode, lookupStartTime, route, false);
    throw new CommunicationException("No route for '" + key + "', " + route.size() + " nodes traversed");
  }

  public Address recursiveLookup(
      Key key,
      boolean mustExist,
      Stack<Address> traversed,
      List<RoutingEntry> callerPending
  ) throws CommunicationException {
    if (traversed.size() >= HOP_LIMIT) {
      throw new RoutingException("Hop limit exceeded");
    }

    final Address ownerAddress = getOwner().getAddress();
    traversed.push(ownerAddress);

    
    //  LATER expand this to chain of routing state snapshots at each hop
    //  also, P-Grid does not presume fixed mapping : address -> node rank-0 key
    //    so this debug thingy would not be as useful
/*
    final List<String> routeForDebug = new ArrayList<String>();
    for (Address addr : traversed) {
      routeForDebug.add(addr.toString());
      routeForDebug.add(addr.getKey().toString());
    }
    routeForDebug.size(); // here you add a breakpoint with message: key.toString() + ": " + routeForDebug.toString()
*/

    if (getOwner().getServices().getStorage().getKeys().contains(key)) {
      return ownerAddress;
    }
    //  LATER !mustExist looks like a redundant/misleading check
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

    final RoutingService routing = getOwner().getServices().getRouting();
    //  memorize route count before iterative phase
    int pendingSize = 0;
    CommunicationException nfe = null;
    while (!pending.isEmpty()) {
      Collections.sort(pending, routing.getLivenessOrder());
      Collections.sort(pending, routing.distanceOrder(key));
      pendingSize = Math.max(pendingSize, pending.size());
      final RoutingEntry route = pending.remove(0);
      if (traversed.contains(route.getAddress())) {
        continue;
      }

      try {
        final Optional<LookupService> remoteLookupOpt =
            getOwner().getServices().getRpc().rpcTo(route, LookupService.class);
        if (remoteLookupOpt.isPresent()) {
          final Address address = remoteLookupOpt.get().recursiveLookup(
              key, mustExist, traversed, pending
          );
          routing.update(route, Event.HEART_BEAT);
          if (address != null) {
            return address;
          }
        } else {
          routing.registerCommunicationFailure(route.getAddress());
          traversed.add(route.getAddress());
        }
      } catch (CommunicationException myNfe) {
        nfe = myNfe;
      } catch (StackOverflowError soe) {
        System.err.println(traversed.toString());
        throw new RuntimeException(soe);
      }
    }

    if (nfe != null) {
      throw new CommunicationException(
          String.format("no result or errors for all of %d routes", pendingSize),
          nfe
      );
    }
    return null;
  }

  protected static int addNewRoutes(
      List<RoutingEntry> globalQueue, 
      List<RoutingEntry> localRoutes
  ) {
    int globalQueueSizeBefore = globalQueue.size();

    //  remove any stale dupes:
    //    global queue is more likely to be stale, as localized
    //    overlay data/structure tends to be more up-to-date
    globalQueue.removeAll(localRoutes);

    //  FIXME NOW remove the traversed?

    //  we need to add fresh recommendations to the head of the pending list
    //    otherwise convergence of the search degrades to Ring instead of Chord
    //    as stale global routing choices obscure more efficient local ones
    globalQueue.subList(0,0).addAll(localRoutes);

    return globalQueue.size() - globalQueueSizeBefore;
  }

  public List<RoutingEntry> lookupRoutes(Key key) throws CommunicationException {
    final List<RoutingEntry> replicas = getRouting().replicaSet(key, Byte.MAX_VALUE);
    final List<RoutingEntry> nodeHandles = getRouting().localLookup(key, 0, true);

    nodeHandles.removeAll(replicas);
    replicas.addAll(nodeHandles);

    return replicas;
  }

  protected RoutingService getRouting() {
    return getOwner().getServices().getRouting();
  }
}