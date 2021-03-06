/*
 Copyright 2012 Anton Kraievoy akraievoy@gmail.com
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

package org.akraievoy.cnet.soo.domain;

import com.google.common.base.Optional;
import org.akraievoy.cnet.opt.api.Condition;
import org.akraievoy.cnet.opt.api.GeneticStrategy;
import org.akraievoy.util.Interpolate;

import java.util.Collection;

public class ConditionSooFitnessCapping implements Condition<GenomeSoo> {
  private Optional<Double> fitnessCap = Optional.absent();

  public boolean isValid(
      GeneticStrategy<GenomeSoo> strategy,
      GenomeSoo child,
      Collection<GenomeSoo> generation,
      int generationIndex
  ) {
    final GeneticStrategySoo strategySoo = (GeneticStrategySoo) strategy;

    if (!fitnessCap.isPresent()) {
      double fcTarget = strategySoo.getFitnessCap();
      fitnessCap = Optional.of(
          Interpolate.norm(
              0, strategySoo.generationNum * 0.5,
              fcTarget * 0.85, fcTarget,
              Interpolate.LINEAR
          ).apply(generationIndex)
      );
    }

    final double fitness = child.getOrComputeFitness(strategy);
    final Double fitnessCap = this.fitnessCap.get();
    final boolean valid = fitness <= fitnessCap;

/*
    System.out.printf("%g <= %g --> %s %n", fitness, fitnessCap, valid ? "valid" : "invalid");
*/

    return valid;
  }

  public String toString() {
    return "[ Fitness <= " + fitnessCap.get() + " ]";
  }
}