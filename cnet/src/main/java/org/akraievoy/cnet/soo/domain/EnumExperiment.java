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

import org.akraievoy.base.Format;
import org.akraievoy.base.ObjArrays;
import org.akraievoy.base.Parse;
import org.akraievoy.base.runner.api.*;
import org.akraievoy.base.runner.vo.Parameter;
import org.akraievoy.cnet.metrics.domain.MetricScalarEigenGap;
import org.akraievoy.cnet.net.ref.RefEdgeData;
import org.akraievoy.cnet.net.vo.EdgeData;
import org.akraievoy.cnet.net.vo.EdgeDataFactory;
import org.apache.log4j.BasicConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;

public class EnumExperiment implements Runnable, ContextInjectable, StandaloneIterator {
  private static final Logger log = LoggerFactory.getLogger(EnumExperiment.class);

  protected Context ctx;

  protected RefLong sizeRef = new RefLong(8);
  protected RefDouble thetaRef = new RefDouble(1.5);
  protected RefDouble thetaTildeRef = new RefDouble(1);
  protected RefDouble lambdaRef = new RefDouble(0.2);
  protected String lambdaParamName;

  public void setCtx(Context ctx) {
    this.ctx = ctx;
  }

  public void setLambdaRef(RefDouble lambdaRef) {
    this.lambdaRef = lambdaRef;
  }

  public void setSizeRef(RefLong sizeRef) {
    this.sizeRef = sizeRef;
  }

  public void setThetaRef(RefDouble thetaRef) {
    this.thetaRef = thetaRef;
  }

  public void setThetaTildeRef(RefDouble thetaTildeRef) {
    this.thetaTildeRef = thetaTildeRef;
  }

  public void setLambdaParamName(String lambdaParamName) {
    this.lambdaParamName = lambdaParamName;
  }

  public List<String> getIteratedParamNames() {
    return lambdaParamName != null ? Collections.singletonList(lambdaParamName) : Collections.<String>emptyList();
  }

  public void run() {
    if (ctx == null) {
      BasicConfigurator.configure();
    }

    final int size = sizeRef.getValue().intValue();
    final int len = size * (size - 1) / 2;

    final int totalLinks = GeneticStrategySoo.getTotalLinkUpperLimit(size, thetaRef.getValue());
    final int nodeLinks = GeneticStrategySoo.getNodeLinkLowerLimit(size, thetaTildeRef.getValue());

    final BitSet links = new BitSet();
    links.set(0, totalLinks, true);

    final double[] lambdas;

    if (ctx != null) {
      final Parameter lambdaParam;
      if (lambdaParamName != null) {
        final ParamSetEnumerator pse = ctx.getEnumerator();
        lambdaParam = pse.getParameter(pse.getParameterIndex(lambdaParamName));

        lambdas = ObjArrays.unbox(ObjArrays.remove(Parse.doubles(lambdaParam.getValues(), null), null));
      } else {
        lambdas = new double[]{lambdaRef.getValue()};
      }
    } else {
      lambdas = new double[]{0.25, 0.5, 0.75, 0.875, 0.9325, 0.96875};
    }

    final BigInteger totalSets = BigInteger.valueOf(2).pow(len);
    BigInteger sparceSets = BigInteger.ZERO;
    BigInteger c_from_len_by_ctl = BigInteger.ONE;
    for (int curTotalLinks = 0; curTotalLinks <= totalLinks; curTotalLinks++) {
      if (curTotalLinks > 0) {
        c_from_len_by_ctl =
            c_from_len_by_ctl.
                multiply(BigInteger.valueOf(len + 1 - curTotalLinks)).
                divide(BigInteger.valueOf(curTotalLinks));
      }

      sparceSets = sparceSets.add(c_from_len_by_ctl);
    }

    long exactSparceSetsExpected = c_from_len_by_ctl.longValue();
    long exactSparceSets = 0;
    long regularSets = 0;
    final long[] eigenSets = new long[lambdas.length];

    log.info("len (max links) = {}", len);
    log.info("totalLinks (upper limit) = {}", totalLinks);
    log.info("nodeLinks = {}", nodeLinks);

    log.info("totalSets = {}", totalSets);
    log.info("sparceSets = {}", sparceSets);
    log.info("exactSparceSets = {}", exactSparceSetsExpected);

    int idx;
    int[] powers = new int[size];

    final EdgeData eData = EdgeDataFactory.dense(true, 0, size);
    final MetricScalarEigenGap mseg = new MetricScalarEigenGap();

    mseg.setSource(new RefEdgeData(eData));

    final long firstStatus = System.currentTimeMillis();
    long lastStatus = firstStatus;
    byte timeThrottle = 0;
    while (true) {
      long nextPerm = exactSparceSets == 0 ? 1 : nextPermExact(len, links, totalLinks);
      if (nextPerm <= 0) {
        break;
      }
      if (exactSparceSets >= exactSparceSetsExpected) {
        throw new IllegalStateException("still enumerating sets?");
      }

      exactSparceSets += 1;
      if (timeThrottle++ == 0) {
        final long now = System.currentTimeMillis();
        if (now - lastStatus >= 60 * 1000) {
          final double percentage = Math.round(10000.0 * exactSparceSets / exactSparceSetsExpected) / 100.0;
          log.debug(
              "completed {}%, ETA {}",
              percentage,
              Format.formatDuration((long) ((now - firstStatus) * (100.0 - percentage) / percentage))
          );
          lastStatus = now;
        }
      }

      Arrays.fill(powers, 0);
      int f = 0, t = 1;
      for (idx = 0; idx < len; idx++) {
        if (links.get(idx)) {
          powers[f]++;
          powers[t]++;
        }

        t++;
        if (t == size) {
          f++;
          t = f + 1;
        }
      }

      boolean regular = true;
      for (int i = 0; i < size; i++) {
        if (powers[i] < nodeLinks) {
          regular = false;
          break;
        }
      }

      if (!regular) {
        continue;
      }

      regularSets++;

      eData.clear();
      f = 0;
      t = 1;
      for (idx = 0; idx < len; idx++) {
        if (links.get(idx)) {
          eData.set(f, t, 1);
        }

        t++;
        if (t == size) {
          f++;
          t = f + 1;
        }
      }

      mseg.run();
      final double eg = mseg.getTarget().getValue();
      for (int ei = 0; ei < lambdas.length; ei++) {
        if (eg >= lambdas[ei]) {
          eigenSets[ei]++;
        }
      }
    }

    if (exactSparceSets < exactSparceSetsExpected) {
      throw new IllegalStateException("should still enumerate: stopped on " + exactSparceSets + " instead of " + exactSparceSetsExpected);
    }


    if (ctx != null) {
      final String[] paramParam = {lambdaParamName};
      for (int ei = 0; ei < lambdas.length; ei++) {
        final int[] paramOffs = {ei};

        ctx.put("len", len, paramParam, paramOffs);
        ctx.put("totalLinks", totalLinks, paramParam, paramOffs);
        ctx.put("nodeLinks", nodeLinks, paramParam, paramOffs);

        ctx.put("totalSets", totalSets, paramParam, paramOffs);
        ctx.put("sparceSets", sparceSets, paramParam, paramOffs);
        ctx.put("exactSparceSets", exactSparceSetsExpected, paramParam, paramOffs);
        ctx.put("regularSets", regularSets, paramParam, paramOffs);
        ctx.put("eigenSets", eigenSets, paramParam, paramOffs);
      }
    }

    log.info("regularSets = {}", regularSets);
    for (int ei = 0; ei < lambdas.length; ei++) {
      log.info("eigenSets (eigengap >= {}) = {}", lambdas[ei], eigenSets[ei]);
    }
  }

  protected static long nextPermExact(int len, BitSet links, int totalLinks) {
    if (links.cardinality() != totalLinks) {
      throw new IllegalStateException("Incorrect number of links");
    }
    //	BA9876543210	sz	fo
    //	000101111100	7	4
    //	....>......<
    //	000110001111	4	3
    //	.......>....
    //	000110010111

    int shiftedZero;
    int filledOnes;
    if (links.get(0)) {
      shiftedZero = links.nextClearBit(0);
      filledOnes = shiftedZero - 1;
    } else {
      final int nsb = links.nextSetBit(0);
      final int ncb = links.nextClearBit(nsb);
      shiftedZero = ncb;
      filledOnes = ncb - nsb - 1;
    }

    if (shiftedZero >= len) {
      return -1;
    }

    links.set(0, shiftedZero, false);
    links.set(shiftedZero, true);
    links.set(0, filledOnes, true);

    return 1;
  }

  public static void main(String[] args) {
/*
		final int size = 10;
		final int num = 7;
		BitSet test = new BitSet();
		test.set(0, num, true);

		do {
			for (int i = 0; i < size; i++) {
				System.out.print(test.get(size - 1 - i) ? '1' : '0');
			}
			System.out.println();
		} while (nextPermExact(size, test, num) > 0);
*/
    new EnumExperiment().run();
  }
}