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
import java.io.{ByteArrayInputStream, StringWriter, PrintWriter, File}
import org.slf4j.LoggerFactory
import org.akraievoy.cnet.net.vo.{EdgeData, VertexData}

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
                "spacePos = {}",
                ParamPos.seqToString(runStore.spacePos, requiredIndexes)
              )
              currentExpPair._1.executeFun(runStore)
          }
        )

        log.info("write shutdown for {}", currentExpPair._1.name)
        expStore.writeShutdown()

        exportPrimitives(expStore, subchain, requiredIndexes, fs)
        exportGraphvis(expStore, subchain, requiredIndexes, fs)

        runChain :+ expStore
    }

    //  FIXME PROCEED on-completion triggers
    log.info("chain complete")
  }

  def exportPrimitives(
    expStore: ExperimentStore,
    subchain: Seq[Registry.ExpConfPair],
    requiredIndexes: Set[Int],
    fs: FileSystem
  ) {
    val primitives = expStore.primitives
    val axis = spaceAxis(subchain, requiredIndexes)
    val primitiveExport =
      Stream(
        Seq("spacePos") ++ axis.map(_.name) ++ primitives.map(_._1)
      ) ++ spacePosMap(
        subchain, requiredIndexes, expStore, {
          runStore =>
            val posInt = ParamPos.pos(runStore.spacePos, requiredIndexes)
            val rowSeq = Seq[Option[Any]](Some(posInt)) ++
                axis.map(p => expStore.get(p.name, runStore.spacePos)(p.mt)) ++
                primitives.map(p => expStore.get(p._1, runStore.spacePos)(p._2))
            rowSeq.map(elem => elem.map(String.valueOf).getOrElse(""))
        }, false
      )

    fs.dumpCSV(
      expStore.uid,
      "export/primitives.csv",
      Map.empty
    )(primitiveExport)
  }

  /** extract method refactoring is for PUSSIES */
  def exportGraphvis(
    expStore: ExperimentStore,
    subchain: Seq[Registry.ExpConfPair],
    requiredIndexes: Set[Int],
    fs: FileSystem
  ) {
    def rangeVData(vData: VertexData) = {
      (0 until vData.getSize).foldLeft(
        (Double.PositiveInfinity, Double.NegativeInfinity)
      ) {
        case ((minR1, maxR1), radius) =>
          (minR1 min radius, maxR1 max radius)
      }
    }

    def rangeEData(eData: EdgeData) = {
      val size = eData.getSize
      val elems = for (
        f <- 0 until size;
        t <- 0 until size
      ) yield {
        eData.get(f,t)
      }
      elems.foldLeft(
        (Double.PositiveInfinity, Double.NegativeInfinity)
      ) {
        case ((minR1, maxR1), radius) =>
          (minR1 min radius, maxR1 max radius)
      }
    }

    val log2 = scala.math.log(2)
    def normalize(range: Pair[Double, Double])(v: Double) = {
      val (min, max) = range
      if (min < max) {
        scala.math.log(1 + (v - min) / (max - min)) / log2
      } else {
        0.0
      }
    }

    val pageSize = 720
    val pageZero = 36
    val pageFullSize = pageSize + 2 * pageZero
    def locToPagePos(x: Double, y: Double) = {
      (x * pageSize + pageZero, y * pageSize + pageZero)
    }
    expStore.experiment.graphvisExports.foreach{
      case (exportName, export) =>
        spacePosMap(
          subchain, requiredIndexes, expStore, {
            rs =>
              val structureRef = export.edgeStructure(rs)
              Option(structureRef.getValue).map{
                structure =>
                  val pos = ParamPos.pos(rs.spacePos, requiredIndexes)
                  val out = new StringWriter()
                  val p = new PrintWriter(out)

                  val (graphToken, linkToken) = if (structure.isSymmetric) {
                    ("graph", "--")
                  } else {
                    ("digraph", "->")
                  }

                  val optVertexCoords = for (
                    refX <- export.vertexCoordX(rs);
                    refY <- export.vertexCoordY(rs);
                    xData <- Option(refX.getValue);
                    yData <- Option(refY.getValue)
                  ) yield {
                    (xData, yData)
                  }
                  val optVertexRadiusRange = for (
                    refVertexRadius <- export.vertexRadius(rs);
                    vertexRadius <- Option(refVertexRadius.getValue)
                  ) yield {
                    (vertexRadius, rangeVData(vertexRadius))
                  }
                  val optVertexColorRange = for (
                    refVertexColor <- export.vertexColor(rs);
                    vertexColor <- Option(refVertexColor.getValue)
                  ) yield {
                     (vertexColor, rangeVData(vertexColor))
                  }
                  val optVertexLabel = for (
                    refVertexLabel <- export.vertexLabel(rs);
                    vertexLabel <- Option(refVertexLabel.getValue)
                  ) yield {
                    vertexLabel
                  }

                  val optEdgeLabel = for (
                    refEdgeLabel <- export.edgeLabel(rs);
                    edgeLabel <- Option(refEdgeLabel.getValue)
                  ) yield {
                    edgeLabel
                  }
                  val optEdgeWidthRange = for (
                    refEdgeWidth <- export.edgeWidth(rs);
                    edgeWidth <- Option(refEdgeWidth.getValue)
                  ) yield {
                    (edgeWidth, rangeEData(edgeWidth))
                  }
                  val optEdgeColorRange = for (
                    refEdgeColor <- export.edgeColor(rs);
                    edgeColor <- Option(refEdgeColor.getValue)
                  ) yield {
                    (edgeColor, rangeEData(edgeColor))
                  }

                  p.println("%s %s {".format(graphToken, pos))
                  p.println(
                    """  size="%s,%s";
                      |  node [fixedsize=true, colorscheme="%s"];
                      |  edge [splines=true, colorscheme="%s"]
                      | """.stripMargin.format(
                    pageFullSize, pageFullSize,
                    export.vertexColorScheme.toString,
                    export.edgeColorScheme.toString
                  ))

                  val size = structure.getSize
                  for (
                    nodeIdx <- 0 until size
                  ) yield {
                    val nodeAttrs = Seq.empty[String] ++
                      optVertexCoords.map{
                        case (coordX, coordY) =>
                          val (pagePosX, pagePosY) = locToPagePos(
                            coordX.get(nodeIdx),
                            coordY.get(nodeIdx)
                          )
                          "pos=\"%.6g,%.6g!\"".format(pagePosX, pagePosY)
                      }.toSeq ++
                      optVertexRadiusRange.map{
                        case (radius, range) =>
                          val radius_1_11 = 1 + 10 * normalize(range)(
                            radius.get(nodeIdx)
                          )
                          val width = 4 * radius_1_11
                          "width=%.2g".format(width)
                      }.toSeq ++
                      optVertexColorRange.map{
                        case (color, range) =>
                          val color_1_11 = 1 + math.round(10 * normalize(range)(
                            color.get(nodeIdx)
                          ))
                          "fillcolor=\"%d\"".format(12 - color_1_11)
                      }.toSeq ++
                      optVertexLabel.map{
                        vertexLabel =>
                          "label=\"%s\"".format(
                            optVertexLabel.map {
                              vertexLabel =>
                                "%6g".format(vertexLabel.get(nodeIdx))
                            }.getOrElse {
                              "%d".format(nodeIdx)
                            }
                          )
                      }.toSeq

                    p.println(
                      "    n%s [%s];".format(nodeIdx, nodeAttrs.mkString(", "))
                    )
                  }

                  for (
                    fromIdx <- 0 until size;
                    toIdx <- (if (structure.isSymmetric) {
                      fromIdx
                    } else {
                      0
                    }) until size if structure.conn(fromIdx, toIdx)
                  ) yield {
                    val edgeAttrs = Seq.empty[String] ++
                      optEdgeLabel.map{
                        edgeLabel =>
                          "xlabel=\"%.6g\"".format(edgeLabel.get(fromIdx, toIdx))
                      }.toSeq ++
                      optEdgeWidthRange.map{
                        case (width, range) =>
                          val width_1_11 = 1 + 10 * normalize(range)(
                            width.get(fromIdx, toIdx)
                          )
                          "penwidth=\"%.2g\"".format(width_1_11 / 2)
                      }.toSeq ++
                      optEdgeColorRange.map{
                        case (color, range) =>
                          val color_1_11 = 1 + math.round(
                            10 * normalize(range)(
                              color.get(fromIdx,toIdx)
                            )
                          )
                          "pencolor=\"%d\"".format(12 - color_1_11)
                      }.toSeq

                    p.println(
                      "    n%s %s n%s [%s];".format(
                        fromIdx, linkToken, toIdx,
                        edgeAttrs.mkString(", ")
                      )
                    )
                  }

                  p.println("}")
                  p.flush()
                  p.close()

                  fs.appendBinary(
                    expStore.uid,
                    "export/graphviz_%s/%s.dot".format(exportName, pos),
                    Map.empty
                  )(
                    new ByteArrayInputStream(out.toString.getBytes)
                  )
                  true
              }.getOrElse{
                false
              }
          }, false
        )
    }
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
