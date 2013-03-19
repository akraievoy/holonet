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

public class ServiceFactory {
  private final OverlayService overlay;
  private final LookupService lookup;
  private final RpcService rpc;
  private final StorageService storage;
  private final RoutingService routing;

  public ServiceFactory() {
    this(
        new StorageServiceBase(),
        new RpcServiceBase(),
        new LookupServiceBase(),
        new ChordRoutingServiceImpl(),
        new ChordServiceBase()
    );
  }

  private ServiceFactory(
      final StorageService storage0,
      final RpcService rpc0,
      final LookupService lookup0,
      final RoutingService routing0,
      final OverlayService overlay0
  ) {
    overlay = overlay0;
    lookup = lookup0;
    rpc = rpc0;
    storage = storage0;
    routing = routing0;
  }

  public double routingRedundancy() {
    return routing.getRedundancy();
  }

  public ServiceFactory routingRedundancy(final double routingRedundancy) {
    final RoutingService routing0 = createRouting();
    routing0.setRedundancy(routingRedundancy);
    return new ServiceFactory(
        storage, rpc, lookup, routing0, overlay
    );
  }

  public double maxFingerFlavorNum() {
    return routing.getMaxFingerFlavorNum();
  }

  public ServiceFactory maxFingerFlavorNum(final int maxFingerFlavorNum) {
    final RoutingService routing0 = createRouting();
    routing0.setMaxFingerFlavorNum(maxFingerFlavorNum);
    return new ServiceFactory(
        storage, rpc, lookup, routing0, overlay
    );
  }

  public ServiceFactory lookup(LookupService lookup0) {
    return new ServiceFactory(
        storage, rpc, lookup0, routing, overlay
    );
  }

  public ServiceFactory overlay(OverlayService overlay0) {
    return new ServiceFactory(
        storage, rpc, lookup, routing, overlay0
    );
  }

  public ServiceFactory setRouting(RoutingService routing0) {
    return new ServiceFactory(
        storage, rpc, lookup, routing0, overlay
    );
  }

  public ServiceFactory rpc(RpcService rpc0) {
    return new ServiceFactory(
        storage, rpc0, lookup, routing, overlay
    );
  }

  public ServiceFactory storage(StorageService storage0) {
    return new ServiceFactory(
        storage0, rpc, lookup, routing, overlay
    );
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