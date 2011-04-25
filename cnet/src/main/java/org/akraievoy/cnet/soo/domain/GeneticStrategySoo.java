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
import org.akraievoy.cnet.metrics.domain.MetricEDataRouteLen;
import org.akraievoy.cnet.metrics.domain.MetricScalarEffectiveness;
import org.akraievoy.cnet.metrics.domain.MetricScalarEigenGap;
import org.akraievoy.cnet.net.ref.RefEdgeData;
import org.akraievoy.cnet.net.vo.EdgeData;
import org.akraievoy.cnet.opt.api.GeneticStrategy;

public class GeneticStrategySoo implements GeneticStrategy<GenomeSoo> {
  protected final MetricScalarEigenGap metricScalarEigenGap;

  protected final MetricEDataRouteLen metricEDataRouteLen;
  protected final MetricScalarEffectiveness metricEff;

  //	connectivity upper limit
  protected double theta = 2;
  //	regularity (vertex power) lower limit
  protected double theta_tilde = 0;
  //	eigengap lower limit
  protected double lambda = .75;
  protected double startLambda = .05;

  protected int lambdaTargetGeneration = 10;
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

  public void setTheta(double theta) {
    this.theta = theta;
  }

  public void setLambda(double lambda) {
    this.lambda = lambda;
  }

  public void setTheta_tilde(double theta_tilde) {
    this.theta_tilde = theta_tilde;
  }

  public void setStartLambda(double startLambda) {
    this.startLambda = startLambda;
  }

  public void setLambdaTargetGeneration(int targetGeneration) {
    this.lambdaTargetGeneration = targetGeneration;
  }

  public void setSteps(int steps) {
    this.steps = steps;
  }

  public int getSteps() {
    return steps;
  }

  public void init(Context ctx) {
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

    final double thetaTildeVal = theta_tilde;
    return getNodeLinkLowerLimit(size, thetaTildeVal);
  }

  public static int getNodeLinkLowerLimit(int size, double thetaTildeVal) {
    return (int) Math.floor(thetaTildeVal * Math.log(size) / Math.log(2));
  }

  public double getMinLambda(int generationIndex) {
    if (generationIndex >= lambdaTargetGeneration) {
      return lambda;
    }

    return (startLambda * (lambdaTargetGeneration - generationIndex) + generationIndex * lambda) / lambdaTargetGeneration;
  }

  public GenomeSoo createGenome(Object[] genomeArr) {
    final GenomeSoo genome = new GenomeSoo();

    genome.setGenomeData(genomeArr);

    return genome;
  }

  public double computeFitness(GenomeSoo genome) {
    metricEDataRouteLen.getRoutes().setDistSource(distSource);
    metricEDataRouteLen.getRoutes().setSource(new RefEdgeData(genome.getSolution()));

    final EdgeData lenEData = (EdgeData) MetricResultFetcher.fetch(metricEDataRouteLen);

    metricEff.setSource(new RefEdgeData(lenEData));
    metricEff.setWeightSource(requestSource);

    return (Double) MetricResultFetcher.fetch(metricEff);
  }
}
