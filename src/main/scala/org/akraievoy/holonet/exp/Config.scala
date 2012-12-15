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
  def withDefault(dflt: Config) = {
    copy(params = params.withDefault(dflt.params))
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
      params.groupBy {
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
      params.groupBy {
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
}