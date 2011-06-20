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
import algores.holonet.core.Env;
import algores.holonet.core.Network;
import algores.holonet.core.Node;
import algores.holonet.core.api.Address;
import algores.holonet.core.api.Key;
import org.akraievoy.cnet.gen.vo.EntropySource;

import java.util.Set;

/**
 * Lookup entry event.
 */
class EventNetLookup extends Event {
  protected int retries = 1;

  public EventComposite.Result executeInternal(final Network network, final EntropySource eSource) {
    final Env.Pair<Node> pair = network.requestPair(eSource);
    final Node lookupSource = pair.source;
    final Node lookupTarget = pair.target;

    final Set<Key> keySet = lookupTarget.getServices().getStorage().getDataEntries().keySet();
    if (keySet.isEmpty()) {
      return EventComposite.Result.PASSIVE;
    }

    EventComposite.Result aggregateResult = EventComposite.Result.PASSIVE;
    for (int sequentialIndex = 0; sequentialIndex < retries; sequentialIndex++) {
      final Key randomKey = eSource.randomElement(keySet);

      final Address address;
      try {
        address = lookupSource.getServices().getLookup().lookup(randomKey, true);
      } catch (CommunicationException e) {
        if (!aggregateResult.equals(EventComposite.Result.FAILURE)) {
          aggregateResult = handleEventFailure(e, null);
        }
        continue;
      }

      if (!address.equals(lookupTarget.getAddress())) {
        network.getInterceptor().reportInconsistentLookup();
      }

      aggregateResult = EventComposite.Result.SUCCESS;
    }

    return aggregateResult;
  }

  public void setRetries(int retryCount) {
    this.retries = retryCount;
  }
}
