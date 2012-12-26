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

import org.akraievoy.base.runner.vo.Parameter

case class Param(
  name: String,
  valueSpec: Seq[String],
  strategy: Parameter.Strategy,
  chainStrategy: Parameter.Strategy,
  desc: String,
  index: Int
) extends Named {
  def toPosSeq(
    chained: Boolean = false,
    expIndex: Int = 0
  ): Seq[ParamPos] = {
    val fullSeq = valueSpec.zipWithIndex.map {
      case (str, idx) =>
        ParamPos(
          name,
          str,
          idx,
          valueSpec.size,
          isParallel(chained),
          index,
          expIndex
        )
    }
    actualStrategy(chained) match {
      case Parameter.Strategy.USE_FIRST =>
        Seq(fullSeq.head)
      case Parameter.Strategy.USE_LAST =>
        Seq(fullSeq.last)
      case anyFull if Parameter.Strategy.full(anyFull) =>
        fullSeq
      case other =>
        throw new IllegalArgumentException(
          "unable to handle strategy %s".format(other)
        )
    }
  }

  def isParallel(chained: Boolean = false) =
    actualStrategy(chained) != Parameter.Strategy.ITERATE

  def actualStrategy(chained: Boolean = false): Parameter.Strategy =
    if (chained) {
      chainStrategy
    } else {
      strategy
    }
}

object Param{
  def apply(
    name: String,
    singleValueSpec: String,
    strategy: Parameter.Strategy = Parameter.Strategy.SPAWN,
    chainStrategy: Parameter.Strategy = Parameter.Strategy.SPAWN,
    desc: String = ""
  ) = {
    val valueSpec =
      if (singleValueSpec.contains(';')) {
        singleValueSpec.split(";").toSeq
      } else if (singleValueSpec.contains("-")) {
        val rangeStr = singleValueSpec.split("-")
        if (rangeStr.length > 2) {
          throw new IllegalArgumentException(
            "param '%s' has more than 2 range limits".format(name)
          )
        }
        val range = rangeStr.map(java.lang.Long.parseLong)
        (range(0) to range(1)).map(String.valueOf)
      } else {
        Seq(singleValueSpec)
      }

    new Param(name, valueSpec, strategy, chainStrategy, desc, -1)
  }
}