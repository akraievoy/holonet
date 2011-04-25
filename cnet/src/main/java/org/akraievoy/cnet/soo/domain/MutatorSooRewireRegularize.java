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
import org.akraievoy.cnet.net.vo.EdgeData;
import org.akraievoy.cnet.opt.api.GeneticState;
import org.akraievoy.cnet.opt.api.GeneticStrategy;
import org.akraievoy.cnet.opt.api.Mutator;
import org.akraievoy.gear.G4Trove;

class MutatorSooRegularize implements Mutator<GenomeSoo> {
  int tolerance = 1;

  public MutatorSooRegularize() {
  }

  public void setTolerance(int tolerance) {
    this.tolerance = tolerance;
  }

  public void mutate(GeneticStrategy strategy, GenomeSoo child, GeneticState state, EntropySource eSource) {
    final GeneticStrategySoo stra = (GeneticStrategySoo) strategy;
    final int limit = stra.getNodeLinkLowerLimit();

    final EdgeData data = child.getSolution();
    final int size = data.getSize();

    final TIntArrayList v2rem = new TIntArrayList();
    final TIntArrayList v2add = new TIntArrayList();
    final TIntArrayList temp = new TIntArrayList();
    final TIntArrayList temp2 = new TIntArrayList();
    final TIntArrayList all = new TIntArrayList();
    G4Trove.listAll(all, size);
    int[] fails = {0};

    while (buildAddRemSets(data, size, limit, v2rem, v2add) && fails[0] < tolerance * size) {
      final int i2rem = v2rem.get(eSource.nextInt(v2rem.size()));
      temp2.clear();
      final TIntArrayList remConn_temp2 = data.connVertexes(i2rem, temp2);

      //	list links of this i2rem vertex to other over-connected vertexes
      G4Trove.union(remConn_temp2, v2rem, temp);
      if (temp.isEmpty()) {
        //	keep the other site connected, but rewire from i2rem to any other node not in v2rem list
        final int j = remConn_temp2.get(eSource.nextInt(remConn_temp2.size()));

        rewire(eSource, data, v2rem, temp, temp2, all, fails, i2rem, j, j);
      } else {
        //	select another pair of nodes to rewire this link
        final int j2rem = temp.get(eSource.nextInt(temp.size()));
        final int j2add = v2add.get(eSource.nextInt(v2add.size()));

        rewire(eSource, data, v2rem, temp, temp2, all, fails, i2rem, j2rem, j2add);
      }
    }
  }

  protected void rewire(
      EntropySource eSource, EdgeData data, TIntArrayList v2rem,
      TIntArrayList temp, TIntArrayList temp2, TIntArrayList all,
      int[] fails,
      int i2rem, int j2rem, int j2add) {
    temp2.clear();
    final TIntArrayList jConn_temp2 = data.connVertexes(j2add, temp2);

    //	list sites not connected to i2add which are also not over-connected
    G4Trove.remove(all, jConn_temp2, temp);
    G4Trove.remove(temp, v2rem, temp2);
    if (temp2.isEmpty()) {
      fails[0]++;

    } else {
      final int i2add = temp2.get(eSource.nextInt(temp2.size()));
      data.set(i2rem, j2rem, 0);
      data.set(i2add, j2add, 1);
    }
  }

  protected boolean buildAddRemSets(EdgeData data, int size, int limit, TIntArrayList v2rem, TIntArrayList v2add) {
    v2rem.clear();
    v2add.clear();
    for (int i = 0; i < size; i++) {
      final double pow = data.power(i);
      if (pow < limit) {
        v2add.add(i);
      } else if (pow > limit) {
        v2rem.add(i);
      }
    }

    return !v2rem.isEmpty() && !v2add.isEmpty();
  }

  public String toString() {
    return "[ " + this.getClass().getSimpleName() + " ]";
  }
}
