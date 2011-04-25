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

import org.akraievoy.cnet.opt.api.Condition;
import org.akraievoy.cnet.opt.api.GeneticStrategy;
import org.akraievoy.cnet.opt.api.Genome;

import java.util.List;

public class CompositeCondition extends Composite<Condition<Genome>> {
  protected Condition<Genome> createReportingWrapper(int breederI, Condition<Genome> wrapped) {
    return new ReportingCondition(breederI, wrapped);
  }

  public String getTitle() {
    return "conditions";
  }

  class ReportingCondition implements Condition<Genome>, Indexed {
    final int index;
    final Condition<Genome> wrapped;

    public ReportingCondition(int index, Condition<Genome> wrapped) {
      this.index = index;
      this.wrapped = wrapped;
    }

    public boolean isValid(GeneticStrategy strategy, Genome child, List<Genome> generation, int generationIndex) {
      elemUses[index]++;

      final long breederStart = System.currentTimeMillis();

      final boolean result = wrapped.isValid(strategy, child, generation, generationIndex);

      final long breederEnd = System.currentTimeMillis();

      elemTimes[index] += breederEnd - breederStart;

      return result;
    }

    public int getIndex() {
      return index;
    }
  }
}