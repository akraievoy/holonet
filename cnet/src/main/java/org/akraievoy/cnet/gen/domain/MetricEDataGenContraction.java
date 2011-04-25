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

public class MetricEDataGenContraction extends MetricEData {
  protected final EntropySource eSource;

  protected int contractedNodeNum = 24;
  protected boolean reflectiveFlag;

  protected RefRO<EdgeData> source = new RefEdgeData();

  public MetricEDataGenContraction(final EntropySource eSource) {
    this.eSource = eSource;
  }

  public int getContractedNodeNum() {
    return contractedNodeNum;
  }

  public void setContractedNodeNum(int contractedNodeNum) {
    this.contractedNodeNum = contractedNodeNum;
  }

  public void setReflectiveFlag(boolean reflectiveFlag) {
    this.reflectiveFlag = reflectiveFlag;
  }

  public void setSource(RefRO<EdgeData> source) {
    this.source = source;
  }

  public String getName() {
    return "Contraction to " + contractedNodeNum + " nodes";
  }

  public void run() {
    final EdgeData eData = source.getValue();
    final int netNodeNum = eData.getSize();

    Die.ifFalse("netNodeNum > contractedNodeNum", netNodeNum > contractedNodeNum);

    for (int i = 0; i < contractedNodeNum; i++) {
      final int nodeNum = netNodeNum - i;

      //	ced = contracted (removed), cee = contractee (hosting the ced's links)
      final int ced = eSource.nextInt(nodeNum);
      final int ceeRand = eSource.nextInt(nodeNum - 1);
      final int cee = ceeRand >= ced ? ceeRand + 1 : ceeRand;

      for (int j = 0; j < nodeNum; j++) {
        final int newJ;
        if (j == ced) {
          if (!reflectiveFlag) {
            continue;
          }
          newJ = cee;
        } else {
          newJ = j;
        }

        if (eData.conn(ced, j)) {
          eData.set(cee, newJ, eData.get(cee, newJ) + eData.get(ced, j));
        }

        if (!eData.isSymmetric() && j != ced && eData.conn(j, ced)) {
          eData.set(newJ, cee, eData.get(newJ, cee) + eData.get(j, ced));
        }
      }

      eData.remove(ced);
    }

    target.setValue(eData);
  }
}