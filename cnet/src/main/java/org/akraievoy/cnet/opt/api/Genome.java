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

package org.akraievoy.cnet.opt.api;

import org.akraievoy.base.runner.api.Context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * To maintain easy access to psolution data, Genome should consist of some number of indexed&typed components.
 * For example, some algo may use two EdgeDatas, one VertexData and a double which then might be analyzed separately.
 * <p/>
 * This type is not written to db via simple Jackson serialization, each component is typed and stored separately (which allows for the goals depicted above).
 */
public abstract class Genome {
  protected Object[] genomeData;
  public Object[] getGenomeData() { return genomeData; }
  public void setGenomeData(Object[] genomeData) { this.genomeData = genomeData; }

  protected Double fitness;
  public Double getFitness() { return fitness; }
  public void setFitness(Double fitness) { this.fitness = fitness; }

  public Genome() {
  }

  public Genome(Object[] genomeData) {
    this.genomeData = genomeData;
  }

  public boolean isDupeOf(Genome that) {
    return Arrays.deepEquals(genomeData, that.genomeData);
  }

  public abstract double similarity(Genome that);

  public void write(final double fitness, String keybase, Context ctx, Map<String, Integer> offset) {
    final String keyFitness = keybase +".fitness";
    final String keyGenome = keybase +".genome";
    final Object[] genomeArr = getGenomeData();

    ctx.put(keyFitness, fitness, offset);
    for (int i = 0, genomeLength = genomeArr.length; i < genomeLength; i++) {
      final Object genomeItem = genomeArr[i];
      ctx.put(keyGenome + i, genomeItem, offset);
    }
  }

  public Genome read(Context ctx, Map<String, Integer> offset, final String keybase) {
    final Double fitness = ctx.get(keybase + ".fitness", Double.class, offset);
    if (fitness == null) {
      return null;
    }

    this.fitness = fitness;
    final List<Object> genome = new ArrayList<Object>();
    for (int i = 0; i == genome.size(); i++) {
      final Object genomeComponent = ctx.get(keybase + ".genome" + i, Object.class, offset);

      if (genomeComponent != null) {
        genome.add(genomeComponent);
      }
    }
    final Object[] genomeArr = genome.toArray(new Object[genome.size()]);

    setGenomeData(genomeArr);

    return this;
  }
}
