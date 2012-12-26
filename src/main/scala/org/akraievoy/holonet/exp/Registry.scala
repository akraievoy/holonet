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
import store.{DataStore, RunUID, RunStore, FileSystem}
import java.io.File

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
    val fs = new FileSystem(new File("data"))
    val runStore = new RunStore(fs)

    //  FIXME validate we have no param name collisions
    (1 to expsWithConf.length).toSeq.foldLeft(Seq.empty[RunUID]){
      (runChain, length) =>
        val subchain = expsWithConf.take(length)
        val currentExpWithConf = expsWithConf(length - 1)
        val emptyParamSpace = Map(
          true -> Config.EMPTY_SPACE,
          false -> Config.EMPTY_SPACE
        )
        val paramSpace = subchain.zipWithIndex.map{
          case ((exp, conf), index) =>
            conf.paramSpace(index == length - 1, index)
        }.foldLeft(emptyParamSpace) {
          case (mapChained, mapCurrent) =>
            mapChained.map{
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

        val currentUID = runStore.registerRun(
          currentExpWithConf._1.name,
          currentExpWithConf._2.name
        )

        val ds = new DataStore(fs, currentUID, runChain)

        val posStream = for (
          sequentialPos <- paramSpace.getOrElse(false, Config.EMPTY_SPACE);
          parallelPos <- paramSpace.getOrElse(true, Config.EMPTY_SPACE).par
        ) yield {
          parallelPos ++ sequentialPos
        }

        posStream.foreach{
          spacePos =>
            //  FIXME run the experiment at last, yeah
        }

        ds.writeShutdown()

        runChain :+ currentUID
    }
  }

  def execute(targetExpName: String, configFun: Experiment => Config) {
    val chain = dependencyChain(targetExpName, configFun)
    execute(chain)
  }
}
