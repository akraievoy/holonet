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
import store.{RunStore, ExperimentStore, RegistryStore, FileSystem}
import java.io.File
import org.slf4j.LoggerFactory

object Registry extends RegistryData {
  val log = LoggerFactory.getLogger(classOf[RegistryData])

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

  type ExpConfPair = (Experiment, Config)

  @tailrec
  private def addDependencies(
    pendingNames: Seq[String],
    resultChain: Seq[ExpConfPair],
    configFun: Experiment => Config
  ): Seq[ExpConfPair] = {
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

  private def indexesOfRequired(pairs: Seq[ExpConfPair]): Set[Int] = {
    val pairsIndexed = pairs.zipWithIndex

    def require(queue: Set[Int], required: Set[Int]): Set[Int] = {
      val newQueue = queue.flatMap{
        qElem =>
          val pairQ = pairs(qElem)
          pairsIndexed.filter{
            case (pairI, index) =>
              pairQ._1.depends.contains(pairI._1.name)
          }.map{
            case (pairI, index) => index
          }
      }
      if (newQueue.isEmpty) {
        queue ++ required
      } else {
        require(newQueue, queue ++ required)
      }
    }

    require(Set(pairs.size - 1), Set.empty[Int])
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

  private def execute(expPairSeq: Seq[ExpConfPair]) = {
    val fs = new FileSystem(new File("data"))
    val registryStore = new RegistryStore(fs)

    //  TODO validate we have no param name/type collisions
    (1 to expPairSeq.length).toSeq.foldLeft(Seq.empty[ExperimentStore]){
      (runChain, length) =>
        val subchain = expPairSeq.take(length)
        val requiredIndexes = indexesOfRequired(subchain)
        val currentExpPair = subchain.last

        log.info(
          "starting {} with conf {}",
          currentExpPair._1.name,
          currentExpPair._2.name
        )

        val currentUID = registryStore.registerRun(
          currentExpPair._1.name,
          currentExpPair._2.name
        )

        val expStore = new ExperimentStore(
          fs,
          currentUID,
          currentExpPair._1,
          currentExpPair._2,
          runChain,
          requiredIndexes
        )

        spacePosMap(
          subchain,
          requiredIndexes,
          expStore,
          {
            runStore =>
              log.debug(
                "spacePos = %s".format(
                  ParamPos.seqToString(runStore.spacePos, requiredIndexes)
                )
              )
              currentExpPair._1.executeFun(runStore)
          }
        )

        println("write shutdown for " + currentExpPair._1.name)

        expStore.writeShutdown()

        val primitives = expStore.primitives
        val axis = spaceAxis(subchain, requiredIndexes)
        val primitiveExport =
          Stream(
            Seq("spacePos") ++ axis.map(_.name) ++ primitives.map(_._1)
          ) ++ spacePosMap(
            subchain, requiredIndexes, expStore, {
              runStore =>
                val posInt = ParamPos.pos(runStore.spacePos, requiredIndexes )
                val rowSeq = Seq[Option[Any]](Some(posInt)) ++
                  axis.map(p => expStore.get(p.name, runStore.spacePos)(p.mt)) ++
                  primitives.map(p => expStore.get(p._1, runStore.spacePos)(p._2))
                rowSeq.map(elem=>elem.map(String.valueOf).getOrElse(""))
            }, false
        )
        fs.dumpCSV(expStore.uid, "export/primitives.csv", Map.empty)(primitiveExport)

        runChain :+ expStore
    }

    //  FIXME PROCEED on-completion triggers
    log.info("chain complete")

  }

  private def execute(targetExpName: String, configFun: Experiment => Config) {
    val chain = dependencyChain(targetExpName, configFun)
    execute(chain)
  }

  def execute(targetExpName: String, configMap: Map[String, String]) {
    execute(
      targetExpName,
      {
        exp =>
          exp.configs(
            configMap.getOrElse(exp.name, "default")
          )
      }
    )
  }
}
