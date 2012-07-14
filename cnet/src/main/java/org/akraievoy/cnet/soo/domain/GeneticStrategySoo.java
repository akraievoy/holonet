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

import org.akraievoy.base.ref.RefRO;
import org.akraievoy.base.runner.api.Context;
import org.akraievoy.cnet.metrics.api.MetricResultFetcher;
import org.akraievoy.cnet.metrics.api.MetricRoutes;
import org.akraievoy.cnet.metrics.domain.EigenMetric;
import org.akraievoy.cnet.metrics.domain.MetricEDataRouteLen;
import org.akraievoy.cnet.metrics.domain.MetricScalarEffectiveness;
import org.akraievoy.cnet.metrics.domain.MetricScalarEigenGap;
import org.akraievoy.cnet.net.ref.RefEdgeData;
import org.akraievoy.cnet.net.vo.EdgeData;
import org.akraievoy.cnet.net.vo.EdgeDataFactory;
import org.akraievoy.cnet.opt.api.GeneticStrategy;
import org.akraievoy.cnet.opt.domain.FitnessKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.SortedMap;

public class GeneticStrategySoo implements GeneticStrategy<GenomeSoo> {
  private static final Logger log = LoggerFactory.getLogger(GeneticStrategySoo.class);
  
  protected final MetricScalarEigenGap metricScalarEigenGap;

  protected final MetricEDataRouteLen metricEDataRouteLen;
  protected final MetricScalarEffectiveness metricEff;

  public static final String MODE_REGULAR = "R";

  protected String modes = "" + MODE_REGULAR;

  //	connectivity upper limit
  protected double theta = 2;
  public void setTheta(double theta) { this.theta = theta; }

  //	regularity (vertex power) lower limit
  protected double thetaTilde = 0;
  public void setThetaTilde(double thetaTilde) { this.thetaTilde = thetaTilde; }

  //	effectiveness lower limit
  protected double minEff = 0;
  protected double minEffStart = 0;
  protected double minEffTarget = -1;
  public void setMinEff(double minEff) { this.minEff = minEff; }
  public double minEff(int generationIndex) {
    if (minEffTarget < 0 || minEffTarget <= generationIndex) {
      return minEff;
    }

    return (minEffStart * (minEffTarget - generationIndex) + generationIndex * minEff) / minEffTarget;
  }

  protected RefRO<EdgeData> distSource = new RefEdgeData();
  protected RefRO<EdgeData> requestSource = new RefEdgeData();

  protected int steps = 1;

  public GeneticStrategySoo(final MetricRoutes metricRoutes) {
    this.metricScalarEigenGap = new MetricScalarEigenGap();
    this.metricEDataRouteLen = new MetricEDataRouteLen(metricRoutes);
    this.metricEff = new MetricScalarEffectiveness();
  }

  public void setDistSource(RefRO<EdgeData> distSource) {
    this.distSource = distSource;
  }

  public RefRO<EdgeData> getDistSource() {
    return distSource;
  }

  public void setRequestSource(RefRO<EdgeData> requestSource) {
    this.requestSource = requestSource;
  }

  public RefRO<EdgeData> getRequestSource() {
    return requestSource;
  }

  public void setSteps(int steps) { this.steps = steps; }
  public int getSteps() { return steps; }

  public void setModes(@Nonnull String newModes) { this.modes = newModes.toUpperCase(); }
  public boolean mode(@Nonnull String someMode) { return modes.contains(someMode); }

  public void init(Context ctx, final String generationParam) {
    final long generation = ctx.getEnumerator().getPos(generationParam);

    if (generation == 0) {
      return; // we have the values already set in the constructor
    }

    final Map<String, Integer> backToSeed = Context.offset(
        generationParam,
        -(int) generation
    );

    final Double minEffStartCtx = ctx.get("minEffStart", Double.class, backToSeed);
    if (minEffStartCtx != null) {
      minEffStart = minEffStartCtx;
      minEffTarget = ctx.get("minEffTarget", Double.class, backToSeed);
    }
  }

  protected int getTotalLinkUpperLimit() {
    final int size = distSource.getValue().getSize();

    final double thetaVal = theta;
    return getTotalLinkUpperLimit(size, thetaVal);
  }

  public static int getTotalLinkUpperLimit(int size, double thetaVal) {
    return (int) Math.ceil(thetaVal * size * Math.log(size) / Math.log(2)) / 2;
  }

  protected int getNodeLinkLowerLimit() {
    final int size = distSource.getValue().getSize();

    final double thetaTildeVal = thetaTilde;
    return getNodeLinkLowerLimit(size, thetaTildeVal);
  }

  public static int getNodeLinkLowerLimit(int size, double thetaTildeVal) {
    return (int) Math.floor(thetaTildeVal * Math.log(size) / Math.log(2));
  }

  public GenomeSoo createGenome() {
    return new GenomeSoo(EdgeDataFactory.sparse(
        true, 0.0, distSource.getValue().getSize()
    ));
  }

  public void initOnSeeds(Context ctx, String generationParam, SortedMap<FitnessKey, GenomeSoo> children) {
    if (children.isEmpty()) {
      return;
    }

    final double eff = computeEff(children.get(children.firstKey()));
    if (eff < minEff) {
      ctx.put("minEffStart", eff * 0.975);
      ctx.put("minEffTarget", ctx.getCount(generationParam) * 0.15);
    }
  }

  public double computeFitness(GenomeSoo genome) {
    metricScalarEigenGap.setSource(new RefEdgeData(genome.getSolution()));
    try {
      return (Double) MetricResultFetcher.fetch(metricScalarEigenGap);
    } catch (EigenMetric.EigenSolverException e) {
      log.warn("IGNORING eigensolver failure: marking child as invalid", e);
      return Double.NaN;
    }
  }

  public Double computeEff(GenomeSoo child) {
    metricEDataRouteLen.getRoutes().setDistSource(distSource);
    metricEDataRouteLen.getRoutes().setSource(new RefEdgeData(child.getSolution()));

    final EdgeData lenEData = (EdgeData) MetricResultFetcher.fetch(metricEDataRouteLen);

    metricEff.setSource(new RefEdgeData(lenEData));
    metricEff.setWeightSource(requestSource);

    final Double eff = (Double) MetricResultFetcher.fetch(metricEff);
    return eff;
  }
}
