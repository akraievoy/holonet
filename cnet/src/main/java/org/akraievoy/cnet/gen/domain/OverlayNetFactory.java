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

import com.google.common.base.Optional;
import org.akraievoy.base.Die;
import org.akraievoy.base.ref.Ref;
import org.akraievoy.base.ref.RefRO;
import org.akraievoy.base.runner.api.Context;
import org.akraievoy.cnet.gen.vo.EntropySource;
import org.akraievoy.cnet.gen.vo.WeightedEventModel;
import org.akraievoy.cnet.gen.vo.WeightedEventModelBase;
import org.akraievoy.cnet.metrics.api.MetricVData;
import org.akraievoy.cnet.net.ref.RefEdgeData;
import org.akraievoy.cnet.net.ref.RefVertexData;
import org.akraievoy.cnet.net.vo.EdgeData;
import org.akraievoy.cnet.net.vo.RefKeys;
import org.akraievoy.cnet.net.vo.VertexData;

import java.util.HashMap;
import java.util.Map;

public class OverlayNetFactory extends MetricVData {
  protected final EntropySource eSource;
  protected Context ctx;

  protected RefRO<EdgeData> source = RefEdgeData.forPath(RefKeys.LAYER_STRUCTURE);
  protected Map<RefRO<EdgeData>, Ref<EdgeData>> edgeDataMap = new HashMap<RefRO<EdgeData>, Ref<EdgeData>>();
  protected Map<RefRO<VertexData>, Ref<VertexData>> vertexDataMap = new HashMap<RefRO<VertexData>, Ref<VertexData>>();

  protected double omega = 0.5;
  protected double nu = 0.2;

  public OverlayNetFactory(EntropySource eSource) {
    this.eSource = eSource;
    if (target instanceof RefVertexData) {
      ((RefVertexData) target).setPath(RefKeys.LAYER_INDEX);
    }
  }

  @Override
  public String getName() {
    return "Overlay Node Selection";
  }

  public void setEdgeDataMap(Map<RefRO<EdgeData>, Ref<EdgeData>> edgeDataMap) {
    this.edgeDataMap = edgeDataMap;
  }

  public void setSource(RefRO<EdgeData> source) {
    this.source = source;
  }

  public void setVertexDataMap(Map<RefRO<VertexData>, Ref<VertexData>> vertexDataMap) {
    this.vertexDataMap = vertexDataMap;
  }

  /**
   * @param nu fraction of physical nodes elected to host virtual nodes
   */
  public void setNu(double nu) {
    Die.ifFalse("nu > 0", nu > 0);
    Die.ifFalse("nu <= 1", nu <= 1);
    this.nu = nu;
  }

  /**
   * @param omega degree-based preference (as power of degree) to elect physical node as host to a virtual
   */
  public void setOmega(double omega) {
    this.omega = omega;
  }

  public void run() {
    final EdgeData structure = source.getValue();
    final int nodes = structure.getSize();

    final WeightedEventModel eventModel = new WeightedEventModelBase(Optional.of("overlay"));
    for (int i = 0; i < nodes; i++) {
      eventModel.add(i, Math.pow(structure.power(i), omega));
    }

    final int vNodeNum = (int) Math.ceil(nodes * nu);
    final VertexData indexes = new VertexData(-1, vNodeNum);
    for (int i = 0; i < vNodeNum; i++) {
      final Integer physIndex = eventModel.generate(eSource, true, null);

      indexes.set(i, physIndex);
    }

    target.setValue(indexes);

    for (RefRO<VertexData> sourceSource : vertexDataMap.keySet()) {
      final VertexData vData = sourceSource.getValue();
      final VertexData vNodeVData = vData.proto(vNodeNum);

      for (int i = 0; i < vNodeNum; i++) {
        final double physIndex = indexes.get(i);

        //noinspection unchecked
        vNodeVData.set(i, vData.get((int) physIndex));
      }

      final Ref<VertexData> targetRef = vertexDataMap.get(sourceSource);
      targetRef.setValue(vNodeVData);
    }

    for (RefRO<EdgeData> sourceRef : edgeDataMap.keySet()) {
      final EdgeData eData = sourceRef.getValue();
      final EdgeData vNodeEData = eData.proto(vNodeNum);

      for (int i = 0; i < vNodeNum; i++) {
        final double physI = indexes.get(i);

        for (int j = 0; j < vNodeNum; j++) {
          final double physJ = indexes.get(j);

          //noinspection unchecked
          vNodeEData.set(i, j, eData.get((int) physI, (int) physJ));
        }
      }

      final Ref<EdgeData> targetRef = edgeDataMap.get(sourceRef);
      targetRef.setValue(vNodeEData);
    }
  }

  public void setCtx(Context ctx) {
    this.ctx = ctx;
  }
}