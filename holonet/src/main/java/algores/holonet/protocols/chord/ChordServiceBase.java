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

package algores.holonet.protocols.chord;

import algores.holonet.capi.Event;
import algores.holonet.core.CommunicationException;
import algores.holonet.core.SimulatorException;
import algores.holonet.core.api.Address;
import algores.holonet.core.api.Key;
import algores.holonet.core.api.KeySpace;
import algores.holonet.core.api.tier0.routing.RoutingEntry;
import algores.holonet.core.api.tier1.delivery.LookupService;
import algores.holonet.protocols.ring.RingService;

public class ChordServiceBase extends RingService implements ChordService {
  public ChordServiceBase copy() {
    return new ChordServiceBase();
  }

  /**
   * The protocol is running on node, a predecessor node address is provided
   * to join the old DHT Ring/Chord.
   *
   * @param dhtNodeAddress a random node address in the old DHT.
   *                       <code>null</code> if node is the first node in the DHT.
   */
  public void join(final Address dhtNodeAddress) throws SimulatorException {
    // check if node is the first node in DHT
    if (dhtNodeAddress == null) {
      //  successor must be the same node, that will mean that all of keys are managed by this node
      final RoutingEntry ownRoute = getRouting().getOwnRoute();
      getRouting().setSuccessor(ownRoute);
      getRouting().setPredecessor(ownRoute);
      return;
    } else {
      final Address successor = rpcToDelivery(dhtNodeAddress).lookup(
          owner.getKey(), false, LookupService.Mode.JOIN
      );
      getRouting().setSuccessor(rpcToRouting(successor).getOwnRoute());
    }

    stabilize();
  }

  public void leave() throws CommunicationException {
    rpcToStorage(getRouting().getSuccessor()).putAll(owner.getServices().getStorage().getDataEntries());
    rpcToRouting(getRouting().getPredecessor()).setSuccessor(getRouting().getSuccessor());
    rpcToRouting(getRouting().getSuccessor()).setPredecessor(getRouting().getPredecessor());
  }

  /**
   * For maintaining finger table/link purpose
   */
  public void stabilize() throws CommunicationException {
    if (!owner.getServices().getRpc().isAlive(getRouting().getSuccessor().getAddress())) {
      throw new CommunicationException("Successor is not alive, stabilize (possibly on join) failed.");
    }

    final RoutingEntry succPred = rpcToRouting(getRouting().getSuccessor()).getPredecessor();
    if (KeySpace.isInOpenRange(succPred, getRouting().getSuccessor(), owner.getAddress())) {
      getRouting().setPredecessor(succPred);
      rpcToRouting(getRouting().getPredecessor()).setSuccessor(getRouting().getOwnRoute());
    } else if (KeySpace.isInOpenLeftRange(owner, getRouting().getSuccessor(), succPred)) {
      getRouting().setSuccessor(succPred);
    }
    rpcToRouting(getRouting().getSuccessor()).setPredecessor(getRouting().getOwnRoute());

    if (getRouting().getPredecessor().getAddress().getKey().equals(owner.getKey())) {
      throw new CommunicationException("Duplicated with predecessor: " + owner.getKey());
    }
    if (getRouting().getSuccessor().getAddress().getKey().equals(owner.getKey())) {
      throw new CommunicationException("Duplicated with successor: " + owner.getKey());
    }

    owner.getServices().getStorage().putAll(rpcToStorage(getRouting().getSuccessor()).filter(getRouting().getPredecessor(), false, owner.getKey(), true));

    fixFingers();
  }

  /**
   * Should not be called remotely.
   *
   * @throws algores.holonet.core.CommunicationException
   *          chained
   */
  protected void fixFingers() throws CommunicationException {
    final Key ownerKey = owner.getAddress().getKey();

    //	TODO omit queries in case finger is responsible for multiple powers
    for (int power = 0; power < Key.BITNESS; power++) {
      try {
        final Address address = owner.getServices().getLookup().lookup(
            ownerKey.next(power), false, LookupService.Mode.FIXFINGERS
        );

        //  it's possible that finger ends up pointing to the same node
        //    especially this is true for initial stages
        if (address.equals(owner.getAddress())) {
          continue;
        }

        final RoutingEntry entry = rpcToRouting(address).getOwnRoute();
        getRouting().update(entry, Event.HEART_BEAT);
      } catch (CommunicationException nfe) {
        //	should be ignored...
      }
    }
  }

  public ChordService rpcTo(Address target) throws CommunicationException {
    return owner.getServices().getRpc().rpcTo(target, ChordService.class);
  }
}