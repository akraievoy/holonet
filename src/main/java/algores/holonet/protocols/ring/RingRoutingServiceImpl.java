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

package algores.holonet.protocols.ring;

import algores.holonet.capi.Event;
import algores.holonet.core.CommunicationException;
import algores.holonet.core.Node;
import algores.holonet.core.api.Address;
import algores.holonet.core.api.Key;
import algores.holonet.core.api.Range;
import algores.holonet.core.api.RangeBase;
import algores.holonet.core.api.tier0.routing.RoutingDistance;
import algores.holonet.core.api.tier0.routing.RoutingEntry;
import algores.holonet.core.api.tier0.routing.RoutingServiceBase;
import algores.holonet.core.api.tier0.rpc.RpcService;
import com.google.common.base.Optional;
import org.akraievoy.base.Die;

import java.util.Collections;
import java.util.List;

import static algores.holonet.core.api.tier0.routing.RoutingPackage.*;

/**
 * Default implementation.
 */
public class RingRoutingServiceImpl extends RoutingServiceBase implements RingRoutingService {
  protected RoutingDistance routingDistance;

  protected RoutingEntry successor;
  protected RoutingEntry predecessor;

  public RingRoutingServiceImpl() {
    routingDistance = new RingRoutingDistance();
  }

  public RingRoutingServiceImpl copy() {
    final RingRoutingServiceImpl copy = new RingRoutingServiceImpl();

    copy.setRedundancy(getRedundancy());
    copy.setRoutingDistance(getRoutingDistance());

    return copy;
  }

  public void init(Node ownerNode) {
    super.init(ownerNode);

    final RoutingEntry ownRoute = new RoutingEntry(
        owner.getKey(),
        this.owner.getAddress(),
        new RangeBase(owner.getKey(), owner.getKey()),
        owner.getServices().getStorage().getEntryCount()
    );
    routez.add(FLAVOR_OWNER, ownRoute);

    successor = ownRoute;
    predecessor = ownRoute;
  }

  public RoutingDistance getRoutingDistance() {
    return routingDistance;
  }

  public void setRoutingDistance(RoutingDistance routingDistance) {
    this.routingDistance = routingDistance;
  }

  public RoutingEntry getSuccessor() {
    return successor;
  }

  public void setSuccessor(RoutingEntry successor) {
    Die.ifNull("successor", successor);
    this.successor = successor;

    update(successor, Event.DISCOVERED);
  }

  public RoutingEntry getPredecessor() {
    return predecessor;
  }

  public RoutingEntry setPredecessor(RoutingEntry predecessor) {
    Die.ifNull("predecessor", predecessor);
    this.predecessor = predecessor;
    final RoutingEntry ownRoute = ownRoute();

    final Range newRange = new RangeBase(predecessor.getKey().next(), ownRoute.getKey().next());
    ownRoute.updateRanges(newRange);

    update(predecessor, Event.DISCOVERED);
    routez.add(FLAVOR_OWNER, ownRoute);
    return ownRoute;
  }

  protected Flavor flavorize(RoutingEntry entry) {
    final Address address = entry.getAddress();

    if (address.equals(getOwner().getAddress())) {
      return FLAVOR_OWNER;
    }

    if (address.equals(predecessor.getAddress())) {
      return FLAVOR_PREDECESSOR;
    }

    if (address.equals(successor.getAddress())) {
      return FLAVOR_SUCCESSOR;
    }

    return FLAVOR_EXTRA;
  }

  @Override
  public RoutingEntry getPredecessorSafe() throws CommunicationException {
    final RpcService rpc = owner.getServices().getRpc();

    final RoutingEntry predEntry = getPredecessor();
    final Address predAddr = predEntry.getAddress();
    final Optional<RingRoutingService> predRouting =
        rpc.rpcTo(predAddr, RingRoutingService.class);
    if (predRouting.isPresent()) {
      return predRouting.get().ownRoute();
    }

    registerCommunicationFailure(predAddr);

    final RecoverRefTuple tuple =
        recoverRef("predecessor", predEntry.getKey());
    final RoutingEntry newPredecessor =
        tuple.remoteRoutingOpt.get().ownRoute();
    setPredecessor(newPredecessor);

    return newPredecessor;
  }

  public static class RecoverRefTuple {
    public final RoutingEntry recovered;
    public final Optional<RingRoutingService> remoteRoutingOpt;

    public RecoverRefTuple(
        final RoutingEntry recovered,
        final Optional<RingRoutingService> remoteRoutingOpt
    ) {
      this.recovered = recovered;
      this.remoteRoutingOpt = remoteRoutingOpt;
    }
  }

  public RecoverRefTuple recoverRef(
      final String refFlavor,
      final Key targetKey
  ) {
    final RpcService rpc = owner.getServices().getRpc();

    final List<RoutingEntry> routes = getRoutes();
    Collections.sort(routes, distanceOrder(targetKey));
    int triedRoutes = 0;
    for (RoutingEntry route : routes) {
      if (route.equals(ownRoute())) {
        continue;
      }

      triedRoutes++;
      final Optional<RingRoutingService> routingOpt =
          rpc.rpcTo(route.getAddress(), RingRoutingService.class);

      if (routingOpt.isPresent()) {
        return new RecoverRefTuple(route, routingOpt);
      }
      registerCommunicationFailure(route.getAddress());
    }

    throw new CommunicationException(
        String.format(
            "unable to recover any " + refFlavor + " after peer failure, tried %d routes",
            triedRoutes
        )
    );
  }
}
