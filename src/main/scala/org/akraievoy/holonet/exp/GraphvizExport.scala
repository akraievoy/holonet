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

import store.RunStore
import org.akraievoy.cnet.net.vo.{EdgeData, VertexData}
import org.akraievoy.base.ref.RefRO

case class GraphvizExport(
  name: String,
  desc: String = "",

  edgeStructure: RunStore => RefRO[_ <: EdgeData],
  edgeLabel: RunStore => Option[RefRO[_ <: EdgeData]] = {rs => None},
  edgeWidth: RunStore => Option[RefRO[_ <: EdgeData]] = {rs => None},
  edgeColor: RunStore => Option[RefRO[_ <: EdgeData]] = {rs => None},
  edgeColorScheme: GraphvizExport.ColorScheme.Value =
    GraphvizExport.ColorScheme.BLUE_RED,

  vertexLabel: RunStore => Option[RefRO[VertexData]] = {rs => None},
  vertexCoordX: RunStore => Option[RefRO[VertexData]] = {rs => None},
  vertexCoordY: RunStore => Option[RefRO[VertexData]] = {rs => None},
  vertexRadius: RunStore => Option[RefRO[VertexData]] = {rs => None},
  vertexColor: RunStore => Option[RefRO[VertexData]] = {rs => None},
  vertexColorScheme: GraphvizExport.ColorScheme.Value =
    GraphvizExport.ColorScheme.VIOLET_RED
) extends Named

object GraphvizExport {
  object ColorScheme extends Enumeration {
    val GREEN_VIOLET = Value("prgn11")
    val VIOLET_RED = Value("rdylbu11")
    val GRAY_RED = Value("rdgy11")
    val BLUE_RED = Value("rdbu11")
  }
}