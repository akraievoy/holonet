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

package org.akraievoy.cnet.gen.vo;

import com.google.common.base.Optional;
import gnu.trove.TDoubleArrayList;

public class WeightedEventModelBase extends WeightedEventModel {
  public WeightedEventModelBase() {
  }

  public WeightedEventModelBase(Optional<String> name) {
    super(name);
  }

  protected void initSums() {
    final int size = getSize();

    sums = new TDoubleArrayList(weights.size());

    double prevSum = 0;
    for (int i = 0; i < size; i++) {
      final double w = weights.get(i);

      prevSum = prevSum + w;

      weights.set(i, w);
      sums.add(prevSum);
    }
  }

  protected void extendSums(double weight) {
    if (sums != null) {
      sums.add(sums.get(sums.size() - 1) + weight);
    }
  }
}