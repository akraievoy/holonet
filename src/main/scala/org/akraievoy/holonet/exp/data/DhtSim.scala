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
import scala.collection.JavaConversions._
import org.akraievoy.holonet.exp.store.{RefObject, RunStore}
import algores.holonet.core.{Network, EnvCNet}
import algores.holonet.testbench.Testbench
import algores.holonet.core.events._
import org.akraievoy.cnet.metrics.domain.MetricVDataPowers
import org.akraievoy.cnet.net.vo.{EdgeDataSparse, EdgeDataDense, VertexData}
import org.akraievoy.holonet.exp.GraphvizExport.ColorScheme
import algores.holonet.core.api.tier1.delivery.LookupService.Mode

object DhtSim {
  import java.lang.{
    Byte => JByte, Integer => JInt, Long => JLong,
    Float => JFloat, Double => JDouble
  }

  object ParamNames {
    //  stage 1 inputs
    val p4initSeed = ParamName[JLong]("p4initSeed")
    val p4runSeed = ParamName[JLong]("p4runSeed")
    //  stage 2 inputs
    val p5nodes = ParamName[JLong]("p5nodes")
    val p5Elems = ParamName[JLong]("p5Elems")
    val p5loops = ParamName[JLong]("p5loops")
    val p5failProb = ParamName[JDouble]("p5failProb")
    val p5joinProb = ParamName[JDouble]("p5joinProb")
    val p5stabilizeProb = ParamName[JDouble]("p5stabilizeProb")
    val p5attackProb = ParamName[JDouble]("p5attackProb")
    val p5routingRedundancy = ParamName[JDouble]("p5routingRedundancy")
    val p5maxFingerFlavorNum = ParamName[JInt]("p5maxFingerFlavorNum")
    //  stage 3 outputs
    val p6report = ParamName[JDouble]("p6report")

    val p6rangeSizes = ParamName[VertexData]("p6rangeSizes")
    val p6rpcCounts = ParamName[EdgeDataDense]("p6rpcCounts")
    val p6rpcFailures = ParamName[EdgeDataDense]("p6rpcFailures")
    val p6lookupCounts = ParamName[EdgeDataDense]("p6lookupCounts")
    val p6lookupFailures = ParamName[EdgeDataDense]("p6lookupFailures")
  }

  import ParamNames._

  val experiment1seeds = Experiment(
    "p2p-stage1-seed",
    "P2P [stage0] Choose your seeds",
    Nil,
    { rs => },
    Config(
      Param(p4initSeed, "9348524"),
      Param(p4runSeed, "91032845")
    ),
    Config(
      "3x2",
      "3 inits * 2 runs",
      Param(p4initSeed, "9348523--9348525"),
      Param(p4runSeed, "91032844--91032845")
    ),
    Config(
      "42x3",
      "42 inits * 3 runs",
      Param(p4initSeed, "9348524--9348533"),  //  TODO revert back to full range
      Param(p4runSeed, "91032843--91032845")
    )
  )

  val experiment2paramSpace = Experiment(
    "p2p-stage2-paramSpace",
    "P2P [stage2] Choose param space",
    Seq("p2p-stage1-seed"),
    { rs => },
    Config(
      Param(p5nodes, "60"),
      Param(p5Elems, "32"),
      Param(p5loops, "64"),
      Param(p5failProb, "0.003"),
      Param(p5joinProb, "0.01"),
      Param(p5stabilizeProb, "0.01"),
      Param(p5attackProb, "0.01"),
      Param(p5routingRedundancy, "1.0"),
      Param(p5maxFingerFlavorNum, "32")
    ),
    Config(
      "extraLoops",
      "extra loops",
      Param(p5loops, "256")
    ),
    Config(
      "def24",
      "default - 24 nodes",
      Param(p5nodes, "24")
    ),
    Config(
      "stabProf",
      "stabilize profiling",
      Param(p5stabilizeProb, "0.0;0.01;0.02;0.04;0.08;0.16;0.32;0.64")
    ),
    Config(
      "nodesProf",
      "nodes profiling",
      Param(p5nodes, "002;004;008;016;032;064;128;256;512")
    ),
    Config(
      "elemsProf",
      "elements profiling",
      Param(p5Elems, "002;004;008;016;032;064;128;256")
    ),
    Config(
      "loopProf",
      "loop profiling",
      Param(p5loops, "8;16;32;64;128")
    ),
    Config(
      "joinProf",
      "joins profiling",
      Param(p5joinProb, "0.0;0.01;0.02;0.04;0.08;0.16;0.32;0.64")
    ),
    Config(
      "failProf-large-192",
      "failure profiling, more loops and data for 192 nodes",
      Param(p5nodes, "192"),
      Param(p5failProb, "0.0;0.01;0.02;0.04;0.08;0.16;0.32;0.64"),
      Param(p5Elems, "256"),
      Param(p5loops, "256")
    ),
    Config(
      "corrStudy-large-16",
      "correlation study, more loops for 16 nodes",
      Param(p5nodes, "16"),
      Param(p5attackProb, "0.50"),
      Param(p5Elems, "256"),
      Param(p5loops, "80"),
      Param(p5routingRedundancy, "1.0"),
      Param(p5maxFingerFlavorNum, "0")
    ),
    Config(
      "corrStudy-large-256",
      "correlation study, more loops for 256 nodes",
      Param(p5nodes, "256"),
      Param(p5attackProb, "0.46875"),
      Param(p5Elems, "256"),
      Param(p5loops, "80"),
      Param(p5routingRedundancy, "1.0"),
      Param(p5maxFingerFlavorNum, "0")
    ),
    Config(
      "attackProf",
      "attacks profiling",
      Param(p5attackProb, "0.0;0.01;0.02;0.04;0.08;0.16;0.32;0.64")
    )
  )

  import OverlayGA.ParamNames._

  val experiment3attack = commonExports(Experiment(
    "p2p-stage3-attack",
    "P2P [stage3] Attack and profile lookups",
    Seq("p2p-stage2-paramSpace"),
    {
      rs =>
        val initEvent = commonInitEvent(rs)
        val runtimeEvent = new EventCompositeSequence(
          Seq(
            new EventCompositeLoop(
              new EventNodeAttackRoutingRank()
            ).withCountRef(
              new RefObject[JLong](
                math.ceil(rs.lens(p5nodes).get.get * rs.lens(p5attackProb).get.get).asInstanceOf[Long]
              )
            ),
            new EventNetStabilize(),
            new EventNetDiscover().mode(Mode.FIXFINGERS),
            new EventNetDiscover().excludeOffgridNodes(true).mode(Mode.GET)
          )
        )

        createTestBench(rs, initEvent, runtimeEvent).run()
    },
    Config(
    )
  ))

  def commonExports(exp: Experiment) = {
    val withExports = exp.withGraphvizExport(
      GraphvizExport(
        name = "rpc_failures", desc = "overlay stats - rpc failures",
        edgeStructure = {_.lens(p6rpcCounts)},
        edgeWidth = {rs => Some(rs.lens(p6rpcCounts))},
        edgeColor = {rs => Some(rs.lens(p6rpcFailures))},
        vertexColor = {
          rs =>
            val powers = new MetricVDataPowers()
            powers.setSource(rs.lens(p6rpcFailures))
            Some(powers)
        },
        vertexCoordX = {rs => Some(rs.lens(p2locX))},
        vertexCoordY = {rs => Some(rs.lens(p2locY))},
        vertexRadius = {rs => Some(rs.lens(p6rangeSizes))},
        vertexLabel = {rs => Some(rs.lens(p2nodeIndex))},
        edgeColorScheme = ColorScheme.VIOLET_RED,
        vertexColorScheme = ColorScheme.VIOLET_RED
      )
    ).withGraphvizExport(
      GraphvizExport(
        name = "lookup_failures", desc = "overlay stats - lookup failures",
        edgeStructure = {_.lens(p6lookupCounts)},
        edgeWidth = {rs => Some(rs.lens(p6lookupCounts))},
        edgeColor = {rs => Some(rs.lens(p6lookupFailures))},
        vertexColor = {
          rs =>
            val powers = new MetricVDataPowers()
            powers.setSource(rs.lens(p6lookupFailures))
            Some(powers)
        },
        vertexCoordX = {rs => Some(rs.lens(p2locX))},
        vertexCoordY = {rs => Some(rs.lens(p2locY))},
        vertexRadius = {rs => Some(rs.lens(p6rangeSizes))},
        vertexLabel = {rs => Some(rs.lens(p2nodeIndex))},
        edgeColorScheme = ColorScheme.VIOLET_RED,
        vertexColorScheme = ColorScheme.VIOLET_RED
      )
    )

    Seq(
      withLinkExport("preRun_links", "pre-run links", "preRun_linksAll") _,
      withLinkExport("preRun_linksDht", "pre-run DHT links", "preRun_linksDht") _,
      withLinkExport("preRun_linksSeed", "pre-run seed links", "preRun_linksSeed") _,
      withLinkExport("postRun_links", "post-run links", "postRun_linksAll") _,
      withLinkExport("postRun_linksDht", "post-run DHT links", "postRun_linksDht") _,
      withLinkExport("postRun_linksSeed", "post-run seed links", "postRun_linksSeed") _
    ).foldLeft(withExports){
      (exp, modFun) =>
        modFun(exp)
    }
  }

  def withLinkExport(
      exportName: String, desc: String, edgeDataSparseName: String
  )(e: Experiment): Experiment = {
    e.withGraphvizExport(
      GraphvizExport(
        name = exportName, desc = desc,
        edgeStructure = {_.lens(ParamName[EdgeDataSparse](edgeDataSparseName))},
        edgeWidth = {rs => Some(rs.lens(p6lookupCounts))},
        edgeColor = {rs => Some(rs.lens(p6lookupFailures))},
        vertexColor = {
          rs =>
            val powers = new MetricVDataPowers()
            powers.setSource(rs.lens(p6lookupFailures))
            Some(powers)
        },
        vertexCoordX = {rs => Some(rs.lens(p2locX))},
        vertexCoordY = {rs => Some(rs.lens(p2locY))},
        vertexRadius = {rs => Some(rs.lens(p6rangeSizes))},
        vertexLabel = {rs => Some(rs.lens(p2nodeIndex))},
        edgeColorScheme = ColorScheme.VIOLET_RED,
        vertexColorScheme = ColorScheme.VIOLET_RED
      )
    )
  }

  val experiment3static = commonExports(Experiment(
    "p2p-stage3-static",
    "P2P [stage3] Static scenario",
    Seq("p2p-stage2-paramSpace"),
    {
      rs =>
        val initEvent = commonInitEvent(rs)

        val runtimeEvent = new EventCompositeLoop(
          new EventCompositeLoop(
            new EventNetLookup()
          ).withCountRef(rs.lens(p5nodes))
        ).withCountRef(rs.lens(p5loops))

        createTestBench(rs, initEvent, runtimeEvent).run()
    },
    Config(
    )
  ))

  val experiment3destab = commonExports(Experiment(
    "p2p-stage3-staticDestab",
    "P2P [stage3] Destabilize while running",
    Seq("p2p-stage2-paramSpace"),
    {
      rs =>
        val initEvent = commonInitEvent(rs)

        val runtimeEvent = new EventCompositeLoop(
          new EventCompositeSequence(
            Seq(
              new EventCompositeSequence(
                Seq(
                  new EventNodeJoin().withFailOnError(true),
                  new EventNodeFail()
                )
              ).withProbabilityRef(rs.lens(p5failProb)),
              new EventCompositeSequence(
                Seq(
                  new EventNodeJoin().withFailOnError(true),
                  new EventNodeLeave()
                )
              ).withProbabilityRef(rs.lens(p5joinProb)),
              new EventNetStabilize().withProbabilityRef(rs.lens(p5stabilizeProb)),
              new EventCompositeLoop(
                new EventNetLookup()
              ).withCountRef(rs.lens(p5nodes))
            )
          )
        ).withCountRef(rs.lens(p5loops))

        createTestBench(rs, initEvent, runtimeEvent).run()
    },
    Config(
    )
  ))

  val experiment3attackDestab = commonExports(Experiment(
    "p2p-stage3-attackDestab",
    "P2P [stage3] Attack scenario",
    Seq("p2p-stage2-paramSpace"),
    {
      rs =>
        val initEvent = new EventCompositeSequence(
          Seq(
            new EventNodeJoin().withCountRef(rs.lens(p5nodes)),
            new EventCompositeLoop(
              new EventNetPutEntry().withCountRef(rs.lens(p5Elems))
            ).withCountRef(rs.lens(p5nodes)),
            new EventNetStabilize(),
            new EventCompositeLoop(
              new EventNodeFail().withProbabilityRef(rs.lens(p5attackProb))
            ).withCountRef(rs.lens(p5nodes))
          )
        )

        val runtimeEvent = new EventCompositeLoop(
          new EventCompositeSequence(
            Seq(
              new EventCompositeSequence(
                Seq(
                  new EventNodeJoin().withFailOnError(true),
                  new EventNodeFail()
                )
              ).withProbabilityRef(rs.lens(p5failProb)),
              new EventCompositeSequence(
                Seq(
                  new EventNodeJoin().withFailOnError(true),
                  new EventNodeLeave()
                )
              ).withProbabilityRef(rs.lens(p5joinProb)),
              new EventNetStabilize().withProbabilityRef(rs.lens(p5stabilizeProb)),
              new EventCompositeLoop(
                new EventNetLookup().withRetries(3)
              ).withCountRef(rs.lens(p5nodes))
            )
          )
        ).withCountRef(rs.lens(p5loops))

        createTestBench(rs, initEvent, runtimeEvent).run()
    },
    Config(
    )
  ))

  val experiment3attackChained = chainWithGA(experiment3attack)
  val experiment3staticChained = chainWithGA(experiment3static)
  val experiment3attackDestabChained = chainWithGA(experiment3attackDestab)
  val experiment3destabChained = chainWithGA(experiment3destab)

  def chainWithGA(exp: Experiment): Experiment =
    exp.copy(
      name = exp.name + "-chained",
      desc = exp.desc + " (chained over GA)",
      depends = exp.depends :+ "overlayGO-3-genetics"
    )

  private def commonInitEvent(rs: RunStore): EventCompositeSequence = {
    new EventCompositeSequence(
      Seq(
        new EventNodeJoin().withCountRef(rs.lens(p5nodes)),
        new EventNetStabilize(),
        new EventNetDiscover(),
        new EventNetReflavor(),
        new EventCompositeLoop(
          new EventNetPutEntry().withCountRef(rs.lens(p5Elems))
        ).withCountRef(rs.lens(p5nodes)),
        new EventNetDumpStructure()
    )
    )
  }

  private def createTestBench(
    rs: RunStore,
    initEvent: Event[_],
    runtimeEvent: Event[_]
  ): Testbench = {
    val env = new EnvCNet()
    env.setDensity(rs.lens(p2density))
    env.setLocX(rs.lens(p2locX))
    env.setLocY(rs.lens(p2locY))
    env.setDist(rs.lens(p2nodeDist))
    env.setReq(rs.lens(p2req))
    env.setOverlay(rs.lens(p3genomeBest))

    val network = new Network()
    network.setEnv(env)
    network.setFactory(
      network.getFactory.routingRedundancy(
        rs.lens(p5routingRedundancy).get.get
      ).maxFingerFlavorNum(
        rs.lens(p5maxFingerFlavorNum).get.get
      )
    )

    val testBench = new Testbench()

    testBench.setNetwork(network)
    testBench.setReportLens(rs.lens(p6report))

    testBench.setInitSeedRef(rs.lens(p4initSeed))
    testBench.setInitialEvent(initEvent)

    testBench.setRunSeedRef(rs.lens(p4runSeed))
    testBench.setRuntimeEvent(runtimeEvent)

    testBench.setRangeSizes(rs.lens(p6rangeSizes))
    testBench.setRpcCounts(rs.lens(p6rpcCounts))
    testBench.setRpcFailures(rs.lens(p6rpcFailures))
    testBench.setLookupCounts(rs.lens(p6lookupCounts))
    testBench.setLookupFailures(rs.lens(p6lookupFailures))

    testBench
  }
}
