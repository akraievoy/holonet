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

object Runner extends App {
  args match {
    case Array("attack-mini") =>
      AttackMini.main(args)
    case Array("curr-smoke") =>
      Registry.execute(
        "p2p-stage3-attack-chained",
        Map(
          "overlayGO-1-physDataset" -> "phys-64",
          "overlayGO-2-ovlDataset" -> "nu25",
          "overlayGO-3-genetics" -> "corrStudy-smoke",
          "p2p-stage1-seed" -> "42x3",
          "p2p-stage2-paramSpace" -> "corrStudy-large-16",
          "p2p-stage3-attack-chained" -> "default"
        )
      )
    case Array("curr-full") =>
      Registry.execute(
        "p2p-stage3-attack-chained",
        Map(
          "overlayGO-1-physDataset" -> "phys-1024",
          "overlayGO-2-ovlDataset" -> "nu25",
          "overlayGO-3-genetics" -> "corrStudy-full",
          "p2p-stage1-seed" -> "42x3",
          "p2p-stage2-paramSpace" -> "corrStudy-large-256",
          "p2p-stage3-attack-chained" -> "default"
        )
      )
    case Array("dla") =>
      Registry.execute(
        "dlaGenImages",
        Map("dlaGenImages" -> "dimensions")
      )
    case Array("ovlenum") =>
      Registry.execute(
        "overlayEnum",
        Map("overlayEnum" -> "default")
      )
    case other =>
      println(
        """Usage:
          |  sbt 'run batchName'
          |
          |Available experiment batchNames:
          | dla        - dla model with varying dimensions
          | ovlenum    - overlay enumeration
          | curr-smoke - current experiment; smoke testing version
          | curr-full  - current experiment; full version (takes time)
        """.stripMargin
      )
  }
}
