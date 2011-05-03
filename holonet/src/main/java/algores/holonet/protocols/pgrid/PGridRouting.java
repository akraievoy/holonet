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

package algores.holonet.protocols.pgrid;

import algores.holonet.core.Node;
import algores.holonet.core.api.Range;
import algores.holonet.core.api.RangeBase;
import algores.holonet.core.api.tier0.routing.RoutingEntry;
import algores.holonet.core.api.tier0.routing.RoutingPreference;
import algores.holonet.core.api.tier0.routing.RoutingServiceBase;

class PGridRouting extends RoutingServiceBase {
  protected static final int MAX_SAME_PATHS = 5;
  protected static final int MAX_SAME_COMPLEMENT = 7;

  protected final TrivialPreference routingPreference = new TrivialPreference();

  public PGridRouting() {
    super();
  }

  public PGridRouting copy() {
    return new PGridRouting();
  }

  public void init(Node ownerNode) {
    super.init(ownerNode);

    ownRoute = new RoutingEntry(
        ownerNode.getKey(),
        ownerNode.getAddress(),
        new RangeBase(ownerNode.getKey(), 0),
        ownerNode.getServices().getStorage().getEntryCount()
    );

    setRedundancy(MAX_SAME_PATHS);
  }

  protected String flavorize(RoutingEntry owner, RoutingEntry created) {
    final Range ownerRange = owner.getRange();
    final Range otherRange = created.getRange();

    if (ownerRange.isPrefixFor(otherRange, true)) {
      return "replica-specialized";
    }
    if (otherRange.isPrefixFor(ownerRange, false)) {
      return "replica-generalized";
    }

    final int commonPrefixLen = ownerRange.getCommonPrefixLen(otherRange);
    return "complement:" + commonPrefixLen;
  }

  public RoutingPreference getRoutingPreference() {
    return routingPreference;
  }

  public Range getPath() {
    return getOwnRoute().getRange();
  }

  public void setPath(Range newPath) {
    final RoutingEntry ownRoute = getOwnRoute();
    updateOwnRoute(new RoutingEntry(newPath.getKey(), ownRoute.getAddress(), newPath, ownRoute.getEntryCount()));
  }

  public String toString() {
    return getPath().toString();
  }
}
