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
import org.akraievoy.cnet.metrics.api.Metric;
import org.akraievoy.holonet.exp.store.RefObject;
import org.akraievoy.cnet.metrics.domain.MetricScalarEigenGap;
import org.akraievoy.cnet.net.vo.EdgeData;
import org.akraievoy.cnet.net.vo.EdgeDataFactory;
import org.akraievoy.cnet.opt.api.GeneticStrategy;
import org.akraievoy.cnet.opt.api.SeedSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
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

    final Map<Integer, GenomeSoo> range = generateSeed(links, size, dist, req);

    return new ArrayList<GenomeSoo>(range.values());
  }

  protected Map<Integer, GenomeSoo> generateSeed(
      int limit, int size, EdgeData dist, EdgeData req
  ) {
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
    final double eigenGap = Metric.fetch(eigenGapScalar);
    log.info("initial seed eigengap = {}", eigenGap);

    final Map<Integer, GenomeSoo> seedRange =
        new HashMap<Integer, GenomeSoo>();
    storeSeed(seedRange, solution, eigenGapScalar);

    final int clusterNum = 2;
    final Map<Integer, Integer> nodeToCluster =
        assignNodeToClusters(size, starRoots, clusterNum);

    final double[] clusterMinPowVal = new double[clusterNum];
    final int[] clusterMinPowPos = new int[clusterNum];
    final double[] interclusterLinkCount = new double[size];
    final double[] clusterCapacity = new double[size];
    boolean moarRewires;
    do {
      Arrays.fill(interclusterLinkCount, 0);
      Arrays.fill(clusterCapacity, 0);
      for (int nodeFrom = 0; nodeFrom < size; nodeFrom++) {
        for (int nodeInto = 0; nodeInto < size; nodeInto++) {
          if (nodeToCluster.get(nodeFrom).equals(nodeToCluster.get(nodeInto))) {
            double link = solution.get(nodeFrom, nodeInto);
            if (nodeFrom != nodeInto && link < 1.0) {
              clusterCapacity[nodeFrom] += 1 - link;
            }
            continue;
          }
          if (!solution.conn(nodeFrom, nodeInto)) {
            continue;
          }
          interclusterLinkCount[nodeFrom] += solution.get(nodeFrom, nodeInto);
        }
      }

      Arrays.fill(clusterMinPowPos, -1);
      Arrays.fill(clusterMinPowVal, Double.POSITIVE_INFINITY);
      double icLinkMaxVal = Double.NEGATIVE_INFINITY;
      int icLinkMaxPos = -1;
      for (int node = 0; node < size; node++) {
        double nodeICLinks = interclusterLinkCount[node];
        if (clusterCapacity[node] > 0 && icLinkMaxVal < nodeICLinks) {
          icLinkMaxVal = nodeICLinks;
          icLinkMaxPos = node;
        }
        final double pow = solution.power(node);
        final Integer cluster = nodeToCluster.get(node);
        if (clusterMinPowVal[cluster] > pow) {
          clusterMinPowVal[cluster] = pow;
          clusterMinPowPos[cluster] = node;
        }
      }

      //  TODO this does not separate clusters completely --- stars remain congested
      moarRewires = icLinkMaxVal > 0;
      if (moarRewires) {
        @SuppressWarnings("UnnecessaryLocalVariable")
        final int nodeRewire = icLinkMaxPos;
        final int receivingCluster = nodeToCluster.get(nodeRewire);
        int nodeDonor = -1;
        int nodeReceiver = clusterMinPowPos[receivingCluster];
        if (nodeReceiver == nodeRewire || solution.conn(nodeRewire, nodeReceiver)) {
          nodeReceiver = -1;
        }
        for (int node = 0; node < size; node++) {
          if (node == nodeRewire) {
            continue;
          }
          final int nodeCluster = nodeToCluster.get(node);
          if (
              nodeDonor == -1 &&
              nodeCluster != receivingCluster &&
              solution.conn(nodeRewire, node)
          ) {
            nodeDonor = node;
          }
          if (
              nodeReceiver == -1 &&
              nodeCluster == receivingCluster &&
              !solution.conn(nodeRewire, node)
          ) {
            nodeReceiver = node;
          }
        }
        if (nodeReceiver != -1 && nodeDonor != -1) {
          double delta = solution.get(nodeRewire, nodeDonor);
          solution.set(nodeRewire, nodeDonor, 0);
          solution.set(nodeRewire, nodeReceiver, delta);
          storeSeed(seedRange, solution, eigenGapScalar);
        } else {
          moarRewires = false;
        }
      }
    } while (moarRewires);

    return seedRange;
  }

  protected static Map<Integer, Integer> assignNodeToClusters(
      final int size,
      final BitSet starRoots,
      final int clusterNum
  ) {
    final Map<Integer, Integer> nodeToCluster =
        new HashMap<Integer, Integer>();
    int starCluster = 0;
    int coronaCluster = 0;
    for (int node = 0; node < size; node++) {
      if (starRoots.get(starCluster)) {
        nodeToCluster.put(node, starCluster);
        starCluster = ( starCluster + 1 ) % clusterNum;
      } else {
        nodeToCluster.put(node, coronaCluster);
        coronaCluster = ( coronaCluster + 1 ) % clusterNum;
      }
    }
    return nodeToCluster;
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
    System.out.printf("!!! eigenGap = %g, rangePos = %d %n", eigenGap, rangePos);
    if (seedRange.containsKey(rangePos)) {
      return false;
    } else {
      final EdgeData solutionClone = solution.proto(solution.getSize());
      solution.visitNonDef(
          new EdgeData.EdgeVisitor() {
            @Override
            public void visit(int from, int into, double e) {
              solutionClone.set(from, into, e);
            }
          }
      );
      seedRange.put(rangePos, new GenomeSoo(solutionClone));
      return true;
    }
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