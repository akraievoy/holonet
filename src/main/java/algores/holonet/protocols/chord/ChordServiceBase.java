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
import algores.holonet.core.api.tier0.rpc.RpcService;
import algores.holonet.core.api.tier1.delivery.LookupService;
import algores.holonet.protocols.ring.RingRoutingService;
import algores.holonet.protocols.ring.RingRoutingServiceImpl;
import algores.holonet.protocols.ring.RingService;
import com.google.common.base.Optional;

import static algores.holonet.core.api.tier0.routing.Routing.*;

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
      final RoutingEntry ownRoute = getRouting().ownRoute();
      getRouting().setSuccessor(ownRoute);
      getRouting().setPredecessor(ownRoute);
      return;
    } else {
      final Address successor = rpcToDelivery(dhtNodeAddress).lookup(
          owner.getKey(), false, LookupService.Mode.JOIN,
          Optional.<Address>absent()
      );
      getRouting().setSuccessor(rpcToRouting(successor).ownRoute());
    }

    stabilize();
  }

  public void leave() throws CommunicationException {
    rpcToStorage(getRouting().getSuccessor()).putAll(owner.getServices().getStorage().getDataEntries());
    final RoutingEntry updatedSuccessor = rpcToRouting(getRouting().getSuccessor()).setPredecessor(getRouting().getPredecessor());
    rpcToRouting(getRouting().getPredecessor()).setSuccessor(updatedSuccessor);
  }

  /**
   * For maintaining finger table/link purpose
   */
  public void stabilize() throws CommunicationException {
    final RingRoutingServiceImpl ownRouting =
        (RingRoutingServiceImpl) getRouting();
    final RpcService rpc = owner.getServices().getRpc();

    final Address succAddr = getRouting().getSuccessor().getAddress();
    Optional<RingRoutingService> succRoutingOpt =
        rpc.rpcTo(succAddr, RingRoutingService.class);
    if (!succRoutingOpt.isPresent()) {
      final Optional<RoutingEntry> succOpt = ownRouting.registerCommunicationFailure(succAddr, true);
      if (succOpt.isPresent()) {
        succRoutingOpt = rpc.rpcTo(succOpt.get(), RingRoutingService.class);
      } else {
        throw new CommunicationException("unable to recover successor");
      }
    }

    final RoutingEntry succPred = succRoutingOpt.get().getPredecessorSafe();
    if (KeySpace.isInOpenRange(succPred, ownRouting.getSuccessor(), owner.getAddress())) {
      ownRouting.setPredecessor(succPred);
      final Optional<RingRoutingService> rsOpt =
          rpc.rpcTo(succPred.getAddress(), RingRoutingService.class);
      if (rsOpt.isPresent()) {
        rsOpt.get().setSuccessor(ownRouting.ownRoute());
      } else {
        ownRouting.registerCommunicationFailure(succPred.getAddress(), false);
        throw new CommunicationException("predecessor just was alive?");
      }
    } else if (KeySpace.isInOpenLeftRange(owner, ownRouting.getSuccessor(), succPred)) {
      ownRouting.setSuccessor(succPred);
    }
    rpcToRouting(ownRouting.getSuccessor()).setPredecessor(ownRouting.ownRoute());

    owner.getServices().getStorage().putAll(
        rpcToStorage(ownRouting.getSuccessor()).filter(
            ownRouting.getPredecessor(), false, owner.getKey(), true
        )
    );

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
            ownerKey.next(power), false, LookupService.Mode.FIXFINGERS,
            Optional.<Address>absent()
        );

        //  it's possible that finger ends up pointing to the same node
        //    especially this is true for initial stages
        if (address.equals(owner.getAddress())) {
          continue;
        }

        final RoutingEntry entry = rpcToRouting(address).ownRoute();
        getRouting().update(eventToRoute(Event.HEART_BEAT, entry));
      } catch (CommunicationException nfe) {
        //	should be ignored...
      }
    }
  }

}