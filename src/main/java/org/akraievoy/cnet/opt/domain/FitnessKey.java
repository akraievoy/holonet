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

package org.akraievoy.cnet.opt.domain;

/**
 * Organize specimens into ordered collection so that iterations move towards
 * inferiorer fitnesses (and later added specimens for fitness ties).
 * <p/>
 * The index field tracks addition (while generating) or db lookup order (while reading).
 * No need for that field to be consistent: it must be unique to avoid map collisions of Genomes with same fitness. 
 */
public class FitnessKey implements Comparable<FitnessKey> {
  private double fitness;
  private int index;

  FitnessKey(int index, double fitness) {
    this.fitness = fitness;
    this.index = index;
  }

  public double getFitness() {
    return fitness;
  }

  public int getIndex() {
    return index;
  }

  public int compareTo(FitnessKey o) {
    if (equals(o)) {
      return 0;
    }

    final int fitnessCompare = Double.compare(fitness, o.fitness);
    boolean greater = fitnessCompare < 0 || (fitnessCompare == 0 && index > o.index);

    return greater ? 1 : -1;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    FitnessKey that = (FitnessKey) o;

    return Double.compare(that.fitness, fitness) == 0 && index == that.index;
  }

  @Override
  public int hashCode() {
    int result;
    long temp;
    temp = fitness != +0.0d ? Double.doubleToLongBits(fitness) : 0L;
    result = (int) (temp ^ (temp >>> 32));
    result = 31 * result + index;
    return result;
  }
}
