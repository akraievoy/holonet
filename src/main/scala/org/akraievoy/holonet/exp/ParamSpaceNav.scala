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

import store.{RunStore, ExperimentStore}

trait ParamSpaceNav {
  private val emptyAxis = Map(
    false -> Seq.empty[Param],
    true -> Seq.empty[Param]
  )

  def spaceAxis(
    subchain: Seq[Registry.ExpConfPair],
    requiredIndexes: Set[Int]
  ): Seq[Param] = {
    val axisMap = subchain.zipWithIndex.filter {
      case (expPair, index) =>
        requiredIndexes.contains(index)
    }.foldLeft(emptyAxis) {
      case (axisMap, ((exp, conf), index)) =>
        val curMap = conf.spacePosAxis(index < subchain.length - 1)
        axisMap.map {
          case (key, valueSeq) =>
            (key, valueSeq ++ curMap.getOrElse(key, Seq.empty))
        }
    }
    axisMap(false) ++ axisMap(true)
  }


  private val emptyParamSpace = Map(
    true -> Config.EMPTY_SPACE,
    false -> Config.EMPTY_SPACE
  )

  private def spacePosStreams(
    subchain: Seq[Registry.ExpConfPair],
    requiredIndexes: Set[Int]
  ): Map[Boolean, Stream[Seq[ParamPos]]] = {
    subchain.zipWithIndex.filter {
      case (expPair, index) =>
        requiredIndexes.contains(index)
    }.map {
      case ((exp, conf), index) =>
        conf.spacePosStreams(
          index < subchain.length - 1,
          index
        )
    }.foldLeft(emptyParamSpace) {
      case (mapChained, mapCurrent) =>
        mapChained.map {
          case (parallelFlag, paramPosSeq) =>
            (
                parallelFlag,
                for (
                  chainedPoses <- paramPosSeq;
                  currentPoses <- mapCurrent.getOrElse(
                    parallelFlag,
                    Config.EMPTY_SPACE
                  )
                ) yield {
                  val posSeq = chainedPoses ++ currentPoses
                  posSeq
                }
            )
        }
    }
  }

  def spacePosMap[T](
    subchain: Seq[Registry.ExpConfPair],
    requiredIndexes: Set[Int],
    expStore: ExperimentStore,
    visitFun: RunStore => T,
    parallel: Boolean = true
  ): IndexedSeq[T] = {
    val posStreams = spacePosStreams(subchain, requiredIndexes)
    posStreams.getOrElse(
      false,
      Config.EMPTY_SPACE
    ).flatMap {
      sequentialPos =>
        val stream = posStreams.getOrElse(true, Config.EMPTY_SPACE)
        def visitFun0(parallelPos: Seq[ParamPos]) = {
          visitFun(RunStore(expStore, sequentialPos ++ parallelPos))
        }
        if (parallel) {
          stream.par.map(visitFun0)
        } else {
          stream.map(visitFun0)
        }
    }.toIndexedSeq
  }
}
