package org.akraievoy.holonet.exp

import store.{FileSystem, ExperimentStore}
import java.text.{DecimalFormat, NumberFormat}
import org.akraievoy.cnet.net.vo.{StoreUtils, EdgeData, VertexData}
import java.io._
import com.google.common.io.ByteStreams
import scala.Some
import org.slf4j.LoggerFactory
import org.akraievoy.holonet.exp.space.ParamSpaceNav
import scala.collection.BitSet

trait Exports extends ParamSpaceNav {
  private val log = LoggerFactory.getLogger(classOf[RegistryData])

  def exportPrimitives(
    expStore: ExperimentStore,
    subchain: Seq[Registry.ExpConfPair],
    requiredIndexes: BitSet,
    fs: FileSystem,
    primitives: Seq[ParamName[_]],
    exportName: String = "primitives"
  ) {
    val axisSorted = spaceAxis(subchain, requiredIndexes).sortBy(_.name)
    val primitivesSorted = primitives.sortBy(_.name)
    val primitiveExport =
      Stream(
        Seq("spacePos") ++
          axisSorted.map(_.name) ++
          primitivesSorted.map(_.name)
      ) ++ spacePosMap(
        subchain, requiredIndexes, expStore, {
          runStore =>
            val posInt = ParamPos.pos(runStore.spacePos, requiredIndexes)
            val rowSeq = Seq[Option[Any]](Some(posInt)) ++
                axisSorted.map(p => expStore.get(p.name, runStore.spacePos)(p.mt)) ++
                primitivesSorted.map{
                  pn =>
                    expStore.get(pn.name, runStore.spacePos)(pn.mt)
                }
            rowSeq.map(elem => elem.map(String.valueOf).getOrElse(""))
        }, false
      )

    fs.dumpCSV(
      expStore.uid,
      "export/%s.csv".format(exportName),
      Map.empty
    )(primitiveExport)
  }

  private val nf: NumberFormat = new DecimalFormat("0.##")

  private def rangeVData(vData: VertexData) = {
    val range = (0 until vData.getSize).foldLeft(
      (Double.PositiveInfinity, Double.NegativeInfinity)
    ) {
      case ((min0, max0), index) =>
        val elem = vData.get(index)
        (min0 min elem, max0 max elem)
    }
    range
  }

  private def rangeEData(eData: EdgeData) = {
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
      case ((min0, max0), elem) =>
        (min0 min elem, max0 max elem)
    }
  }

  private def normalize(range: Pair[Double, Double])(v: Double) = {
    val (min, max) = range
    if (min < max) {
      (v - min) / (max - min)
    } else {
      0.5
    }
  }

  val pointToInch = 1.0 / 72

  val pageSizeBase = 776
  val pageMargin = 8
  val nodeFont = 5
  val nodeSizeMin = 20
  val nodeSizeMax = 40
  val edgeSizeMin = 1
  val edgeSizeMax = 4

  def pageFullSize(pageSize: Int) =
    pageSize + 2 * pageMargin

  def locToPagePos(pageSize: Int)(x: Double, y: Double) = {
    (x * pageSize + pageMargin, y * pageSize + pageMargin)
  }

  val fullPathToDot = "/usr/bin/dot"

  def exportGraphvis(
    expStore: ExperimentStore,
    subchain: Seq[Registry.ExpConfPair],
    requiredIndexes: BitSet,
    fs: FileSystem
  ) {
    val graphvisExecutable = new File(fullPathToDot).isFile

    expStore.experiment.graphvisExports.foreach{
      case (exportName, export) =>
        spacePosMap(
          subchain, requiredIndexes, expStore, {
            rs => try {
              val structureRef = export.edgeStructure(rs)
              Option(structureRef.getValue).map{
                structure =>
                  val pageSize =
                    pageSizeBase * (
                      if (structure.getSize <= 64) {
                        1
                      } else if (structure.getSize <= 256) {
                        2
                      } else {
                        3
                      }
                    )
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
                    """  size="%s,%s!"; ratio="fill"; splines="true"; bgcolor="#99CCFF";
                      |  node [fixedsize=true, fontsize=%d, style=filled,
                      |          shape="circle", colorscheme="%s"];
                      |  edge [colorscheme="%s"];
                      | """.stripMargin.format(
                    nf.format(pageFullSize(pageSize) * pointToInch),
                    nf.format(pageFullSize(pageSize) * pointToInch),
                    nodeFont,
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
                          val (pagePosX, pagePosY) = locToPagePos(pageSize)(
                            coordX.get(nodeIdx),
                            coordY.get(nodeIdx)
                          )
                          "pos=\"%s,%s\"".format(
                            nf.format(pagePosX),
                            nf.format(pagePosY)
                          )
                      }.toSeq ++
                      optVertexRadiusRange.map{
                        case (radius, range) =>
                          val r_0_1 = normalize(range)(
                            radius.get(nodeIdx)
                          )
                          "width=%s".format(
                            nf.format(
                              (
                                nodeSizeMin + r_0_1*(nodeSizeMax-nodeSizeMin)
                              ) * pointToInch
                            )
                          )
                      }.toSeq ++
                      optVertexColorRange.map{
                        case (color, range) =>
                          val color_1_11 = 1 + math.round(10 * normalize(range)(
                            color.get(nodeIdx)
                          ))
                          "fillcolor=\"%d\"".format(12 - color_1_11)
                      }.toSeq :+
                      "label=\"%s\"".format(
                        optVertexLabel.map{
                          vertexLabel =>
                            nf.format(vertexLabel.get(nodeIdx))
                        }.getOrElse {
                          "%d".format(nodeIdx)
                        }
                      )

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
                          "xlabel=\"%s\"".format(
                            nf.format(edgeLabel.get(fromIdx, toIdx))
                          )
                      }.toSeq ++
                      optEdgeWidthRange.map{
                        case (width, range) =>
                          val w = normalize(range)(
                            width.get(fromIdx, toIdx)
                          )
                          "penwidth=\"%s\"".format(
                            nf.format(
                              (edgeSizeMin + w*(edgeSizeMax-edgeSizeMin))
                            )
                          )
                      }.toSeq ++
                      optEdgeColorRange.map{
                        case (color, range) =>
                          val color_1_11 = 1 + math.round(
                            10 * normalize(range)(
                              color.get(fromIdx,toIdx)
                            )
                          )
                          "color=\"%d\"".format(12 - color_1_11)
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

                  val fileName = "%s.dot".format(pos)
                  val filePath =
                    "export/graphviz_%s/%s".format(exportName, fileName)

                  fs.appendBinary(
                    expStore.uid,
                    filePath,
                    Map.empty
                  )(
                    new ByteArrayInputStream(out.toString.getBytes)
                  )

                  val file = fs.fileForUid(expStore.uid, filePath)

                  if (graphvisExecutable) {
                    val dotProcess = new ProcessBuilder(
                      fullPathToDot,
                      "-Kneato", "-n", /*"-Tpng", */"-Tsvg", "-O", fileName
                    ).directory(file.getParentFile).redirectErrorStream(true).start()

                    //  LATER it's of course lame to keep logs
                    //    in memory of parent process, but let it be for now
                    val dotProcessOutput = new ByteArrayOutputStream()
                    ByteStreams.copy(
                      dotProcess.getInputStream,
                      dotProcessOutput
                    )
                    dotProcess.waitFor()
                    if (dotProcess.exitValue() != 0) {
                      fs.appendBinary(expStore.uid, filePath+".log", Map.empty)(
                        new ByteArrayInputStream(dotProcessOutput.toByteArray)
                      )
                    } else {
                      fs.fileForUid(expStore.uid, filePath).delete()
                    }
                  }
              }
        } catch {
          case e: Exception => log.warn("export failed for pos %s".format(rs.spacePos), e)
        } }, false
      )
      log.info("GraphViz export {} completed", export.desc)
    }
  }

  def exportStore(
    expStore: ExperimentStore,
    subchain: Seq[Registry.ExpConfPair],
    requiredIndexes: BitSet,
    fs: FileSystem
  ) = {
    expStore.experiment.storeExports.foreach{
      case (exportName, export) =>
        val stores = export.paramNames
        val axis = spaceAxis(subchain, requiredIndexes)
        val storeExport =
          Stream(
            Seq("spacePos") ++ axis.map(_.name) ++ stores.map(_.name)
          ) ++ spacePosMap(
            subchain, requiredIndexes, expStore, {
              runStore =>
                val storeValues = stores.map(
                  p =>
                    expStore.get(p.name, runStore.spacePos)(p.mt).map {
                      store =>
                        for (pos <- 0 until store.size()) yield {
                          StoreUtils.get(store, pos)
                        }
                    }
                )

                val maxLen = storeValues.foldLeft(0){
                  (l,optSeq) => optSeq.map(_.length max l).getOrElse(l)
                }
                val storeValuesRect = storeValues.map{
                  _.map{
                    valueSeq =>
                      valueSeq.map(String.valueOf) ++ (valueSeq.length until maxLen).map {idx => ""}
                  }.getOrElse{
                    (0 until maxLen).map{idx => ""}
                  }
                }

                val posInt = ParamPos.pos(runStore.spacePos, requiredIndexes)
                val rowHeader =
                  Seq[String](String.valueOf(posInt)) ++
                    axis.map(
                      p =>
                        expStore.get(p.name, runStore.spacePos)(p.mt).map(
                          String.valueOf
                        ).getOrElse("")
                    )

                storeValuesRect.transpose.map {
                  row =>
                    rowHeader ++ row
                }
            }, false
          ).flatten

        fs.dumpCSV(
          expStore.uid,
          "export/store_%s.csv".format(export.name),
          Map.empty
        )(storeExport)

        log.info("Store export {} completed", export.desc)
    }
  }
}
