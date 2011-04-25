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

import algores.holonet.core.Node;
import algores.holonet.core.ServiceFactory;
import algores.holonet.core.api.LocalService;
import algores.holonet.core.api.LocalServiceBase;
import algores.holonet.core.api.tier0.routing.RoutingService;
import algores.holonet.core.api.tier0.storage.StorageService;
import algores.holonet.core.api.tier1.delivery.LookupService;
import algores.holonet.core.api.tier1.overlay.OverlayService;
import org.akraievoy.base.Die;

/**
 * Default implementation.
 */
public class ServiceRegistryBase extends LocalServiceBase implements ServiceRegistry {
  protected ServiceFactory factory;

  protected StorageService storage;
  protected LookupService lookup;
  protected OverlayService overlay;
  protected RoutingService routing;
  protected RpcService rpc;

  public ServiceRegistryBase() {
  }

  public void setFactory(ServiceFactory factory) {
    this.factory = factory;
  }

  public void init(final Node ownerNode) {
    Die.ifNull("factory", factory);

    super.init(ownerNode);

    storage = factory.createStorage();
    overlay = factory.createOverlay();
    routing = factory.createRouting();
    rpc = factory.createRpc();
    lookup = factory.createLookup();

    ((LocalService) storage).init(ownerNode);
    ((LocalService) overlay).init(ownerNode);
    ((LocalService) routing).init(ownerNode);
    ((LocalService) rpc).init(ownerNode);
    ((LocalService) lookup).init(ownerNode);
  }

  public ServiceRegistryBase copy() {
    final ServiceRegistryBase copy = new ServiceRegistryBase();

    copy.setFactory(factory);

    return copy;
  }

  @SuppressWarnings({"unchecked"})
  public <E> E resolveService(final Class<E> serviceInterface) {
    if (StorageService.class.isAssignableFrom(serviceInterface)) {
      return (E) getStorage();
    }

    if (OverlayService.class.isAssignableFrom(serviceInterface)) {
      return (E) getOverlay();
    }

    if (RoutingService.class.isAssignableFrom(serviceInterface)) {
      return (E) getRouting();
    }

    if (LookupService.class.isAssignableFrom(serviceInterface)) {
      return (E) getLookup();
    }

    if (ServiceRegistryBase.class.isAssignableFrom(serviceInterface)) {
      return (E) getRegistry();
    }

    throw Die.unexpected("serviceInterface", serviceInterface);
  }

  public StorageService getStorage() {
    Die.ifNull("storage", storage);

    return storage;
  }

  public RoutingService getRouting() {
    Die.ifNull("routing", routing);

    return routing;
  }

  public LookupService getLookup() {
    Die.ifNull("lookup", lookup);

    return lookup;
  }

  public OverlayService getOverlay() {
    Die.ifNull("routing", routing);

    return overlay;
  }

  public RpcService getRpc() {
    Die.ifNull("rpc", rpc);

    return rpc;
  }

  public ServiceRegistry getRegistry() {
    return this;
  }
}
