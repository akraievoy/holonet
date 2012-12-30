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

import org.akraievoy.cnet.gen.vo.EntropySourceRandom
import org.akraievoy.holonet.runner.dlaGenTest.LocationGeneratorImages
import org.akraievoy.cnet.gen.domain.LocationGeneratorRecursive
import org.akraievoy.base.runner.api.ExperimentTiming

import java.lang.{
  Byte => JByte, Integer => JInt, Long => JLong,
  Float => JFloat, Double => JDouble
}


trait RegistryData {
  lazy val experiments: Seq[Experiment] = Seq(
    Experiment(
      "dlaGen-1-images",
      "DLA density distribution [stage1] Images",
      Nil,
      {
        rs =>
          val entropySource = new EntropySourceRandom()
          entropySource.setSeed(
            rs.lens[JLong]("entropySource.seed").get.get
          )
          val locationGenerator = new LocationGeneratorRecursive(
            entropySource
          )
          locationGenerator.setDimensionRatio(
            rs.lens[JDouble]("locationGenerator.dimensionRatio").get.get
          )
          locationGenerator.setGridSize(
            rs.lens[JInt]("locationGenerator.gridSize").get.get
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
        Param[Long]("entropySource.seed", "13311331"),
        Param[Int]("locationGenerator.gridSize", "1024"),
        Param[Double]("locationGenerator.dimensionRatio", "1.5")
      ),
      Config(
        "dimensions",
        "Multiple dimensions",
        Param[Double](
          "locationGenerator.dimensionRatio",
          "1;1.25;1.5;1.75;2"
        )
      ),
      Config(
        "seeds",
        "Multiple seeds",
        Param[Long](
          "entropySource.seed",
          "13311331;31133113;53355335;35533553;51155115"
        )
      )
    )
  )

}
