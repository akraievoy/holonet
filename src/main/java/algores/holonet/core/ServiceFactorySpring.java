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

import algores.holonet.core.api.LocalService;
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

public class ServiceFactorySpring implements ServiceFactory {
  protected OverlayService overlay = new ChordServiceBase();
  protected LookupService lookup = new LookupServiceBase();
  protected RpcService rpc = new RpcServiceBase();
  protected StorageService storage = new StorageServiceBase();
  protected RoutingService routing = new ChordRoutingServiceImpl();

  public ServiceFactorySpring() {
    //  nothing to do here
  }

  public void setRoutingRedundancy(final double routingRedundancy) {
    routing.setRedundancy(routingRedundancy);
  }

  public void setMaxFingerFlavorNum(final int maxFingerFlavorNum) {
    routing.setMaxFingerFlavorNum(maxFingerFlavorNum);
  }

  public void setLookup(LookupService lookup) {
    this.lookup = lookup;
  }

  public void setOverlay(OverlayService overlay) {
    this.overlay = overlay;
  }

  public void setRouting(RoutingService routing) {
    this.routing = routing;
  }

  public void setRpc(RpcService rpc) {
    this.rpc = rpc;
  }

  public void setStorage(StorageService storage) {
    this.storage = storage;
  }

  public RoutingService createRouting() {
    return (RoutingService) ((LocalService) routing).copy();
  }

  public OverlayService createOverlay() {
    return (OverlayService) ((LocalService) overlay).copy();
  }

  public LookupService createLookup() {
    return (LookupService) ((LocalService) lookup).copy();
  }

  public RpcService createRpc() {
    return (RpcService) ((LocalService) rpc).copy();
  }

  public StorageService createStorage() {
    return (StorageService) ((LocalService) storage).copy();
  }
}