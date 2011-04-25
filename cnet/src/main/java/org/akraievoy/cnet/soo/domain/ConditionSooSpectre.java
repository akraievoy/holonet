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

import org.akraievoy.cnet.metrics.api.MetricResultFetcher;
import org.akraievoy.cnet.net.ref.RefEdgeData;
import org.akraievoy.cnet.opt.api.Condition;
import org.akraievoy.cnet.opt.api.GeneticStrategy;

import java.util.List;

public class ConditionSooSpectre implements Condition<GenomeSoo> {
  protected double minLambda;

  public boolean isValid(GeneticStrategy strategy, GenomeSoo child, List<GenomeSoo> generation, int generationIndex) {
    final GeneticStrategySoo strategySoo = (GeneticStrategySoo) strategy;

    strategySoo.metricScalarEigenGap.setSource(new RefEdgeData(child.getSolution()));
    final Double eigenGap = (Double) MetricResultFetcher.fetch(strategySoo.metricScalarEigenGap);

    minLambda = strategySoo.getMinLambda(generationIndex);
    final boolean valid = eigenGap >= minLambda;
    return valid;
  }

  public String toString() {
    return "[ EigenGap >= " + minLambda + " ]";
  }

}
