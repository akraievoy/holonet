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
import org.akraievoy.cnet.net.vo._
import org.akraievoy.cnet.metrics.domain._
import scala.collection.JavaConversions._
import org.akraievoy.base.ref.{RefRO, Ref}
import org.akraievoy.cnet.opt.api._
import org.akraievoy.cnet.soo.domain._
import org.akraievoy.cnet.opt.domain.ExperimentGeneticOpt
import scala.Some
import org.akraievoy.cnet.net.vo.Store.Width

object OverlayGA {
  import java.lang.{
    Byte => JByte, Integer => JInt, Long => JLong,
    Float => JFloat, Double => JDouble
  }

  object ParamNames {
    //  stage 1 inputs
    val p1locProbSeed = ParamName[JLong]("p1locProbSeed")
    val p1locSeed = ParamName[JLong]("p1locSeed")
    val p1structSeed = ParamName[JLong]("p1structSeed")
    val p1physNodes = ParamName[JInt]("p1physNodes")
    val p1physPowFactor = ParamName[JDouble]("p1physAlpha")
    val p1physDistFactor = ParamName[JDouble]("p1physBeta")
    val p1physDegree = ParamName[JInt]("p1physDegree")
    //  stage 1 outputs
    val p1physInit = ParamName[EdgeDataSparse]("p1physInit")
    val p1phys = ParamName[EdgeDataSparse]("p1phys")
    val p1dist = ParamName[EdgeDataDense]("p1dist")
    val p1routeLen = ParamName[EdgeDataDense]("p1routeLen")
    val p1locX = ParamName[VertexData]("p1locX")
    val p1locY = ParamName[VertexData]("p1locY")
    val p1density = ParamName[VertexData]("p1density")
    val p1lambda = ParamName[JDouble]("p1lambda")
    val p1powers = ParamName[StoreInt]("p1powers")
    val p1densities = ParamName[StoreDouble]("p1densities")
    val p1distances = ParamName[StoreDouble]("p1distances")
    //  stage 2 inputs
    val p2nodeSeed = ParamName[JLong]("p2nodeSeed")
    val p2reqSeed = ParamName[JLong]("p2reqSeed")
    val p2nodePowFactor = ParamName[JDouble]("p2nodePowFactor")
    val p2nodeRatio = ParamName[JDouble]("p2nodeRatio")
    val p2reqClientFactor = ParamName[JDouble]("p2reqClientFactor")
    val p2reqServerFactor = ParamName[JDouble]("p2reqServerFactor")
    val p2reqVariance = ParamName[JDouble]("p2reqVariance")
    val p2reqMinVal = ParamName[JDouble]("p2reqMinVal")
    val p2reqMinRatio = ParamName[JDouble]("p2reqMinRatio")
    val p2reqStoreVol = ParamName[StoreDouble]("p2reqStoreVol")
    val p2reqStoreDist = ParamName[StoreDouble]("p2reqStoreDist")
    val p2reqStoreFromDensity = ParamName[StoreDouble]("p2reqStoreFromDensity")
    val p2reqStoreIntoDensity = ParamName[StoreDouble]("p2reqStoreIntoDensity")
    //  stage 2 outputs
    val p2nodeIndex = ParamName[VertexData]("p2nodeIndex")
    val p2nodeDist = ParamName[EdgeDataDense]("p2nodeDist")
    val p2req = ParamName[EdgeDataSparse]("p2req")
    val p2locX = ParamName[VertexData]("p2locX")
    val p2locY = ParamName[VertexData]("p2locY")
    val p2density = ParamName[VertexData]("p2density")
    //  stage 3 inputs
    val p3seed = ParamName[JLong]("p3gaSeed")
    val p3generation = ParamName[JInt]("p3generation")
    val p3specimen = ParamName[JInt]("p3specimen")
    val p3generateMax = ParamName[JDouble]("p3generateMax")
    val p3generatePow = ParamName[JDouble]("p3generatePow")
    val p3elite = ParamName[JDouble]("p3gaElite")
    val p3crossover = ParamName[JDouble]("p3crossover")
    val p3mutate = ParamName[JDouble]("p3mutate")
    val p3stateCrossoverMax = ParamName[JDouble]("p3stateCrossoverMax")
    val p3stateFitPowMax = ParamName[JDouble]("p3stateFitPowMax")
    val p3stateMutateMax = ParamName[JDouble]("p3stateMutateMax")
    val p3densityMax = ParamName[JDouble]("p3densityMax")
    val p3densityMin = ParamName[JDouble]("p3densityMin")
    val p3stepDelta = ParamName[JInt]("p3stepDelta")
    val p3minEff = ParamName[JDouble]("p3minEff")
    val p3flags = ParamName[String]("p3flags")
    val p3fitCap = ParamName[JDouble]("p3fitCap")
    //  stage 3 outputs
    val p3genome = ParamName[JDouble]("p3genome")
    val p3genomeBest = ParamName[EdgeDataSparse]("p3genomeBest.0")
    val p3time = ParamName[String]("p3time")
    val p3timeMillis = ParamName[JLong]("p3timeMillis")
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
          rs.lens(p1locProbSeed).get.get
        )

        val locationGenerator = new LocationGeneratorRecursive(
          entropySourceLocGen
        )
        locationGenerator.setGridSize(1024)
        locationGenerator.setDimensionRatio(1.5)

        val netFactory = new MetricEDataGenStructural()
        netFactory.setNetNodeNum(3)
        netFactory.setType("path")
        netFactory.setTarget(rs.lens(p1physInit))

        val entropySourceLocation = new EntropySourceRandom()
        entropySourceLocation.setSeed(
          rs.lens(p1locSeed).get.get
        )

        val locationMetric = new MetricVDataLocation(
          entropySourceLocation,
          locationGenerator
        )
        locationMetric.setTargetX(rs.lens(p1locX))
        locationMetric.setTargetY(rs.lens(p1locY))
        locationMetric.setNodes(rs.lens(p1physNodes).get.get)

        val distMetric = new MetricEDataDistance(
          new MetricEuclidean()
        )
        distMetric.setSourceX(rs.lens(p1locX))
        distMetric.setSourceY(rs.lens(p1locY))
        distMetric.setTarget(rs.lens(p1dist))

        val entropySource = new EntropySourceRandom()
        entropySource.setSeed(
          rs.lens(p1structSeed).get.get
        )

        val connPreference = new ConnPreferenceYookJeongBarabasi()
        connPreference.setAlpha(rs.lens(p1physPowFactor).get.get)
        connPreference.setBeta(rs.lens(p1physDistFactor).get.get)

        val structure = new MetricEDataStructure(
          connPreference,
          entropySource
        )
        structure.setDistSource(rs.lens(p1dist))
        structure.setStructureSource(rs.lens(p1physInit))
        structure.setTarget(rs.lens(p1phys))
        structure.setBaseDegree(rs.lens(p1physDegree).get.get)

        val densityMetric = new MetricVDataDensity(
          locationGenerator
        )
        densityMetric.setSourceX(rs.lens(p1locX))
        densityMetric.setSourceY(rs.lens(p1locY))
        densityMetric.setTarget(rs.lens(p1density))

        val metricRoutesJohnson = new MetricRoutesJohnson()
        metricRoutesJohnson.setSource(rs.lens(p1phys))
        metricRoutesJohnson.setDistSource(rs.lens(p1dist))

        val routeLenMetric = new MetricEDataRouteLen(
          metricRoutesJohnson
        )
        routeLenMetric.setTarget(rs.lens(p1routeLen))

        val eigenGapMetric = new MetricScalarEigenGap()
        eigenGapMetric.setSource(rs.lens(p1phys))
        eigenGapMetric.setTarget(rs.lens(p1lambda))

        val powersMetric = new MetricVDataPowers(
          rs.lens(p1phys)
        )
        val powersStoreMetric = new MetricStoreVData(
          powersMetric, rs.lens(p1powers), Width.INT
        )

        val densitiesStoreMetric = new MetricStoreVData(
          rs.lens(p1density), rs.lens(p1densities), Width.DOUBLE
        )

        val distancesStoreMetric = new MetricStoreEData(
          rs.lens(p1phys),
          rs.lens(p1dist),
          rs.lens(p1distances),
          Width.DOUBLE
        )

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
            eigenGapMetric,
            powersStoreMetric,
            densitiesStoreMetric,
            distancesStoreMetric
          )
        )

        main.run()  //  d'oh, at last

    },
    Config(
      Param(p1locProbSeed, "234453"),
      Param(p1locSeed, "634567"),
      Param(p1structSeed, "967436"),
      Param(p1physNodes, "24"),
      Param(p1physPowFactor, "1.5"),
      Param(p1physDistFactor, "2.75"),
      Param(p1physDegree, "3")
    ),
    Config(
      "vis-requests",
      "Visual for Requests",
      Param(p1physNodes, "64"),
      Param(p1physDegree, "2")
    ),
    Config(
      "vis-byAlpha",
      "Visual : By Alpha (64 nodes)",
      Param(p1physNodes, "64"),
      Param(p1physPowFactor, "-4;2;4"),
      Param(p1physDegree, "2")
    ),
    Config(
      "vis-byBeta",
      "Visual : By Beta (64 nodes)",
      Param(p1physNodes, "64"),
      Param(p1physDistFactor, "-4;2;4"),
      Param(p1physDegree, "2")
    ),
    Config(
      "med",
      "Medium (128 nodes, 1 seed)",
      Param(p1physNodes, "128")
    ),
    Config(
      "big",
      "Big (240 nodes, 1 seed)",
      Param(p1physNodes, "240")
    ),
    Config(
      "big-1k",
      "Big (1k nodes, 1 seed)",
      Param(p1locProbSeed, "123098"),
      Param(p1locSeed, "579384"),
      Param(p1structSeed, "780293"),
      Param(p1physNodes, "1024"),
      Param(p1physDegree, "3"),
      Param(p1physDistFactor, "5")
    ),
    Config(
      "big-2k",
      "Big (2k nodes, 1 seed)",
      Param(p1physNodes, "2048")
    ),
    Config(
      "big-4k",
      "Big (4k nodes, 1 seed)",
      Param(p1physNodes, "4096")
    )
  ).withGraphvizExport(
    GraphvizExport(
      name = "physical", desc = "physical network structure export",
      edgeStructure = {_.lens(p1phys)},
      edgeLabel = {rs => Some(rs.lens(p1dist))},
      edgeColor = {rs => Some(rs.lens(p1dist))},
      vertexColor = {rs => Some(rs.lens(p1density))},
      vertexCoordX = {rs => Some(rs.lens(p1locX))},
      vertexCoordY = {rs => Some(rs.lens(p1locY))},
      vertexRadius = {rs => Some(rs.lens(p1density))}
    )
  ).withStoreExport(
    StoreExport(
      "powers", desc = "physical network, power distribution",
      Seq(p1powers)
    )
  ).withStoreExport(
    StoreExport(
      "distances", desc = "physical network, distance distribution",
      Seq(p1distances)
    )
  ).withStoreExport(
    StoreExport(
      "densities", desc = "physical network, density distribution",
      Seq(p1densities)
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
          rs.lens(p2nodeSeed).get.get
        )

        val reqESource = new EntropySourceRandom()
        reqESource.setSeed(
          rs.lens(p2reqSeed).get.get
        )

        val ovlNetFactory = new OverlayNetFactory(ovlESource)
        ovlNetFactory.setOmega(
          rs.lens(p2nodePowFactor).get.get
        )
        ovlNetFactory.setNu(
          rs.lens(p2nodeRatio).get.get
        )
        ovlNetFactory.setSource(
          rs.lens(p1phys)
        )
        ovlNetFactory.setTarget(rs.lens(p2nodeIndex))
        ovlNetFactory.setEdgeDataMap(
          Map[RefRO[_ <: EdgeData], Ref[EdgeData]](
            rs.lens(p1routeLen) ->
              rs.lens(p2nodeDist).asInstanceOf[Ref[EdgeData]]
          )
        )
        ovlNetFactory.setVertexDataMap(
          Map[RefRO[VertexData], Ref[VertexData]](
            rs.lens(p1locX) -> rs.lens(p2locX),
            rs.lens(p1locY) -> rs.lens(p2locY),
            rs.lens(p1density) -> rs.lens(p2density)
          )
        )

        val ovlRequests = new MetricEDataOverlayRequest(reqESource)
        ovlRequests.setSource(rs.lens(p2density))
        ovlRequests.setPhi(rs.lens(p2reqClientFactor).get.get)
        ovlRequests.setPsi(rs.lens(p2reqServerFactor).get.get)
        ovlRequests.setSigma(rs.lens(p2reqVariance).get.get)

        val reqThreshold = new MetricEDataThreshold(ovlRequests)
        reqThreshold.setTarget(rs.lens(p2req))
        reqThreshold.setMinAbsValue(rs.lens(p2reqMinVal).get.get)
        reqThreshold.setMinToMaxRatio(rs.lens(p2reqMinRatio).get.get)

        val volumesStoreMetric = new MetricStoreEData(
          rs.lens(p2req),
          rs.lens(p2req),
          rs.lens(p2reqStoreVol),
          Width.DOUBLE
        ).withFromData(
          rs.lens(p2density),
          rs.lens(p2reqStoreFromDensity)
        ).withIntoData(
          rs.lens(p2density),
          rs.lens(p2reqStoreIntoDensity)
        )

        val distancesStoreMetric = new MetricStoreEData(
          rs.lens(p2req),
          rs.lens(p2nodeDist),
          rs.lens(p2reqStoreDist),
          Width.DOUBLE
        )

        ovlNetFactory.run()
        reqThreshold.run()
        volumesStoreMetric.run()
        distancesStoreMetric.run()
    },
    Config(
      Param(p2nodeSeed, "31013"),
      Param(p2reqSeed, "11311"),
      Param(p2nodePowFactor, "-1"),
      Param(p2nodeRatio, "0.25"),
      Param(p2reqClientFactor, "0.25"),
      Param(p2reqServerFactor, "1.25"),
      Param(p2reqVariance, "2"),
      Param(p2reqMinVal, "0.2"),
      Param(p2reqMinRatio, "0.02")
    ),
    Config(
      "nu20",
      "Default (select 20% of nodes)",
      Param(p2nodeRatio, "0.2")
    ),
    Config(
      "nu50",
      "Default (select 50% of nodes)",
      Param(p2nodeRatio, "0.5")
    ),
    Config(
      "nu20_omegaProf",
      "Visual (select 20% of nodes), profile by Omega",
      Param(p2nodeRatio, "0.2"),
      Param(p2nodePowFactor, "-4;2;4"),
      Param(p2reqClientFactor, "1"),
      Param(p2reqServerFactor, "1"),
      Param(p2reqMinVal, "0.05"),
      Param(p2reqMinRatio, "0.05")
    )
  ).withGraphvizExport(
    GraphvizExport(
      name = "request", desc = "overlay request network",
      edgeStructure = {_.lens(p2req)},
      edgeWidth = {rs => Some(rs.lens(p2req))},
      edgeLabel = {rs => Some(rs.lens(p2req))},
      edgeColor = {rs => Some(rs.lens(p2nodeDist))},
      vertexColor = {rs => Some(rs.lens(p2density))},
      vertexCoordX = {rs => Some(rs.lens(p2locX))},
      vertexCoordY = {rs => Some(rs.lens(p2locY))},
      vertexRadius = {
        rs =>
          val powers = new MetricVDataPowers()
          powers.setSource(rs.lens(p2req))
          Some(powers)
      },
      vertexLabel = {rs => Some(rs.lens(p2nodeIndex))}
    )
  ).withStoreExport(
    StoreExport(
      "requests", desc = "overlay network, request distribution",
      Seq(p2reqStoreVol, p2reqStoreDist, p2reqStoreFromDensity, p2reqStoreIntoDensity)
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
          rs.lens(p3seed).get.get
        )

        val gaState = new GeneticState()
        gaState.setFitnessDeviationMax(0.98)
        gaState.setFitnessDeviationMin(0.02)
        gaState.setMinElemFitnessNorm(0.005)

        gaState.setMaxCrossover(rs.lens(p3stateCrossoverMax).get.get)
        gaState.setMaxElemFitPow(rs.lens(p3stateFitPowMax).get.get)
        gaState.setMaxMutation(rs.lens(p3stateMutateMax).get.get)

        val gaStrategy = new GeneticStrategySoo(
          new MetricRoutesFloydWarshall()
        )
        gaStrategy.setMinEff(rs.lens(p3minEff).get.get)
        gaStrategy.setTheta(rs.lens(p3densityMax).get.get)
        gaStrategy.setThetaTilde(rs.lens(p3densityMin).get.get)
        gaStrategy.setModes(rs.lens(p3flags).get.get)
        gaStrategy.setSteps(rs.lens(p3stepDelta).get.get)
        gaStrategy.setDistSource(rs.lens(p2nodeDist))
        gaStrategy.setRequestSource(rs.lens(p2req))
        gaStrategy.setFitnessCap(rs.lens(p3fitCap).get.get)

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
        ga.setAdaptMutators(
          Seq(
            new MutatorSooRegularize(),
            new MutatorSooClusterize(),
            new MutatorSooNoop()
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
        ga.setEliteRatio(rs.lens(p3elite).get.get)
        ga.setCrossoverRatio(rs.lens(p3crossover).get.get)
        ga.setMutationRatio(rs.lens(p3mutate).get.get)
        ga.setGenerateLimitRatioMax(rs.lens(p3generateMax).get.get)
        ga.setGenerateLimitRatioPow(rs.lens(p3generatePow).get.get)
        ga.setSpecimenLens(rs.lens(p3specimen))
        ga.setGenerationLens(rs.lens(p3generation))
        ga.setGenomeLens(rs.lens(p3genome))

        val timing = new ExperimentTiming(ga)
        timing.setDurationTextRef(rs.lens(p3time))
        timing.setDurationRef(rs.lens(p3timeMillis))

        timing.run()
    },
    Config(
      Param(p3seed, "42600--42602"),
      Param(p3generation, "0--100", Strategy.ITERATE, Strategy.USE_LAST),
      Param(p3specimen, "0--90", Strategy.USE_FIRST, Strategy.USE_FIRST),
      Param(p3generateMax, "233"),
      Param(p3generatePow, "2"),
      Param(p3elite, "0.1"),
      Param(p3stateCrossoverMax, "0.25"),
      Param(p3crossover, "0.15"),
      Param(p3mutate, "0.15"),
      Param(p3stateFitPowMax, "3"),
      Param(p3stateMutateMax, "0.05"),
      Param(p3densityMax, "1.75"),
      Param(p3densityMin, "0.75"),
      Param(p3stepDelta, "1"),
      Param(p3flags, ""),
      Param(p3minEff, "1.2"),
      Param(p3fitCap, "1")
    ),
    Config(
      "corrStudy-smoke",
      "Correlation study --- smoke",
      Param(p3minEff, "0.8"),
      Param(p3seed, "42601"),
      Param(p3flags, "R"),
      Param(p3specimen, "0--21", Strategy.USE_FIRST, Strategy.USE_FIRST),
      Param(
        p3fitCap,
        "0.025;0.3;0.8"
      ),
      Param(p3generateMax, "2"),
      Param(p3generatePow, "10"),
      Param(p3densityMin, "1.5"),
      Param(p3generation, "0--10", Strategy.ITERATE, Strategy.USE_LAST),
      Param(p3elite, "1")
    ),
    Config(
      "corrStudy-full",
      "Correlation study --- full",
      Param(p3minEff, "0.8"),
      Param(p3seed, "42600--42603"),
      Param(p3flags, "R"),
      Param(p3specimen, "0--21", Strategy.USE_FIRST, Strategy.USE_FIRST),
      Param(
        p3fitCap,
        "0.025;0.05;0.1;0.3;0.5;0.8"
      ),
      Param(p3generateMax, "2"),
      Param(p3generatePow, "10"),
      Param(p3densityMin, "1.5"),
      Param(p3generation, "0--22", Strategy.ITERATE, Strategy.USE_LAST),
      Param(p3elite, "0.9")
    ),
    Config(
      "minEff12x2x3",
      "MinEff: 1.2 * 2 seeds * 3 gens (debug)",
      Param(p3minEff, "1.2"),
      Param(p3seed, "42600--42601"),
      Param(p3generation, "0--2", Strategy.ITERATE, Strategy.USE_LAST)
    ),
    Config("minEff13", "MinEff: 1.3", Param(p3minEff, "1.3")),
    Config("minEff14", "MinEff: 1.4", Param(p3minEff, "1.4")),
    Config("minEff15", "MinEff: 1.5", Param(p3minEff, "1.5")),
    Config("minEff16", "MinEff: 1.6", Param(p3minEff, "1.6")),
    Config("minEff17", "MinEff: 1.7", Param(p3minEff, "1.7")),
    Config(
      "minEff13-smoke",
      "MinEff: 1.3 (smoke)",
      Param(p3minEff, "1.3"),
      Param(p3seed, "42600"),
      Param(p3generation, "0--12", Strategy.ITERATE, Strategy.USE_LAST),
      Param(p3specimen, "0--9", Strategy.USE_FIRST, Strategy.USE_FIRST)
    ),
    Config("minEff19", "MinEff: 1.9", Param(p3minEff, "1.9")),
    Config(
      "minEff19x50x768",
      "MinEff: 1.9 * 50 seeds * 768 gens (full opt)",
      Param(p3minEff, "1.9"),
      Param(p3seed, "42600--42649"),
      Param(p3generation, "0--767", Strategy.ITERATE, Strategy.USE_LAST)
    ),
    Config(
      "minEff19x50x256",
      "MinEff: 1.9 * 50 seeds * 256 gens (tuning)",
      Param(p3minEff, "1.9"),
      Param(p3seed, "42600--42649"),
      Param(p3generation, "0--255", Strategy.ITERATE, Strategy.USE_LAST),
      Param(p3elite, "0.01;0.03;0.05;0.1;0.2")
    ),
    Config("minEff20", "MinEff: 2.0", Param(p3minEff, "2.0")),
    Config("minEff21", "MinEff: 2.1", Param(p3minEff, "2.1")),
    Config("minEff215", "MinEff: 2.15", Param(p3minEff, "2.15")),
    Config("minEff23", "MinEff: 2.3", Param(p3minEff, "2.3")),
    Config(
      "thetaProf",
      "Varying Density (Theta)",
      Param(p3densityMax, "1;1.25;1.5;1.75")
    ),
    Config(
      "stepsProf",
      "Varying Steps",
      Param(p3stepDelta, "1;3;5"),
      Param(p3seed, "42600--42620")
    )
  ).withGraphvizExport(
    GraphvizExport(
      name = "overlay-best", desc = "overlay network - best specimen",
      edgeStructure = {_.lens(p3genomeBest)},
      edgeColor = {rs => Some(rs.lens(p2nodeDist))},
      vertexColor = {
        rs =>
          val powers = new MetricVDataPowers()
          powers.setSource(rs.lens(p2req).asInstanceOf[Ref[EdgeData]])
          Some(powers)
      },
      vertexCoordX = {rs => Some(rs.lens(p2locX))},
      vertexCoordY = {rs => Some(rs.lens(p2locY))},
      vertexRadius = {
        rs =>
          val powers = new MetricVDataPowers()
          powers.setSource(rs.lens(p3genomeBest).asInstanceOf[Ref[EdgeData]])
          Some(powers)
      },
      vertexLabel = {rs => Some(rs.lens(p2nodeIndex))}
    )
  )
}
