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

package algores.holonet.core.events;

import algores.holonet.core.CommunicationException;
import algores.holonet.core.Network;
import algores.holonet.core.Node;
import org.akraievoy.cnet.gen.vo.EntropySource;

/**
 * Stabilizes all nodes in the network event.
 */
public class EventNetStabilize extends Event<EventNetStabilize> {
  public Result executeInternal(Network targetNetwork, final EntropySource eSource) {
    Result result = Result.SUCCESS;

    int stabilizeSucceeded = 0;
    int stabilizeTotal = 0;
    for (Node node : targetNetwork.getAllNodes()) {
      try {
        stabilizeTotal += 1;
        node.getServices().getOverlay().stabilize();
        stabilizeSucceeded += 1;
      } catch (CommunicationException e) {
        result = handleEventFailure(e, null);
/*
        log.debug(
            String.format(
                "stabilize failed: %s",
                e.getMessage()
            ),
            e
        );
*/
      }
    }

    if (stabilizeSucceeded != stabilizeTotal) {
      log.warn(
          String.format(
              "stabilize success ratio: %.6g",
              (float) stabilizeSucceeded / stabilizeTotal
          )
      );
    }

    return result;
  }
}
