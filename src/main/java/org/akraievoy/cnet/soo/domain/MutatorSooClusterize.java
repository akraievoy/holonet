/*
 Copyright 2013 Anton Kraievoy akraievoy@gmail.com
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

import org.akraievoy.cnet.gen.vo.EntropySource;
import org.akraievoy.cnet.net.vo.EdgeData;
import org.akraievoy.cnet.opt.api.GeneticState;
import org.akraievoy.cnet.opt.api.GeneticStrategy;
import org.akraievoy.cnet.opt.api.Mutator;

import java.util.*;

public class MutatorSooClusterize implements Mutator<GenomeSoo> {
  protected int clusterNum = 2;

  public MutatorSooClusterize() {
  }

  public void setClusterNum(int clusterNum) {
    this.clusterNum = clusterNum;
  }

  public void mutate(
      GeneticStrategy geneticStrategy,
      GenomeSoo child,
      GeneticState state,
      EntropySource eSource
  ) {
    final GeneticStrategySoo strategy = (GeneticStrategySoo) geneticStrategy;
    if (!strategy.mode(GeneticStrategySoo.MODE_REGULAR)) {
      return;
    }

    final EdgeData solution = child.getSolution();
    int rewireLimit = (int) Math.ceil(
        solution.getNonDefCount() * state.getMutateRatio()
    );

    apply(
        assignNodeToClusters(solution),
        rewireLimit,
        solution
    );
  }

  public int apply(
      final Map<Integer, Integer> nodeToCluster,
      final int rewireLimitOuter,
      final EdgeData solution
  ) {
    final int size = solution.getSize();

    final double[] clusterMinPowVal = new double[clusterNum];
    final int[] clusterMinPowPos = new int[clusterNum];
    final double[] interclusterLinkCount = new double[size];
    final double[] clusterCapacity = new double[size];
    boolean moarRewires;
    int rewireLimit = rewireLimitOuter;
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

      moarRewires = rewireLimit > 0 && icLinkMaxVal > 0;
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
          rewireLimit -= 1;
          final double delta = solution.get(nodeRewire, nodeDonor);
          solution.set(nodeRewire, nodeDonor, 0);
          solution.set(nodeRewire, nodeReceiver, delta);
        } else {
          moarRewires = false;
        }
      }
    } while (moarRewires);

    return rewireLimit;
  }

  protected Map<Integer, Integer> assignNodeToClusters(
      final EdgeData solution
  ) {
    final int size = solution.getSize();

    final Set<Integer> starRoots = new TreeSet<Integer>();
    for (int node = 0; node < size; node++) {
      if (solution.power(node) * 3 > size * 2) {
        starRoots.add(node);
      }
    }

    final Map<Integer, Integer> nodeToCluster =
        new HashMap<Integer, Integer>();

    int starCluster = 0;
    int coronaCluster = 0;
    for (int node = 0; node < size; node++) {
      if (starRoots.contains(node)) {
        nodeToCluster.put(node, starCluster);
        starCluster = ( starCluster + 1 ) % clusterNum;
      } else {
        nodeToCluster.put(node, coronaCluster);
        coronaCluster = ( coronaCluster + 1 ) % clusterNum;
      }
    }
    return nodeToCluster;
  }

  public String toString() {
    return "[ " + this.getClass().getSimpleName() + " ]";
  }
}
