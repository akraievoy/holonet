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

import org.akraievoy.base.Die;
import org.akraievoy.holonet.exp.store.RefObject;
import org.akraievoy.cnet.metrics.api.MetricResultFetcher;
import org.akraievoy.cnet.metrics.domain.MetricScalarEigenGap;
import org.akraievoy.cnet.net.vo.EdgeData;
import org.akraievoy.cnet.net.vo.EdgeDataFactory;
import org.akraievoy.cnet.opt.api.GeneticStrategy;
import org.akraievoy.cnet.opt.api.SeedSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This generates a clique of stars  and presumes your distance matrix is symmetric and conforming to triangle rule.
 */
public class SeedSourceHeuristic implements SeedSource<GenomeSoo> {
  private static final Logger log = LoggerFactory.getLogger(SeedSourceHeuristic.class);

  public List<GenomeSoo> getSeeds(GeneticStrategy strategy) {
    final GeneticStrategySoo strategySoo = (GeneticStrategySoo) strategy;

    final EdgeData dist = strategySoo.getDistSource().getValue();
    final EdgeData req = strategySoo.getRequestSource().getValue();
    final int size = dist.getSize();
    final int links = (int) Math.floor(strategySoo.getTotalLinkUpperLimit());

    final GenomeSoo genome = generateSeed(links, size, dist, req);

    return Collections.singletonList(genome);
  }

  protected GenomeSoo generateSeed(int limit, int size, EdgeData dist, EdgeData req) {
    final BitSet starRoots = new BitSet();
    final EdgeData solution = EdgeDataFactory.sparse(true, 0.0, size);
    final EdgeData minDist = EdgeDataFactory.dense(true, Double.POSITIVE_INFINITY, size);

    AtomicInteger linksToAdd = new AtomicInteger(limit);
    while (linksToAdd.get() > 0 && starRoots.cardinality() < size) {
      int starRoot = selectStarRoot(dist, req, solution, minDist, size, starRoots);

      addStar(solution, dist, minDist, size, linksToAdd, starRoot);
      starRoots.set(starRoot, true);
      log.debug(
          "added {} star roots, or {} of {} links",
          new Object[]{starRoots.cardinality(), limit - linksToAdd.get(), limit}
      );
    }

    final MetricScalarEigenGap eigenGapScalar = new MetricScalarEigenGap();
    eigenGapScalar.setSource(new RefObject<EdgeData>(solution));
    final double eigenGap = (Double) MetricResultFetcher.fetch(eigenGapScalar);
    log.info("seed eigengap = {}", eigenGap);

    return new GenomeSoo(solution);
  }

  protected static void addStar(EdgeData solution, EdgeData dist, EdgeData minDist, int size, AtomicInteger linksToAdd, int starRoot) {
    for (int perifI = 0; perifI < size - 1 && linksToAdd.get() > 0; perifI++) {
      if (perifI == starRoot) {
        continue;
      }

      double toRootMinDist = dist.get(perifI, starRoot);
      boolean toRootAdded = addLink(solution, linksToAdd, perifI, starRoot);
      if (toRootAdded) {
        toRootMinDist = updateMinDist(dist, minDist, starRoot, perifI, toRootMinDist);
      }

      for (int perifJ = perifI + 1; perifJ < size && linksToAdd.get() > 0; perifJ++) {
        if (perifJ == starRoot || solution.conn(perifI, perifJ)) {
          continue;
        }

        final boolean fromRootAdded = addLink(solution, linksToAdd, starRoot, perifJ);
        if (toRootAdded || fromRootAdded) {
          final double routeLen = toRootMinDist + dist.get(starRoot, perifJ);
          updateMinDist(dist, minDist, perifJ, perifI, routeLen);
        }
      }
    }
  }

  protected static double updateMinDist(EdgeData dist, EdgeData minDist, int from, int into, double curRouteLen) {
    final double curMinDist = dist.get(into, from);
    final double newMinDist = Math.min(curMinDist, curRouteLen);

    minDist.set(into, from, newMinDist);

    return newMinDist;
  }

  protected static boolean addLink(EdgeData solution, AtomicInteger linksToAdd, int from, int into) {
    //	this means that this set is an update
    if (solution.set(into, from, 1.0) != 1.0) {
      linksToAdd.decrementAndGet();
      return true;
    } else {
      return false;
    }
  }

  protected static int selectStarRoot(EdgeData dist, EdgeData req, EdgeData solution, EdgeData minDist, int size, BitSet starRoots) {
    int starRoot = -1;
    double starEff = 0;
    for (int rootI = 0; rootI < size; rootI++) {
      if (starRoots.get(rootI)) {
        continue;
      }

      double eff = computeEffChange(dist, req, solution, minDist, size, rootI);

      if (starRoot < 0 || (eff > starEff)) {
        starRoot = rootI;
        starEff = eff;
      }
    }

    Die.ifTrue("starRoot < 0", starRoot < 0);
    return starRoot;
  }

  protected static double computeEffChange(EdgeData dist, EdgeData req, EdgeData solution, EdgeData minDist, int size, int rootI) {
    double eff = 0;

    for (int perifI = 0; perifI < size - 1; perifI++) {
      if (perifI == rootI) {
        continue;
      }

      eff += computeEffChange(req, minDist, perifI, rootI, dist.get(perifI, rootI));

      for (int perifJ = perifI + 1; perifJ < size; perifJ++) {
        if (perifJ == rootI || solution.conn(perifI, perifJ)) {
          continue;
        }

        final double newRouteLen = dist.get(perifI, rootI) + dist.get(rootI, perifJ);
        eff += computeEffChange(req, minDist, perifI, perifJ, newRouteLen);
      }
    }

    return eff;
  }

  protected static double computeEffChange(EdgeData req, EdgeData minDist, int perifI, int perifJ, final double newRouteLen) {
    final double prevMinDist = minDist.get(perifI, perifJ);

    if (newRouteLen > prevMinDist) {
      return 0.0;
    }

    double diff = Math.pow(newRouteLen, -1) - Math.pow(prevMinDist, -1);

    if (diff > 0) {
      final double reqSum = req.get(perifI, perifJ) + req.get(perifJ, perifI);
      return reqSum * diff;
    }

    return 0.0;
  }
}