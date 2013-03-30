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
import algores.holonet.core.api.Range;
import algores.holonet.core.api.RangeBase;
import algores.holonet.core.api.tier0.routing.RoutingDistance;
import algores.holonet.core.api.tier0.routing.RoutingServiceBase;
import algores.holonet.core.api.tier0.rpc.RpcService;
import com.google.common.base.Optional;
import org.akraievoy.base.Die;

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

    final RoutingEntry ownRoute = RoutingEntry.own(
        owner.getKey(),
        this.owner.getAddress(),
        owner.getServices().getStorage().getEntryCount(),
        new RangeBase(owner.getKey(), owner.getKey())
    );
    routes.add(FLAVOR_OWNER, ownRoute);

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

    update(Event.DISCOVERED, successor);
  }

  public RoutingEntry getPredecessor() {
    return predecessor;
  }

  public RoutingEntry setPredecessor(RoutingEntry predecessor) {
    Die.ifNull("predecessor", predecessor);
    this.predecessor = predecessor;
    final RoutingEntry ownRoute = ownRoute();

    final Range newRange = new RangeBase(predecessor.getKey().next(), ownRoute.getKey().next());
    final RoutingEntry newOwnRoute = ownRoute.ranges(newRange);

    update(Event.DISCOVERED, predecessor);
    routes.update(newOwnRoute);
    return newOwnRoute;
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

    final Optional<RoutingEntry> tuple =
      registerCommunicationFailure(predAddr, true);

    if (tuple.isPresent()) {
      return tuple.get();
    } else {
      throw new CommunicationException("unable to recover " + FLAVOR_PREDECESSOR);
    }
  }

  @Override
  protected void updateOnRecover(
      final Flavor refFlavor,
      final RoutingEntry recoveredRoute
  ) {
    if (refFlavor.equals(FLAVOR_PREDECESSOR)) {
      setPredecessor(recoveredRoute);
    } else if (refFlavor.equals(FLAVOR_SUCCESSOR)) {
      setSuccessor(recoveredRoute);
    }
  }
}
