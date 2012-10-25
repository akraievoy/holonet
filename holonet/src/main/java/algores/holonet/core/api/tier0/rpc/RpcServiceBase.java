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

package algores.holonet.core.api.tier0.rpc;

import algores.holonet.core.CommunicationException;
import algores.holonet.core.api.Address;
import algores.holonet.core.api.AddressSource;
import algores.holonet.core.api.LocalServiceBase;
import org.akraievoy.base.Die;

/**
 * Base implementation of RpcService.
 */
public class RpcServiceBase extends LocalServiceBase implements RpcService {
  public LocalServiceBase copy() {
    return new RpcServiceBase();
  }

  /**
   * Should be used only from Protocol implementors, as it counts as RPC call to remote node.
   *
   * @param anotherAddress address to ping
   * @return true if node is alive
   */
  public boolean isAlive(Address anotherAddress) {
    owner.getNetwork().registerRpcCall(owner, anotherAddress);
    return owner.getNetwork().getEnv().getNode(anotherAddress) != null;
  }

  /**
   * The way to simulate RPC method invocation.
   * <p/>
   * Each thread maintains only one RPC proxy, which is reset dynamically upon call if this method.
   * <p/>
   * So, the best way to work with RPC calls is to call rpcTo(aNode).neededMethod(),
   * without storing remote proxy instance for future use as only one call is possible at a time.
   *
   * @param calleeAddress to talk to.
   * @param service       service interface class to query for, see {@link ServiceRegistry#resolveService(Class)}
   * @return proxy of protocol running on callee, that will automatically register all method invocations as RPC calls.
   * @throws algores.holonet.core.CommunicationException
   *          propagated
   */
  public <E> E rpcTo(final AddressSource calleeAddress, final Class<E> service) throws CommunicationException {
    Die.ifNull("calleeAddress", calleeAddress);

    if (owner.getAddress().equals(calleeAddress)) {
      return owner.getServices().resolveService(service);
    }

    return owner.getNetwork().getRpc().getProxy(owner, calleeAddress.getAddress(), service);
  }

  public Address getCaller() {
    return owner.getNetwork().getRpc().getCall().getSource().getAddress();
  }
}