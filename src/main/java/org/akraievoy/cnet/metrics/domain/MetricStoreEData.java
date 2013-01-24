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
import org.akraievoy.cnet.net.vo.EdgeData;
import org.akraievoy.cnet.net.vo.Store;
import org.akraievoy.cnet.net.vo.StoreInt;
import org.akraievoy.holonet.exp.store.RefObject;

public class MetricStoreEData extends MetricStore {
  protected Optional<RefRO<? extends EdgeData>> optStructSource = Optional.absent();
  protected RefRO<? extends EdgeData> valueSource = new RefObject<EdgeData>();

  protected Optional<Ref<StoreInt>> optFromIndexesTarget = Optional.absent();
  protected Optional<Ref<StoreInt>> optIntoIndexesTarget = Optional.absent();

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
          }
        }
    );

    target.setValue(targetValue);
    if (optFromIndexesTarget.isPresent() && optIntoIndexesTarget.isPresent()) {
      optFromIndexesTarget.get().setValue(fromIndexesTarget);
      optIntoIndexesTarget.get().setValue(intoIndexesTarget);
    }
  }
}
