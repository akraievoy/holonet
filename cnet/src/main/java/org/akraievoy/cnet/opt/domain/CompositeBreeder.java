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

import org.akraievoy.cnet.gen.vo.EntropySource;
import org.akraievoy.cnet.opt.api.Breeder;
import org.akraievoy.cnet.opt.api.GeneticState;
import org.akraievoy.cnet.opt.api.GeneticStrategy;
import org.akraievoy.cnet.opt.api.Genome;

public class CompositeBreeder extends Composite<Breeder<Genome>> {
  protected ReportingBreeder createReportingWrapper(int breederI, Breeder<Genome> wrapped) {
    return new ReportingBreeder(breederI, wrapped);
  }

  public String getTitle() {
    return "breeders";
  }

  class ReportingBreeder implements Breeder<Genome>, Indexed {
    final int index;
    final Breeder<Genome> wrapped;

    public ReportingBreeder(int index, Breeder<Genome> wrapped) {
      this.index = index;
      this.wrapped = wrapped;
    }

    public Genome crossover(GeneticStrategy strategy, Genome parentA, Genome parentB, GeneticState state, EntropySource eSource) {
      elemUses[index]++;

      final long breederStart = System.currentTimeMillis();

      final Genome result = wrapped.crossover(strategy, parentA, parentB, state, eSource);

      final long breederEnd = System.currentTimeMillis();

      elemTimes[index] += breederEnd - breederStart;

      return result;
    }

    public int getIndex() {
      return index;
    }
  }
}
