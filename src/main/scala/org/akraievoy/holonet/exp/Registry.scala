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

object Registry {
  lazy val experiments: Seq[Experiment] = Seq(
    Experiment(
      "dlaGen-1-images",
      "DLA density distribution [stage1] Images",
      Nil,
      Config(
        Param("entropySource.seed", "13311331"),
        Param("locationGenerator.gridSize", "1024"),
        Param("locationGenerator.dimensionRatio", "1.5")
      ),
      Config(
        "dimensions",
        "Multiple dimensions",
        Param(
          "locationGenerator.dimensionRatio",
          "1;1.25;1.5;1.75;2"
        )
      ),
      Config(
        "seeds",
        "Multiple seeds",
        Param(
          "entropySource.seed",
          "13311331;31133113;53355335;35533553;51155115"
        )
      )
    )
  )

}
