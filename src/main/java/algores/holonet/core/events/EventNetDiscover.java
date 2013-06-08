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
 * Forces pair-wise lookups across the network.
 */
public class EventNetDiscover extends Event<EventNetDiscover> {
  private LookupService.Mode mode = LookupService.Mode.GET;
  private float successRatio;
  private boolean excludeOffgridNodes = false;

  public EventNetDiscover mode(LookupService.Mode newMode) {
    this.mode = newMode;
    return this;
  }

  public float successRatio() {
    return successRatio;
  }

  public EventNetDiscover excludeOffgridNodes(boolean excludeOffgridNodes0) {
    this.excludeOffgridNodes = excludeOffgridNodes0;
    return this;
  }

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
        //  nodes which are on-line but have no links may be excluded from stats
        if (
            excludeOffgridNodes && mode == LookupService.Mode.GET &&
            nodeFrom.getServices().getRouting().getStats().routeCount <= 1
        ) {
          continue;
        }

        final Node nodeInto = allNodes.get(into);
          try {
            discoverTotal += 1;
            nodeFrom.getServices().getLookup().lookup(
                nodeInto.getKey(),
                false,
                mode,
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

    successRatio = (float) discoverSucceeded / discoverTotal;

    if (discoverSucceeded != discoverTotal) {
      log.warn(
          String.format(
              "discover success ratio: %.6g",
              successRatio
          )
      );
    }

    return result;
  }
}
