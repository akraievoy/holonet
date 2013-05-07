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

  val posNumbers =
    RunStore.posNumbersForPos(es.withChain, spacePos)

  val posNumber =
    posNumbers(es.experiment.name)

  def lens[T](
    paramName: ParamName[T]
  ): StoreLens[T] = {
    StoreLens[T](
      es,
      paramName.name,
      spacePos,
      posNumbers,
      paramName.mt
    )
  }

}

object RunStore {
  def posNumbersForPos(chain: Seq[ExperimentStore], spacePos0: Seq[ParamPos]): Map[String, Long] = {
    chain.foldLeft(Map.empty[String, Long]) {
      (map, es0) =>
        map.updated(es0.experiment.name, ParamPos.pos(spacePos0, es0.requiredIndexes))
    }
  }


}