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

package algores.holonet.protocols.chord;

import algores.holonet.core.api.Key;
import algores.holonet.core.api.tier0.routing.RoutingEntry;
import algores.holonet.protocols.ring.RingRoutingServiceImpl;

import java.math.BigInteger;

/**
 * Default implementation.
 */
public class ChordRoutingServiceImpl extends RingRoutingServiceImpl implements ChordRoutingService {
  public ChordRoutingServiceImpl() {
  }

  public ChordRoutingServiceImpl copy() {
    final ChordRoutingServiceImpl copy = new ChordRoutingServiceImpl();

    copy.setRedundancy(getRedundancy());
    copy.setRoutingDistance(getRoutingDistance());

    return copy;
  }

  @Override
  protected FlavorTuple flavorize(RoutingEntry owner, RoutingEntry entry) {
    final FlavorTuple superFlavor = super.flavorize(owner, entry);

    if (!FLAVOR_EXTRA.equals(superFlavor.flavor)) {
      return superFlavor;
    }

    final Key fromKey = getOwner().getKey();
    final Key toKey = entry.getNodeId();
    final BigInteger distance = fromKey.distance(toKey);
    final String flavor = FLAVORBASE_FINGER + ":" + (distance.bitLength() - 1);

    return new FlavorTuple(flavor, false);
  }
}