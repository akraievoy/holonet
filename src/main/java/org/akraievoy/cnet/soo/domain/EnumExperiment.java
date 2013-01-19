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
import org.akraievoy.base.ref.Ref;
import org.akraievoy.cnet.gen.vo.EntropySource;
import org.akraievoy.cnet.gen.vo.EntropySourceRandom;
import org.akraievoy.cnet.metrics.domain.MetricScalarEigenGap;
import org.akraievoy.cnet.net.vo.EdgeData;
import org.akraievoy.cnet.net.vo.EdgeDataFactory;
import org.akraievoy.holonet.exp.store.RefObject;
import org.akraievoy.holonet.exp.store.StoreLens;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.BitSet;

public class EnumExperiment implements Runnable {
  private static final Logger log = LoggerFactory.getLogger(EnumExperiment.class);

  protected static final double MAX_EVALS = 1e6;

  protected Ref<Long> sizeRef = new RefObject<Long>(8L);
  protected Ref<Double> thetaRef = new RefObject<Double>(1.5);
  protected Ref<Double> thetaTildeRef = new RefObject<Double>(1.0);
  protected Ref<Double> lambdaRef = new RefObject<Double>(0.2);
  protected EntropySource evalSource = new EntropySourceRandom();
  protected static final int TIME_THROTTLE_RANGE = -8192;

  public void setEvalSource(EntropySource evalSource) {
    this.evalSource = evalSource;
  }

  public void setLambdaRef(Ref<Double> lambdaRef) {
    this.lambdaRef = lambdaRef;
  }

  public void setSizeRef(Ref<Long> sizeRef) {
    this.sizeRef = sizeRef;
  }

  public void setThetaRef(Ref<Double> thetaRef) {
    this.thetaRef = thetaRef;
  }

  public void setThetaTildeRef(Ref<Double> thetaTildeRef) {
    this.thetaTildeRef = thetaTildeRef;
  }

  //  LATER simplify this method, as soon as the code upsets you enough
  public void run() {
    final int size = sizeRef.getValue().intValue();
    final int len = size * (size - 1) / 2;

    final int totalLinks = GeneticStrategySoo.getTotalLinkUpperLimit(size, thetaRef.getValue());
    final int nodeLinks = GeneticStrategySoo.getNodeLinkLowerLimit(size, thetaTildeRef.getValue());

    final BitSet links = new BitSet();
    links.set(0, totalLinks, true);

    final double[] lambdas;

    if (lambdaRef instanceof StoreLens) {
      final StoreLens<Double> lambdaLens = (StoreLens<Double>) lambdaRef;
      final Double[] doubleValues = (Double[]) lambdaLens.axisGetValueArr();
      lambdas = ObjArrays.unbox(doubleValues);
    } else {
      lambdas = new double[]{lambdaRef.getValue()};
    }

    final BigInteger totalSets = BigInteger.valueOf(2).pow(len);
    BigInteger sparseSets = BigInteger.ZERO;
    BigInteger c_from_len_by_totalLinks = BigInteger.ONE;
    for (int curTotalLinks = 0; curTotalLinks <= totalLinks; curTotalLinks++) {
      if (curTotalLinks > 0) {
        c_from_len_by_totalLinks =
            c_from_len_by_totalLinks.
                multiply(BigInteger.valueOf(len + 1 - curTotalLinks)).
                divide(BigInteger.valueOf(curTotalLinks));
      }

      sparseSets = sparseSets.add(c_from_len_by_totalLinks);
    }

    BigInteger exactSparseSetsExpected = c_from_len_by_totalLinks;

    BigInteger exactSparseSets = BigInteger.ZERO;
    BigInteger regularSets = BigInteger.ZERO;
    final BigInteger[] eigenSets = new BigInteger[lambdas.length];
    Arrays.fill(eigenSets, BigInteger.ZERO);

    log.info("len (max links) = {}", len);
    log.info("totalLinks (upper limit) = {} ({}% density)", totalLinks, Format.format2(100 * totalLinks / (double) len));
    log.info("nodeLinks = {}", nodeLinks);

    log.info("totalSets = {}", totalSets);
    log.info("sparseSets = {} ({}% of total)", sparseSets, percentageStr(sparseSets, totalSets));
    log.info("exactSparseSets = {} ({}% of sparse)", exactSparseSetsExpected, percentageStr(exactSparseSetsExpected, sparseSets));

    int idx;
    int[] powers = new int[size];

    final EdgeData eData = EdgeDataFactory.dense(true, 0, size);
    final MetricScalarEigenGap mseg = new MetricScalarEigenGap();

    mseg.setSource(new RefObject<EdgeData>(eData));

    final long firstStatus = System.currentTimeMillis();
    long lastStatus = firstStatus;
    int timeThrottle = TIME_THROTTLE_RANGE;
    final double ess = exactSparseSetsExpected.doubleValue();
    double evalMargin = ess > MAX_EVALS ? MAX_EVALS / ess : 1;
    log.info("eval margin: {}%", Format.format6(evalMargin * 100));
    BigInteger evals = BigInteger.ZERO;
    while (true) {
      long nextPerm = exactSparseSets.equals(BigInteger.ZERO) ? 1 : nextPermExact(len, links, totalLinks);
      if (nextPerm <= 0) {
        break;
      }
      if (exactSparseSets.compareTo(exactSparseSetsExpected) >= 0) {
        throw new IllegalStateException("still enumerating sets?");
      }

      exactSparseSets = exactSparseSets.add(BigInteger.ONE);
      if (timeThrottle++ == 0) {
        timeThrottle = TIME_THROTTLE_RANGE;
        final long now = System.currentTimeMillis();
        if (now - lastStatus >= 60 * 1000) {
          final double percentage = percentage(exactSparseSets, exactSparseSetsExpected);
          log.debug(
              "completed {}%, ETA {}",
              percentage,
              Format.formatDuration((long) ((now - firstStatus) * (100.0 - percentage) / percentage))
          );
          lastStatus = now;
        }
      }

      if (evalSource.nextDouble() >= evalMargin) {
        continue;
      }
      evals = evals.add(BigInteger.ONE);

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

      regularSets = regularSets.add(BigInteger.ONE);

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

      double eg;
      try {
        mseg.run();
        eg = mseg.getTarget().getValue();
      } catch (Throwable throwable) {
        throwable.printStackTrace();
        eg = 0;
      }
      for (int ei = 0; ei < lambdas.length; ei++) {
        if (eg < lambdas[ei]) {
          break;
        }
        eigenSets[ei] = eigenSets[ei].add(BigInteger.ONE);
      }
    }

    if (evalMargin >= 1 && exactSparseSets.compareTo(exactSparseSetsExpected) < 0) {
      throw new IllegalStateException(
          "should still enumerate: stopped on " + exactSparseSets + " instead of " + exactSparseSetsExpected
      );
    }

    regularSets = extrapolate(regularSets, evals, exactSparseSetsExpected);
    for (int ei = 0; ei < lambdas.length; ei++) {
      eigenSets[ei] = extrapolate(eigenSets[ei], evals, exactSparseSetsExpected);
    }

    if (lambdaRef instanceof StoreLens) {
      final StoreLens<Double> lambdaLens = (StoreLens<Double>) lambdaRef;
      final StoreLens<Double>[] lambdaAxis = lambdaLens.axisArr();

      for (int ei = 0; ei < lambdas.length; ei++) {
        final StoreLens<Double> lambdaLensOffs = lambdaAxis[ei];

        lambdaLensOffs.forTypeName(Integer.class, "len").set(len);
        lambdaLensOffs.forTypeName(Integer.class, "totalLinks").set(totalLinks);
        lambdaLensOffs.forTypeName(Integer.class, "nodeLinks").set(nodeLinks);

        putWithLog("totalSets", lambdaLensOffs, totalSets);
        putWithLog("sparseSets", lambdaLensOffs, sparseSets);
        putWithLog("exactSparseSets", lambdaLensOffs, exactSparseSetsExpected);
        putWithLog("regularSets", lambdaLensOffs, regularSets);
        putWithLog("eigenSets", lambdaLensOffs, eigenSets[ei]);
      }
    }

    log.info("actual evaluate ratio = {}%", percentageStr(evals, exactSparseSetsExpected));
    log.info("regularSets = {} ({}% of exact sparse)", regularSets, percentageStr(regularSets, exactSparseSetsExpected));
    for (int ei = 0; ei < lambdas.length; ei++) {
      log.info(
          "eigenSets (eigengap >= {}) = {} ({}% of exact sparse)",
          new Object[] {lambdas[ei], eigenSets[ei], percentageStr(eigenSets[ei], exactSparseSetsExpected) }
      );
    }
  }

  protected static BigInteger extrapolate(BigInteger regularSets, BigInteger evals, BigInteger exactSparseSetsExpected) {
    return regularSets.multiply(exactSparseSetsExpected).divide(evals);
  }

  public void putWithLog(
      final String path,
      StoreLens<Double> lambdaLensOffs,
      BigInteger number
  ) {
    lambdaLensOffs.forTypeName(String.class, path).set(number.toString());
    lambdaLensOffs.forName(path+"_log10").set(Math.log10(number.doubleValue()));
  }

  protected static final BigInteger BIG_INT_10K = BigInteger.valueOf(10000);
  protected static String percentageStr(BigInteger fraction, BigInteger total) {
    return Format.format6(percentage(fraction, total));
  }
  protected static double percentage(BigInteger fraction, BigInteger total) {
    return fraction.multiply(BIG_INT_10K).divide(total).intValue() / 100.0;
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