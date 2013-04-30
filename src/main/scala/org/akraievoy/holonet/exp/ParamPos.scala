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

package org.akraievoy.holonet.exp

import scala.collection.BitSet

/**
 * Parameter position within an experiment space.
 *
 * @param value effective value for given position
 * @param pos position index from 0 to total-1
 * @param total total number of positions for given parameter
 * @param parallel are we okay run experiments in parallel for this param?
 * @param index within original experiment definition
 * @param expIndex of experiment within experiment chain
 */
case class ParamPos(
  name: String,
  mt: Manifest[_ <: Any],
  value: String,
  pos: Int,
  total: Int,
  parallel: Boolean,
  index: Int,
  expIndex: Int = 0
) extends Ordered[ParamPos] {

  def compare(that: ParamPos) = {
    val expCompare = expIndex compare that.expIndex
    if (expCompare != 0) {
      expCompare
    } else {
      index compare that.index
    }
  }

  override def toString = {
    val str =
      if (total > 1) {
        "%s=%s %d/%d".format(name, value, pos, total)
      } else {
        "%s=%s".format(name, value)
      }

    val format = if (parallel) {
      "<%s>"
    } else {
      "[%s]"
    }

    format.format(str)
  }
}

object ParamPos {
  /**
   * @param spacePos
   * @param requiredExpIndexes
   * @return
   */
  def pos(spacePos: Seq[ParamPos], requiredExpIndexes: BitSet): Int = {
    spacePos.filter {
      pPos => requiredExpIndexes.contains(pPos.expIndex)
    }.sorted.foldRight((0, 1)){
      case (paramPos, (expPrevPos, expPrevTotal)) =>
        combine(expPrevPos, expPrevTotal, paramPos.pos, paramPos.total)
    }._1
  }

  private def combine(
    prevPos: Int,
    prevTotal: Int,
    curPos: Int,
    curTotal: Int
  ): (Int, Int) = {
    (
        prevPos + curPos * prevTotal,
        curTotal * prevTotal
    )
  }

  def seqToString(
    spacePos: Seq[ParamPos],
    requiredIndexes: BitSet,
    filterFixed: Boolean = true
  ) = {
    val spacePosFiltered = if (filterFixed) {
      spacePos.filter(_.total > 1)
    } else {
      spacePos
    }

    val requiredToPos = spacePosFiltered.groupBy{
      pos =>
        requiredIndexes.contains(pos.expIndex)
    }.mapValues(
      seq =>
        seq.mkString(":")
    ).withDefaultValue("-")

    if (requiredToPos.contains(false)) {
      "%s // %s".format(
        requiredToPos(true),
        requiredToPos(false)
      )
    } else {
      requiredToPos(true)
    }
  }
}
