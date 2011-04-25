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
import org.akraievoy.cnet.opt.api.GeneticState;
import org.akraievoy.cnet.opt.api.GeneticStrategy;
import org.akraievoy.cnet.opt.api.Genome;
import org.akraievoy.cnet.opt.api.Mutator;

public class CompositeMutator extends Composite<Mutator<Genome>> {
  protected Mutator<Genome> createReportingWrapper(int breederI, Mutator<Genome> wrapped) {
    return new ReportingMutator(breederI, wrapped);
  }

  public String getTitle() {
    return "mutators";
  }

  class ReportingMutator implements Mutator<Genome>, Indexed {
    final int index;
    final Mutator<Genome> wrapped;

    public ReportingMutator(int index, Mutator<Genome> wrapped) {
      this.index = index;
      this.wrapped = wrapped;
    }

    public void mutate(GeneticStrategy strategy, Genome child, GeneticState state, EntropySource eSource) {
      elemUses[index]++;

      final long breederStart = System.currentTimeMillis();

      wrapped.mutate(strategy, child, state, eSource);

      final long breederEnd = System.currentTimeMillis();

      elemTimes[index] += breederEnd - breederStart;
    }

    public int getIndex() {
      return index;
    }
  }
}