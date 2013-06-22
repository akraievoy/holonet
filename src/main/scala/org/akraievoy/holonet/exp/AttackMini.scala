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

package org.akraievoy.holonet.exp

import org.akraievoy.cnet.net.vo.{EdgeDataFactory, EdgeData}
import java.util.Random
import scala.math.{pow, sqrt}
import org.akraievoy.cnet.gen.vo.{EntropySourceRandom, WeightedEventModelBase}
import com.google.common.base.Optional
import org.akraievoy.cnet.net.Net.{toId, toFrom, toInto}

class AttackMini extends App {
  val locRng: Random = new Random(123456L)
  val linkRngSeed = 234567L

  val size: Int = 256
  val attackFraction: Double = .5
  val linksPerNode: Int = 6

  val range: Range = 0 until size
  val distExpRange = -2.0 to 2.0 by 0.125
  val powExpRange = -2.0 to 2.0 by 0.125

  val locX: Array[Double] = range.map(i => (i + .0) / size + locRng.nextDouble() / size).toArray
  val locY: Array[Double] = range.map(i => locRng.nextDouble()).toArray

  val dist: EdgeData = EdgeDataFactory.dense(true, 0, size)
  for (f <- range; t <- range if f < t) {
    dist.set(f, t, sqrt(pow(locX(f) - locX(t), 2) + pow(locY(f) - locY(t), 2)))
  }

  val links: EdgeData = EdgeDataFactory.sparse(true, 0, size)
  val linkModel = new WeightedEventModelBase(Optional.of("linkModel"))

  println("distExp;powExp;lambda;eff;conn")
  for (distExp <- distExpRange; powExp <- powExpRange) {
    val linkESource = new EntropySourceRandom().seed(linkRngSeed)
    links.clear()
    linkModel.clear()
    val powers: Array[Int] = range.map(i => 0).toArray

    do {
      for (f <- range; t <- range if f < t && powers(f) < linksPerNode && powers(t) < linksPerNode) {
        linkModel.add(
          toId(f, t),
          pow(powers(f) + powers(t), powExp) + pow(dist.get(f, t), distExp)
        )
      }

      if (linkModel.getSize > 0) {
        val linkId = linkModel.generate(linkESource, false, null)
        val linkF = toFrom(linkId)
        val linkT = toInto(linkId)
        links.set(linkF, linkT, 1)
        powers(linkF) += 1
        powers(linkT) += 1
      }
    } while (linkModel.getSize > 0)


  }
}
