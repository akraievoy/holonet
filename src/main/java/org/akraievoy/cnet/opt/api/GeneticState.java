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

package org.akraievoy.cnet.opt.api;

import org.akraievoy.base.Format;
import org.akraievoy.holonet.exp.store.StoreLens;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeneticState {
  private static final Logger log = LoggerFactory.getLogger(GeneticState.class);

  protected String keyPrefix = "state";

  //	statically set parameters
  protected double maxMutation = 0.05;
  protected double maxCrossover = 0.10;
  protected double maxElemFitPow = 8;

  protected double minElemFitnessNorm = 0.05;
  protected double fitnessDeviationMin = 0.02;
  protected double fitnessDeviationMax = 0.98;

  //	parameters inferred from the prev. generation
  protected double completeness;
  protected double similarityMean;
  protected double fitnessDeviation;

  //	computed parameters
  protected double minElemFitness;
  protected double elemFitPow;
  protected double crossoverRatio;
  protected double mutateRatio;

  public void setKeyPrefix(String keyPrefix) {
    this.keyPrefix = keyPrefix;
  }

  public double getMaxElemFitPow() {
    return maxElemFitPow;
  }

  public void setMaxElemFitPow(double maxElemFitPow) {
    this.maxElemFitPow = maxElemFitPow;
  }

  public double getMaxCrossover() {
    return maxCrossover;
  }

  public void setMaxCrossover(double maxCrossover) {
    this.maxCrossover = maxCrossover;
  }

  public double getMaxMutation() {
    return maxMutation;
  }

  public void setMaxMutation(double maxMutation) {
    this.maxMutation = maxMutation;
  }

  public double getFitnessDeviationMax() {
    return fitnessDeviationMax;
  }

  public void setFitnessDeviationMax(double fitnessDeviationMax) {
    this.fitnessDeviationMax = fitnessDeviationMax;
  }

  public double getFitnessDeviationMin() {
    return fitnessDeviationMin;
  }

  public void setFitnessDeviationMin(double fitnessDeviationMin) {
    this.fitnessDeviationMin = fitnessDeviationMin;
  }

  public double getMinElemFitnessNorm() {
    return minElemFitnessNorm;
  }

  public void setMinElemFitnessNorm(double minElemFitnessNorm) {
    this.minElemFitnessNorm = minElemFitnessNorm;
  }

  public double getCompleteness() {
    return completeness;
  }

  public void setCompleteness(double completeness) {
    this.completeness = completeness;
  }

  public double getFitnessDeviation() {
    return fitnessDeviation;
  }

  public void setFitnessDeviation(double fitnessDeviation) {
    this.fitnessDeviation = fitnessDeviation;
  }

  public double getSimilarityMean() {
    return similarityMean;
  }

  public void setSimilarityMean(double similarityMean) {
    this.similarityMean = similarityMean;
  }

  public double getElemFitPow() {
    return elemFitPow;
  }

  public double getMinElemFitness() {
    return minElemFitness;
  }

  public double getCrossoverRatio() {
    return crossoverRatio;
  }

  public double getMutateRatio() {
    return mutateRatio;
  }

  public void calibrate(StoreLens<Integer> generationLens) {
    final double fitnessDev = Math.max(Math.min(fitnessDeviationMax, fitnessDeviation), fitnessDeviationMin);

    log.info(
        "fitness dev = {} ({}); completeness = {}, similarity mean = {}",
        new Object[]{
            Format.format2(fitnessDev),
            Format.format4(getFitnessDeviation()),
            Format.format2(getCompleteness()),
            Format.format4(getSimilarityMean())
        }
    );

    final StoreLens<Double> reportLens;
    if (generationLens != null) {
      reportLens = generationLens.forTypeName(
          Double.class,
          generationLens.paramName()+"-report"
      );
      reportLens.forName(keyPrefix + ".fitnessDev").set(getFitnessDeviation());
      reportLens.forName(keyPrefix + ".completeness").set(getCompleteness());
      reportLens.forName(keyPrefix + ".similarityMean").set(getSimilarityMean());
    } else {
      reportLens = null;
    }

    minElemFitness = (1 - completeness) * similarityMean * minElemFitnessNorm;
    elemFitPow = 1 + Math.ceil(2 * fitnessDev * (maxElemFitPow - 1)) / 2;
    mutateRatio = (1 - completeness) * similarityMean * maxMutation;
    crossoverRatio = (1 - fitnessDev) * maxCrossover;

    log.info(
        "min elem fitness = {}; elem fit pow = {}, crossover = {}, mutate = {}",
        new Object[]{
            Format.format6(getMinElemFitness()),
            Format.format2(getElemFitPow()),
            Format.format6(getCrossoverRatio()),
            Format.format6(getMutateRatio())
        }
    );

    if (reportLens != null) {
      reportLens.forName(keyPrefix + ".minElemFitness").set(getMinElemFitness());
      reportLens.forName(keyPrefix + ".elemFitPow").set(getElemFitPow());
      reportLens.forName(keyPrefix + ".crossover").set(getCrossoverRatio());
      reportLens.forName(keyPrefix + ".mutate").set(getMutateRatio());
    }
  }
}
