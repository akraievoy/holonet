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

import algores.holonet.core.CommunicationException;
import algores.holonet.core.SimulatorException;
import algores.holonet.core.api.Address;
import algores.holonet.core.api.AddressSource;
import algores.holonet.core.api.Key;
import algores.holonet.core.api.KeySpace;
import algores.holonet.core.api.tier0.routing.RoutingEntry;
import algores.holonet.core.api.tier0.storage.StorageService;
import algores.holonet.core.api.tier1.delivery.LookupService;
import algores.holonet.core.api.tier1.overlay.OverlayServiceBase;
import com.google.common.base.Throwables;

import java.util.Map;

/**
 * Default implementation of generic DhtProtocol operations - actually is a Ring implementation.
 */
public class RingService extends OverlayServiceBase {
  public RingService copy() {
    return new RingService();
  }

  public void join(Address oldNode) throws SimulatorException {
    if (oldNode == null) {
      return;
    }

    final Address successor = rpcToDelivery(oldNode).lookup(
        owner.getKey(), false, LookupService.Mode.JOIN
    );
    getRouting().setSuccessor(new RoutingEntry(successor.getKey(), successor));

    stabilize();
  }

  public void leave() throws CommunicationException {
    final RoutingEntry successor = getRouting().getSuccessor();
    final RoutingEntry predecessor = getRouting().getPredecessor();

    rpcToStorage(successor).putAll(owner.getServices().getStorage().getDataEntries());

    rpcToRouting(predecessor).setSuccessor(successor);
    rpcToRouting(successor).setPredecessor(predecessor);
  }

  public void stabilize() throws CommunicationException {
    final RingRoutingService routing = getRouting();
    if (!owner.getServices().getRpc().isAlive(routing.getSuccessor().getAddress())) {
      throw new CommunicationException("Successor is not alive, stabilize (possibly on join) failed.");
    }

    final RoutingEntry succPred = rpcToRouting(routing.getSuccessor()).getPredecessor();
    if (KeySpace.isInOpenRange(succPred, routing.getSuccessor().getAddress(), owner.getAddress())) {
      routing.setPredecessor(succPred);
      rpcToRouting(succPred).setSuccessor(routing.getOwnRoute());
    } else if (KeySpace.isInOpenLeftRange(owner, routing.getSuccessor(), succPred)) {
      routing.setSuccessor(succPred);
    }
    rpcToRouting(routing.getSuccessor()).setPredecessor(routing.getOwnRoute());

    if (routing.getPredecessor().getKey().equals(owner.getKey())) {
      throw new CommunicationException("Duplicated with predecessor: " + owner.getKey());
    }
    if (routing.getSuccessor().getKey().equals(owner.getKey())) {
      throw new CommunicationException("Duplicated with successor: " + owner.getKey());
    }

    final StorageService succStorage = rpcToStorage(routing.getSuccessor());
    final Map<Key, Object> ownData = succStorage.filter(routing.getPredecessor().getKey(), false, owner.getKey(), true);
    owner.getServices().getStorage().putAll(ownData);
  }

  protected RingRoutingService rpcToRouting(final AddressSource target) throws CommunicationException {
    return owner.getServices().getRpc().rpcTo(target.getAddress(), RingRoutingService.class);
  }

  protected RingRoutingService getRouting() {
    return (RingRoutingService) owner.getServices().getRouting();
  }

  public String toString() {
    final StorageService storage = owner.getServices().getStorage();
    final RingRoutingService routing = getRouting();

    final String toString;

    try {
      toString = storage.getKeys().size() + " @ (" + routing.getPredecessor().getNodeId() + ":" + owner.getKey() + "] > " + routing.getSuccessor().getNodeId();
    } catch (CommunicationException e) {
      throw Throwables.propagate(e);
    }

    return toString;
  }
}
