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

package org.akraievoy.cnet.metrics.vo;

import gnu.trove.TDoubleArrayList;

import java.util.Arrays;

public class StatImpl implements Stat {
  protected final TDoubleArrayList data;

  public StatImpl() {
    this(10);
  }

  public StatImpl(final int capacity) {
    data = new TDoubleArrayList(capacity);
  }

  public double[] getData() {
    return data.toNativeArray();
  }

  @SuppressWarnings({"UnusedDeclaration"})
  public void setData(double[] data) {
    this.data.clear();
    this.data.add(data);
  }

  public void put(double value) {
    data.add(value);
  }

  public Histogram createHistogram(int minStripes, double maxStripeWidth) {
    final int size = data.size();

    if (size == 0) {
      return Histogram.EMPTY;
    }

    final StatProps props = computeProps();

    if (props.isSingleValued()) {
      return new HistogramImpl(
          new double[]{props.min, props.max},
          new double[]{size}
      );
    }

    final int stripes = Math.max(minStripes, (int) Math.ceil(props.diff / maxStripeWidth));
    final double[] args = new double[stripes + 1];
    final double width = props.diff / stripes;

    for (int i = 0; i < stripes; i++) {
      args[i] = props.min + width * i;
    }
    args[stripes] = props.max;

    final double[] values = new double[stripes];
    for (int i = 0; i < size; i++) {
      final double dataI = data.get(i);
      final int searchIndex = Arrays.binarySearch(args, dataI);
      final int incrementIndex;
      if (searchIndex >= 0) {
        //	the last range is inclusive on both boundaries
        incrementIndex = searchIndex == stripes ? searchIndex - 1 : searchIndex;
      } else {
        //	index of max range boundary, but we increment range for a given min range boundary
        final int insertionPoint = -(searchIndex + 1);
        incrementIndex = insertionPoint - 1;
      }

      values[incrementIndex] += 1.0;
    }

    return new HistogramImpl(args, values);
  }

  public StatProps computeProps() {
    final int size = data.size();

    double max = data.get(0);
    double min = data.get(0);
    for (int i = 1; i < size; i++) {
      final double dataI = data.get(i);
      min = Math.min(dataI, min);
      max = Math.max(dataI, max);
    }

    return new StatProps(min, max);
  }
}
