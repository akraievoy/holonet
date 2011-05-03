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

import algores.holonet.core.api.Address;
import algores.holonet.core.api.Key;
import algores.holonet.core.api.Range;

import java.util.Comparator;

/**
 * Defines an algorithm to select next hop.
 * This class should be stateless.
 */
public interface RoutingPreference {
  /**
   * Returns true in case <code>curRange</code> is preferred over <code>bestRange</code>.
   *
   * @param localAddress for which preference is evaluated
   * @param target    the Key we route towards
   * @param curAddress address of the range we are looking at
   * @param curRange  range we are looking at
   * @param bestAddress address of the range currently deemed as best
   * @param bestRange best range for now
   *  
   * @return true in case <code>curRange</code> is preferred over <code>bestRange</code>.
   */
  boolean isPreferred(
      Address localAddress, Key target,
      Address curAddress, Range curRange,
      Address bestAddress, Range bestRange
  );
}
