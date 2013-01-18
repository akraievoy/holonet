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
import org.akraievoy.holonet.exp.store.StoreLens
import org.akraievoy.base.runner.vo.Parameter.Strategy
import org.akraievoy.cnet.opt.api._
import org.akraievoy.cnet.soo.domain._
import org.akraievoy.cnet.opt.domain.ExperimentGeneticOpt
import org.akraievoy.holonet.exp.store.StoreLens
import org.akraievoy.holonet.exp.store.StoreLens
import org.akraievoy.base.runner.api.ExperimentTiming
import scala.Some
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
    //  stage 3 inputs
    val entropySourceGASeed = ParamName[JLong]("entropySourceGenetics.seed")
    val gaGeneration = ParamName[JInt]("ovlGenOpt.generation")
    val gaSpecimen = ParamName[JInt]("ovlGenOpt.specimenIndex")
    val gaGenLimitRatio = ParamName[JDouble]("ovlGenOpt.generateLimitRatio")
    val gaEliteRatio = ParamName[JDouble]("ovlGenOpt.eliteRatio")
    val gaCrossoverRatio = ParamName[JDouble]("ovlGenOpt.crossOverRatio")
    val gaMutationRatio = ParamName[JDouble]("ovlGenOpt.mutationRatio")
    val gaStateMaxCrossover = ParamName[JDouble]("geneticState.maxCrossover")
    val gaStateMaxElemFitPow = ParamName[JDouble]("geneticState.maxElemFitPow")
    val gaStateMaxMutation = ParamName[JDouble]("geneticState.maxMutation")
    val gaStrategyTheta = ParamName[JDouble]("geneticStrategy.theta")
    val gaStrategyThetaTilde = ParamName[JDouble]("geneticStrategy.thetaTilde")
    val gaStrategySteps = ParamName[JInt]("geneticStrategy.steps")
    val gaStrategyMinEff = ParamName[JDouble]("geneticStrategy.minEff")
    val gaStrategyModes = ParamName[String]("geneticStrategy.modes")
    val gaStrategyFitnessCap = ParamName[JDouble]("geneticStrategy.fitnessCap")
    //  stage 3 outputs
    val gaGenome = ParamName[JDouble]("ovlGenOpt.genome")
    val gaGenomeBest = ParamName[EdgeDataSparse]("ovlGenOpt.genome.best.0")
    val gaDurationText = ParamName[String]("ovlGenOpt.duration.text")
    val gaDurationMillis = ParamName[JLong]("ovlGenOpt.duration.millis")
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
      Param(locationMetricNodes, "1024"),
      Param(structureBaseDegree, "2"),
      Param(connPreferenceBeta, "2.75")
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
  ).withGraphvizExport(
    GraphvizExport(
      name = "physical", desc = "physical network structure export",
      edgeStructure = {_.lens(physStructure)},
      edgeLabel = {rs => Some(rs.lens(physDistance))},
      edgeColor = {rs => Some(rs.lens(physDistance))},
      vertexColor = {rs => Some(rs.lens(physDensity))},
      vertexCoordX = {rs => Some(rs.lens(physLocationX))},
      vertexCoordY = {rs => Some(rs.lens(physLocationY))},
      vertexRadius = {rs => Some(rs.lens(physDensity))}
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
      "nu20",
      "Default (select 20% of nodes)",
      Param(ovlNetFactoryNu, "0.2")
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
  ).withGraphvizExport(
    GraphvizExport(
      name = "request", desc = "overlay request network",
      edgeStructure = {_.lens(overlayRequest)},
      edgeWidth = {rs => Some(rs.lens(overlayRequest))},
      edgeLabel = {rs => Some(rs.lens(overlayRequest))},
      edgeColor = {rs => Some(rs.lens(overlayDistance))},
      vertexColor = {rs => Some(rs.lens(overlayDensity))},
      vertexCoordX = {rs => Some(rs.lens(overlayLocationX))},
      vertexCoordY = {rs => Some(rs.lens(overlayLocationY))},
      vertexRadius = {
        rs =>
          val powers = new MetricVDataPowers()
          powers.setSource(rs.lens(overlayRequest).asInstanceOf[Ref[EdgeData]])
          Some(powers)
      },
      vertexLabel = {rs => Some(rs.lens(overlayIndex))}
    )
  )

  val experiment3genetics = Experiment(
    "overlayGO-3-genetics",
    "Overlay GO [stage3] Overlay Genetics",
    Seq("overlayGO-2-ovlDataset"),
    {
      rs =>
        val entropySourceGenetics = new EntropySourceRandom()
        entropySourceGenetics.setSeed(
          rs.lens(entropySourceGASeed).get.get
        )

        val gaState = new GeneticState()
        gaState.setFitnessDeviationMax(0.98)
        gaState.setFitnessDeviationMin(0.02)
        gaState.setMinElemFitnessNorm(0.005)

        gaState.setMaxCrossover(rs.lens(gaStateMaxCrossover).get.get)
        gaState.setMaxElemFitPow(rs.lens(gaStateMaxElemFitPow).get.get)
        gaState.setMaxMutation(rs.lens(gaStateMaxMutation).get.get)

        val gaStrategy = new GeneticStrategySoo(
          new MetricRoutesFloydWarshall()
        )
        gaStrategy.setMinEff(rs.lens(gaStrategyMinEff).get.get)
        gaStrategy.setTheta(rs.lens(gaStrategyTheta).get.get)
        gaStrategy.setThetaTilde(rs.lens(gaStrategyThetaTilde).get.get)
        gaStrategy.setModes(rs.lens(gaStrategyModes).get.get)
        gaStrategy.setSteps(rs.lens(gaStrategySteps).get.get)
        gaStrategy.setDistSource(rs.lens(overlayDistance).asInstanceOf[StoreLens[EdgeData]])
        gaStrategy.setRequestSource(rs.lens(overlayRequest).asInstanceOf[StoreLens[EdgeData]])
        gaStrategy.setFitnessCap(rs.lens(gaStrategyFitnessCap).get.get)

        val ga = new ExperimentGeneticOpt(
          gaStrategy.asInstanceOf[GeneticStrategy[Genome]],
          entropySourceGenetics
        )
        ga.setState(gaState)
        ga.setSeedSource(new SeedSourceHeuristic().asInstanceOf[SeedSource[Genome]])
        ga.setBreeders(
          Seq(
            new BreederSooExpand(),
            new BreederSooLocalize()
          ).map(_.asInstanceOf[Breeder[Genome]])
        )
        ga.setMutators(
          Seq(
            new MutatorSooRewireExpand(),
            new MutatorSooRewireLocalize()
          ).map(_.asInstanceOf[Mutator[Genome]])
        )
        ga.setConditions(
          Seq(
            new ConditionSooFitnessCapping(),
            new ConditionSooVertexDensity(),
            new ConditionSooEffectiveness(),
            new ConditionSooDensity(),
            new ConditionUnique()
          ).map(_.asInstanceOf[Condition[Genome]])
        )
        ga.setEliteRatio(rs.lens(gaEliteRatio).get.get)
        ga.setCrossoverRatio(rs.lens(gaCrossoverRatio).get.get)
        ga.setMutationRatio(rs.lens(gaMutationRatio).get.get)
        ga.setGenerateLimitRatio(rs.lens(gaGenLimitRatio).get.get)
        ga.setSpecimenLens(rs.lens(gaSpecimen))
        ga.setGenerationLens(rs.lens(gaGeneration))
        ga.setGenomeLens(rs.lens(gaGenome))

        val timing = new ExperimentTiming(ga)
        timing.setDurationTextRef(rs.lens(gaDurationText))
        timing.setDurationRef(rs.lens(gaDurationMillis))

        timing.run()
    },
    Config(
      Param(entropySourceGASeed, "42600--42602"),
      Param(gaGeneration, "0--100", Strategy.ITERATE, Strategy.USE_LAST),
      Param(gaSpecimen, "0--90", Strategy.USE_FIRST, Strategy.USE_FIRST),
      Param(gaGenLimitRatio, "233"),
      Param(gaEliteRatio, "0.1"),
      Param(gaStateMaxCrossover, "0.25"),
      Param(gaCrossoverRatio, "0.15"),
      Param(gaMutationRatio, "0.15"),
      Param(gaStateMaxElemFitPow, "3"),
      Param(gaStateMaxMutation, "0.05"),
      Param(gaStrategyTheta, "1.75"),
      Param(gaStrategyThetaTilde, "0.75"),
      Param(gaStrategySteps, "1"),
      Param(gaStrategyModes, ""),
      Param(gaStrategyMinEff, "1.2"),
      Param(gaStrategyFitnessCap, "1")
    ),
    Config(
      "minEff12x2x3",
      "MinEff: 1.2 * 2 seeds * 3 gens (debug)",
      Param(gaStrategyMinEff, "1.2"),
      Param(entropySourceGASeed, "42600--42601"),
      Param(gaGeneration, "0--2", Strategy.ITERATE, Strategy.USE_LAST)
    ),
    Config("minEff13", "MinEff: 1.3", Param(gaStrategyMinEff, "1.3")),
    Config(
      "minEff13x4x64",
      "MinEff: 1.3 * 4 seeds * 64 gens (fast sim corr study)",
      Param(gaStrategyMinEff, "1.3"),
      Param(entropySourceGASeed, "42600--42603"),
      Param(gaStrategyModes, "R"),
      Param(
        gaStrategyFitnessCap,
        "0.3;0.35;0.4;0.45;0.5;0.55;0.6;0.65;0.7;0.75;0.8;0.85"
      ),
      Param(gaStrategyThetaTilde, "0.75"),
      Param(gaGeneration, "0--63", Strategy.ITERATE, Strategy.USE_LAST),
      Param(gaEliteRatio, "0.2")
    ),
    Config("minEff14", "MinEff: 1.4", Param(gaStrategyMinEff, "1.4")),
    Config("minEff15", "MinEff: 1.5", Param(gaStrategyMinEff, "1.5")),
    Config("minEff16", "MinEff: 1.6", Param(gaStrategyMinEff, "1.6")),
    Config("minEff17", "MinEff: 1.7", Param(gaStrategyMinEff, "1.7")),
    Config(
      "minEff13-smoke",
      "MinEff: 1.3 (smoke)",
      Param(gaStrategyMinEff, "1.3"),
      Param(entropySourceGASeed, "42600"),
      Param(gaGeneration, "0--12", Strategy.ITERATE, Strategy.USE_LAST),
      Param(gaSpecimen, "0--9", Strategy.USE_FIRST, Strategy.USE_FIRST)
    ),
    Config("minEff19", "MinEff: 1.9", Param(gaStrategyMinEff, "1.9")),
    Config(
      "minEff19x50x768",
      "MinEff: 1.9 * 50 seeds * 768 gens (full opt)",
      Param(gaStrategyMinEff, "1.9"),
      Param(entropySourceGASeed, "42600--42649"),
      Param(gaGeneration, "0--767", Strategy.ITERATE, Strategy.USE_LAST)
    ),
    Config(
      "minEff19x50x256",
      "MinEff: 1.9 * 50 seeds * 256 gens (tuning)",
      Param(gaStrategyMinEff, "1.9"),
      Param(entropySourceGASeed, "42600--42649"),
      Param(gaGeneration, "0--255", Strategy.ITERATE, Strategy.USE_LAST),
      Param(gaEliteRatio, "0.01;0.03;0.05;0.1;0.2")
    ),
    Config("minEff20", "MinEff: 2.0", Param(gaStrategyMinEff, "2.0")),
    Config("minEff21", "MinEff: 2.1", Param(gaStrategyMinEff, "2.1")),
    Config("minEff215", "MinEff: 2.15", Param(gaStrategyMinEff, "2.15")),
    Config("minEff23", "MinEff: 2.3", Param(gaStrategyMinEff, "2.3")),
    Config(
      "thetaProf",
      "Varying Density (Theta)",
      Param(gaStrategyTheta, "1;1.25;1.5;1.75")
    ),
    Config(
      "stepsProf",
      "Varying Steps",
      Param(gaStrategySteps, "1;3;5"),
      Param(entropySourceGASeed, "42600--42620")
    )
  ).withGraphvizExport(
    GraphvizExport(
      name = "overlay-best", desc = "overlay network - best specimen",
      edgeStructure = {_.lens(gaGenomeBest)},
      edgeColor = {rs => Some(rs.lens(overlayDistance))},
      vertexColor = {
        rs =>
          val powers = new MetricVDataPowers()
          powers.setSource(rs.lens(overlayRequest).asInstanceOf[Ref[EdgeData]])
          Some(powers)
      },
      vertexCoordX = {rs => Some(rs.lens(overlayLocationX))},
      vertexCoordY = {rs => Some(rs.lens(overlayLocationY))},
      vertexRadius = {
        rs =>
          val powers = new MetricVDataPowers()
          powers.setSource(rs.lens(gaGenomeBest).asInstanceOf[Ref[EdgeData]])
          Some(powers)
      },
      vertexLabel = {rs => Some(rs.lens(overlayIndex))}
    )
  )
}
