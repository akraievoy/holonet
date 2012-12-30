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

package org.akraievoy.holonet.exp.store

import scalaz.Lens

case class ValueSerializer[P, T](
  mt: Manifest[_ <: T],
  alias: String,
  lens: Lens[P, T]
) extends Serializer[T]

object ValueSerializer {
  def apply[P, T](
    alias: String,
    lens: Lens[P, T]
  )(
    implicit mt: Manifest[T]
  ) =
    new ValueSerializer[P, T](mt, alias, lens)
}
