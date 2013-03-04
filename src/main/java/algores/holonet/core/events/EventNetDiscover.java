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

import algores.holonet.core.CommunicationException;
import algores.holonet.core.Network;
import algores.holonet.core.Node;
import algores.holonet.core.api.tier1.delivery.LookupService;
import com.google.common.base.Optional;
import org.akraievoy.cnet.gen.vo.EntropySource;

import java.util.ArrayList;
import java.util.List;

/**
 * Stabilizes all nodes in the network event.
 */
public class EventNetDiscover extends Event<EventNetDiscover> {
  public Result executeInternal(Network targetNetwork, final EntropySource eSource) {
    Result result = Result.SUCCESS;

    final List<Node> allNodes = new ArrayList<Node>(targetNetwork.getAllNodes());
    int discoverSucceeded = 0;
    int discoverTotal = 0;
    for (int from = 0; from < allNodes.size(); from++) {
      final Node nodeFrom =  allNodes.get(from);
      for (int into = 0; into < allNodes.size(); into++) {
        if (from == into) {
          continue;
        }

        final Node nodeInto = allNodes.get(into);
          try {
            discoverTotal += 1;
            nodeFrom.getServices().getLookup().lookup(
                nodeInto.getKey(),
                false,
                LookupService.Mode.GET,
                Optional.of(nodeInto.getAddress())
            );
            discoverSucceeded += 1;
          } catch (CommunicationException e) {
            result = handleEventFailure(e, null);
/*
            log.debug(
                String.format(
                    "stabilize failed: %s",
                    e.getMessage()
                )
            );
*/
          }

      }
    }

    if (discoverSucceeded != discoverTotal) {
      log.warn(
          String.format(
              "stabilize success ratio: %.6g",
              (float) discoverSucceeded / discoverTotal
          )
      );
    }

    return result;
  }
}
