/*
 Copyright 2013 Anton Kraievoy akraievoy@gmail.com
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

package algores.holonet.core.events;

import algores.holonet.core.Network;
import algores.holonet.core.Node;
import org.akraievoy.cnet.gen.vo.EntropySource;

import java.util.Collection;

/**
 * Stabilizes all nodes in the network event.
 */
public class EventNetReflavor extends Event<EventNetReflavor> {
  public Result executeInternal(Network targetNetwork, final EntropySource eSource) {
    Result result = Result.SUCCESS;

    final Collection<Node> allNodes = targetNetwork.getAllNodes();
    for (Node node : allNodes) {
      node.getServices().getRouting().fullReflavor();
    }

    return result;
  }
}
