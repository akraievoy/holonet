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
import com.google.common.base.Optional;

/**
 * RPC facility.
 */
public interface RpcService {
  /**
   * Should be used only from Protocol implementors, as it counts as RPC call to remote node.
   *
   * @param anotherAddress address to ping
   * @return true if node is alive
   */
  //  FIXME recode this
  boolean isAlive(Address anotherAddress);

  /**
   * Simulating RPC method invocation.
   * <p/>
   * Each thread maintains only one RPC proxy, which is reset dynamically upon call if this method.
   * <p/>
   * RPC calls should be done via call rpcTo(aNode, serviceClass).serviceMethod(),
   * without storing remote proxy instance for future use as only one call is possible at a time.
   *
   * @param calleeAddress to talk to.
   * @param service       service interface class to query for, see {@link algores.holonet.core.api.tier0.rpc.ServiceRegistry#resolveService(Class)}
   * @return proxy to protocol running on callee, registering only one method invocation as RPC call and failing subsequent ones, or null if node is offline.
   * @throws algores.holonet.core.CommunicationException propagated
   */
  <E> Optional<E> rpcTo(AddressSource calleeAddress, Class<E> service) throws CommunicationException;

  Address getCaller();
}