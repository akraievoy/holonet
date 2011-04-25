/*
 Copyright 2011 Anton Kraievoy akraievoy@gmail.com
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

package org.akraievoy.cnet.gen.domain;

import org.akraievoy.base.ref.Ref;
import org.akraievoy.cnet.metrics.api.MetricVData;
import org.akraievoy.cnet.net.vo.VertexData;

import java.util.ArrayList;
import java.util.List;

//	TODO this is an update, not a metric
public class MetricVDataGenSizeUpdate extends MetricVData {
  protected int netNodeNum = 24;

  protected List<Ref<VertexData>> vertexDatas = new ArrayList<Ref<VertexData>>();

  public MetricVDataGenSizeUpdate() {
  }

  public int getSize() {
    return netNodeNum;
  }

  public void setSize(int netNodeNum) {
    this.netNodeNum = netNodeNum;
  }

  public void setVertexDatas(List<Ref<VertexData>> vertexDatas) {
    this.vertexDatas = vertexDatas;
  }

  public String getName() {
    return "Size update to " + netNodeNum;
  }

  public void run() {
    for (Ref<VertexData> vRef : vertexDatas) {
      final VertexData vData = vRef.getValue();

      while (vData.getSize() < netNodeNum) {
        vData.insert(vData.getSize());
      }

      vRef.setValue(vData);
    }
  }
}