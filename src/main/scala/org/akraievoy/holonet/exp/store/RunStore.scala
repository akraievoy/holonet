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

import org.akraievoy.holonet.exp.{ParamName, ParamPos}

case class RunStore(
  es: ExperimentStore,
  spacePos: Seq[ParamPos]
) {
  def lens[T](
    paramName: String
  )(
    implicit mt: Manifest[T]
  ): StoreLens[T] = {
    es.lens[T](paramName, spacePos, Map.empty)
  }

  def lens[T](
    paramName: ParamName[T]
  ): StoreLens[T] = {
    lens(paramName.name)(paramName.mt)
  }

}
