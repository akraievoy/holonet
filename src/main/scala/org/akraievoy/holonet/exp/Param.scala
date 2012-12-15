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

import org.akraievoy.base.runner.vo.Parameter

case class Param(
  name: String,
  valueSpec: Seq[String],
  strategy: Parameter.Strategy,
  chainStrategy: Parameter.Strategy,
  desc: String
) extends Named

object Param{
  def apply(
    name: String,
    singleValueSpec: String,
    strategy: Parameter.Strategy = Parameter.Strategy.SPAWN,
    chainStrategy: Parameter.Strategy = Parameter.Strategy.SPAWN,
    desc: String = ""
  ) = {
    if (singleValueSpec.contains(';')) {
      new Param(name, singleValueSpec.split(";"), strategy, chainStrategy, desc)
    } else {
      new Param(name, Seq(singleValueSpec), strategy, chainStrategy, desc)
    }
  }
}