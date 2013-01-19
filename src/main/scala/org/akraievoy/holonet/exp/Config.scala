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

case class Config(
  name: String,
  desc: String = "",
  params: Map[String, Param]
) extends Named {

  def spacePosAxis(
    chained: Boolean
  ): Map[Boolean, IndexedSeq[Param]] = {
    params.values.groupBy {
      param => param isParallel chained
    }.mapValues{
      paramIterable => paramIterable.toIndexedSeq
    }
  }

  def spacePosStreams(
    chained: Boolean,
    expIndex: Int
  ): Map[Boolean, Stream[Seq[ParamPos]]] = {
    val posSeqMap = spacePosAxis(chained).mapValues{
      paramSeq => paramSeq.map(_.toPosSeq(chained, expIndex))
    }

    posSeqMap.mapValues{
      posSeq => posSeq.foldLeft(Config.EMPTY_SPACE) {
      (seqSeq, seq) =>
        for (
          posSeq <- seqSeq.toStream;
          pos <- seq
        ) yield {
          posSeq :+ pos
        }
      }
    }.withDefaultValue(Config.EMPTY_SPACE)

  }

  def withDefault(dflt: Config) = {
    copy(
      params = dflt.params.foldLeft(params){
        case (params0, (paramName, param)) =>
          if (params0.contains(paramName)) {
            params0
          } else {
            params0.updated(paramName, param)
          }
      }
    )
  }
}

object Config {
  def apply(
    name: String,
    desc: String,
    params: Param*
  ) = {
    new Config(
      name,
      desc,
      params.zipWithIndex.map{
        case (p,idx) => p.copy(index = idx)
      }.groupBy{
        p => p.name
      }.mapValues(
        pSeq =>
          if (pSeq.length > 1) {
            throw new IllegalArgumentException(
              "config '%s' has duplicate param '%s'".format(name, pSeq.head.name)
            )
          } else {
            pSeq.head
          }
      )
    )
  }

  def apply(
    params: Param*
  ) = {
    new Config(
      "default",
      "Default",
      params.zipWithIndex.map{
        case (p, idx) => p.copy(index = idx)
      }.groupBy{
        p => p.name
      }.mapValues(
        pSeq =>
          if (pSeq.length > 1) {
            throw new IllegalArgumentException(
              "config '%s' has duplicate param '%s'".format(
                "default",
                pSeq.head.name
              )
            )
          } else {
            pSeq.head
          }
      )
    )
  }

  val EMPTY_SPACE = Seq(Seq.empty[ParamPos]).toStream
}