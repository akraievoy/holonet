/*
 Copyright 2013 Anton Kraievoy akraievoy@gmail.com
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

package org.akraievoy.holonet.exp

object Strategy extends Enumeration {
  /**
   * Use the axis fully while iterating over parameter space, no parallelization.
   */
  val ITERATE = Value

  /**
   * Use the axis fully while iterating over parameter space, with possible parallelization.
   */
  val SPAWN = Value

  /**
   * Use only the first value of axis while iterating over parameter space.
   */
  val USE_FIRST = Value

  /**
   * Use only the last value of axis while iterating over parameter space.
   */
  val USE_LAST = Value

  def full(strategy: Strategy.Value): Boolean = {
    (ITERATE eq strategy) || (SPAWN eq strategy)
  }

  def fixed(strategy: Strategy.Value): Boolean = {
    !full(strategy)
  }
}
