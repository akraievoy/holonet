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

package org.akraievoy.cnet.metrics.domain;

import org.akraievoy.base.ref.RefRO;
import org.akraievoy.holonet.exp.store.RefObject;
import org.akraievoy.cnet.metrics.api.MetricScalar;
import org.akraievoy.cnet.net.vo.EdgeData;

public class MetricScalarEffectiveness extends MetricScalar {
  protected double minThresh = 1e-9;
  protected boolean includeReflexive = false;

  protected double pow = -1;

  protected RefRO<? extends EdgeData> weightSource = new RefObject<EdgeData>();
  protected RefRO<? extends EdgeData> source = new RefObject<EdgeData>();

  public String getName() {
    return "Effectiveness";
  }

  public MetricScalarEffectiveness configure(
      RefRO<? extends EdgeData> source0,
      RefRO<? extends EdgeData> weightSource0
  ) {
    setSource(source0);
    setWeightSource(weightSource0);
    return this;
  }

  public void setMinThresh(double minThresh) {
    this.minThresh = minThresh;
  }

  public void setIncludeReflexive(boolean includeReflexive) {
    this.includeReflexive = includeReflexive;
  }

  public void setPow(double pow) {
    this.pow = pow;
  }

  public void setWeightSource(RefRO<? extends EdgeData> weightSource) {
    this.weightSource = weightSource;
  }

  public void setSource(RefRO<? extends EdgeData> source) {
    this.source = source;
  }

  public void run() {
    final EdgeData net = source.getValue();
    final EdgeData weights = weightSource.getValue();

    if (weights != null && weights.getSize() != net.getSize()) {
      throw new IllegalArgumentException(
          "net.size(" + net.getSize() + ") != weights.size(" + weights.getSize() + ")"
      );
    }

    final int size = net.getSize();

    double effSum = 0;
    double weightSum = 0;
    for (int from = 0; from < size; from++) {
      for (int into = 0; into < size; into++) {
        if (!includeReflexive && from == into) {
          continue;
        }

        final double e = net.weight(from, into);
        if (e < minThresh) {
          continue;
        }

        final double w = weights == null ? 1 : weights.weight(from, into);

        effSum += w * Math.pow(e, pow);
        weightSum += w;
      }
    }

    final double result = weightSum == 0 ? 0 : effSum / weightSum;
    target.setValue(result);
  }
}