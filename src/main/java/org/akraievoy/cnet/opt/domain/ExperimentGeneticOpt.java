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

package org.akraievoy.cnet.opt.domain;

import com.google.common.base.Optional;
import gnu.trove.TDoubleArrayList;
import org.akraievoy.base.Format;
import org.akraievoy.base.ref.Ref;
import org.akraievoy.base.ref.RefSimple;
import org.akraievoy.cnet.gen.vo.EntropySource;
import org.akraievoy.cnet.gen.vo.EntropySourceRandom;
import org.akraievoy.cnet.gen.vo.WeightedEventModelRenorm;
import org.akraievoy.cnet.metrics.domain.EigenMetric;
import org.akraievoy.cnet.opt.api.*;
import org.akraievoy.gear.G4Stat;
import org.akraievoy.holonet.exp.store.StoreLens;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ExperimentGeneticOpt implements Runnable {
  private static final Logger log = LoggerFactory.getLogger(ExperimentGeneticOpt.class);

  protected final SortedMap<FitnessKey, Genome> parents = new TreeMap<FitnessKey, Genome>();
  protected final SortedMap<FitnessKey, Genome> children = new TreeMap<FitnessKey, Genome>();

  protected final GeneticStrategy<Genome> strategy;
  protected final EntropySource eSource;

  protected final CompositeMutator mutators = new CompositeMutator();
  protected final CompositeBreeder breeders = new CompositeBreeder();
  protected final CompositeCondition conditions = new CompositeCondition();
  protected final WeightedEventModelRenorm events =
      new WeightedEventModelRenorm(Optional.of("parents"));

  protected SeedSource<Genome> seedSource;
  protected GeneticState state = new GeneticState();

  protected double eliteRatio = 0.05;
  protected double mutationRatio = 0.05;
  protected double crossoverRatio = 0.25;
  protected SortedMap<Double, Double> generateLimitRatios =
      new TreeMap<Double, Double>();
  {
    generateLimitRatios.put(1.0, 1.0);
    generateLimitRatios.put(0.9, 1.24);
    generateLimitRatios.put(0.8, 2.0);
    generateLimitRatios.put(0.7, 3.0);
    generateLimitRatios.put(0.6, 5.0);
    generateLimitRatios.put(0.5, 8.0);
    generateLimitRatios.put(0.4, 13.0);
    generateLimitRatios.put(0.3, 21.0);
    generateLimitRatios.put(0.2, 34.0);
    generateLimitRatios.put(0.1, 55.0);
    generateLimitRatios.put(0.05, 89.0);
    generateLimitRatios.put(0.01, 144.0);
    generateLimitRatios.put(0.00, 233.0);
  }

  protected int generation = 0;

  protected StoreLens<Integer> specimenLens;
  protected StoreLens<Integer> generationLens;
  protected StoreLens<Double> genomeLens;

  protected long specimenLimit;
  protected SortedMap<Long, Long> generateLimits =
      new TreeMap<Long, Long>();
  protected int eliteLimit;

  protected long reportPeriod = 30000;

  public ExperimentGeneticOpt(GeneticStrategy<Genome> strategy, final EntropySourceRandom eSource) {
    this.strategy = strategy;
    this.eSource = eSource;
  }

  public void setBreeders(List<Breeder<Genome>> breeders) {
    this.breeders.setElems(breeders);
  }

  public void setConditions(List<Condition<Genome>> conditions) {
    this.conditions.setElems(conditions);
  }

  public void setMutators(List<Mutator<Genome>> mutators) {
    this.mutators.setElems(mutators);
  }

  public void setSeedSource(final SeedSource<Genome> seedSource) {
    this.seedSource = seedSource;
  }

  public void setGenerationLens(final StoreLens<Integer> generationLens) {
    this.generationLens = generationLens;
  }

  public void setGenomeLens(final StoreLens<Double> genomeLens) {
    this.genomeLens = genomeLens;
  }

  public void setSpecimenLens(StoreLens<Integer> specimenLens) {
    this.specimenLens = specimenLens;
  }

  public void setCrossoverRatio(double crossoverRatio) {
    this.crossoverRatio = crossoverRatio;
  }

  public void setEliteRatio(double eliteRatio) {
    this.eliteRatio = eliteRatio;
  }

  public void setGenerateLimitRatio(double glr) {
    final SortedMap<Double, Double> glrMap = generateLimitRatios;

    final double scale =
        glr / glrMap.get(glrMap.firstKey());

    for (Map.Entry<Double, Double> glrE : glrMap.entrySet()) {
      glrMap.put(glrE.getKey(), scale * glrE.getValue());
    }
  }

  public void setGenerateLimitRatios(
      SortedMap<Double, Double> generateLimitRatios
  ) {
    this.generateLimitRatios = generateLimitRatios;
  }

  public void setMutationRatio(double mutationRatio) {
    this.mutationRatio = mutationRatio;
  }

  public void setReportPeriod(long reportPeriod) {
    this.reportPeriod = reportPeriod;
  }

  public void setState(GeneticState state) {
    this.state = state;
  }

  protected long generateLimit(final long childrenSize) {
    final SortedMap<Long, Long> glMap = generateLimits;
    final SortedMap<Long, Long> tailMap = glMap.tailMap(childrenSize);
    if (tailMap.isEmpty()) {
      throw new IllegalStateException(
          "no limit for childrenSize = " + childrenSize + "\n" +
          "generateLimits = " + glMap
      );
    }

    final Long genLimit = glMap.get(tailMap.firstKey());

    return genLimit;
  }

  public void run() {
    initOnContext();

    if (generation == 0) {
      //  LATER we don't have constraint validation on the seeds here
      final List<Genome> genomes = seedSource.getSeeds(strategy);
      for (Genome genome : genomes) {
        storeToPopulation(children, genome);
      }
      strategy.initOnSeeds(generationLens, children);
      storeToContext(children);
      log.info("Initialization complete; fitness = {}", fitnessReport(children));
      return;
    }

    loadGen(parents);

    if (parents.isEmpty()) {
      log.info("Generation #{} empty: specimens died-out", generation);
      return;
    }

    state.setCompleteness((generation + 1.0) / generationLens.fullCount());
    state.setFitnessDeviation(fitnessDeviation(parents));
    state.setSimilarityMean(similarityMean(parents.values()));

    state.calibrate(generationLens);

    breeders.calibrate(generationLens);
    mutators.calibrate(generationLens);

    events.setMinWeight(state.getMinElemFitness());
    events.setAmp(state.getElemFitPow());

    final FitnessKey[] fKeys = parents.keySet().toArray(new FitnessKey[parents.size()]);
    for (int i = 0, genSize = fKeys.length; i < genSize; i++) {
      events.add(i, fKeys[i].getFitness());
    }

    Ref<Long> lastReport = new RefSimple<Long>(System.currentTimeMillis());
    eliteLimit = Math.min(parents.size(), eliteLimit);
    int elitePointer = 0;
    int generateCount = 0;
    boolean generateValid;
    boolean eliteValid = false;
    while (
        children.size() < specimenLimit && (
            (generateValid = generateCount < generateLimit((long) children.size())) ||
            (eliteValid = elitePointer < eliteLimit && elitePointer < parents.size())
        )
    ) {
      report(lastReport);

      final Ref<Breeder<Genome>> breeder = new RefSimple<Breeder<Genome>>(null);
      final Ref<Mutator<Genome>> mutator = new RefSimple<Mutator<Genome>>(null);

      boolean valid = false;
      Genome child = null;
      try {
        if (eliteValid && (!generateValid || children.size() + eliteLimit >= specimenLimit) ) {
          child = parents.get(fKeys[elitePointer++]);
        } else {
          child = generateChild(
              state, fKeys, breeder, mutator
          );
          generateCount++;
        }

        valid = validate(child);
      } catch (EigenMetric.EigenSolverException e) {
        log.warn("IGNORING eigensolver failure: marking child as invalid", e);
      }

      if (valid && child != null) {
        final Optional<FitnessKey> fkOpt = storeToPopulation(children, child);
        if (fkOpt.isPresent()) {
          //  fraction of children which are not better than this one
          double rank = children.tailMap(fkOpt.get()).size() / (double) children.size();

          mutators.rankFeedback(mutator.getValue(), rank);
          breeders.rankFeedback(breeder.getValue(), rank);
          continue;
        }
      }

      mutators.onFailure(mutator.getValue());
      breeders.onFailure(breeder.getValue());
    }

    report(null);

    storeToContext(children);
    mutators.storeRatios(generationLens);
    breeders.storeRatios(generationLens);
    log.info(
        "Generation #{} filled; fitness {}, consumed {} bits of entropy",
        new Object[] {generation, fitnessReport(children), eSource.consumedBits()}
    );
  }

  public Optional<FitnessKey> storeToPopulation(
      final SortedMap<FitnessKey, Genome> population,
      final Genome genome
  ) {
    final double fitness = genome.getOrComputeFitness(strategy);
    if (Double.isNaN(fitness)) {
      return Optional.absent();
    }

    final FitnessKey fKey = new FitnessKey(population.size(), fitness);

    genome.setFitness(fitness);
    population.put(fKey, genome);
    return Optional.of(fKey);
  }

  protected void report(Ref<Long> lastReport) {
    if (lastReport == null) {
      log.info(breeders.buildReport() + mutators.buildReport() + conditions.buildReport());
      return;
    }

    if (System.currentTimeMillis() - lastReport.getValue() > reportPeriod) {
      lastReport.setValue(System.currentTimeMillis());

      log.debug(breeders.buildReport() + mutators.buildReport() + conditions.buildReport());
    }
  }

  protected static double similarityMean(final Collection<Genome> gen) {
    final Iterator<Genome> genomeIt = gen.iterator();
    final Genome bestFit = genomeIt.next();

    double similarity = 1;
    while (genomeIt.hasNext()) {
      similarity += bestFit.similarity(genomeIt.next());
    }

    return similarity / gen.size();
  }

  protected void initOnContext() {
    strategy.init(generationLens);

    generation = generationLens.getValue();
    specimenLimit = specimenLens.fullCount();
    eliteLimit = (int) Math.ceil(specimenLimit * eliteRatio);
    generateLimits.clear();
    for (Map.Entry<Double, Double> glre : generateLimitRatios.entrySet()) {
      generateLimits.put(
          (long) Math.ceil(specimenLimit * glre.getKey()),
          (long) Math.ceil(specimenLimit * glre.getValue())
      );
    }
  }

  protected Genome generateChild(
      GeneticState state, FitnessKey[] fKeys, 
      Ref<Breeder<Genome>> breeder, Ref<Mutator<Genome>> mutator
  ) {
    final FitnessKey fKeyA = fKeys[events.generate(eSource, false, null)];
    final FitnessKey fKeyB = fKeys[events.generate(eSource, false, null)];
    final Genome parentA;
    final Genome parentB;
    if (fKeyA.getFitness() > fKeyB.getFitness()) {
      parentA = parents.get(fKeyA);
      parentB = parents.get(fKeyB);
    } else {
      parentA = parents.get(fKeyB);
      parentB = parents.get(fKeyA);
    }

    breeder.setValue(breeders.select(eSource));
    final Genome child = breeder.getValue().crossover(strategy, parentA, parentB, state, eSource);

    mutator.setValue(mutators.select(eSource));
    mutator.getValue().mutate(strategy, child, state, eSource);

    return child;
  }

  protected static String fitnessReport(SortedMap<FitnessKey, Genome> population) {
    if (population.isEmpty()) {
      return "empty";
    }

    final TDoubleArrayList fitnesses = fitnesses(population);

    final double max = G4Stat.max(fitnesses);
    final double min = G4Stat.min(fitnesses);

    return Format.format6(min) + " .. " + max;
  }

  protected static double fitnessDeviation(SortedMap<FitnessKey, Genome> population) {
    final TDoubleArrayList fitnesses = fitnesses(population);
    final double mean = G4Stat.mean(fitnesses);
    final double dev = G4Stat.meanDeviation(fitnesses, mean);

    return dev;
  }

  protected static TDoubleArrayList fitnesses(SortedMap<FitnessKey, Genome> population) {
    final TDoubleArrayList fitnesses = new TDoubleArrayList(population.size());
    for (FitnessKey fitnessKey : population.keySet()) {
      fitnesses.add(fitnessKey.getFitness());
    }
    return fitnesses;
  }

  protected boolean validate(Genome child) {
    for (int i = 0, conditionsSize = conditions.size(); i < conditionsSize; i++) {
      final Condition<Genome> cond = conditions.wrap(i);

      final boolean valid = cond.isValid(strategy, child, children.values(), generation);

      if (!valid) {
        conditions.onFailure(cond);
        return false;
      }
    }

    return true;
  }

  protected void loadGen(final SortedMap<FitnessKey, Genome> gen) {
    gen.clear();

    final StoreLens<Integer>[] specimenAxis =
        specimenLens.offset(generationLens.paramName(), -1).axisArr();
    for (int i = 0; i < specimenAxis.length; i++) {
      final StoreLens<Double> prevGenomeLens =
          specimenAxis[i].forTypeName(Double.class, genomeLens.paramName());
      final Genome value = strategy.createGenome();
      if (value.read(prevGenomeLens) == null) {
        break;
      }
      gen.put(new FitnessKey(i, value.getFitness()), value);
    }
  }

  protected void storeToContext(Map<FitnessKey, Genome> genomes) {
    if (genomes.size() == 0) {
      return;
    }

    final StoreLens<Integer>[] specimenAxis = specimenLens.axisArr();
    int specimenIndex = 0;
    for (FitnessKey fKey : children.keySet()) {
      final StoreLens<Double> currGenomeLens =
          specimenAxis[specimenIndex].forTypeName(Double.class, genomeLens.paramName());
      final Genome genome = genomes.get(fKey);

      genome.write(currGenomeLens, fKey.getFitness());

      if (specimenIndex == 0) {
        genome.write(
            currGenomeLens.forName(currGenomeLens.paramName() + ".best"),
            fKey.getFitness()
        );
      }

      specimenIndex++;
    }
  }
}
