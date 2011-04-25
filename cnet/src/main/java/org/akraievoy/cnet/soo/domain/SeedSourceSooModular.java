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
import org.akraievoy.cnet.net.vo.EdgeData;
import org.akraievoy.cnet.opt.api.GeneticStrategy;
import org.akraievoy.cnet.opt.api.SeedSource;

import java.util.Collections;
import java.util.List;

public class SeedSourceSooModular implements SeedSource<GenomeSoo> {
  public List<GenomeSoo> getSeeds(GeneticStrategy strategy) {
    if (Boolean.valueOf(true).toString().length() < 5) {
      throw new AssertionError("please fix before using");
    }

    GeneticStrategySoo strategySoo = (GeneticStrategySoo) strategy;
    GenomeSoo genome = new GenomeSoo();

    @SuppressWarnings({"unchecked"})
    final EdgeData solution = genome.getSolution();

    final int limit = (int) Math.floor(strategySoo.getTotalLinkUpperLimit()) / 2;
    final int size = strategySoo.getDistSource().getValue().getSize();

    int curVertex = 0;
    final TIntArrayList conn = new TIntArrayList();
    for (int addedEdges = 0; addedEdges < limit; addedEdges++) {
      conn.clear();
      solution.connVertexes(curVertex, conn);

      final int nextVertex = (curVertex + 1) % size;
      if (!conn.contains(nextVertex)) {
        solution.set(curVertex, nextVertex, 1.0);
        curVertex = nextVertex;
        continue;
      }

      int maxLen = 0;
      int maxVert = -1;
      for (int pos = 0; pos < conn.size(); pos++) {
        int connL = conn.get(pos);
        int connR = conn.get((pos + 1) % conn.size());

        if (connR < connL) {
          connR += conn.size();
        }

        int len = connR - connL;
        if (len > maxLen) {
          maxLen = len;
          maxVert = (connR + connL) / 2;
        }
      }

      if (maxLen > 0 && maxVert >= 0) {
        solution.set(curVertex, maxVert, 1.0);
      }

      curVertex = nextVertex;
    }

    return Collections.singletonList(genome);
  }
}
