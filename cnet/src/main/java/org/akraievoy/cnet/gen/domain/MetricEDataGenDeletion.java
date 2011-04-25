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

import org.akraievoy.base.Die;
import org.akraievoy.base.ref.RefRO;
import org.akraievoy.cnet.gen.vo.EntropySource;
import org.akraievoy.cnet.metrics.api.MetricEData;
import org.akraievoy.cnet.net.ref.RefEdgeData;
import org.akraievoy.cnet.net.vo.EdgeData;

public class MetricEDataGenDeletion extends MetricEData {
  protected final EntropySource eSource;

  protected int deletedNodeNum = 24;

  protected RefRO<EdgeData> source = new RefEdgeData();

  public MetricEDataGenDeletion(final EntropySource eSource) {
    this.eSource = eSource;
  }

  public int getDeletedNodeNum() {
    return deletedNodeNum;
  }

  public void setDeletedNodeNum(int deletedNodeNum) {
    this.deletedNodeNum = deletedNodeNum;
  }

  public void setSource(RefRO<EdgeData> source) {
    this.source = source;
  }

  public String getName() {
    return "Deletion of " + deletedNodeNum + " nodes";
  }

  public void run() {
    final EdgeData eData = source.getValue();
    final int netNodeNum = eData.getSize();
    Die.ifFalse("netNodeNum > contractedNodeNum", netNodeNum > deletedNodeNum);

    for (int i = 0; i < deletedNodeNum; i++) {
      final int nodeNum = netNodeNum - i;
      final int removed = eSource.nextInt(nodeNum);

      eData.remove(removed);
    }

    target.setValue(eData);
  }
}