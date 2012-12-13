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

package org.akraievoy.cnet.stat.domain;

import org.akraievoy.cnet.stat.api.Median;

import java.util.Arrays;

public class MedianClustering implements Median {
  public double computeMedian(final double[] sample) {
    Arrays.sort(sample);

    double maxDist = sample[1] - sample[0];
    int medianIndex = 0;
    for (int p = 1; p < sample.length - 1; p++) {
      final double dist = sample[p + 1] - sample[p];
      if (maxDist < dist) {
        maxDist = dist;
        medianIndex = p;
      }
    }

    double median = (sample[medianIndex] + sample[medianIndex + 1]) / 2;

    return median;
  }
}
