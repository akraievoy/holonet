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
import scala.math.{pow, sqrt, floor}
import org.akraievoy.cnet.gen.vo.{EntropySourceRandom, WeightedEventModelBase}
import com.google.common.base.Optional
import org.akraievoy.cnet.net.Net.{toId, toFrom, toInto, eClone}
import org.akraievoy.cnet.metrics.domain._
import org.akraievoy.cnet.metrics.api.Metric
import org.akraievoy.holonet.exp.store.RefObject
import java.util
import gnu.trove.TIntArrayList

object AttackMini extends App {
  val locRng: Random = new Random(123456L)
  val linkRngSeed = 345678L
  val attackRngSeed = 456789L

  val size: Int = 256
  val attackFraction: Double = .5
  val linksPerNode: Int = 6

  val distExpRange = -3.0 to 3.0 by 1
  val powExpRange = -3.0 to 3.0 by 1
  val linkStructNum = 8
  val attackNum = 8
  val attackSize = floor(size * attackFraction).toInt

  val range: Range = 0 until size

  val locX: Array[Double] = range.map(i => (i + .0) / size + locRng.nextDouble() / size).toArray
  val locY: Array[Double] = range.map(i => locRng.nextDouble()).toArray

  val dist: EdgeData = EdgeDataFactory.dense(true, 0, size)
  val distRef: RefObject[EdgeData] = new RefObject(dist)
  for (f <- range; t <- range if f < t) {
    dist.set(f, t, sqrt(pow(locX(f) - locX(t), 2) + pow(locY(f) - locY(t), 2)))
  }

  val links: EdgeData = EdgeDataFactory.sparse(true, 0, size)
  val linksRef: RefObject[EdgeData] = new RefObject(links)
  val linkModel = new WeightedEventModelBase(Optional.of("linkModel"))

  val lambdaMetric = new MetricScalarEigenGap()
  val routeLenMetric: MetricEDataRouteLen = new MetricEDataRouteLen(
    new MetricRoutesJohnson().configure(linksRef, distRef)
  )
  val effMetric = new MetricScalarEffectiveness()
  val connMetric = new MetricScalarConnectedness()

  println("distExp;powExp;lambdaInit;effInit;connInit;attFrac;lambdaAtt;effAtt;connAtt")
  for (distExp <- distExpRange; powExp <- powExpRange) {
    val linkESource = new EntropySourceRandom().seed(linkRngSeed)
    for (linkStruct <- 0 until linkStructNum) {
      initLinks(powExp, distExp, linkESource)

      val lambdaInit = Metric.fetch(lambdaMetric.configure(linksRef))
      val routeLen = Metric.fetch(routeLenMetric)
      val effInit = Metric.fetch(effMetric.configure(new RefObject(routeLen), new RefObject[EdgeData](null)))
      val connInit = Metric.fetch(connMetric.configure(new RefObject(routeLen), false))

      for (a <- 0 until attackNum) {
        performAttack(a, eClone(links)) {
          case (linksAttack, lambdaIndex, attFrac) =>
            val linksCompactRef = new RefObject(compact(linksAttack, lambdaIndex))
            val distCompactRef = new RefObject(compact(dist, lambdaIndex))
            val lambdaAttack = Metric.fetch(lambdaMetric.configure(linksCompactRef))
            val routeLenMetricAttack = new MetricEDataRouteLen(
              new MetricRoutesJohnson().configure(linksCompactRef, distCompactRef)
            )
            val routeLenAttack = Metric.fetch(routeLenMetricAttack)
            val effAttack = Metric.fetch(
              effMetric.configure(
                new RefObject(routeLenAttack),
                new RefObject[EdgeData](null)
              )
            )
            val connAttack = Metric.fetch(connMetric.configure(new RefObject(routeLenAttack), false))

            println(
              Seq(
                distExp,
                powExp,
                lambdaInit,
                effInit,
                connInit,
                attFrac,
                lambdaAttack,
                effAttack,
                connAttack
              ).mkString(";")
            )
        }
      }
    }
  }

  def compact(e: EdgeData, index: Seq[Int]): EdgeData = {
    val eCompact = EdgeDataFactory.sparse(true, 0, index.size)
    for (f <- 0 until index.size; t <- f + 1 until index.size if f < t) {
      eCompact.set(f, t, e.get(index(f), index(t)))
    }
    eCompact
  }

  def performAttack(attackNum: Int, linksAttack: EdgeData)(reportFun: (EdgeData, Seq[Int], Double) => Unit) {
    val attackModel = new WeightedEventModelBase(Optional.of("attackModel"))
    val attackESource = new EntropySourceRandom().seed(attackRngSeed * 31 + attackNum)
    var lambdaIndex: Seq[Int] = range.toSeq
    for (d <- 0 until attackSize) {
      var minPower = 1
      attackModel.clear()
      for (i <- range) {
        val powI = linksAttack.power(i)
        if (powI > minPower) {
          minPower = floor(powI).toInt
          attackModel.clear()
          attackModel.add(i, 1)
        } else if (powI == minPower) {
          attackModel.add(i, 1)
        }
      }
      val attackeeF = attackModel.generate(attackESource, false, null)
      for (attackeeT <- range if attackeeF != attackeeT) {
        linksAttack.set(attackeeF, attackeeT, 0)
      }
      lambdaIndex = lambdaIndex.filterNot(_ == attackeeF)
      if (d % 8 == 7 || d == attackSize - 1) {
        reportFun(linksAttack, lambdaIndex, (d + 1 + .0)/size)
      }
    }
  }

  def initLinks(powExp: Double, distExp: Double, linkESource: EntropySourceRandom) {
    links.clear()
    val powers: Array[Int] = range.map(i => 0).toArray
    val connCache0 = new TIntArrayList()
    do {
      linkModel.clear()
      for (
        f <- range if powers(f) < linksPerNode;
        connCache <- Some({ connCache0.clear(); links.connVertexes(f, connCache0) });
        t <- f + 1 until size if powers(t) < linksPerNode && connCache.binarySearch(t) < 0
      ) {
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
