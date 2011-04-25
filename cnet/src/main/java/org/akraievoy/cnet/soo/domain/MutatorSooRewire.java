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

import gnu.trove.TIntArrayList;
import org.akraievoy.cnet.gen.vo.EntropySource;
import org.akraievoy.cnet.gen.vo.WeightedEventModelRenorm;
import org.akraievoy.cnet.net.vo.EdgeData;
import org.akraievoy.cnet.net.vo.IndexCodec;
import org.akraievoy.cnet.opt.api.GeneticState;
import org.akraievoy.cnet.opt.api.GeneticStrategy;
import org.akraievoy.cnet.opt.api.Mutator;

public abstract class MutatorSooRewire implements Mutator<GenomeSoo> {
  protected final IndexCodec codec;
  protected WeightedEventModelRenorm oldModel;
  protected WeightedEventModelRenorm newModel;

  protected boolean symmetric = true;

  protected MutatorSooRewire() {
    codec = new IndexCodec(false);
    oldModel = new WeightedEventModelRenorm(!isFavoringMinimal(), 0);
    newModel = new WeightedEventModelRenorm(isFavoringMinimal(), 0);
  }

  public void setSymmetric(boolean symmetric) {
    this.symmetric = symmetric;
  }

  public void mutate(GeneticStrategy strategy, GenomeSoo genome, GeneticState state, EntropySource eSource) {
    final GeneticStrategySoo strategySoo = (GeneticStrategySoo) strategy;

    final EdgeData linkFitness = buildLinkFitness(strategySoo, genome);

    final int size = strategySoo.getDistSource().getValue().getSize();

    final int linkNum = (int) Math.ceil(strategySoo.getTotalLinkUpperLimit() * state.getMutateRatio());

    //	TODO join three loops into one: avoid buffering indices into arraylists
    //	TODO renorm relatively to steps
    final TIntArrayList newWireId = chooseNewWire(linkFitness, linkNum, eSource, state, genome, size);
    final TIntArrayList oldWireId = chooseOldWire(linkFitness, linkNum, eSource, state, genome);

    for (int i = 0, links = Math.min(newWireId.size(), oldWireId.size()); i < links; i++) {
      rewire(strategySoo, genome, oldWireId.get(i), newWireId.get(i));
    }
  }

  protected TIntArrayList chooseOldWire(final EdgeData linkFitness, int linkNum, EntropySource eSource, GeneticState state, GenomeSoo genome) {
    oldModel.clear();
    oldModel.setMinWeight(state.getMinElemFitness());
    oldModel.setAmp(state.getElemFitPow());

    genome.getSolution().visitNotNull(
        new EdgeData.EdgeVisitor() {
          public void visit(int from, int into, double value) {
            oldModel.add(codec.fi2id(from, into), linkFitness.get(from, into));
          }
        }
    );

    final TIntArrayList result = new TIntArrayList();
    while (oldModel.getSize() > 0 && result.size() < linkNum) {
      result.add(oldModel.generate(eSource, true, null));
    }

    return result;
  }

  protected TIntArrayList chooseNewWire(EdgeData linkFitness, int linkNum, EntropySource eSource, GeneticState state, GenomeSoo genome, int size) {
    newModel.clear();
    newModel.setMinWeight(state.getMinElemFitness());
    newModel.setAmp(state.getElemFitPow());

    for (int from = 0; from < size; from++) {
      for (int into = 0, maxInto = symmetric ? from : size; into < maxInto; into++) {
        if (from == into || genome.getSolution().get(from, into) >= 1) {  //	TODO performance
          continue;
        }

        newModel.add(codec.fi2id(from, into), linkFitness.get(from, into));
      }
    }

    final TIntArrayList result = new TIntArrayList();

    while (newModel.getSize() > 0 && result.size() < linkNum) {
      result.add(newModel.generate(eSource, true, null));
    }

    return result;
  }

  protected void rewire(GeneticStrategySoo strategySoo, GenomeSoo genome, int oldWireId, int newWireId) {
    final double step = 1.0 / strategySoo.getSteps();
    final int oldWireFrom = codec.id2leading(oldWireId);
    final int oldWireInto = codec.id2trailing(oldWireId);

    final EdgeData solution = genome.getSolution();
    solution.set(oldWireFrom, oldWireInto, solution.get(oldWireFrom, oldWireInto) - step);

    final int newWireFrom = codec.id2leading(newWireId);
    final int newWireInto = codec.id2trailing(newWireId);

    solution.set(newWireFrom, newWireInto, solution.get(newWireFrom, newWireInto) + step);
  }

  protected abstract EdgeData buildLinkFitness(GeneticStrategySoo strategySoo, GenomeSoo genome);

  protected abstract boolean isFavoringMinimal();

  public String toString() {
    return "[ " + this.getClass().getSimpleName() + " ]";
  }
}
