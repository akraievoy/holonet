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
import org.akraievoy.holonet.exp.{Param, ParamPos}
import collection.immutable.IndexedSeq

case class StoreLens[T](
  expStore: ExperimentStore,
  paramName: String,
  spacePos: Seq[ParamPos],
  posNumbers: Map[String, Long],
  mt: Manifest[T]
) extends Ref[T] {

  def get = {
    expStore.get(paramName, spacePos, posNumbers)(mt)
  }

  def set(t: T) {
    expStore.set(paramName, t, spacePos, posNumbers(expStore.experiment.name))(mt)
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
    {
      val spacePos1 = applyOffset(spacePos, Map(paramName -> posDelta))
      val posNumbers1 = RunStore.posNumbersForPos(expStore.withChain, spacePos1)

      copy[T](
        spacePos = spacePos1,
        posNumbers = posNumbers1
      )
    }

  def offsetAxis(posDelta: Int) =
    offset(paramName, posDelta)

  def offsetToPos(newPos: Int) =
    offset(paramName, newPos - paramPos.pos)

  def axis: IndexedSeq[StoreLens[T]] = {
    val paramPos = spacePos.find(pPos => pPos.name == paramName).get
    val range = -paramPos.pos until (paramPos.total - paramPos.pos)
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

  lazy val param: Param = {
    expStore.config.params.get(paramName).orElse(
      expStore.chain.find(_.config.params.contains(paramName)).map{
        _.config.params(paramName)
      }
    ).getOrElse{
      throw new IllegalStateException(
        "param %s not found".format(paramName)
      )
    }
  }

  lazy val paramPos: ParamPos = {
    spacePos.find(_.name == paramName).getOrElse {
      throw new IllegalStateException(
        "param %s not found".format(paramName)
      )
    }
  }

  lazy val fullCount: Int = {
    param.valueSpec.length
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
