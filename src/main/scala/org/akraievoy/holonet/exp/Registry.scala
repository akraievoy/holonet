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

import annotation.tailrec

object Registry {
  lazy val experiments: Seq[Experiment] = Seq(
    Experiment(
      "dlaGen-1-images",
      "DLA density distribution [stage1] Images",
      Nil,
      Config(
        Param("entropySource.seed", "13311331"),
        Param("locationGenerator.gridSize", "1024"),
        Param("locationGenerator.dimensionRatio", "1.5")
      ),
      Config(
        "dimensions",
        "Multiple dimensions",
        Param(
          "locationGenerator.dimensionRatio",
          "1;1.25;1.5;1.75;2"
        )
      ),
      Config(
        "seeds",
        "Multiple seeds",
        Param(
          "entropySource.seed",
          "13311331;31133113;53355335;35533553;51155115"
        )
      )
    )
  )

  lazy val expByName = experiments.groupBy(_.name).mapValues {
    expSeq =>
      if (expSeq.size > 1) {
        throw new IllegalArgumentException(
          "multiple experiments with name '%s'".format(
            expSeq.head.name
          )
        )
      }
      expSeq.head
  }

  type ExpWithConf = (Experiment, Config)

  @tailrec
  private def addDependencies(
    pendingNames: Seq[String],
    resultChain: Seq[ExpWithConf],
    configFun: Experiment => Config
  ): Seq[ExpWithConf] = {
    if (resultChain.size + pendingNames.size > 128) {
      throw new IllegalStateException(
        "cyclic dependency of experiments"
      )
    } else if (pendingNames.isEmpty) {
      resultChain
    } else {
      val head = pendingNames.head
      val headExp = expByName.get(head).getOrElse {
        throw new IllegalArgumentException(
          "experiment with name '%s' not found".format(head)
        )
      }
      val newPending =
        pendingNames.tail ++ headExp.depends.filterNot {
          depName =>
            pendingNames.contains(depName)
        }.map {
          depName =>
            expByName.get(depName).getOrElse(
              throw new IllegalArgumentException(
                "experiment '%s' dependency '%s' not registered".format(
                  head, depName
                )
              )
            )
            depName
        }
      addDependencies(
        newPending,
        (headExp, configFun(headExp)) +: resultChain,
        configFun
      )
    }
  }

  private def dependencyChain(
    targetName: String,
    configFun: Experiment => Config
  ) = {
    addDependencies(
      Seq(targetName),
      Nil,
      configFun
    )
  }

  private def execute(expsWithConf: Seq[ExpWithConf]) = {
    (1 to expsWithConf.length).toSeq.map{
      length =>
        val subchain = expsWithConf.take(length)
        val paramSpace = subchain.zipWithIndex.map{
          case ((exp, conf), index) =>
            conf.paramSpace(index == subchain.length - 1, index)
        }.foldLeft(Map(true -> Config.EMPTY_SPACE, false -> Config.EMPTY_SPACE)) {
          case (mapChained, mapCurrent) =>
            mapChained.map{
              case (parallelFlag, paramPosSeq) =>
                (
                    parallelFlag,
                    for (
                      chainedPoses <- paramPosSeq;
                      currentPoses <- mapCurrent.getOrElse(parallelFlag,
                        Config.EMPTY_SPACE
                      )
                    ) yield {
                      val posSeq = chainedPoses ++ currentPoses
                      posSeq
                    }
                )
            }
        }

        for (
          sequentialPos <- paramSpace.getOrElse(false, Config.EMPTY_SPACE);
          parallelPos <- paramSpace.getOrElse(true, Config.EMPTY_SPACE).par
        ) yield {
            val completePos = parallelPos ++ sequentialPos
            completePos
            //  FIXME: now we have to do *something* for that space pos
        }
    }
  }

  def execute(targetExpName: String, configFun: Experiment => Config) {
    val chain = dependencyChain(targetExpName, configFun)
    execute(chain)
  }
}
