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

import com.google.common.base.Optional;
import org.akraievoy.cnet.gen.vo.*;

public class LocationGeneratorFractalDLA extends LocationGeneratorBase {
  protected final EntropySource eSource;
  protected final WeightedEventModel eventModel;

  protected double dimensionRatio = 1.5;
  protected double densityRatio = 0.2;
  protected double genPointsRatio = 1.2;
  protected int generationNum = 12;
  protected int tryNum = 25;

  protected int seedPointNum = 1;

  protected double[] weights;

  public LocationGeneratorFractalDLA(final EntropySource eSource) {
    this.eSource = eSource;
    this.eventModel = new WeightedEventModelBase(Optional.of("locations"));
  }

  public void setDimensionRatio(double dimensionRatio) {
    this.dimensionRatio = dimensionRatio;
  }

  public void setGenerationNum(int generationNum) {
    this.generationNum = generationNum;
  }

  public void setDensityRatio(double densityRatio) {
    this.densityRatio = densityRatio;
  }

  public double getDimensionRatio() {
    return dimensionRatio;
  }

  public int getGenerationNum() {
    return generationNum;
  }

  public double getDensityRatio() {
    return densityRatio;
  }

  public int getTryNum() {
    return tryNum;
  }

  public void setTryNum(int tryNum) {
    this.tryNum = tryNum;
  }

  public void setGenPointsRatio(double genPointsRatio) {
    this.genPointsRatio = genPointsRatio;
  }

  public double getGenPointsRatio() {
    return genPointsRatio;
  }

  public int getSeedPointNum() {
    return seedPointNum;
  }

  public void run() {
    eSource.diagnoseSeed(this.getClass().getSimpleName());
    computeWeights();
  }

  protected void computeWeights() {
    weights = new double[cells];
    final int[] dirs = new int[]{-gridSize - 1, -gridSize, -gridSize + 1, 1, gridSize + 1, gridSize, gridSize - 1, -1};

    final double totalWeight = Math.pow(gridSize, dimensionRatio);
    final double totalArea = totalWeight / densityRatio;

    double sum = 0;
    for (int g = 0; g < generationNum; g++) {
      sum += Math.pow(genPointsRatio, g);
    }

    final double genWeight = totalWeight / generationNum;
    double points = totalArea / sum;
    seedPointNum = (int) Math.floor(points);

    for (int g = 0; g < generationNum; g++) {
      if (g > 0) {
        points *= genPointsRatio;
      }
      final double pointWeight = genWeight / points;

      int tries = tryNum;
      for (int p = 0; p < points; tries--) {
        final int pos = eSource.nextInt(cells);

        if (g == 0 || tries == 0) {  //	that's a seed (or tries exhausted), distribute it uniformly
          weights[pos] += pointWeight;
          p++;
          tries = tryNum;
          continue;
        }

        int dir = eSource.nextInt(dirs.length) % 8;

        int curPos = pos;
        while (isDep(curPos)) {  //	atempting to drift out of deposit
          curPos += dirs[dir];
        }
        if (!isPos(curPos)) {  //	clipped off
          continue;
        }

        int stepCounter = 0;
        while (isPos(curPos)) {  //	drift to deposit, looking at near cells also
          final int nextPos = curPos + dirs[dir];
          final int nextPosL = curPos + dirs[(dir + 1) % 8];
          final int nextPosR = curPos + dirs[(dir + 7) % 8];

          if (isDep(nextPos) || isDep(nextPosL) || isDep(nextPosR)) {
            break;
          }

          curPos = nextPos;
          if (stepCounter % 8 == 0) {
            dir = (dir + eSource.nextInt(3) + 7) % 8;
          }
          stepCounter++;
        }

        if (!isPos(curPos)) {
          continue; //	failed to land on deposit: clipped off
        }

        weights[curPos] += pointWeight;
        p++;
        tries = tryNum;
      }
    }

    for (int i = 0; i < cells; i++) {
      if (weights[i] > 0) {
        eventModel.add(i, weights[i]);
      }
    }
  }

  protected boolean isDep(int nextPos) {
    return isPos(nextPos) && weights[nextPos] > 0;
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