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
import algores.holonet.core.Node;
import algores.holonet.core.api.Range;
import algores.holonet.core.api.RangeBase;
import algores.holonet.core.api.tier0.routing.RoutingDistance;
import algores.holonet.core.api.tier0.routing.RoutingEntry;
import algores.holonet.core.api.tier0.routing.RoutingServiceBase;
import org.akraievoy.base.Die;

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

    ownRoute = new RoutingEntry(
        owner.getKey(),
        this.owner.getAddress(),
        new RangeBase(owner.getKey(), owner.getKey()),
        owner.getServices().getStorage().getEntryCount()
    );

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

    final Range newRange = new RangeBase(predecessor.getKey().next(), ownRoute.getKey().next());
    ownRoute.updateRanges(newRange);

    update(predecessor, Event.DISCOVERED);

    return ownRoute;
  }

  protected FlavorTuple flavorize(RoutingEntry owner, RoutingEntry entry) {
    if (entry.equals(predecessor)) {
      return new FlavorTuple(FLAVOR_PREDECESSOR, true);
    }

    if (entry.equals(successor)) {
      return new FlavorTuple(FLAVOR_SUCCESSOR, true);
    }

    return new FlavorTuple(FLAVOR_EXTRA, false);
  }
}
