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
import org.akraievoy.cnet.net.vo.*;
import org.akraievoy.holonet.exp.store.RefObject;

import java.util.HashMap;
import java.util.Map;

public class MetricStoreEData extends MetricStore {
  protected Optional<RefRO<? extends EdgeData>> optStructSource = Optional.absent();
  protected RefRO<? extends EdgeData> valueSource = new RefObject<EdgeData>();

  protected Optional<Ref<StoreInt>> optFromIndexesTarget = Optional.absent();
  protected Optional<Ref<StoreInt>> optIntoIndexesTarget = Optional.absent();
  protected final Map<RefRO<VertexData>,Ref<StoreDouble>> fromTargets =
      new HashMap<RefRO<VertexData>,Ref<StoreDouble>>();
  protected final Map<RefRO<VertexData>,Ref<StoreDouble>> intoTargets =
      new HashMap<RefRO<VertexData>,Ref<StoreDouble>>();

  public MetricStoreEData() {
  }

  @SuppressWarnings("unchecked")
  public MetricStoreEData(
      RefRO<? extends EdgeData> valueSource,
      Ref<? extends Store> target,
      Store.Width width
  ) {
    this.valueSource = valueSource;
    this.target = (Ref<Store>) target;
    this.width = width;
  }

  @SuppressWarnings("unchecked")
  public MetricStoreEData(
      RefRO<? extends EdgeData> structSource,
      RefRO<? extends EdgeData> valueSource,
      Ref<? extends Store> target,
      Store.Width width
  ) {
    this.valueSource = valueSource;
    //  if you still think Java does not suck at covariant generic typing:
    //    just read the line below, think again, try to find any way to
    //    avoid explicit call typing via type casts of any kind,
    //    which is not uglier than original, and then think yet again
    this.optStructSource =
        Optional.<RefRO<? extends EdgeData>>of(structSource);
    this.target = (Ref<Store>) target;
    this.width = width;
  }

  @SuppressWarnings("unchecked")
  public MetricStoreEData(
      RefRO<? extends EdgeData> structSource,
      RefRO<? extends EdgeData> valueSource,
      Ref<StoreInt> fromIndexesTarget,
      Ref<StoreInt> intoIndexesTarget,
      Ref<? extends Store> target,
      Store.Width width
  ) {
    this.valueSource = valueSource;
    this.optStructSource =
        Optional.<RefRO<? extends EdgeData>>of(structSource);
    this.target = (Ref<Store>) target;
    this.width = width;
    this.optFromIndexesTarget = Optional.of(fromIndexesTarget);
    this.optIntoIndexesTarget = Optional.of(intoIndexesTarget);
  }

  public MetricStoreEData withFromData(
      final RefRO<VertexData> fromSource,
      final Ref<StoreDouble> fromTarget
  ) {
    fromTargets.put(fromSource, fromTarget);
    return this;
  }

  public MetricStoreEData withIntoData(
      final RefRO<VertexData> intoSource,
      final Ref<StoreDouble> intoTarget
  ) {
    intoTargets.put(intoSource, intoTarget);
    return this;
  }

  public String getName() {
    return "Edge Data";
  }

  @SuppressWarnings("unchecked")
  public void setStructSource(RefRO<? extends EdgeData> structSource) {
    this.optStructSource =
        Optional.<RefRO<? extends EdgeData>>of(structSource);
  }

  public void setValueSource(RefRO<? extends EdgeData> valueSource) {
    this.valueSource = valueSource;
  }

  public void setFromIndexesTarget(Ref<StoreInt> optFromIndexesTarget) {
    this.optFromIndexesTarget = Optional.of(optFromIndexesTarget);
  }

  public void setIntoIndexesTarget(Ref<StoreInt> optIntoIndexesTarget) {
    this.optIntoIndexesTarget = Optional.of(optIntoIndexesTarget);
  }

  public void run() {
    final EdgeData structData;
    if (optStructSource.isPresent()) {
      structData = optStructSource.get().getValue();
    } else {
      structData = null;
    }
    final EdgeData valueData = valueSource.getValue();
    final StoreInt fromIndexesTarget;
    final StoreInt intoIndexesTarget;
    if (optFromIndexesTarget.isPresent() && optIntoIndexesTarget.isPresent()) {
      fromIndexesTarget = new StoreInt();
      intoIndexesTarget = new StoreInt();
    } else {
      fromIndexesTarget = null;
      intoIndexesTarget = null;
    }

    final Map<RefRO<VertexData>,StoreDouble> fromStores =
      new HashMap<RefRO<VertexData>,StoreDouble>();
    for (RefRO<VertexData> refRO : fromTargets.keySet()) {
      fromStores.put(refRO, new StoreDouble());
    }
    final Map<RefRO<VertexData>,StoreDouble> intoStores =
      new HashMap<RefRO<VertexData>,StoreDouble>();
    for (RefRO<VertexData> refRO : intoTargets.keySet()) {
      intoStores.put(refRO, new StoreDouble());
    }

    final Store targetValue = width.create();
    final EdgeData iteratee = structData != null ? structData : valueData;
    iteratee.visitNonDef(
        new EdgeData.EdgeVisitor() {
          public void visit(int from, int into, double e) {
            final double newV;
            if (iteratee == valueData) {
              newV = e;
            } else {
              newV = valueData.get(from, into);
            }

            targetValue.ins(
                targetValue.size(),
                targetValue.size() + 1,
                newV
            );
            if (fromIndexesTarget != null && intoIndexesTarget != null) {
              fromIndexesTarget.ins(
                  fromIndexesTarget.size(), fromIndexesTarget.size() + 1, from
              );
              intoIndexesTarget.ins(
                  intoIndexesTarget.size(), intoIndexesTarget.size() + 1, into
              );
            }

            for (RefRO<VertexData> refRO : fromTargets.keySet()) {
              final double fromVal = refRO.getValue().get(from);
              final StoreDouble fromStore = fromStores.get(refRO);
              fromStore.ins(
                  fromStore.size(), fromStore.size() + 1, fromVal
              );
            }

            for (RefRO<VertexData> refRO : intoTargets.keySet()) {
              final double intoVal = refRO.getValue().get(into);
              final StoreDouble intoStore = intoStores.get(refRO);
              intoStore.ins(
                  intoStore.size(), intoStore.size() + 1, intoVal
              );
            }
          }
        }
    );

    target.setValue(targetValue);
    if (optFromIndexesTarget.isPresent() && optIntoIndexesTarget.isPresent()) {
      optFromIndexesTarget.get().setValue(fromIndexesTarget);
      optIntoIndexesTarget.get().setValue(intoIndexesTarget);
    }
    for (RefRO<VertexData> refRO : fromTargets.keySet()) {
      fromTargets.get(refRO).setValue(fromStores.get(refRO));
    }
    for (RefRO<VertexData> refRO : intoTargets.keySet()) {
      intoTargets.get(refRO).setValue(intoStores.get(refRO));
    }
  }
}
