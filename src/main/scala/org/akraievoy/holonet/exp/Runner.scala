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

import org.apache.log4j.{PatternLayout, ConsoleAppender, BasicConfigurator}

object Runner extends App {
  //  see http://logging.apache.org/log4j/1.2//apidocs/org/apache/log4j/PatternLayout.html
  BasicConfigurator.configure(
    new ConsoleAppender(
      new PatternLayout("%5.5p %-8.8t %-24.24c{1} %m%n")
    )
  )

  args match {
    case Array("curr-smoke") =>
      Registry.execute(
        "p2p-stage3-attack-chained",
        Map(
          "overlayGO-1-physDataset" -> "big-1k",
          "overlayGO-2-ovlDataset" -> "nu20",
          "overlayGO-3-genetics" -> "corrStudy-smoke",
          "p2p-stage1-seed" -> "42x3",
          "p2p-stage2-paramSpace" -> "corrStudy-large-192",
          "p2p-stage3-attack-chained" -> "default"
        )
      )
    case Array("curr-full") =>
      Registry.execute(
        "p2p-stage3-attack-chained",
        Map(
          "overlayGO-1-physDataset" -> "big-1k",
          "overlayGO-2-ovlDataset" -> "nu20",
          "overlayGO-3-genetics" -> "corrStudy-full",
          "p2p-stage1-seed" -> "42x3",
          "p2p-stage2-paramSpace" -> "corrStudy-large-192",
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
