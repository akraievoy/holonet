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

package algores.holonet.core.api.tier0.routing;

import algores.holonet.core.api.Key;
import algores.holonet.core.api.Range;

import java.util.Comparator;

/**
 * Base implementation of {@link RoutingPreference#createRangeComparator(algores.holonet.core.api.Key)}.
 */
public abstract class RoutingPreferenceBase implements RoutingPreference {
  public Comparator<Range> createRangeComparator(final Key target) {
    return new Comparator<Range>() {
      public int compare(Range r1, Range r2) {
        if (isPreferred(target, r1, r2)) {
          return -1;
        }

        if (isPreferred(target, r2, r1)) {
          return 1;
        }

        return 0;
      }
    };
  }

  public Comparator<RoutingEntry> createReComparator(final Key target) {
    return new Comparator<RoutingEntry>() {
      public int compare(RoutingEntry r1, RoutingEntry r2) {
        final Range bestR1 = r1.selectRange(target, RoutingPreferenceBase.this);
        final Range bestR2 = r2.selectRange(target, RoutingPreferenceBase.this);
        if (isPreferred(target, bestR1, bestR2)) {
          return -1;
        }

        if (isPreferred(target, bestR2, bestR1)) {
          return 1;
        }

        return 0;
      }
    };
  }
}
