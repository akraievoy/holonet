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
import org.akraievoy.cnet.gen.vo.EntropySourceRandom
import org.akraievoy.cnet.soo.domain.EnumExperiment
import org.akraievoy.base.runner.vo.Parameter.Strategy

object OverlayEnum {
  import java.lang.{
    Byte => JByte, Integer => JInt, Long => JLong,
    Float => JFloat, Double => JDouble
  }

  object ParamNames {
    val entropySourceSeed = ParamName[JLong]("entropySource.seed")
    val size = ParamName[JLong]("size.value")
    val theta = ParamName[JDouble]("theta.value")
    val thetaTilde = ParamName[JDouble]("thetaTilde.value")
    val lambda = ParamName[JDouble]("lambda.value")
  }

  import ParamNames._

  val experiment = Experiment(
    "overlayEnum",
    "Overlay GO [Stage 1] Enumerate feasible solutions",
    Nil,
    {
      rs =>
        val entropySource = new EntropySourceRandom()
        entropySource.setSeed(
          rs.lens(entropySourceSeed).getValue
        )

        val enumExp = new EnumExperiment

        enumExp.setEvalSource(entropySource)
        enumExp.setSizeRef(rs.lens(size))
        enumExp.setThetaRef(rs.lens(theta))
        enumExp.setThetaTildeRef(rs.lens(thetaTilde))
        enumExp.setLambdaRef(rs.lens(lambda))

        enumExp.run()
    },
    Config(
      Param(entropySourceSeed, "308692"),
      Param(size, "7;8"),
      Param(theta, "1"),
      Param(thetaTilde, "0.75"),
      Param(
        lambda,
        "0.1;0.2;0.3;0.4;0.45;0.5;0.55;0.6;0.65;0.7;" +
            "0.725;0.75;0.775;0.8;0.825;0.85;0.875;0.9;" +
            "0.91;0.92;0.93;0.94;0.95;" +
            "0.955;0.96;0.965;0.97;0.975;0.98;0.985;0.99;0.995;",
        Strategy.USE_FIRST
      )
    ),
    Config(
      "upto12",
      "Up to 12 nodes",
      Param(size, "7;8;9;10;11;12")
    ),
    Config(
      "upto16",
      "Up to 16 nodes",
      Param(size, "7;8;9;10;11;12;13;14;15;16")
    )
  )
}
