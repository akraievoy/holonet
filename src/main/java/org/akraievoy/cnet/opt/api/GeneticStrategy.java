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
import org.akraievoy.cnet.opt.domain.FitnessKey;

import java.util.SortedMap;

public interface GeneticStrategy<G extends Genome> {
  /**
   * Set up some static data, which shall be cached across the whole genetics experiment.
   *
   * @param ctx to load your data from
   * @param generationParam the name of generation axis
   */
  void init(Context ctx, String generationParam);

  double computeFitness(G genome);

  G createGenome();

  void initOnSeeds(Context ctx, String generationParamName, SortedMap<FitnessKey, G> children);
}
