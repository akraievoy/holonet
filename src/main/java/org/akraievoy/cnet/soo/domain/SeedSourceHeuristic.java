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

import algores.holonet.core.ProgressSimple;
import org.akraievoy.cnet.metrics.api.Metric;
import org.akraievoy.cnet.metrics.domain.MetricEDataRouteLen;
import org.akraievoy.cnet.metrics.domain.MetricRoutesFloydWarshall;
import org.akraievoy.holonet.exp.store.RefObject;
import org.akraievoy.cnet.metrics.domain.MetricScalarEigenGap;
import org.akraievoy.cnet.net.vo.EdgeData;
import org.akraievoy.cnet.net.vo.EdgeDataFactory;
import org.akraievoy.cnet.opt.api.GeneticStrategy;
import org.akraievoy.cnet.opt.api.SeedSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.akraievoy.cnet.net.Net.*;
import static org.akraievoy.util.Cruft.*;

public class SeedSourceHeuristic implements SeedSource<GenomeSoo> {
  private static final Logger log = LoggerFactory.getLogger(SeedSourceHeuristic.class);

  protected final MetricEDataRouteLen metricRouteLen = new MetricEDataRouteLen(new MetricRoutesFloydWarshall());
  protected final MutatorSooClusterize clusterize = new MutatorSooClusterize();

  public List<GenomeSoo> getSeeds(GeneticStrategy strategy) {
    final GeneticStrategySoo strategySoo = (GeneticStrategySoo) strategy;
    final Map<Integer, GenomeSoo> range = generateSeedRange(strategySoo);

    return new ArrayList<GenomeSoo>(range.values());
  }

  protected Map<Integer, GenomeSoo> generateSeedRange(
      final GeneticStrategySoo strategy
  ) {
    final EdgeData dist = strategy.getDistSource().getValue();
    final EdgeData reqSum = eSparseSymSum(strategy.getRequestSource().getValue());
    final double[] reqMinMax = eMinMax(reqSum);

    final int size = dist.getSize();
    final int nodeLinkLowerLimit = strategy.getNodeLinkLowerLimit();
    final int nodeLinkUpperLimit = strategy.getNodeLinkUpperLimit();
    final int totalLinkUpperLimit = (int) Math.floor(strategy.getTotalLinkUpperLimit());

    final EdgeData solution = EdgeDataFactory.sparse(true, 0.0, size);
    metricRouteLen.getRoutes().setDistSource(strategy.getDistSource());
    metricRouteLen.getRoutes().setSource(new RefObject<EdgeData>(solution));

    final List<LinkCandidate> links = new ArrayList<LinkCandidate>(size * (size - 1));
    final int[] nodePowers = new int[size];
    int linksToAdd = totalLinkUpperLimit;
    final ProgressSimple progress = new ProgressSimple(totalLinkUpperLimit).start();
    while (linksToAdd > 0) {
      final int[] minNodePowers = min2(nodePowers);
      final int bestPowerSum = minNodePowers[0] + minNodePowers[1];
      final EdgeData routeLen = Metric.fetch(metricRouteLen);

      links.clear();
      final Fun1<Integer, Boolean> linkable =
          powerToLinkable(nodeLinkLowerLimit, nodeLinkUpperLimit, minNodePowers[0]);

      for (int from = 1; from < size; from++) {
        final int fromPower = nodePowers[from];
        if (!linkable.apply(fromPower)) {
          continue;
        }

        for (int into = 0; into < from; into++) {
          final int intoPower = nodePowers[into];
          final int powerSum = fromPower + intoPower;
          if (bestPowerSum < powerSum) {
            continue;
          }

          if (!linkable.apply(intoPower) || solution.conn(from, into)) {  //  TODO performance (pre-load connected intos for current from)
            continue;
          }

          final double effDelta = effChangeForLink(reqSum, routeLen, reqMinMax, from, into, dist.get(from, into));
          final double bestEffDelta = links.isEmpty() ? 0.0 : links.get(0).effDelta;
          if (effDelta > bestEffDelta) {
            links.clear();
          }
          if (bestEffDelta <= effDelta) {
            links.add(
                new LinkCandidate(from, into, effDelta, powerSum)
            );
          }
        }
      }

      if (links.isEmpty()) {
        throw new IllegalStateException("no valid link candidates: " + linksToAdd + " links pending, powers: " + Arrays.toString(nodePowers));
      }
      Collections.sort(links);

      final LinkCandidate link = links.get(links.size() - 1);
      solution.set(link.from, link.into, 1);
      nodePowers[link.from] += 1;
      nodePowers[link.into] += 1;
      linksToAdd -= 1;

      final int linksAdded = totalLinkUpperLimit - linksToAdd;
      log.info(
          "effDelta {} with {} choices --- {}",
          new Object[]{link.effDelta, links.size(), progress.iter(linksAdded)}
      );
    }

    final MetricScalarEigenGap eigenGapScalar = new MetricScalarEigenGap();
    eigenGapScalar.setSource(new RefObject<EdgeData>(solution));
    final double eigenGap = Metric.fetch(eigenGapScalar);
    log.info("initial seed eigengap = {}", eigenGap);

    final Map<Integer, GenomeSoo> seedRange =
        new HashMap<Integer, GenomeSoo>();
    storeSeed(seedRange, solution, eigenGapScalar);

    final Map<Integer, Integer> nodeToCluster =
        clusterize.assignNodeToClusters(solution);
    while (clusterize.apply(nodeToCluster, 1, solution, dist) == 0) {
      storeSeed(seedRange, solution, eigenGapScalar);
    }

    //  TODO this does not separate clusters completely, stars remain congested
    //    but eigengap is low enough (0.017), so that last zero-slot may be filled too
    if (!seedRange.containsKey(0)) {
      seedRange.put(0, new GenomeSoo(solution));
    }

    return seedRange;
  }

  private Fun1<Integer, Boolean> powerToLinkable(
      final int nodeLinkLowerLimit,
      final int nodeLinkUpperLimit,
      final int minNodePower
  ) {
    return new Fun1<Integer, Boolean>() {
      @Override
      public Boolean apply(final Integer power) {
        return
            power < nodeLinkLowerLimit ||
            power < nodeLinkUpperLimit && minNodePower >= nodeLinkLowerLimit;
      }
    };
  }

  protected static boolean storeSeed(
      final Map<Integer, GenomeSoo> seedRange,
      final EdgeData solution,
      final MetricScalarEigenGap eigenGapScalar
  ) {
    eigenGapScalar.setSource(new RefObject<EdgeData>(solution));
    final double eigenGap = Metric.fetch(eigenGapScalar);

    //  unconnected network has eigengap of 1e-15, so...
    final int rangePos = (int) Math.ceil(eigenGap * 20 - 1e-9);
/*
    System.out.printf("!!! eigenGap = %g, rangePos = %d %n", eigenGap, rangePos);
*/
    if (seedRange.containsKey(rangePos)) {
      return false;
    } else {
      seedRange.put(rangePos, new GenomeSoo(eClone(solution)));
      return true;
    }
  }

  protected static double effChangeForLink(
      final EdgeData reqSum, final EdgeData routeLen, final double[] reqMinMax,
      final int n2, final int n3, final double newLen23
  ) {
    final double oldLen23 = routeLen.get(n2, n3);
    if (oldLen23 <= newLen23) {
      return 0;
    }

    final double fakeDirectRequest = reqMinMax[0] / reqMinMax[1];
    final double[] effDeltaTotal = { effDelta(oldLen23, newLen23) * fakeDirectRequest};

    reqSum.visitNonDef(new EdgeData.EdgeVisitor() {
      @Override
      public void visit(final int n1, final int n4, final double reqSum) {
        final double prevLen14 = routeLen.get(n1, n4);
        if (newLen23 >= prevLen14) {
          return;
        }

        final double len12 = routeLen.get(n1, n2);
        final double len13 = routeLen.get(n1, n3);
        if (len12 + newLen23 >= prevLen14 && len13 + newLen23 >= prevLen14) {
          return;
        }

        final double len34 = routeLen.get(n3, n4);
        final double len24 = routeLen.get(n2, n4);
        final double newLen1234 = len12 + newLen23 + len34;
        final double newLen1324 = len13 + newLen23 + len24;
        final double newLen14 = newLen1234 < newLen1324 ? newLen1234 : newLen1324;
        if (newLen14 >= prevLen14) {
          return;
        }

        effDeltaTotal[0] += effDelta(prevLen14, newLen14) * reqSum;
      }
    });

    return effDeltaTotal[0];
  }

  protected static double effDelta(final double oldLen, final double newLen) {
    final double effDelta = Math.pow(newLen, -1) - Math.pow(oldLen, -1);

    if (effDelta < 0) {
      throw new IllegalStateException(
          "negative effDelta " + effDelta + " for newLen " + newLen + " and oldLen " + oldLen
      );
    }

    return effDelta;
  }
}

class LinkCandidate implements Comparable<LinkCandidate>{
  final int from;
  final int into;
  final double effDelta;
  final int powerSum;

  LinkCandidate(final int from0, final int into0, final double effDelta0, final int powerSum0) {
    effDelta = effDelta0;
    from = from0;
    into = into0;
    powerSum = powerSum0;
  }

  @Override
  public int compareTo(final LinkCandidate o) {
    //  favor less connected nodes
    final int powerSumCompare = intCompare(o.powerSum, powerSum);
    if (powerSumCompare != 0) {
      return powerSumCompare;
    }

    //  within ties by minimal power favor ones delivering most efficiency
    final int deltaCompare = Double.compare(effDelta, o.effDelta);

    return deltaCompare;
  }
}