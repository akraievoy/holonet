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
import org.akraievoy.cnet.metrics.api.Metric;
import org.akraievoy.holonet.exp.store.RefObject;
import org.akraievoy.cnet.metrics.api.MetricRoutes;
import org.akraievoy.cnet.metrics.domain.EigenMetric;
import org.akraievoy.cnet.metrics.domain.MetricEDataRouteLen;
import org.akraievoy.cnet.metrics.domain.MetricScalarEffectiveness;
import org.akraievoy.cnet.metrics.domain.MetricScalarEigenGap;
import org.akraievoy.cnet.net.vo.EdgeData;
import org.akraievoy.cnet.net.vo.EdgeDataFactory;
import org.akraievoy.cnet.opt.api.GeneticStrategy;
import org.akraievoy.cnet.opt.domain.FitnessKey;
import org.akraievoy.holonet.exp.store.StoreLens;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
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
  protected long generationNum;

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

  protected double fitnessCap = Double.POSITIVE_INFINITY;
  public double getFitnessCap() { return fitnessCap; }
  public void setFitnessCap(double fitnessCap) { this.fitnessCap = fitnessCap; }

  protected RefRO<? extends EdgeData> distSource = new RefObject<EdgeData>();
  protected RefRO<? extends EdgeData> requestSource = new RefObject<EdgeData>();

  protected int steps = 1;

  public GeneticStrategySoo(final MetricRoutes metricRoutes) {
    this.metricScalarEigenGap = new MetricScalarEigenGap();
    this.metricEDataRouteLen = new MetricEDataRouteLen(metricRoutes);
    this.metricEff = new MetricScalarEffectiveness();
  }

  public void setDistSource(RefRO<? extends EdgeData> distSource) {
    this.distSource = distSource;
  }

  public RefRO<? extends EdgeData> getDistSource() {
    return distSource;
  }

  public void setRequestSource(RefRO<? extends EdgeData> requestSource) {
    this.requestSource = requestSource;
  }

  public RefRO<? extends EdgeData> getRequestSource() {
    return requestSource;
  }

  public void setSteps(int steps) { this.steps = steps; }
  public int getSteps() { return steps; }

  public void setModes(@Nonnull String newModes) { this.modes = newModes.toUpperCase(); }
  public boolean mode(@Nonnull String someMode) { return modes.contains(someMode); }

  public void init(
      StoreLens<Integer> generationLens
  ) {
    final long generation = generationLens.paramPos().index();

    if (generation == 0) {
      return; // we have the values already set in the constructor
    }

    final StoreLens<Double> minEffStartLens =
        generationLens.axisArr()[0].forTypeName(Double.class, "minEffStart");
    final Double minEffStartCtx = minEffStartLens.getValue();
    if (minEffStartCtx != null) {
      minEffStart = minEffStartCtx;
      final StoreLens<Double> minEffTargetLens =
          minEffStartLens.forName("minEffTarget");
      minEffTarget = minEffTargetLens.getValue();
    }

    generationNum = generationLens.fullCount();
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

  public void initOnSeeds(
      final StoreLens<Integer> generationLens,
      final SortedMap<FitnessKey, GenomeSoo> children
  ) {
    if (children.isEmpty()) {
      return;
    }

    final double eff = computeEff(children.get(children.firstKey()));
    if (eff < minEff) {
      final StoreLens<Double> minEffStartLens =
          generationLens.axisArr()[0].forTypeName(Double.class, "minEffStart");
      final StoreLens<Double> minEffTargetLens =
          minEffStartLens.forName("minEffTarget");
      minEffStartLens.set(eff * 0.975);
      minEffTargetLens.set(generationLens.fullCount() * 0.15);
    }
  }

  public double computeFitness(GenomeSoo genome) {
    metricScalarEigenGap.setSource(new RefObject<EdgeData>(genome.getSolution()));
    try {
      return Metric.fetch(metricScalarEigenGap);
    } catch (EigenMetric.EigenSolverException e) {
      log.warn("IGNORING eigensolver failure: marking child as invalid", e);
      return Double.NaN;
    }
  }

  public Double computeEff(GenomeSoo child) {
    metricEDataRouteLen.getRoutes().setDistSource(distSource);
    metricEDataRouteLen.getRoutes().setSource(new RefObject<EdgeData>(child.getSolution()));

    final EdgeData lenEData = Metric.fetch(metricEDataRouteLen);

    metricEff.setSource(new RefObject<EdgeData>(lenEData));
    metricEff.setWeightSource(requestSource);

    final Double eff = Metric.fetch(metricEff);
    return eff;
  }
}
