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

import org.akraievoy.base.ref.RefRO;
import org.akraievoy.cnet.metrics.api.MetricScalar;
import org.akraievoy.cnet.net.vo.EdgeData;
import org.akraievoy.holonet.exp.store.RefObject;

public class MetricScalarConnectedness extends MetricScalar {
  protected RefRO<? extends EdgeData> source = new RefObject<EdgeData>();
  protected boolean includeReflexive = false;

  public MetricScalarConnectedness() {
  }

  public String getName() {
    return "Eigenvalue Gap";
  }

  public MetricScalarConnectedness configure(
      RefRO<? extends EdgeData> source0,
      boolean includeReflexive0
  ) {
    setSource(source0);
    setIncludeReflexive(includeReflexive0);
    return this;
  }

  public void setIncludeReflexive(final boolean includeReflexive0) {
    includeReflexive = includeReflexive0;
  }

  public void setSource(RefRO<? extends EdgeData> source) {
    this.source = source;
  }

  public void run() {
    final EdgeData src = source.getValue();
    final int size = src.getSize();
    final int links = includeReflexive ? size * size : size * (size - 1);

    int finiteOrNegativeLinks = 0;
    for (int i = 0; i < size; i++) {
      for (int j = 0; j < size; j++) {
        if (i == j && !includeReflexive) {
          continue;
        }
        final double linkVal = src.get(i, j);
        if (Double.isInfinite(linkVal) && linkVal > 0) {
          continue;
        }

        finiteOrNegativeLinks++;
      }
    }

    target.setValue((finiteOrNegativeLinks + .0) / links);
  }
}