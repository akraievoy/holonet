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

package org.akraievoy.cnet.metrics.domain;

import com.google.common.base.Optional;
import org.akraievoy.base.ref.Ref;
import org.akraievoy.base.ref.RefRO;
import org.akraievoy.cnet.metrics.api.MetricStore;
import org.akraievoy.cnet.net.vo.Store;
import org.akraievoy.cnet.net.vo.StoreInt;
import org.akraievoy.cnet.net.vo.VertexData;
import org.akraievoy.holonet.exp.store.RefObject;

public class MetricStoreVData extends MetricStore {
  protected RefRO<VertexData> source = new RefObject<VertexData>();
  protected Optional<Ref<StoreInt>> optIndexesTarget = Optional.absent();

  public MetricStoreVData() {
  }

  @SuppressWarnings("unchecked")
  public MetricStoreVData(
      RefRO<VertexData> source,
      Ref<? extends Store> target,
      Store.Width width
  ) {
    this.source = source;
    this.target = (Ref<Store>) target;
    this.width = width;
  }

  @SuppressWarnings("unchecked")
  public MetricStoreVData(
      RefRO<VertexData> source,
      Ref<? extends Store> target,
      Store.Width width,
      Ref<StoreInt> optIndexesTarget
  ) {
    this.source = source;
    this.target = (Ref<Store>) target;
    this.width = width;
    this.optIndexesTarget = Optional.of(optIndexesTarget);
  }

  public String getName() {
    return "Vertex Data";
  }

  public void setSource(RefRO<VertexData> source) {
    this.source = source;
  }

  public void setIndexesTarget(Ref<StoreInt> optIndexesTarget) {
    this.optIndexesTarget = Optional.of(optIndexesTarget);
  }

  public void run() {
    final VertexData vData = source.getValue();
    final int nodes = vData.getSize();

    final Store targetValue = width.create();
    targetValue.ins(0, nodes, false);

    final StoreInt indexesTargetValue;
    if (optIndexesTarget.isPresent()) {
      indexesTargetValue = new StoreInt();
      indexesTargetValue.ins(0, nodes, false);
    } else {
      indexesTargetValue = null;
    }

    for (int i = 0; i < nodes; i++) {
      targetValue.set(i, vData.get(i));
      if (indexesTargetValue != null) {
        indexesTargetValue.set(i, i);
      }
    }

    target.setValue(targetValue);
    if (indexesTargetValue != null) {
      optIndexesTarget.get().setValue(indexesTargetValue);
    }
  }
}
