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
import store.{RefObject, RunStore}
import algores.holonet.core.{Network, EnvCNet}
import algores.holonet.testbench.Testbench
import algores.holonet.core.events._

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
    val p5routingRedundancy= ParamName[JDouble]("p5routingRedundancy")
    //  stage 3 outputs
    val p6report= ParamName[JDouble]("p6report")
  }

  import ParamNames._

  val experiment1seeds = Experiment(
    "p2p-stage1-seed",
    "P2P [stage0] Choose your seeds",
    Nil,
    { rs => },
    Config(
      Param(p4initSeed, "1234567"),
      Param(p4runSeed, "7654321")
    ),
    Config(
      "7x5",
      "7 inits * 5 runs",
      Param(p4initSeed, "91032843--91032849"),
      Param(p4runSeed, "9348492--9348496")
    ),
    Config(
      "42x7",
      "42 inits * 7 runs",
      Param(p4initSeed, "9348492--9348533"),
      Param(p4runSeed, "91032843--91032849")
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
      Param(p5attackProb, "0.01")
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
      "corrStudy-large-192",
      "correlation study, more loops for 192 nodes",
      Param(p5nodes, "192"),
      Param(p5attackProb, "0.48"),
      Param(p5Elems, "256"),
      Param(p5loops, "80"),
      Param(p5routingRedundancy, "1.05;1.1;1.15;1.2")
    ),
    Config(
      "attackProf",
      "attacks profiling",
      Param(p5attackProb, "0.0;0.01;0.02;0.04;0.08;0.16;0.32;0.64")
    )
  )

  import OverlayGO.ParamNames._

  val experiment3attack = Experiment(
    "p2p-stage3-attack",
    "P2P [stage3] Attack and profile lookups",
    Seq("p2p-stage2-paramSpace"),
    {
      rs =>
        val initEvent = commonInitEvent(rs)
        val runtimeEvent = new EventCompositeSequence(
          Seq(
            new EventCompositeLoop(
              new EventNodeAttack()
            ).withCountRef(
              new RefObject[JLong](
                math.ceil(rs.lens(p5nodes).get.get * rs.lens(p5attackProb).get.get).asInstanceOf[Long]
              )
            ),
            new EventCompositeLoop(
              new EventCompositeLoop(
                new EventNetLookup()
              ).withCountRef(rs.lens(p5nodes))
            ).withCountRef(rs.lens(p5loops))
          )
        )

        createTestBench(rs, initEvent, runtimeEvent).run()
    },
    Config(
    )
  )

  val experiment3static = Experiment(
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
  )

  val experiment3destab = Experiment(
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
  )

  val experiment3attackDestab = Experiment(
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
  )

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
        new EventCompositeLoop(
          new EventNetPutEntry().withCountRef(rs.lens(p5Elems))
        ).withCountRef(rs.lens(p5nodes)),
        new EventNetStabilize(),
        new EventNetDiscover()
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
    network.getFactory.setRoutingRedundancy(
      rs.lens(p5routingRedundancy).get.get
    )

    val testBench = new Testbench()
    testBench.setNetwork(network)
    testBench.setReportLens(rs.lens(p6report))

    testBench.setInitSeedRef(rs.lens(p4initSeed))
    testBench.setInitialEvent(initEvent)

    testBench.setRunSeedRef(rs.lens(p4runSeed))
    testBench.setRuntimeEvent(runtimeEvent)

    testBench
  }
}
