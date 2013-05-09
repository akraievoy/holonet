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

package org.akraievoy.holonet.exp.space

import org.akraievoy.holonet.exp.{ParamPos, Param, Registry}
import org.akraievoy.holonet.exp.store.{RunStore, ExperimentStore}
import org.slf4j.LoggerFactory
import java.util.Date
import scala.collection.BitSet
import java.util.concurrent.atomic.AtomicLong

trait ParamSpaceNav {
  private val log = LoggerFactory.getLogger(classOf[ParamSpaceNav])

  def spaceAxis(
    subchain: Seq[Registry.ExpConfPair],
    requiredIndexes: BitSet
  ): Seq[Param] = {
    val axisMap = subchain.zipWithIndex.filter {
      case (expPair, index) =>
        requiredIndexes.contains(index)
    }.foldLeft(PAR_AXIS_EMPTY) {
      case (axisMap0, ((exp, conf), index)) =>
        val curMap = conf.spacePosAxis(index < subchain.length - 1)
        axisMap0.map {
          case (key, valueSeq) =>
            (key, valueSeq ++ curMap.getOrElse(key, Seq.empty))
        }
    }
    axisMap(false) ++ axisMap(true)
  }

  private def spacePosStreams(
    subchain: Seq[Registry.ExpConfPair],
    requiredIndexes: BitSet
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
    }.foldLeft(PAR_SPACE_EMPTY) {
      case (mapChained, mapCurrent) =>
        mapChained.map {
          case (parallelFlag, paramPosSeq) =>
            (
                parallelFlag,
                for (
                  chainedPoses <- paramPosSeq;
                  currentPoses <- mapCurrent.getOrElse(
                    parallelFlag,
                    SPACE_EMPTY
                  )
                ) yield {
                  val posSeq = chainedPoses ++ currentPoses
                  posSeq
                }
            )
        }
    }
  }

  private def spacePosCount(
    subchain: Seq[Registry.ExpConfPair],
    requiredIndexes: BitSet
  ): Long = {
    subchain.zipWithIndex.filter {
      case (expPair, index) =>
        requiredIndexes.contains(index)
    }.map {
      case ((exp, conf), index) =>
        conf.spacePosCount(index < subchain.length - 1, index)
    }.product
  }

  def spacePosMap[T](
    subchain: Seq[Registry.ExpConfPair],
    requiredIndexes: BitSet,
    expStore: ExperimentStore,
    visitFun: RunStore => T,
    parallel: Boolean = true
  ): IndexedSeq[T] = {
    val posCount = spacePosCount(subchain, requiredIndexes)
    val posStreams = spacePosStreams(subchain, requiredIndexes)
    val mapStart = System.currentTimeMillis
    val posMapped = new AtomicLong(0)
    posStreams.getOrElse(
      false,
      SPACE_EMPTY
    ).flatMap {
      sequentialPos =>
        val stream = posStreams.getOrElse(true, SPACE_EMPTY)
        def visitFun0(parallelPos: Seq[ParamPos]) = {
          try {
            visitFun(RunStore(expStore, sequentialPos ++ parallelPos))
          } finally {
            val posMappedLocal: Long = posMapped.incrementAndGet()
            val mapNow = System.currentTimeMillis
            val progress = (0.0 + posMappedLocal) / posCount
            val spent = mapNow - mapStart
            val left = math.ceil(spent / progress).toLong
            if (math.max(spent, left) > 30 * 1000) {
              val eta = mapStart + left
              log.warn("%.6g complete --- ETA %s".format(progress, new Date(eta)))
            }

          }
        }
        if (parallel) {
          stream.par.map(visitFun0)
        } else {
          stream.map(visitFun0)
        }
    }.toIndexedSeq
  }
}
