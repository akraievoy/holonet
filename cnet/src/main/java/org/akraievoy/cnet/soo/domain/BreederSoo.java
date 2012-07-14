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
import org.akraievoy.cnet.gen.vo.WeightedEventModelRenorm;
import org.akraievoy.cnet.net.vo.EdgeData;
import org.akraievoy.cnet.net.vo.IndexCodec;
import org.akraievoy.cnet.opt.api.Breeder;
import org.akraievoy.cnet.opt.api.GeneticState;
import org.akraievoy.cnet.opt.api.GeneticStrategy;
import org.akraievoy.cnet.opt.api.Genome;

public abstract class BreederSoo implements Breeder {
  protected final IndexCodec codec;
  protected WeightedEventModelRenorm unwireModel;

  public BreederSoo() {
    codec = new IndexCodec(false);

    unwireModel = new WeightedEventModelRenorm(!isFavoringMinimal(), 0);
  }

  protected abstract boolean isFavoringMinimal();

  public Genome crossover(GeneticStrategy strategy, Genome parentA, Genome parentB, GeneticState state, EntropySource eSource) {
    final GeneticStrategySoo strategySoo = (GeneticStrategySoo) strategy;
    final GenomeSoo genomeA = (GenomeSoo) parentA;
    final GenomeSoo genomeB = (GenomeSoo) parentB;

    final GenomeSoo child = createChild(genomeA, genomeB);

    final EdgeData linkFitness = buildLinkFitness(strategySoo, child);

    unwire(strategySoo, genomeA, genomeB, child, linkFitness, eSource, state);

    return child;
  }

  protected abstract EdgeData buildLinkFitness(GeneticStrategySoo strategySoo, GenomeSoo child);

  protected void unwire(final GeneticStrategySoo strategy, final GenomeSoo genomeA, final GenomeSoo genomeB, GenomeSoo child,
                        final EdgeData linkFitness, EntropySource eSource, final GeneticState state) {
    unwireModel.clear();
    unwireModel.setMinWeight(state.getMinElemFitness());
    unwireModel.setAmp(state.getElemFitPow());

    final double linksToUnwire = computeLinkDiff(genomeA, child);
    if (!Soft.MILLI.positive(linksToUnwire)) {
      return;
    }
    final double linksToUnwireFromA = (1 - state.getCrossoverRatio()) * linksToUnwire;

    child.getSolution().visitNotNull(new EdgeSubsetVisitor(unwireModel, linkFitness, genomeA, genomeB, codec));

    final double[] removedRef = {0};
    remove(strategy, child, eSource, linksToUnwireFromA, removedRef);

    unwireModel.clear();
    child.getSolution().visitNotNull(new EdgeSubsetVisitor(unwireModel, linkFitness, genomeB, null, codec));

    remove(strategy, child, eSource, linksToUnwire, removedRef);
  }

  protected void remove(
      GeneticStrategySoo strategy, GenomeSoo child, EntropySource eSource,
      double unwireLimit, double[] removedRef
  ) {
    final double step = 1.0 / strategy.getSteps();
    final int[] indexRef = new int[1];
    for (; Soft.MILLI.less(removedRef[0], unwireLimit)  && unwireModel.getSize() > 0; removedRef[0] += step) {
      final int unwireId = unwireModel.generate(eSource, false, indexRef);

      final int unwireFrom = codec.id2leading(unwireId);
      final int unwireInto = codec.id2trailing(unwireId);

      final double newVal = child.getSolution().get(unwireFrom, unwireInto) - step;
      final double newValStrict = Soft.MILLI.positive(newVal) ? newVal : 0;
      child.getSolution().set(unwireFrom, unwireInto, newValStrict);
      if (newValStrict == 0) {
        unwireModel.removeByIndex(indexRef[0]);
      }
    }
  }

  protected double computeLinkDiff(GenomeSoo genomeA, GenomeSoo child) {
    final double connectivityDiff = child.getSolution().total() - genomeA.getSolution().total();
    return connectivityDiff;
  }

  protected GenomeSoo createChild(GenomeSoo genomeA, GenomeSoo genomeB) {
    final EdgeData solA = genomeA.getSolution();
    final EdgeData solB = genomeB.getSolution();

    final GenomeSoo child = new  GenomeSoo(solA.proto(solA.getSize()));
    final EdgeData solution = child.getSolution();
    final EdgeData.EdgeVisitor buildChildVisitor = new EdgeData.EdgeVisitor() {
      public void visit(int from, int into, double linkFitness) {
        solution.set(from, into, Math.max(linkFitness, solution.get(from, into)));
      }
    };

    solA.visitNotNull(buildChildVisitor);
    solB.visitNotNull(buildChildVisitor);

    return child;
  }

  public String toString() {
    return "[ " + this.getClass().getSimpleName() + " ]";
  }

  protected static class EdgeSubsetVisitor implements EdgeData.EdgeVisitor {
    protected final IndexCodec codec;
    protected final WeightedEventModel unwireModel;
    protected final GenomeSoo included;
    protected final GenomeSoo excluded;
    protected final EdgeData linkFitness;

    public EdgeSubsetVisitor(WeightedEventModel unwireModel, EdgeData linkFitness, GenomeSoo included, GenomeSoo excluded, IndexCodec codec) {
      this.codec = codec;
      this.unwireModel = unwireModel;
      this.included = included;
      this.excluded = excluded;
      this.linkFitness = linkFitness;
    }

    public void visit(int from, int into, double value) {
      final boolean include = Soft.MILLI.positive(included.getSolution().get(from, into));

      if (include) {
        final boolean skipped = excluded != null && Soft.MILLI.positive(excluded.getSolution().get(from, into));

        if (!skipped) {
          unwireModel.add(codec.fi2id(from, into), linkFitness.get(from, into));
        }
      }
    }
  }
}
