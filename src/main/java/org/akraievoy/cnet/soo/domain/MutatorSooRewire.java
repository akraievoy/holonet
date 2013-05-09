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

import com.google.common.base.Optional;
import gnu.trove.TIntHashSet;
import org.akraievoy.cnet.gen.vo.EntropySource;
import org.akraievoy.cnet.gen.vo.WeightedEventModelRenorm;
import org.akraievoy.cnet.net.vo.EdgeData;
import org.akraievoy.cnet.opt.api.GeneticState;
import org.akraievoy.cnet.opt.api.GeneticStrategy;
import org.akraievoy.cnet.opt.api.Mutator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.akraievoy.cnet.net.Net.*;

public abstract class MutatorSooRewire implements Mutator<GenomeSoo> {
  private static final Logger log = LoggerFactory.getLogger(MutatorSooRewire.class);

  private static final int REWIRE_TRY_LIMIT = 3;
  private static final int CONNECT_TRY_LIMIT = 8;
  private static final int CONNECT_TRY_CHOICES = 3;

  protected WeightedEventModelRenorm disconnectModel;
  protected WeightedEventModelRenorm connectModel;
  protected TIntHashSet connectIds;

  protected boolean symmetric;

  protected MutatorSooRewire() {
    disconnectModel = new WeightedEventModelRenorm(
        !isFavoringMinimal(),
        0,
        Optional.of(getClass().getSimpleName() + "-disconnect")
    );
    connectModel = new WeightedEventModelRenorm(
        isFavoringMinimal(),
        0,
        Optional.of(getClass().getSimpleName() + "-connect")
    );
    connectIds = new TIntHashSet(256, 0.125f);
    symmetric = true;
  }

  public void setSymmetric(boolean symmetric) {
    this.symmetric = symmetric;
  }

  public void mutate(GeneticStrategy strategy, GenomeSoo genome, GeneticState state, EntropySource eSource) {
    disconnectModel.clear();
    disconnectModel.setMinWeight(state.getMinElemFitness());
    disconnectModel.setAmp(state.getElemFitPow());

    connectIds.clear();
    connectModel.clear();
    connectModel.setMinWeight(state.getMinElemFitness());
    connectModel.setAmp(state.getElemFitPow());

    final GeneticStrategySoo strategySoo = (GeneticStrategySoo) strategy;
    final EdgeData linkFitness = buildLinkFitness(strategySoo, genome);
    final EdgeData solution = genome.getSolution();
    final double step = 1.0 / strategySoo.getSteps();

    final int linkNum = (int) Math.ceil(strategySoo.getTotalLinkUpperLimit() * state.getMutateRatio());
    final int[] disconnectIndexRef = new int[1];
    final int[] connectIndexRef = new int[1];

    double totalRewired = 0;
    int tryCount = linkNum * REWIRE_TRY_LIMIT;
    while (tryCount > 0 && totalRewired < linkNum) {
      tryCount -= 1;
      if (connectModel.getSize() == 0) {
        configureConnectModel(linkFitness, genome, linkNum, eSource);
        if (connectModel.getSize() == 0) {
          log.warn("failed to populate connect model");
          break;
        }
      }

      if (disconnectModel.getSize() == 0) {
        configureDisconnectModel(linkFitness, genome);
        if (disconnectModel.getSize() == 0) {
          log.warn("failed to populate disconnect model");
          break;
        }
      }

      final int disconnectId = disconnectModel.generate(eSource, false, disconnectIndexRef);
      final int disconnectFrom = toFrom(disconnectId);
      final int disconnectInto = toInto(disconnectId);

      final double disconnectVal = solution.get(disconnectFrom, disconnectInto) - step;
      solution.set(disconnectFrom, disconnectInto, disconnectVal);

      final int connectId = connectModel.generate(eSource, false, connectIndexRef);
      final int connectFrom = toFrom(connectId);
      final int connectInto = toInto(connectId);

      final double connectVal = solution.get(connectFrom, connectInto) + step;
      solution.set(connectFrom, connectInto, connectVal);

      if (disconnectVal < 1e-9) {
        disconnectModel.removeByIndex(disconnectIndexRef[0]);
      }
      if (connectVal > 1 - 1e-9) {
        connectModel.removeByIndex(connectIndexRef[0]);
        connectIds.remove(connectId);
      }
    }

    if (genome.getSolution().total() > strategySoo.getTotalLinkUpperLimit() * 2) {
      log.warn("too much links: {} > {}",  genome.getSolution().total() / 2, strategySoo.getTotalLinkUpperLimit());
    }

    genome.resetFitness();
  }

  protected void configureDisconnectModel(
      final EdgeData linkFitness,
      final GenomeSoo genome
  ) {
    disconnectModel.clear();
    genome.getSolution().visitNonDef(
        new EdgeData.EdgeVisitor() {
          public void visit(int from, int into, double value) {
            if (symmetric && from > into) {
              return;
            }
            if (value < 1e-9) {
              return;
            }
            disconnectModel.add(toId(from, into), linkFitness.get(from, into));
          }
        }
    );
  }

  protected void configureConnectModel(
      final EdgeData linkFitness,
      final GenomeSoo genome,
      final int linkNum,
      final EntropySource eSource
  ) {
    connectIds.clear();
    connectModel.clear();

    int tryCount = linkNum * CONNECT_TRY_LIMIT;
    while (tryCount > 0 && connectModel.getSize() < linkNum * CONNECT_TRY_CHOICES) {
      tryCount -= 1;

      int from = eSource.nextInt(linkFitness.getSize());
      int into = symmetric && from == 0 ? 0 : eSource.nextInt(symmetric ? from : linkFitness.getSize());


      if (from == into || genome.getSolution().get(from, into) >= 1) {
        continue;
      }

      final int connectId = toId(from, into);
      if (connectIds.contains(connectId)) {
        continue;
      }

      connectIds.add(connectId);
      connectModel.add(connectId, linkFitness.get(from, into));
    }

    if (tryCount == 0 && connectModel.getSize() < linkNum * CONNECT_TRY_CHOICES) {
      log.warn(
          "configureConnectModel() exiting with {} links instead of {}",
          connectModel.getSize(),
          linkNum * CONNECT_TRY_CHOICES
      );
    }
  }

  protected abstract EdgeData buildLinkFitness(GeneticStrategySoo strategySoo, GenomeSoo genome);

  protected abstract boolean isFavoringMinimal();

  public String toString() {
    return "[ " + this.getClass().getSimpleName() + " ]";
  }
}
