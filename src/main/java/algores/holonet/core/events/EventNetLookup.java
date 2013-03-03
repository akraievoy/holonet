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
import com.google.common.base.Optional;
import org.akraievoy.cnet.gen.vo.EntropySource;

import java.util.Collection;

/**
 * Lookup entry event.
 */
public class EventNetLookup extends Event<EventNetLookup> {
  protected int retries = 1;

  public Result executeInternal(final Network network, final EntropySource eSource) {
    Result aggregateResult = Result.PASSIVE;
    for (int sequentialIndex = 0; sequentialIndex < retries; sequentialIndex++) {
      Optional<RequestPair> optRequest =
          network.generateRequestPair(eSource);
      if (!optRequest.isPresent()) {
        if (sequentialIndex > 0) {
          throw new IllegalStateException(
              "request model became empty amid request generation streak?"
          );
        }
        break;
      }
      RequestPair request = optRequest.get();

      Collection<Key> serverKeys =
          request.server.getServices().getStorage().getKeys();

      final Key mapping =
          serverKeys.isEmpty() ?
              //  we may also pull other keys from the range, not only the greatest one
              request.server.getServices().getRouting().ownRoute().getRange().getRKey().prev() :
              eSource.randomElement(serverKeys);
      final LookupService lookupSvc =
          request.client.getServices().getLookup();

      final Address address;
      try {
        address = lookupSvc.lookup(
            mapping.getKey(), true, LookupService.Mode.GET,
            Optional.of(request.server.getAddress())
        );
      } catch (CommunicationException e) {
        if (!aggregateResult.equals(Result.FAILURE)) {
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

      aggregateResult = Result.SUCCESS;
    }

    return aggregateResult;
  }

  public void setRetries(int retryCount) {
    this.retries = retryCount;
  }

  public EventNetLookup withRetries(int retryCount) {
    setRetries(retryCount);
    return this;
  }
}
