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

package org.akraievoy.holonet.exp.data

import org.akraievoy.holonet.exp._
import org.akraievoy.cnet.gen.vo.{ConnPreferenceYookJeongBarabasi, MetricEuclidean, EntropySourceRandom}
import org.akraievoy.cnet.gen.domain.{OverlayNetFactory, MetricEDataGenStructural, LocationGeneratorRecursive}
import org.akraievoy.cnet.net.vo.{EdgeDataDense, EdgeDataSparse, VertexData, EdgeData}
import org.akraievoy.cnet.metrics.domain._
import org.akraievoy.base.runner.domain.RunnableComposite
import scala.collection.JavaConversions._
import org.akraievoy.base.ref.{RefRO, Ref}
import store.StoreLens

object OverlayGO {
  import java.lang.{
    Byte => JByte, Integer => JInt, Long => JLong,
    Float => JFloat, Double => JDouble
  }

  object ParamNames {
    //  stage 1 inputs
    val entropySourceLocGenSeed = ParamName[JLong]("entropySourceLocGen.seed")
    val entropySourceLocationSeed = ParamName[JLong]("entropySourceLocation.seed")
    val entropySourceSeed = ParamName[JLong]("entropySource.seed")
    val locationMetricNodes = ParamName[JInt]("locationMetric.nodes")
    val connPreferenceAlpha = ParamName[JDouble]("connPreference.alpha")
    val connPreferenceBeta = ParamName[JDouble]("connPreference.beta")
    val structureBaseDegree = ParamName[JInt]("structure.baseDegree")
    //  stage 1 outputs
    val physStructureInit = ParamName[EdgeDataSparse]("default.structure.init")
    val physStructure = ParamName[EdgeDataSparse]("default.structure")
    val physDistance = ParamName[EdgeDataDense]("default.distance")
    val physRouteLen = ParamName[EdgeDataDense]("default.routeLen")
    val physLocationX = ParamName[VertexData]("default.location.x")
    val physLocationY = ParamName[VertexData]("default.location.y")
    val physDensity = ParamName[VertexData]("default.density")
    val physEigenGap = ParamName[JDouble]("eigengap.physical")
    //  stage 2 inputs
    val entropySourceOvlSeed = ParamName[JLong]("esOvl.seed")
    val entropySourceReqSeed = ParamName[JLong]("esReq.seed")
    val ovlNetFactoryOmega = ParamName[JDouble]("onf.omega")
    val ovlNetFactoryNu = ParamName[JDouble]("onf.nu")
    val ovlReqFactoryPhi = ParamName[JDouble]("ovlReq.phi")
    val ovlReqFactoryPsi = ParamName[JDouble]("ovlReq.psi")
    val ovlReqFactorySigma = ParamName[JDouble]("ovlReq.sigma")
    val ovlReqFactoryThreshMinAbsValue = ParamName[JDouble]("ovlReqThresh.minAbsValue")
    val ovlReqFactoryThreshMinToMaxRatio = ParamName[JDouble]("ovlReqThresh.minToMaxRatio")
    //  stage 2 outputs
    val overlayIndex = ParamName[VertexData]("overlay.index")
    val overlayDistance = ParamName[EdgeDataDense]("overlay.distance")
    val overlayRequest = ParamName[EdgeDataSparse]("overlay.request")
    val overlayLocationX = ParamName[VertexData]("overlay.locationX")
    val overlayLocationY = ParamName[VertexData]("overlay.locationY")
    val overlayDensity = ParamName[VertexData]("overlay.density")

  }

  import ParamNames._

  val experiment1physDataset = Experiment(
    "overlayGO-1-physDataset",
    "Overlay GO [stage1] Physical Dataset",
    Nil,
    {
      rs =>
        val entropySourceLocGen = new EntropySourceRandom()
        entropySourceLocGen.setSeed(
          rs.lens(entropySourceLocGenSeed).get.get
        )

        val locationGenerator = new LocationGeneratorRecursive(
          entropySourceLocGen
        )
        locationGenerator.setGridSize(1024)
        locationGenerator.setDimensionRatio(1.5)

        val netFactory = new MetricEDataGenStructural()
        netFactory.setNetNodeNum(3)
        netFactory.setType("path")
        netFactory.setTarget(rs.lens(physStructureInit).asInstanceOf[Ref[EdgeData]])

        val entropySourceLocation = new EntropySourceRandom()
        entropySourceLocation.setSeed(
          rs.lens(entropySourceLocationSeed).get.get
        )

        val locationMetric = new MetricVDataLocation(
          entropySourceLocation,
          locationGenerator
        )
        locationMetric.setTargetX(rs.lens(physLocationX))
        locationMetric.setTargetY(rs.lens(physLocationY))
        locationMetric.setNodes(rs.lens(locationMetricNodes).get.get)

        val distMetric = new MetricEDataDistance(
          new MetricEuclidean()
        )
        distMetric.setSourceX(rs.lens(physLocationX))
        distMetric.setSourceY(rs.lens(physLocationY))
        distMetric.setTarget(rs.lens(physDistance).asInstanceOf[Ref[EdgeData]])

        val entropySource = new EntropySourceRandom()
        entropySource.setSeed(
          rs.lens(entropySourceSeed).get.get
        )

        val connPreference = new ConnPreferenceYookJeongBarabasi()
        connPreference.setAlpha(rs.lens(connPreferenceAlpha).get.get)
        connPreference.setBeta(rs.lens(connPreferenceBeta).get.get)

        val structure = new MetricEDataStructure(
          connPreference,
          entropySource
        )
        structure.setDistSource(rs.lens(physDistance).asInstanceOf[Ref[EdgeData]])
        structure.setStructureSource(rs.lens(physStructureInit).asInstanceOf[Ref[EdgeData]])
        structure.setTarget(rs.lens(physStructure).asInstanceOf[Ref[EdgeData]])
        structure.setBaseDegree(rs.lens(structureBaseDegree).get.get)

        val densityMetric = new MetricVDataDensity(
          locationGenerator
        )
        densityMetric.setSourceX(rs.lens(physLocationX))
        densityMetric.setSourceY(rs.lens(physLocationY))
        densityMetric.setTarget(rs.lens(physDensity))

        val metricRoutesJohnson = new MetricRoutesJohnson()
        metricRoutesJohnson.setSource(rs.lens(physStructure).asInstanceOf[Ref[EdgeData]])
        metricRoutesJohnson.setDistSource(rs.lens(physDistance).asInstanceOf[Ref[EdgeData]])

        val routeLenMetric = new MetricEDataRouteLen(
          metricRoutesJohnson
        )
        routeLenMetric.setTarget(rs.lens(physRouteLen).asInstanceOf[Ref[EdgeData]])

        val eigenGapMetric = new MetricScalarEigenGap()
        eigenGapMetric.setSource(rs.lens(physStructure).asInstanceOf[Ref[EdgeData]])
        eigenGapMetric.setTarget(rs.lens(physEigenGap))

        val main = new RunnableComposite()
        main.setGroup(
          Seq(
            locationGenerator,
            netFactory,
            locationMetric,
            distMetric,
            structure,
            densityMetric,
            routeLenMetric,
            eigenGapMetric
          )
        )

        main.run()  //  d'oh, at last

    },
    Config(
      Param(entropySourceLocGenSeed, "234453"),
      Param(entropySourceLocationSeed, "634567"),
      Param(entropySourceSeed, "967436"),
      Param(locationMetricNodes, "24"),
      Param(connPreferenceAlpha, "1.5"),
      Param(connPreferenceBeta, "2.75"),
      Param(structureBaseDegree, "3")
    ),
    Config(
      "vis-requests",
      "Visual for Requests",
      Param(locationMetricNodes, "64"),
      Param(structureBaseDegree, "2")
    ),
    Config(
      "vis-byAlpha",
      "Visual : By Alpha (64 nodes)",
      Param(locationMetricNodes, "64"),
      Param(connPreferenceAlpha, "-4;2;4"),
      Param(structureBaseDegree, "2")
    ),
    Config(
      "vis-byBeta",
      "Visual : By Beta (64 nodes)",
      Param(locationMetricNodes, "64"),
      Param(connPreferenceBeta, "-4;2;4"),
      Param(structureBaseDegree, "2")
    ),
    Config(
      "med",
      "Medium (128 nodes, 1 seed)",
      Param(locationMetricNodes, "128")
    ),
    Config(
      "big",
      "Big (240 nodes, 1 seed)",
      Param(locationMetricNodes, "240")
    ),
    Config(
      "big-1k",
      "Big (1k nodes, 1 seed)",
      Param(locationMetricNodes, "1024")
    ),
    Config(
      "big-2k",
      "Big (2k nodes, 1 seed)",
      Param(locationMetricNodes, "2048")
    ),
    Config(
      "big-4k",
      "Big (4k nodes, 1 seed)",
      Param(locationMetricNodes, "4096")
    )
  )

  val experiment2overlayDataset = Experiment(
    "overlayGO-2-ovlDataset",
    "Overlay GO [stage2] Overlay Dataset",
    Seq("overlayGO-1-physDataset"),
    {
      rs =>
        val ovlESource = new EntropySourceRandom()
        ovlESource.setSeed(
          rs.lens(entropySourceOvlSeed).get.get
        )

        val reqESource = new EntropySourceRandom()
        reqESource.setSeed(
          rs.lens(entropySourceReqSeed).get.get
        )

        val ovlNetFactory = new OverlayNetFactory(ovlESource)
        ovlNetFactory.setOmega(
          rs.lens(ovlNetFactoryOmega).get.get
        )
        ovlNetFactory.setNu(
          rs.lens(ovlNetFactoryNu).get.get
        )
        ovlNetFactory.setSource(
          rs.lens(physStructure).asInstanceOf[StoreLens[EdgeData]]
        )
        ovlNetFactory.setTarget(rs.lens(overlayIndex))
        ovlNetFactory.setEdgeDataMap(
          Map(
            rs.lens(physRouteLen).asInstanceOf[RefRO[EdgeData]] ->
              rs.lens(overlayDistance).asInstanceOf[Ref[EdgeData]]
          )
        )
        ovlNetFactory.setVertexDataMap(
          Map[RefRO[VertexData], Ref[VertexData]](
            rs.lens(physLocationX) -> rs.lens(overlayLocationX),
            rs.lens(physLocationY) -> rs.lens(overlayLocationY),
            rs.lens(physDensity) -> rs.lens(overlayDensity)
          )
        )

        val ovlRequests = new MetricEDataOverlayRequest(reqESource)
        ovlRequests.setSource(rs.lens(overlayDensity))
        ovlRequests.setPhi(rs.lens(ovlReqFactoryPhi).get.get)
        ovlRequests.setPsi(rs.lens(ovlReqFactoryPsi).get.get)
        ovlRequests.setSigma(rs.lens(ovlReqFactorySigma).get.get)

        val reqThreshold = new MetricEDataThreshold(ovlRequests)
        reqThreshold.setTarget(rs.lens(overlayRequest).asInstanceOf[StoreLens[EdgeData]])
        reqThreshold.setMinAbsValue(rs.lens(ovlReqFactoryThreshMinAbsValue).get.get)
        reqThreshold.setMinToMaxRatio(rs.lens(ovlReqFactoryThreshMinToMaxRatio).get.get)

        ovlNetFactory.run()
        reqThreshold.run()
    },
    Config(
      Param(entropySourceOvlSeed, "31013"),
      Param(entropySourceReqSeed, "11311"),
      Param(ovlNetFactoryOmega, "-1"),
      Param(ovlNetFactoryNu, "0.25"),
      Param(ovlReqFactoryPhi, "0.25"),
      Param(ovlReqFactoryPsi, "0.75"),
      Param(ovlReqFactorySigma, "0.03"),
      Param(ovlReqFactoryThreshMinAbsValue, "0.003"),
      Param(ovlReqFactoryThreshMinToMaxRatio, "0.001")
    ),
    Config(
      "nu50",
      "Default (select 50% of nodes)",
      Param(ovlNetFactoryNu, "0.5")
    ),
    Config(
      "nu20_omegaProf",
      "Visual (select 20% of nodes), profile by Omega",
      Param(ovlNetFactoryNu, "0.2"),
      Param(ovlNetFactoryOmega, "-4;2;4"),
      Param(ovlReqFactoryPhi, "1"),
      Param(ovlReqFactoryPsi, "1"),
      Param(ovlReqFactoryThreshMinAbsValue, "0.05"),
      Param(ovlReqFactoryThreshMinToMaxRatio, "0.05")
    )
  )
}
