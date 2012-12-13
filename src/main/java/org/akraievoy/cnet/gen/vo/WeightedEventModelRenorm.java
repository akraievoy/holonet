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
import org.akraievoy.base.Die;

public class WeightedEventModelRenorm extends WeightedEventModel {
  protected boolean favoringMinimal;
  protected double minWeight;
  protected double amp;

  public WeightedEventModelRenorm(
      final boolean favoringMinimal,
      final double minWeight,
      final Optional<String> name
  ) {
    this(favoringMinimal, minWeight, 1, name);
  }

  public WeightedEventModelRenorm(
      final boolean favoringMinimal,
      final double minWeight,
      final double amp,
      final Optional<String> name
  ) {
    super(name);

    this.favoringMinimal = favoringMinimal;
    this.minWeight = minWeight;
    this.amp = amp;
  }

  public WeightedEventModelRenorm(final Optional<String> name) {
    this(false, MIN_WEIGHT_DEFAULT, name);
  }

  public WeightedEventModelRenorm() {
    this(Optional.<String>absent());
  }

  public void setMinWeight(double minWeight) {
    this.minWeight = minWeight;
  }

  public void setAmp(double amp) {
    this.amp = amp;
  }

  public void setFavoringMinimal(boolean favoringMinimal) {
    this.favoringMinimal = favoringMinimal;
  }

  //	LATER maybe it's better to use median here, not the average
  protected void initSums() {
    final int size = getSize();
    double med, min, max;
    med = min = max = weights.get(0);

    for (int i = 1; i < size; i++) {
      final double wI = weights.get(i);

      med += wI;
      min = Math.min(min, wI);
      max = Math.max(max, wI);
    }
    med /= size;

    sums = new TDoubleArrayList(weights.size());

    final double scale = favoringMinimal ? -0.5 : 0.5;
    final double minWeightScale = 1 - minWeight;

    double prevSum = 0;
    for (int i = 0; i < size; i++) {
      final double w = weights.get(i);

      final double offs = w > med ? max - med : med - min;
      //	this should be in -1 .. 1 range
      final double diff = (w - med) / offs;
      //	this should be in 0 .. 1 range
      final double norm = scale * diff + 0.5;
      //	this should be in minWeight .. 1 range
      final double bump = minWeight + Math.pow(norm, amp) * minWeightScale;

      prevSum = prevSum + bump;

      weights.set(i, bump);
      sums.add(prevSum);
    }
  }

  protected void extendSums(double weight) {
    Die.ifNotNull("sums", sums);
  }
}
