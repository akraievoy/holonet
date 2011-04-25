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

import algores.holonet.core.api.Key;
import algores.holonet.core.api.KeySpace;
import algores.holonet.core.api.Range;
import algores.holonet.core.api.tier0.routing.RoutingPreferenceBase;

class TrivialPreference extends RoutingPreferenceBase {
  public boolean isPreferred(Key target, Range curRange, Range bestRange) {
    final int curPrefix = KeySpace.getCommonPrefixLen(curRange.getKey(), target, Key.BITNESS);
    final int bestPrefix = KeySpace.getCommonPrefixLen(bestRange.getKey(), target, Key.BITNESS);

    final int prefixPreference = curPrefix - bestPrefix;

    if (prefixPreference != 0) {
      return prefixPreference > 0;
    }

    final int bitsPreference = curRange.getBits() - bestRange.getBits();

    return bitsPreference > 0;
  }
}
