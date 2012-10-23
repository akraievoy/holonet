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
import algores.holonet.core.ServiceFactorySpring;
import algores.holonet.core.api.tier0.routing.RoutingService;
import algores.holonet.core.api.tier1.overlay.OverlayService;
import org.akraievoy.cnet.gen.vo.EntropySourceRandom;

public class TextContextMeta {
  private final ServiceFactorySpring services = new ServiceFactorySpring();

  public TextContextMeta() {
    //  nothing to do here
  }

  public TextContextMeta withRouting(RoutingService routing) {
    services.setRouting(routing);
    return this;
  }

  public TextContextMeta withOverlay(OverlayService overlay) {
    services.setOverlay(overlay);
    return this;
  }

  public Context create(final long seed) {
    final EntropySourceRandom eSource = new EntropySourceRandom();
    eSource.setSeed(seed);

    final Network net = new Network();
    net.setFactory(services);

    return new Context(eSource, net);
  }
}
