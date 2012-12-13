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

import java.util.ArrayList;
import java.util.List;

/**
 * Sequentially apply some number of mutations.
 */
public class MutatorChain<G extends Genome> implements Mutator<G> {
  protected List<Mutator<G>> chain = new ArrayList<Mutator<G>>();

  public void mutate(GeneticStrategy strategy, G child, GeneticState state, EntropySource eSource) {
    for (Mutator<G> m : chain) {
      m.mutate(strategy, child, state, eSource);
    }
  }

  public void setChain(List<Mutator<G>> chain) {
    this.chain.clear();
    this.chain.addAll(chain);
  }

  public String toString() {
    final StringBuilder res = new StringBuilder();

    for (Mutator<G> m : chain) {
      res.append(m.toString());
    }

    return res.toString();
  }
}
