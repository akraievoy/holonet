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
    val tbInitSeed = ParamName[JLong]("tbInitSeed.value")
    val tbRunSeed = ParamName[JLong]("tbRunSeed.value")
    //  stage 2 inputs
    val tbNodes = ParamName[JLong]("tbNodes.value")
    val tbElems = ParamName[JLong]("tbElems.value")
    val tbLoops = ParamName[JLong]("tbLoops.value")
    val tbFailProb = ParamName[JDouble]("tbFailProb.value")
    val tbJoinProb = ParamName[JDouble]("tbJoinProb.value")
    val tbStabProb = ParamName[JDouble]("tbStabProb.value")
    val tbAttackProb = ParamName[JDouble]("tbAttackProb.value")
    //  stage 3 outputs
    val tbReport= ParamName[JDouble]("tb.report")
  }

  import ParamNames._

  val experiment1seeds = Experiment(
    "p2p-stage1-seed",
    "P2P [stage0] Choose your seeds",
    Nil,
    { rs => },
    Config(
      Param(tbInitSeed, "1234567"),
      Param(tbRunSeed, "7654321")
    ),
    Config(
      "7x3",
      "7 inits * 3 runs",
      Param(tbInitSeed, "91032843--91032849"),
      Param(tbRunSeed, "9348492--9348492")
    ),
    Config(
      "7x42",
      "7 inits * 42 runs",
      Param(tbInitSeed, "91032843--91032849"),
      Param(tbRunSeed, "9348492--9348533")
    ),
    Config(
      "3x2",
      "3 inits * 2 runs",
      Param(tbInitSeed, "7654321;91032843;8574829"),
      Param(tbRunSeed, "1234567;1928483")
    )
  )

  val experiment2paramSpace = Experiment(
    "p2p-stage2-paramSpace",
    "P2P [stage2] Choose param space",
    Seq("p2p-stage1-seed"),
    { rs => },
    Config(
      Param(tbNodes, "60"),
      Param(tbElems, "32"),
      Param(tbLoops, "64"),
      Param(tbFailProb, "0.003"),
      Param(tbJoinProb, "0.01"),
      Param(tbStabProb, "0.01"),
      Param(tbAttackProb, "0.01")
    ),
    Config(
      "extraLoops",
      "extra loops",
      Param(tbLoops, "256")
    ),
    Config(
      "def24",
      "default - 24 nodes",
      Param(tbNodes, "24")
    ),
    Config(
      "stabProf",
      "stabilize profiling",
      Param(tbStabProb, "0.0;0.01;0.02;0.04;0.08;0.16;0.32;0.64")
    ),
    Config(
      "nodesProf",
      "nodes profiling",
      Param(tbNodes, "002;004;008;016;032;064;128;256;512")
    ),
    Config(
      "elemsProf",
      "elements profiling",
      Param(tbElems, "002;004;008;016;032;064;128;256")
    ),
    Config(
      "loopProf",
      "loop profiling",
      Param(tbLoops, "8;16;32;64;128")
    ),
    Config(
      "joinProf",
      "joins profiling",
      Param(tbJoinProb, "0.0;0.01;0.02;0.04;0.08;0.16;0.32;0.64")
    ),
    Config(
      "failProf-large-192",
      "failure profiling, more loops and data for 192 nodes",
      Param(tbNodes, "192"),
      Param(tbFailProb, "0.0;0.01;0.02;0.04;0.08;0.16;0.32;0.64"),
      Param(tbElems, "256"),
      Param(tbLoops, "256")
    ),
    Config(
      "corrStudy-large-192",
      "correlation study, more loops for 192 nodes",
      Param(tbNodes, "192"),
      Param(tbFailProb, "0.64"),
      Param(tbElems, "256"),
      Param(tbLoops, "80")
    ),
    Config(
      "attackProf",
      "attacks profiling",
      Param(tbAttackProb, "0.0;0.01;0.02;0.04;0.08;0.16;0.32;0.64")
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
              new EventNodeFail()
            ).withCountRef(
              new RefObject[JLong](
                math.ceil(rs.lens(tbNodes).get.get * rs.lens(tbFailProb).get.get).asInstanceOf[Long]
              )
            ),
            new EventNetStabilize(),
            new EventCompositeLoop(
              new EventCompositeLoop(
                new EventNetLookup()
              ).withCountRef(rs.lens(tbNodes))
            ).withCountRef(rs.lens(tbLoops))
          )
        )

        createTestBench(rs, initEvent, runtimeEvent).run()
    },
    Config()
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
          ).withCountRef(rs.lens(tbNodes))
        ).withCountRef(rs.lens(tbLoops))

        createTestBench(rs, initEvent, runtimeEvent).run()
    },
    Config()
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
              ).withProbabilityRef(rs.lens(tbFailProb)),
              new EventCompositeSequence(
                Seq(
                  new EventNodeJoin().withFailOnError(true),
                  new EventNodeLeave()
                )
              ).withProbabilityRef(rs.lens(tbJoinProb)),
              new EventNetStabilize().withProbabilityRef(rs.lens(tbStabProb)),
              new EventCompositeLoop(
                new EventNetLookup()
              ).withCountRef(rs.lens(tbNodes))
            )
          )
        ).withCountRef(rs.lens(tbLoops))

        createTestBench(rs, initEvent, runtimeEvent).run()
    },
    Config()
  )

  val experiment3attackDestab = Experiment(
    "p2p-stage3-attackDestab",
    "P2P [stage3] Attack scenario",
    Seq("p2p-stage2-paramSpace"),
    {
      rs =>
        val initEvent = new EventCompositeSequence(
          Seq(
            new EventNodeJoin().withCountRef(rs.lens(tbNodes)),
            new EventCompositeLoop(
              new EventNetPutEntry().withCountRef(rs.lens(tbElems))
            ).withCountRef(rs.lens(tbNodes)),
            new EventNetStabilize(),
            new EventCompositeLoop(
              new EventNodeFail().withProbabilityRef(rs.lens(tbAttackProb))
            ).withCountRef(rs.lens(tbNodes))
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
              ).withProbabilityRef(rs.lens(tbFailProb)),
              new EventCompositeSequence(
                Seq(
                  new EventNodeJoin().withFailOnError(true),
                  new EventNodeLeave()
                )
              ).withProbabilityRef(rs.lens(tbJoinProb)),
              new EventNetStabilize().withProbabilityRef(rs.lens(tbStabProb)),
              new EventCompositeLoop(
                new EventNetLookup().withRetries(3)
              ).withCountRef(rs.lens(tbNodes))
            )
          )
        ).withCountRef(rs.lens(tbLoops))

        createTestBench(rs, initEvent, runtimeEvent).run()
    },
    Config()
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
        new EventNodeJoin().withCountRef(rs.lens(tbNodes)),
        new EventCompositeLoop(
          new EventNetPutEntry().withCountRef(rs.lens(tbElems))
        ).withCountRef(rs.lens(tbNodes)),
        new EventNetStabilize()
      )
    )
  }

  private def createTestBench(
    rs: RunStore,
    initEvent: Event[_],
    runtimeEvent: Event[_]
  ): Testbench = {
    val env = new EnvCNet()
    env.setDensity(rs.lens(overlayDensity))
    env.setLocX(rs.lens(overlayLocationX))
    env.setLocY(rs.lens(overlayLocationY))
    env.setDist(rs.lens(overlayDistance))
    env.setReq(rs.lens(overlayRequest))
    env.setOverlay(rs.lens(gaGenomeBest))

    val network = new Network()
    network.setEnv(env)

    val testBench = new Testbench()
    testBench.setNetwork(network)
    testBench.setReportLens(rs.lens(tbReport))

    testBench.setInitSeedRef(rs.lens(tbInitSeed))
    testBench.setInitialEvent(initEvent)

    testBench.setRunSeedRef(rs.lens(tbRunSeed))
    testBench.setRuntimeEvent(runtimeEvent)

    testBench
  }
}
