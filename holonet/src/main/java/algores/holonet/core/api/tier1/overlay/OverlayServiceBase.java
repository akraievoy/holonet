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

package algores.holonet.core.api.tier1.overlay;

import algores.holonet.core.CommunicationException;
import algores.holonet.core.api.Address;
import algores.holonet.core.api.AddressSource;
import algores.holonet.core.api.LocalServiceBase;
import algores.holonet.core.api.tier0.routing.RoutingService;
import algores.holonet.core.api.tier0.rpc.RpcService;
import algores.holonet.core.api.tier0.storage.StorageService;
import algores.holonet.core.api.tier1.delivery.LookupService;

/**
 * Base implementation.
 */
public abstract class OverlayServiceBase extends LocalServiceBase implements OverlayService {
  protected OverlayService rpcTo(final AddressSource target) throws CommunicationException {
    if (target.equals(owner.getAddress())) {
      return this;
    }

    return getRpc().rpcTo(target.getAddress(), OverlayService.class);
  }

  protected StorageService rpcToStorage(final AddressSource target) throws CommunicationException {
    if (target.equals(owner.getAddress())) {
      return getStorage();
    }

    return getRpc().rpcTo(target.getAddress(), StorageService.class);
  }

  protected RoutingService rpcToRouting(final AddressSource target) throws CommunicationException {
    if (target.equals(owner.getAddress())) {
      return getRouting();
    }

    return getRpc().rpcTo(target.getAddress(), RoutingService.class);
  }

  protected LookupService rpcToDelivery(AddressSource address) throws CommunicationException {
    if (address.equals(owner.getAddress())) {
      return getDelivery();
    }

    return getRpc().rpcTo(address.getAddress(), LookupService.class);
  }

  public Address getCaller() {
    return getRpc().getCaller();
  }

  protected RoutingService getRouting() {
    return owner.getServices().getRouting();
  }

  protected StorageService getStorage() {
    return owner.getServices().getStorage();
  }

  protected RpcService getRpc() {
    return owner.getServices().getRpc();
  }

  protected LookupService getDelivery() {
    return owner.getServices().getLookup();
  }

  public String toString() {
    return owner.getKey().toString();
  }
}
