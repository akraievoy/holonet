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

package algores.holonet.protocols.ring;

import algores.holonet.core.api.Key;
import algores.holonet.core.api.KeySpace;
import algores.holonet.core.api.Range;
import algores.holonet.core.api.tier0.routing.RoutingPreferenceBase;

/**
 * Used for ring topology.
 */
public class RingRoutingPreference extends RoutingPreferenceBase {
  public boolean isPreferred(Key target, Range curRange, Range bestRange) {
    final boolean preferred = KeySpace.isInOpenRightRange(target, bestRange.getRKey(), curRange.getRKey());

    return preferred;
  }
}
