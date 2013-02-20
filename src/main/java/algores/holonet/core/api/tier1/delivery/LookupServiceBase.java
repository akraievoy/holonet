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
import algores.holonet.core.api.tier0.rpc.ServiceRegistry;
import com.google.common.base.Optional;

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
   * @param key       to be looked up.
   * @param mustExist would not search the replicas if current node is
   *                    responsible for the key and does not have it
   * @param mode which operation this lookup supports
   * @param actualTarget used for reporting, should not be used in lookup logic
   * @return the final address that is responsible for the key.
   * @throws algores.holonet.core.CommunicationException
   *          propagated
   */
  public Address lookup(
      Key key,
      boolean mustExist,
      Mode mode,
      Optional<Address> actualTarget
  ) throws CommunicationException {
    final double lookupStartTime = getOwner().getNetwork().getElapsedTime();
    RecursiveLookupState state = new RecursiveLookupState(
        getOwner().getServices().getRouting().getOwnRoute()
    );

    try {
      state = recursiveLookup(key, mustExist, mode, state);

      if (state.replicaOpt.isPresent()) {
        getOwner().getNetwork().registerLookupSuccess(
            mode, lookupStartTime, state.replicaPath, key, actualTarget, state.getStats(), true
        );
        return state.replicaOpt.get();
      }

    } catch (CommunicationException nfe) {
      getOwner().getNetwork().registerLookupSuccess(
          mode, lookupStartTime, state.replicaPath, key, actualTarget, state.getStats(), false
      );
      throw nfe;
    }

    getOwner().getNetwork().registerLookupSuccess(
        mode, lookupStartTime, state.replicaPath, key, actualTarget, state.getStats(), false
    );
    throw new CommunicationException(
        String.format(
            "No route for '%s', %d nodes traversed",
            key,
            state.replicaPath.size()
        )
    );
  }

  public RecursiveLookupState recursiveLookup(
      Key key,
      boolean mustExist,
      Mode mode,
      RecursiveLookupState state
  ) throws CommunicationException {
    if (state.hopCount >= HOP_LIMIT) {
      throw new RoutingException("Hop limit exceeded");
    }

    final Address ownerAddress = getOwner().getAddress();
    final ServiceRegistry services = getOwner().getServices();
    final RoutingService routing = services.getRouting();
    if (
        services.getStorage().getKeys().contains(key) ||
        !mustExist && routing.getOwnRoute().isReplicaFor(key, (byte) 0)
    ) {
      return updateRoutes(mode, state.withReplica(ownerAddress));
    }

    List<RoutingEntry> replicaPath = state.replicaPath;
    Optional<Address> replicaOpt = Optional.absent();

    final SortedMap<Address, Traversal> localTraversed =
        new TreeMap<Address, Traversal>(state.traversed);
    final SortedMap<Address, Traversal> localPending =
        new TreeMap<Address, Traversal>(state.pending);
    final List<RoutingEntry> localRoutes =
        routing.replicaSet(key, Byte.MAX_VALUE);
    final List<RoutingEntry> fingers = routing.localLookup(
        key, (int) Math.ceil(routing.getRedundancy()), true
    );
    final List<RoutingEntry> neighbors = routing.neighborSet(0);
    fingers.removeAll(localRoutes);
    localRoutes.addAll(fingers);
    neighbors.removeAll(localRoutes);
    localRoutes.addAll(neighbors);

    for (Iterator<RoutingEntry> rIt = localRoutes.iterator(); rIt.hasNext(); ) {
      final Address address = rIt.next().getAddress();
      if (localTraversed.containsKey(address) || localPending.containsKey(address)) {
        rIt.remove();
      }
    }
    //  add new routes to traversal history
    for (RoutingEntry re : localRoutes) {
      localPending.put(
          re.getAddress(),
          new Traversal(re, state.hopCount, -1, Event.DISCOVERED)
      );
    }
    int liftCount = 0;
    for (Traversal t : state.pending.values()) {
      //  some of globally pending routes became closer?
        if (routing.routingDistance(t.re, key) * 2 < state.hopDistance) {
        final int localIndex = localRoutes.indexOf(t.re);
        if (localIndex <= 0) {
          localRoutes.add(t.re);
          liftCount ++;
          if (liftCount * 8 > localRoutes.size()) {
            break;
          }
        }
      }
    }
    Collections.sort(localRoutes, routing.getLivenessOrder());
    Collections.sort(localRoutes, routing.distanceOrder(key));

    while (!localRoutes.isEmpty()) {
      final RoutingEntry route = localRoutes.remove(0);

      try {
        final Optional<LookupService> remoteLookupOpt =
            services.getRpc().rpcTo(route, LookupService.class);
        if (remoteLookupOpt.isPresent()) {
          //  mark called
          localTraversed.put(
              route.getAddress(),
              localPending.remove(route.getAddress()).calledAtHop(state.hopCount)
          );
          final ArrayList<RoutingEntry> remoteReplicaPath =
              new ArrayList<RoutingEntry>(state.replicaPath);
          remoteReplicaPath.add(route);
          final RecursiveLookupState remoteState =
              remoteLookupOpt.get().recursiveLookup(
                  key, mustExist,
                  mode,
                  new RecursiveLookupState(
                      replicaOpt,
                      remoteReplicaPath,
                      localTraversed,
                      localPending,
                      state.hopCount + 1,
                      routing.routingDistance(route, key)
                  )
              );
          localTraversed.putAll(remoteState.traversed);
          //  remote may invoke some fraction of our local queue,
          //    so we have to renew localPending completely
          localPending.clear();
          localPending.putAll(remoteState.pending);
          if (remoteState.replicaOpt.isPresent()) {
            replicaOpt = remoteState.replicaOpt;
            replicaPath = remoteState.replicaPath;
            break;
          } else {
            //  remote may invoke some fraction of our local queue,
            //    so we also have to analyse the localRoutes
            for (Iterator<RoutingEntry> rIt = localRoutes.iterator(); rIt.hasNext(); ) {
              final Address address = rIt.next().getAddress();
              if (localTraversed.containsKey(address)) {
                rIt.remove();
              }
            }
          }
        } else {
          //  mark failed
          localTraversed.put(
              route.getAddress(),
              localPending.remove(route.getAddress()).failedAtHop(state.hopCount)
          );
        }
      } catch (StackOverflowError soe) {
        throw new RuntimeException(soe);
      }
    }

    return updateRoutes(
        mode,
        new RecursiveLookupState(
            replicaOpt,
            replicaPath,
            localTraversed,
            localPending,
            state.hopCount,
            state.hopDistance
        )
    );
  }

  protected RecursiveLookupState updateRoutes(Mode mode, RecursiveLookupState state) {
    final RoutingService routing = getOwner().getServices().getRouting();
    final RoutingService.RoutingStatsTuple statsBefore =
        routing.getStats();

    for (Traversal t : state.traversed.values()) {
        routing.update(t.re, t.event);
    }
    for (Traversal t : state.pending.values()) {
        routing.update(t.re, t.event);
    }
    final RoutingService.RoutingStatsTuple statsAfter =
        routing.getStats();
    getOwner().getNetwork().getInterceptor().modeToLookups(mode).registerRoutingStats(
        statsAfter.routeCount,
        statsAfter.routeRedundancy,
        statsAfter.routeRedundancy / statsBefore.routeRedundancy
    );
    return state;
  }
}