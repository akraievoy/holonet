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

import org.akraievoy.base.ref.Ref

case class StoreLens[T](
  getFun: (Map[String, Int]) => Option[T],
  setFun: (Map[String, Int], T) => Unit,
  offsets: Map[String, Int] = Map.empty,
  mt: Manifest[T]
) extends Ref[T] {

  def get = getFun(offsets)

  def set(t: T) {
    setFun(offsets, t)
  }

  def getValue = get.get  //  LATER pull the parameter name into the failure vortex

  def setValue(value: T) {
    set(value)
  }

  def offset(paramName: String, posDelta: Int) =
    copy(
      offsets = offsets.updated(
        paramName,
        offsets.getOrElse(paramName,0) + posDelta
      )
    )
}
