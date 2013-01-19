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

package org.akraievoy.cnet.gen.domain;

import org.akraievoy.base.Die;
import org.akraievoy.cnet.metrics.api.MetricEData;
import org.akraievoy.cnet.net.vo.EdgeData;
import org.akraievoy.cnet.net.vo.EdgeDataFactory;

import java.util.BitSet;

public class MetricEDataGenStructural extends MetricEData {
  public static final String TYPE_STAR = "star";
  public static final String TYPE_PATH = "path";
  public static final String TYPE_CYCLE = "cycle";
  public static final String TYPE_EMPTY = "empty";
  public static final String TYPE_FULL = "full";
  public static final String TYPE_CONSTANT = "constant";
  public static final String TYPE_PALEY = "paley";
  public static final String TYPE_MARGULIS = "margulis";

  protected String type;
  protected int netNodeNum;

  public MetricEDataGenStructural() {
    this(TYPE_PATH, 3);
  }

  public MetricEDataGenStructural(String type, int netNodeNum) {
    this.netNodeNum = netNodeNum;
    this.type = type;
  }

  public MetricEDataGenStructural(String type) {
    this.type = type;
  }

  public void setNetNodeNum(int netNodeNum) {
    this.netNodeNum = netNodeNum;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getName() {
    return "Factory: " + netNodeNum + "-node " + type;
  }

  public void run() {
    if (TYPE_STAR.equals(type)) {
      target.setValue(star(netNodeNum));
    } else if (TYPE_PATH.equals(type)) {
      target.setValue(path(netNodeNum));
    } else if (TYPE_CYCLE.equals(type)) {
      target.setValue(cycle(netNodeNum));
    } else if (TYPE_EMPTY.equals(type)) {
      target.setValue(empty(netNodeNum));
    } else if (TYPE_FULL.equals(type)) {
      target.setValue(full(netNodeNum));
    } else if (TYPE_CONSTANT.equals(type)) {
      target.setValue(EdgeDataFactory.constant(netNodeNum, 1.0));
    } else if (TYPE_PALEY.equals(type)) {
      target.setValue(paley(netNodeNum));
    } else if (TYPE_MARGULIS.equals(type)) {
      target.setValue(margulis(netNodeNum));
    } else {
      throw Die.unexpected("type", type);
    }
  }

  public static EdgeData empty(int netSize) {
    return EdgeDataFactory.sparse(true, 0.0, netSize);
  }

  public static EdgeData path(int netSize) {
    final EdgeData result = empty(netSize);

    for (int i = 0; i < netSize - 1; i++) {
      result.set(i, i + 1, 1.0);
    }

    return result;
  }

  public static EdgeData star(int netSize) {
    final EdgeData result = empty(netSize);

    for (int i = 1; i < netSize; i++) {
      result.set(0, i, 1.0);
    }

    return result;
  }

  public static EdgeData cycle(int netSize) {
    final EdgeData result = path(netSize);

    result.set(0, netSize - 1, 1.0);

    return result;
  }

  public static EdgeData full(int netSize) {
    return EdgeDataFactory.sparse(true, 1.0, netSize);
  }

  public static EdgeData paley(final int netSize) {
    if (!isPrime(netSize) || netSize % 4 != 1) {
      throw new IllegalStateException("netSize should be prime, equal to 1 mod 4");
    }

    final EdgeData result = empty(netSize);

    final BitSet residues = precompResidues(netSize);

    for (int from = 0; from < netSize - 1; from++) {
      for (int into = from + 1; into < netSize; into++) {
        if (residues.get(into - from)) {
          result.set(from, into, 1.0);
        }
      }
    }

    return result;
  }

  protected static BitSet precompResidues(int modulo) {
    BitSet residueFlags = new BitSet();

    for (int i = 0; i < modulo; i++) {
      residueFlags.set(i * i % modulo, true);
    }

    return residueFlags;
  }

  protected static boolean isPrime(final int num) {
    if (num <= 3) {
      return true;
    }

    if (num % 2 == 0 || num % 3 == 0) {
      return false;
    }

    int[] diff = {2, 4};
    int diffPos = diff.length - 1;
    final double divisorLim = Math.sqrt(num);
    for (int divisor = 5;
         divisor <= divisorLim;
         divisor += diff[diffPos = (diffPos + 1) % diff.length]) {
      if (num % divisor == 0) {
        return false;
      }
    }

    return true;
  }

  public static EdgeData margulis(final int netSize) {
    Die.ifFalse("isSquare(netSize)", isSquare(netSize));
    final int root = (int) Math.floor(Math.sqrt(netSize));

    final EdgeData result = empty(netSize);

    for (int u = 0; u < netSize - 1; u++) {
      for (int v = u; v < netSize; v++) {

        int edges = countEdges(root, u, v) + countEdges(root, v, u);

        if (edges > 0) {
          result.set(u, v, (double) edges);
        }
      }
    }

    return result;
  }

  protected static int countEdges(int root, int u, int v) {
    int edges = 0;

    final int uX = u / root;
    final int uY = u % root;
    final int vX = v / root;
    final int vY = v % root;

    if (vX == uX && vY == (uY + 2 * uX) % root) {
      edges++;
    }
    if (vX == uX && vY == (uY + 2 * uX + 1) % root) {
      edges++;
    }
    if (vX == uX && vY == (uY + 2 * uX + 2) % root) {
      edges++;
    }
    if (vX == (uX + 2 * uY) % root && vY == uY) {
      edges++;
    }
    if (vX == (uX + 2 * uY + 1) % root && vY == uY) {
      edges++;
    }
    if (vX == (uX + 2 * uY + 2) % root && vY == uY) {
      edges++;
    }

    return edges;
  }

  public static boolean isSquare(final int num) {
    final double sqrt = Math.sqrt(num);
    return sqrt - Math.floor(sqrt) == 0;
  }
}
