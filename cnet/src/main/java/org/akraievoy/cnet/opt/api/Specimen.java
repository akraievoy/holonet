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

import org.akraievoy.base.Die;

/**
 * An actual phenotype, a genome with associated fitness.
 * <p/>
 * Subclasses may add some other possible transient parameters, which should not be persisted as parts of Genome.
 */
public class Specimen<G extends Genome> {
  protected Double fitness = null;
  protected G genome;

  public Specimen() {
  }

  public Specimen(Double fitness, G genome) {
    this.fitness = fitness;
    this.genome = genome;
  }

  public double getFitness() {
    Die.ifNull("fitness", fitness);

    return fitness;
  }

  public void setFitness(Double fitness) {
    this.fitness = fitness;
  }

  public G getGenome() {
    return genome;
  }

  public void setGenome(G genome) {
    this.genome = genome;
  }
}
