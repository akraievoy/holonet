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
import algores.holonet.core.RequestPair;
import algores.holonet.core.api.Address;
import algores.holonet.core.api.Key;
import algores.holonet.core.api.tier1.delivery.LookupService;
import org.akraievoy.cnet.gen.vo.EntropySource;

import java.util.Collection;

/**
 * Lookup entry event.
 */
class EventNetLookup extends Event {
  protected int retries = 1;

  public EventComposite.Result executeInternal(final Network network, final EntropySource eSource) {
    EventComposite.Result aggregateResult = EventComposite.Result.PASSIVE;
    for (int sequentialIndex = 0; sequentialIndex < retries; sequentialIndex++) {
      RequestPair request =
          network.generateRequestPair(eSource);
      Collection<Key> serverKeys =
          request.server.getServices().getStorage().getKeys();

      final Key mapping =
          serverKeys.isEmpty() ?
              //  we may also pull other keys from the range, not only the greatest one
              request.server.getServices().getRouting().getOwnRoute().getRange().getRKey().prev() :
              eSource.randomElement(serverKeys);
      final LookupService lookupSvc =
          request.client.getServices().getLookup();

      final Address address;
      try {
        address = lookupSvc.lookup(
            mapping.getKey(), true, LookupService.Mode.GET
        );
      } catch (CommunicationException e) {
        if (!aggregateResult.equals(EventComposite.Result.FAILURE)) {
          aggregateResult = handleEventFailure(e, null);
        }
        continue;
      }

      final Node lookupResult = network.getEnv().getNode(address);
      if (
        !lookupResult.equals(request.server)
      ) {
        network.getInterceptor().reportInconsistentLookup(LookupService.Mode.GET);
      }

      aggregateResult = EventComposite.Result.SUCCESS;
    }

    return aggregateResult;
  }

  public void setRetries(int retryCount) {
    this.retries = retryCount;
  }
}
