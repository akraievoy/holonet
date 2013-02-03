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
import org.akraievoy.cnet.gen.domain.LocationGeneratorRecursive

object DlaGenImages {
  import java.lang.{
    Byte => JByte, Integer => JInt, Long => JLong,
    Float => JFloat, Double => JDouble
  }

  object ParamNames {
    val entropySourceSeed = ParamName[JLong]("entropySource.seed")
    val locationGenDimension = ParamName[JDouble]("locationGenerator.dimensionRatio")
    val locationGenGridSize= ParamName[JInt]("locationGenerator.gridSize")
  }

  import ParamNames._

  val experiment = Experiment(
    "dlaGenImages",
    "DLA density distribution [stage1] Images",
    Nil, {
      rs =>
        val entropySource = new EntropySourceRandom()
        entropySource.setSeed(
          rs.lens(entropySourceSeed).get.get
        )
        val locationGenerator = new LocationGeneratorRecursive(
          entropySource
        )
        locationGenerator.setDimensionRatio(
          rs.lens(locationGenDimension).get.get
        )
        locationGenerator.setGridSize(
          rs.lens(locationGenGridSize).get.get
        )
        val images = new LocationGeneratorImages(
          locationGenerator
        )

        val genSubExp = new ExperimentTiming(locationGenerator)
        genSubExp.setDurationRef(rs.lens[JLong]("location_time_millis"))
        genSubExp.setDurationTextRef(rs.lens[String]("location_time_text"))

        val imgSubExp = new ExperimentTiming(images)
        imgSubExp.setDurationRef(rs.lens[JLong]("images_time_millis"))
        imgSubExp.setDurationTextRef(rs.lens[String]("images_time_text"))

        genSubExp.run()
        imgSubExp.run()
    },
    Config(
      Param(entropySourceSeed, "13311331"),
      Param(locationGenGridSize, "1024"),
      Param(locationGenDimension, "1.5")
    ),
    Config(
      "dimensions",
      "Multiple dimensions",
      Param(
        locationGenDimension,
        "1;1.05;1.1;1.15;1.2;1.25;1.3;1.35;1.4;1.45;1.5;"+
          "1.55;1.6;1.65;1.7;1.75;1.8;1.85;1.9;1.95;2"
      )
    ),
    Config(
      "seeds",
      "Multiple seeds",
      Param(
        entropySourceSeed,
        "13311331;31133113;53355335;35533553;51155115"
      )
    )
  )
}
