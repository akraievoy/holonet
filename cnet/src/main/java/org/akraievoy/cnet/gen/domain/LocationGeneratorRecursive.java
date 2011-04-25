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

import org.akraievoy.cnet.gen.vo.*;

public class LocationGeneratorRecursive extends LocationGeneratorBase {
  protected final EntropySource eSource;
  protected final WeightedEventModel eventModel;

  protected double dimensionRatio = 1.5;

  protected double[] weights;
  protected double cutoff;
  protected int fuzzyness = 6;

  public LocationGeneratorRecursive(final EntropySource eSource) {
    this.eSource = eSource;
    this.eventModel = new WeightedEventModelBase();
  }

  public void setDimensionRatio(double dimensionRatio) {
    this.dimensionRatio = dimensionRatio;
  }

  public double getDimensionRatio() {
    return dimensionRatio;
  }

  public int getFuzzyness() {
    return fuzzyness;
  }

  public void setFuzzyness(int fuzzyness) {
    this.fuzzyness = fuzzyness;
  }

  public void run() {
    eSource.diagnoseSeed(this.getClass().getSimpleName());
    computeWeights();
  }

  protected void computeWeights() {
    weights = new double[cells];

    final double ratio = Math.pow(2, dimensionRatio - 2);

    final double[] curWeights = new double[cells];
    int curGridSize = 1;
    curWeights[0] = 1;

    final double[] subGrid = new double[9];
    final double[] subWeights = new double[4];
    final int totalIterations = (int) Math.round(Math.log(gridSize) / Math.log(2));
    for (int iter = 0; iter < totalIterations; iter++) {
      for (int y = 0; y < curGridSize; y++) {
        for (int x = 0; x < curGridSize; x++) {
          produceSubGrid(y, x, curGridSize, curWeights, subGrid);
          produceSubWeights(subGrid, subWeights);
          randomizeSubWeights(eSource, ratio, subWeights, subGrid[4]);
          zerifySubWeights(subWeights, iter / (double) totalIterations);
          storeSubWeights(subWeights, y, x);
        }
      }

      curGridSize *= 2;
      System.arraycopy(weights, 0, curWeights, 0, curGridSize * gridSize);
    }

    for (int i = 0; i < cells; i++) {
      if (weights[i] > 0) {
        eventModel.add(i, weights[i]);
      }
    }
  }

  protected void zerifySubWeights(double[] subWeights, double completion) {
    cutoff = completion / fuzzyness;
    if (cutoff == 0) {
      return;
    }

    for (int i = 0; i < 4; i++) {
      int zeroed = 0;

      double minNotZeroValue = Double.POSITIVE_INFINITY;
      int minNotZeroIndex = -1;
      for (int j = 0; j < 4; j++) {
        final double value = subWeights[j];
        if (value == 0) {
          zeroed++;
          continue;
        }

        if (minNotZeroValue > value) {
          minNotZeroValue = value;
          minNotZeroIndex = j;
        }
      }

      if (zeroed >= 3 || minNotZeroValue > cutoff) {
        break;
      }

      subWeights[minNotZeroIndex] = 0;
      zeroed++;

      for (int j = 0; j < 4; j++) {
        if (subWeights[j] != 0) {
          subWeights[j] += minNotZeroValue / (4 - zeroed);
        }
      }
    }
  }

  protected void storeSubWeights(double[] subWeights, int y, int x) {
    final int pos = y * 2 * gridSize + x * 2;
    weights[pos] = subWeights[0];
    weights[pos + 1] = subWeights[1];
    weights[pos + gridSize] = subWeights[2];
    weights[pos + gridSize + 1] = subWeights[3];
  }

  protected void randomizeSubWeights(EntropySource eSource, double ratio, double[] subWeights, double totalWeight) {
    final double removeRatio = (1 - ratio) * 2;
    if (removeRatio > 0) {
      subWeights[0] *= 1 - removeRatio * eSource.nextDouble();
      subWeights[1] *= 1 - removeRatio * eSource.nextDouble();
      subWeights[2] *= 1 - removeRatio * eSource.nextDouble();
      subWeights[3] *= 1 - removeRatio * eSource.nextDouble();

      final double weightRandSum = subWeights[0] + subWeights[1] + subWeights[2] + subWeights[3];

      final double renorm = 4 * ratio * totalWeight / weightRandSum;

      subWeights[0] *= renorm;
      subWeights[1] *= renorm;
      subWeights[2] *= renorm;
      subWeights[3] *= renorm;
    }
  }

  protected void produceSubWeights(double[] subGrid, double[] subWeights) {
    subWeights[0] = 0.001 + 0.25 * (subGrid[0] + subGrid[1] + subGrid[3] + subGrid[4]);
    subWeights[1] = 0.001 + 0.25 * (subGrid[1] + subGrid[2] + subGrid[4] + subGrid[5]);
    subWeights[2] = 0.001 + 0.25 * (subGrid[3] + subGrid[4] + subGrid[6] + subGrid[7]);
    subWeights[3] = 0.001 + 0.25 * (subGrid[4] + subGrid[5] + subGrid[7] + subGrid[8]);
  }

  protected void produceSubGrid(int y, int x, int curGridSize, double[] curWeights, double[] subGrid) {
    int pos = y * gridSize + x;

    final boolean yG = y > 0;
    final boolean xG = x > 0;
    final boolean xL = x < curGridSize - 1;
    final boolean yL = y < curGridSize - 1;

    if (yG && xG) {
      subGrid[0] = curWeights[pos - gridSize - 1];
    } else if (xG) {
      subGrid[0] = curWeights[pos - 1];
    } else if (yG) {
      subGrid[0] = curWeights[pos - gridSize];
    } else {
      subGrid[0] = curWeights[pos];
    }

    if (yG) {
      subGrid[1] = curWeights[pos - gridSize];
    } else {
      subGrid[1] = curWeights[pos];
    }

    if (yG && xL) {
      subGrid[2] = curWeights[pos - gridSize + 1];
    } else if (yG) {
      subGrid[2] = curWeights[pos - gridSize];
    } else if (xL) {
      subGrid[2] = curWeights[pos + 1];
    } else {
      subGrid[2] = curWeights[pos];
    }

    if (xG) {
      subGrid[3] = curWeights[pos - 1];
    } else {
      subGrid[3] = curWeights[pos];
    }

    subGrid[4] = curWeights[pos];

    if (xL) {
      subGrid[5] = curWeights[pos + 1];
    } else {
      subGrid[5] = curWeights[pos];
    }

    if (yL && xG) {
      subGrid[6] = curWeights[pos + gridSize - 1];
    } else if (xG) {
      subGrid[6] = curWeights[pos - 1];
    } else if (yL) {
      subGrid[6] = curWeights[pos + gridSize];
    } else {
      subGrid[6] = curWeights[pos];
    }

    if (yL) {
      subGrid[7] = curWeights[pos + gridSize];
    } else {
      subGrid[7] = curWeights[pos];
    }

    if (yL && xL) {
      subGrid[8] = curWeights[pos + gridSize + 1];
    } else if (xL) {
      subGrid[8] = curWeights[pos + 1];
    } else if (yL) {
      subGrid[8] = curWeights[pos + gridSize];
    } else {
      subGrid[8] = curWeights[pos];
    }
  }

  public Point chooseLocation(final EntropySource eSource) {
    final int freeIndex = eventModel.generate(eSource, true, null);
    final Point point = index2p(freeIndex);

    return point;
  }

  public double getDensity(Point location) {
    return weights[p2index(location)];
  }
}