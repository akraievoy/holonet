/*
 Copyright 2012 Anton Kraievoy akraievoy@gmail.com
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

package algores.holonet.protocols.ring;

import algores.holonet.core.api.Address;
import algores.holonet.core.api.Key;
import algores.holonet.core.api.Range;
import algores.holonet.core.api.tier0.routing.RoutingDistance;

import java.math.BigInteger;

/**
 * Used for ring topology.
 */
public class RingRoutingDistance implements RoutingDistance {
  final long BITNESS = BigInteger.ONE.shiftLeft(Key.BITNESS).longValue();

  public double apply(
      Address localAddress,
      Key target,
      Address curAddress,
      Range curRange
  ) {
    final long distance =
        curRange.getRKey().toLong() - target.toLong();
    final long distanceClockwise;
    if (distance < 0) {
      distanceClockwise = distance + BITNESS;
    } else {
      distanceClockwise = distance;
    }

    return distanceClockwise;
  }
}
