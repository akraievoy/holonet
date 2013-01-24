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

import store.RunStore

case class Experiment(
  name: String,
  desc: String = "",
  depends: Seq[String] = Nil,
  configs: Map[String, Config],
  executeFun: RunStore => Unit,
  graphvisExports: Map[String, GraphvizExport] = Map.empty,
  storeExports: Map[String, StoreExport] = Map.empty
) extends Named {
  def withGraphvizExport(ge: GraphvizExport) = {
    val newExports = graphvisExports.get(ge.name).map{
      prevExport =>
        throw new IllegalArgumentException(
          "graphviz export %s is already defined for experiment %s: %s".format(
            ge.name, name, prevExport
          )
        )
    }.getOrElse{
      graphvisExports.updated(ge.name, ge)
    }

    copy(graphvisExports = newExports)
  }

  def withStoreExport(se: StoreExport) = {
    val newExports = storeExports.get(se.name).map{
      prevExport =>
        throw new IllegalArgumentException(
          "store export %s is already defined for experiment %s: %s".format(
            se.name, name, prevExport
          )
        )
    }.getOrElse {
      storeExports.updated(se.name, se)
    }

    copy(storeExports = newExports)
  }
}

object Experiment {
  def apply(
    name: String,
    desc: String,
    depends: Seq[String],
    executeFun: RunStore => Unit,
    configs: Config*
  ) = {

    val configMap = configs.groupBy {
      c => c.name
    }.mapValues(
      cSeq =>
        if (cSeq.length > 1) {
          throw new IllegalArgumentException(
            "experiment '%s' has duplicate config '%s'".format(
              name,
              cSeq.head.name
            )
          )
        } else {
          cSeq.head
        }
    )

    val dfltConfMap = configMap.get("default").map {
      dflt =>
        configMap.mapValues {
          c =>
            if (c == dflt) {
              c
            } else {
              c.withDefault(dflt)
            }
        }
    }.getOrElse {
      configMap
    }

    new Experiment(name, desc, depends, dfltConfMap, executeFun)
  }
}