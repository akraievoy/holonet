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
import org.akraievoy.holonet.exp.ParamPos
import collection.immutable.IndexedSeq

case class StoreLens[T](
  expStore: ExperimentStore,
  paramName: String,
  spacePos: Seq[ParamPos],
  offsets: Map[String, Int] = Map.empty,
  mt: Manifest[T]
) extends Ref[T] {

  def get = {
    val spacePosOffs = applyOffset(spacePos, offsets)
    expStore.get(paramName, spacePosOffs)(mt)
  }

  def set(t: T) {
    val spacePosOffs = applyOffset(spacePos, offsets)
    expStore.set(paramName, t, spacePosOffs)(mt)
  }

  def getValue = {
    get.getOrElse(null.asInstanceOf[T])
  }

  def setValue(value: T) {
    set(value)
  }

  def forName(newName: String): StoreLens[T] =
    copy[T](paramName = newName)

  def forTypeName[T1](c: Class[T1], newName: String): StoreLens[T1] =
    copy[T1](
      paramName = newName,
      mt = Manifest.classType(c)
    )

  def offset(paramName: String, posDelta: Int) =
    copy[T](
      offsets = offsets.updated(
        paramName,
        offsets.getOrElse(paramName, 0) + posDelta
      )
    )

  def axis: IndexedSeq[StoreLens[T]] = {
    val paramPos = spacePos.find(pPos => pPos.name == paramName).get
    val range = -paramPos.pos until paramPos.total
    range.map(offs => offset(paramName, offs))
  }

  def axisGetValue: IndexedSeq[T] = {
    axis.map(_.getValue)
  }

  def axisArr: Array[StoreLens[T]] = {
    axis.toArray
  }

  def axisGetValueArr: Array[T] = {
    implicit val tManifest = mt
    axisGetValue.toArray
  }

  private def applyOffset(
    spacePos: Seq[ParamPos],
    offsets: Map[String, Int]
  ): Seq[ParamPos] = {
    if (offsets.size == 0) {
      spacePos
    } else {
      spacePos.map {
        paramPos =>
          offsets.get(paramPos.name).map {
            offset =>
              val param = (expStore +: expStore.chain).find {
                dataStore => dataStore.config.params.contains(paramPos.name)
              }.getOrElse {
                throw new IllegalStateException(
                  "no config has parameter %s".format(
                    paramPos.name
                  )
                )
              }.config.params(paramPos.name)

              paramPos.copy(
                value = param.valueSpec(paramPos.pos + offset),
                pos = paramPos.pos + offset
              )
          }.getOrElse(paramPos)
      }
    }
  }
}
