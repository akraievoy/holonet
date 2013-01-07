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

package org.akraievoy.cnet.opt.api;

import org.akraievoy.holonet.exp.store.StoreLens;

import java.util.Arrays;

/**
 * To maintain easy access to solution data, Genome should consist of some
 * number of indexed&typed components, stored separately.
 */
public abstract class Genome {
  protected Object[] genomeData;
  public Object[] getGenomeData() { return genomeData; }
  public void setGenomeData(Object[] genomeData) { this.genomeData = genomeData; }

  protected Double fitness;
  public Double getFitness() { return fitness; }
  public void setFitness(Double fitness) { this.fitness = fitness; }

  @SuppressWarnings("unchecked")
  public <G extends Genome> double getOrComputeFitness(GeneticStrategy<G> strategy) {
    if (fitness != null) {
      return fitness;
    }

    return fitness = strategy.computeFitness((G) this);
  }

  public Genome() {
  }

  public Genome(Object[] genomeData) {
    this.genomeData = genomeData;
  }

  public boolean isDupeOf(Genome that) {
    return Arrays.deepEquals(genomeData, that.genomeData);
  }

  public abstract double similarity(Genome that);

  public abstract void write(
      final StoreLens<Double> baseLens,
      final double fitness
  );

  public abstract Genome read(
      StoreLens<Double> baseLens
  );
}
