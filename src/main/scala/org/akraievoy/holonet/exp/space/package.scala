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

package object space {

  val SPACE_EMPTY = Seq(Seq.empty[ParamPos]).toStream

  val PAR_AXIS_EMPTY = Map(
    false -> Seq.empty[Param],
    true -> Seq.empty[Param]
  )

  val PAR_SPACE_EMPTY = Map(
    true -> SPACE_EMPTY,
    false -> SPACE_EMPTY
  )

}
