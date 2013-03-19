/*
 Copyright 2012 Anton Kraievoy akraievoy@gmail.com
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

package algores.holonet.protocols;

import algores.holonet.core.Network;
import algores.holonet.core.ServiceFactory;
import algores.holonet.core.api.tier0.routing.RoutingService;
import algores.holonet.core.api.tier1.overlay.OverlayService;
import org.akraievoy.cnet.gen.vo.EntropySourceRandom;

public class ContextMeta {
  private final ServiceFactory services;

  public ContextMeta() {
    this(
        new ServiceFactory()
    );
  }

  private ContextMeta(final ServiceFactory services0) {
    services = services0;
  }

  public ContextMeta routing(RoutingService routing) {
    return new ContextMeta(services.setRouting(routing));
  }

  public ContextMeta overlay(OverlayService overlay) {
    return new ContextMeta(services.overlay(overlay));
  }

  public ContextMeta routingRedundancy(final double routingRedundancy) {
    return new ContextMeta(services.routingRedundancy(routingRedundancy));
  }

  public ContextMeta maxFingerFlavorNum(final int maxFingerFlavorNum) {
    return new ContextMeta(services.maxFingerFlavorNum(maxFingerFlavorNum));
  }

  public Context create(final long seed) {
    final EntropySourceRandom eSource = new EntropySourceRandom();
    eSource.setSeed(seed);

    final Network net = new Network();
    net.setFactory(services);

    return new Context(eSource, net);
  }
}
