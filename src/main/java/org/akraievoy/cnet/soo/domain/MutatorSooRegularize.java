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

import org.akraievoy.base.soft.Soft;
import org.akraievoy.cnet.gen.vo.EntropySource;
import org.akraievoy.cnet.gen.vo.WeightedEventModel;
import org.akraievoy.cnet.gen.vo.WeightedEventModelBase;
import org.akraievoy.cnet.metrics.api.Metric;
import org.akraievoy.cnet.metrics.domain.MetricVDataPowers;
import org.akraievoy.cnet.net.vo.EdgeData;
import org.akraievoy.cnet.net.vo.VertexData;
import org.akraievoy.cnet.opt.api.GeneticState;
import org.akraievoy.cnet.opt.api.GeneticStrategy;
import org.akraievoy.cnet.opt.api.Mutator;
import org.akraievoy.holonet.exp.store.RefObject;

public class MutatorSooRegularize implements Mutator<GenomeSoo> {
  public MutatorSooRegularize() {
  }

  public void mutate(
      GeneticStrategy geneticStrategy,
      GenomeSoo child,
      GeneticState state,
      EntropySource eSource
  ) {
    final GeneticStrategySoo strategy = (GeneticStrategySoo) geneticStrategy;
    if (!strategy.mode(GeneticStrategySoo.MODE_REGULAR)) {
      return;
    }

    final double step = 1.0 / strategy.getSteps();
    final int limit = strategy.getNodeLinkLowerLimit();
    final EdgeData data = child.getSolution();
    final MetricVDataPowers powersMetric =
        new MetricVDataPowers(new RefObject<EdgeData>(data));
    final int size = data.getSize();

    for (int node = 0; node < data.getSize(); node++) {
      if (data.get(node, node) != 0) {
        throw new Error("Houston, we've got an INPUT cycle at node " + node);
      }
    }

    final WeightedEventModel rewireChoices = new WeightedEventModelBase();
    boolean moarRewires;
    do {
      final VertexData powers = Metric.fetch(powersMetric);
      int minPowNode = 0, maxPowNode = 0;
      double maxPow = powers.get(0), minPow = powers.get(0);
      for (int node = 1; node < size; node++) {
        final double curPow = powers.get(node);
        if (curPow < minPow) {
          minPow = curPow;
          minPowNode = node;
        } else if (curPow > maxPow) {
          maxPow = curPow;
          maxPowNode = node;
        }
      }

      moarRewires = minPow < limit && limit < maxPow && maxPow - minPow >= step;
      if (moarRewires) {
        rewireChoices.clear();
        for (int node = 0; node < size; node++) {
          if (
              node != minPowNode && node != minPowNode &&
              !Soft.NANO.less(data.get(maxPowNode, node), step) &&
              !Soft.NANO.greater(data.get(node, minPowNode), 1.0 - step)
          ) {
            rewireChoices.add(node, 1);
          }
        }
        if (rewireChoices.getSize() == 0) {
          moarRewires = false;
        } else {
          final int rewire = rewireChoices.generate(eSource, false, null);
          data.set(maxPowNode, rewire, data.get(maxPowNode, rewire) - step);
          data.set(rewire, minPowNode, data.get(rewire, minPowNode) + step);
        }
      }

    } while(moarRewires);
  }

  public String toString() {
    return "[ " + this.getClass().getSimpleName() + " ]";
  }
}
