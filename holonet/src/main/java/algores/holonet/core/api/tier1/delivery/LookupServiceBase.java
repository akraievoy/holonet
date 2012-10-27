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
   *
   * @param key       to be looked up.
   * @param mustExist would not search the replicas if current node is responsible for the key and does not have it
   * @param mode which operation this lookup supports
   * @return the final address that is responsible for the key.
   * @throws algores.holonet.core.CommunicationException
   *          propagated
   */
  public Address lookup(Key key, boolean mustExist, Mode mode) throws CommunicationException {
    final double lookupStartTime = getOwner().getNetwork().getElapsedTime();
    RecursiveLookupState state = new RecursiveLookupState(
        getOwner().getServices().getRouting().getOwnRoute()
    );

    try {
      state = recursiveLookup(key, mustExist, state);

      if (state.replicaOpt.isPresent()) {
        getOwner().getNetwork().registerLookupSuccess(
            mode, lookupStartTime, state.replicaPath, true
        );
        return state.replicaOpt.get();
      }

    } catch (CommunicationException nfe) {
      getOwner().getNetwork().registerLookupSuccess(
          mode, lookupStartTime, state.replicaPath, false
      );
      throw nfe;
    }

    getOwner().getNetwork().registerLookupSuccess(
        mode, lookupStartTime, state.replicaPath, false
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
      return updateRoutes(new RecursiveLookupState(
          Optional.of(ownerAddress),
          state.replicaPath,
          state.traversals,
          state.hopCount,
          state.hopDistance
      ));
    }

    List<RoutingEntry> replicaPath = state.replicaPath;
    Optional<Address> replicaOpt = Optional.absent();

    final List<Traversal> localTraversals =
        new ArrayList<Traversal>(state.traversals);
    final List<RoutingEntry> localRoutes =
        routing.replicaSet(key, Byte.MAX_VALUE);
    final List<RoutingEntry> fingers = routing.localLookup(key, 0, true);
    fingers.removeAll(localRoutes);
    localRoutes.addAll(fingers);

    for (Traversal t : localTraversals) {
      //  remove traversed previously
      if (t.called()) {
        final int localIndex = localRoutes.indexOf(t.re);
        if (localIndex >= 0) {
          localRoutes.remove(localIndex);
        }
      }
    }
    //  add new routes to traversal history
    for (RoutingEntry re : localRoutes) {
      localTraversals.add(new Traversal(re, state.hopCount, -1, Event.DISCOVERED));
    }
    for (Traversal t : localTraversals) {
      //  some of globally pending routes became closer?
      if (!t.called()) {
        if (routing.routingDistance(t.re, key) < state.hopDistance) {
          final int localIndex = localRoutes.indexOf(t.re);
          if (localIndex <= 0) {
            localRoutes.add(t.re);
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
          for (int i = 0, size = localTraversals.size(); i < size; i++) {
            Traversal t = localTraversals.get(i);
            if (t.re.equals(route)) {
              localTraversals.set(
                  i,
                  new Traversal(
                      t.re, t.hopAdded, state.hopCount, Event.HEART_BEAT
                  )
              );
              break;
            }
          }
          final ArrayList<RoutingEntry> remoteReplicaPath =
              new ArrayList<RoutingEntry>(state.replicaPath);
          remoteReplicaPath.add(route);
          final RecursiveLookupState remoteState =
              remoteLookupOpt.get().recursiveLookup(
                  key, mustExist,
                  new RecursiveLookupState(
                      replicaOpt,
                      remoteReplicaPath,
                      localTraversals,
                      state.hopCount + 1,
                      routing.routingDistance(route, key)
                  )
              );
          //  remote may invoke some fraction of our local queue,
          //    so we have to renew localTraversals completely
          localTraversals.clear();
          localTraversals.addAll(remoteState.traversals);
          if (remoteState.replicaOpt.isPresent()) {
            replicaOpt = remoteState.replicaOpt;
            replicaPath = remoteState.replicaPath;
            break;
          } else {
            //  remote may invoke some fraction of our local queue,
            //    so we also have to analyse the localRoutes
            for (Traversal t : localTraversals) {
              //  remove traversed previously
              if (t.called()) {
                final int localIndex = localRoutes.indexOf(t.re);
                if (localIndex >= 0) {
                  localRoutes.remove(localIndex);
                }
              }
            }
          }
        } else {
          //  mark failed
          for (int i = 0, size = localTraversals.size(); i < size; i++) {
            Traversal t = localTraversals.get(i);
            if (t.re.equals(route)) {
              localTraversals.set(
                  i,
                  new Traversal(
                      t.re, t.hopAdded, state.hopCount, Event.CONNECTION_FAILED
                  )
              );
              break;
            }
          }
        }
      } catch (CommunicationException myNfe) {
        throw myNfe;
      } catch (StackOverflowError soe) {
        throw new RuntimeException(soe);
      }
    }

    return updateRoutes(new RecursiveLookupState(
        replicaOpt,
        replicaPath,
        localTraversals,
        state.hopCount,
        state.hopDistance
    ));
  }

  protected RecursiveLookupState updateRoutes(RecursiveLookupState state) {
    final RoutingService routing = getOwner().getServices().getRouting();

    for (Traversal t : state.traversals) {
        routing.update(t.re, t.event);
    }

    return state;
  }
}