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
import algores.holonet.protocols.ring.RingRoutingServiceImpl;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static algores.holonet.core.api.tier0.routing.RoutingPackage.*;

/**
 * Default implementation.
 */
public class ChordRoutingServiceImpl extends RingRoutingServiceImpl implements ChordRoutingService {
  public static List<Flavor> FLAVOR_FINGER = createFingerFlavors();

  private static ArrayList<Flavor> createFingerFlavors() {
    final ArrayList<Flavor> fingers = new ArrayList<Flavor>();
    for (int distanceBits = 0; distanceBits <= Key.BITNESS; distanceBits++) {
      fingers.add(new Flavor("finger" + ":" + distanceBits));
    }
    return fingers;
  }

  public ChordRoutingServiceImpl() {
  }

  public ChordRoutingServiceImpl copy() {
    final ChordRoutingServiceImpl copy = new ChordRoutingServiceImpl();

    copy.setRedundancy(getRedundancy());
    copy.setRoutingDistance(getRoutingDistance());
    copy.setMaxFingerFlavorNum(getMaxFingerFlavorNum());

    return copy;
  }

  @Override
  protected Flavor flavorize(RoutingEntry entry) {
    final Flavor superFlavor = super.flavorize(entry);

    if (!FLAVOR_EXTRA.equals(superFlavor)) {
      return superFlavor;
    }

    final Key fromKey = getOwner().getKey();
    final Key toKey = entry.getNodeId();
    final BigInteger distance = fromKey.distance(toKey);
    final Flavor flavor = FLAVOR_FINGER.get(distance.bitLength());

    return flavor;
  }
}