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

package algores.holonet.core;

import algores.holonet.core.api.tier0.routing.RoutingService;
import algores.holonet.core.api.tier0.rpc.RpcService;
import algores.holonet.core.api.tier0.rpc.RpcServiceBase;
import algores.holonet.core.api.tier0.storage.StorageService;
import algores.holonet.core.api.tier0.storage.StorageServiceBase;
import algores.holonet.core.api.tier1.delivery.LookupService;
import algores.holonet.core.api.tier1.delivery.LookupServiceBase;
import algores.holonet.core.api.tier1.overlay.OverlayService;
import algores.holonet.protocols.chord.ChordRoutingServiceImpl;
import algores.holonet.protocols.chord.ChordServiceBase;

public class ServiceFactoryChord implements ServiceFactory {
  public RoutingService createRouting() {
    final ChordRoutingServiceImpl routing = new ChordRoutingServiceImpl();

    routing.setRedundancy(3);

    return routing;
  }

  public OverlayService createOverlay() {
    return new ChordServiceBase();
  }

  public LookupService createLookup() {
    return new LookupServiceBase();
  }

  public RpcService createRpc() {
    return new RpcServiceBase();
  }

  public StorageService createStorage() {
    return new StorageServiceBase();
  }
}
