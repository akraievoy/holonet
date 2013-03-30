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
import com.google.common.base.Optional;

/**
 * Base implementation.
 */
public abstract class OverlayServiceBase extends LocalServiceBase implements OverlayService {

  protected StorageService rpcToStorage(final AddressSource server) throws CommunicationException {
    if (server.equals(owner.getAddress())) {
      return getStorage();
    }

    final Optional<StorageService> storageOpt = getRpc().rpcTo(server.getAddress(), StorageService.class);
    if (storageOpt.isPresent()) {
      return storageOpt.get();
    } else {
      getRouting().registerCommunicationFailure(server.getAddress(), true);
      throw new CommunicationException(
          String.format("%s is offline", server)
      );
    }
  }

  protected LookupService rpcToDelivery(AddressSource server) throws CommunicationException {
    if (server.equals(owner.getAddress())) {
      return getDelivery();
    }

    final Optional<LookupService> lsOpt =
        getRpc().rpcTo(server.getAddress(), LookupService.class);
    if (lsOpt.isPresent()) {
      return lsOpt.get();
    } else {
      getRouting().registerCommunicationFailure(server.getAddress(), true);
      throw new CommunicationException(
          String.format("%s is offline", server)
      );
    }
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
