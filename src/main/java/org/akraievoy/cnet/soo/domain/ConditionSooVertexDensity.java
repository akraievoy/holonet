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

package org.akraievoy.cnet.soo.domain;

import org.akraievoy.cnet.net.vo.EdgeData;
import org.akraievoy.cnet.opt.api.Condition;
import org.akraievoy.cnet.opt.api.GeneticStrategy;

import java.util.Collection;

public class ConditionSooVertexDensity implements Condition<GenomeSoo> {
  protected int lowerLinkLimit = 0;
  protected int upperLinkLimit = 0;

  public boolean isValid(GeneticStrategy<GenomeSoo> strategy, GenomeSoo child, Collection<GenomeSoo> generation, int generationIndex) {
    final GeneticStrategySoo strategySoo = (GeneticStrategySoo) strategy;

    if (!strategySoo.mode(GeneticStrategySoo.MODE_REGULAR)) {
      return true;
    }

    final EdgeData sol = child.getSolution();

    lowerLinkLimit = strategySoo.getNodeLinkLowerLimit();
    upperLinkLimit = strategySoo.getNodeLinkUpperLimit();
    for (int from = 0, size = sol.getSize(); from < size; from++) {
      final double powerFrom = sol.power(from);
      if (powerFrom < lowerLinkLimit || powerFrom > upperLinkLimit) {
        return false;
      }
    }

    return true;
  }

  public String toString() {
    return "[ " + upperLinkLimit + " => Links(per node) >= " + lowerLinkLimit + " ]";
  }
}