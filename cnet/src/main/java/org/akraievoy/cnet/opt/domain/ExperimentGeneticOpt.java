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
import org.akraievoy.base.Die;
import org.akraievoy.base.Format;
import org.akraievoy.base.ref.Ref;
import org.akraievoy.base.ref.RefSimple;
import org.akraievoy.base.runner.api.Context;
import org.akraievoy.base.runner.api.ContextInjectable;
import org.akraievoy.cnet.gen.vo.EntropySource;
import org.akraievoy.cnet.gen.vo.EntropySourceRandom;
import org.akraievoy.cnet.gen.vo.WeightedEventModelRenorm;
import org.akraievoy.cnet.metrics.domain.EigenMetric;
import org.akraievoy.cnet.opt.api.*;
import org.akraievoy.gear.G4Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ExperimentGeneticOpt implements Runnable, ContextInjectable {
  private static final Logger log = LoggerFactory.getLogger(ExperimentGeneticOpt.class);

  protected Context ctx;

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
  protected double missLimitRatio = 60.0;

  protected int generation = 0;
  protected int specimenIndex = 0;

  protected String genKey = "gen";
  protected String generationParamName = "main.generation";
  protected String specimenIndexParamName = "main.specimenIndex";

  protected long specimenLimit;
  protected long missLimit;
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

  public void setGeneration(int generation) {
    this.generation = generation;
  }

  public void setGenerationParamName(final String generationParamName) {
    this.generationParamName = generationParamName;
  }

  public void setSpecimenIndex(int specimenIndex) {
    this.specimenIndex = specimenIndex;
  }

  public void setSpecimenIndexParamName(String specimenIndexParamName) {
    this.specimenIndexParamName = specimenIndexParamName;
  }

  public void setGenKey(String genKey) {
    this.genKey = genKey;
  }

  public void setCrossoverRatio(double crossoverRatio) {
    this.crossoverRatio = crossoverRatio;
  }

  public void setEliteRatio(double eliteRatio) {
    this.eliteRatio = eliteRatio;
  }

  public void setMissLimitRatio(double missLimitRatio) {
    this.missLimitRatio = missLimitRatio;
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

  public void run() {
    initOnContext();

    if (generation == 0) {
      //  LATER we don't have constraint validation on the seeds here
      final List<Genome> genomes = seedSource.getSeeds(strategy);
      for (Genome genome : genomes) {
        storeToPopulation(children, genome);
      }
      strategy.initOnSeeds(ctx, generationParamName, children);
      storeToContext(ctx, children);
      log.info("Initialization complete; fitness = {}", fitnessReport(children));
      return;
    }

    loadGen(ctx, parents);

    if (parents.isEmpty()) {
      log.info("Generation #{} empty: specimens died-out", generation);
      return;
    }

    state.setCompleteness((generation + 1.0) / ctx.getCount(generationParamName));
    state.setFitnessDeviation(fitnessDeviation(parents));
    state.setSimilarityMean(similarityMean(parents.values()));

    state.calibrate(ctx);

    breeders.calibrate(ctx, generationParamName);
    mutators.calibrate(ctx, generationParamName);

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
    while (children.size() < specimenLimit && (generateCount < missLimit || elitePointer < eliteLimit && elitePointer < parents.size())) {
      report(lastReport);

      final Ref<Breeder<Genome>> breeder = new RefSimple<Breeder<Genome>>(null);
      final Ref<Mutator<Genome>> mutator = new RefSimple<Mutator<Genome>>(null);

      boolean valid = false;
      Genome child = null;
      try {
        if (elitePointer < eliteLimit && elitePointer < parents.size() && (generateCount >= missLimit || children.size() + eliteLimit >= specimenLimit) ) {
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

    storeToContext(ctx, children);
    mutators.storeRatios(ctx);
    breeders.storeRatios(ctx);
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
    //	assert iterated params are set to initial values
    Die.ifFalse("specimenIndex == 0", specimenIndex == 0);

    strategy.init(ctx, generationParamName);

    specimenLimit = ctx.getCount(specimenIndexParamName);
    eliteLimit = (int) Math.ceil(specimenLimit * eliteRatio);
    missLimit = (long) Math.ceil(missLimitRatio * specimenLimit);
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

  protected void loadGen(Context ctx, final SortedMap<FitnessKey, Genome> gen) {
    gen.clear();

    for (int specIndex = 0; specIndex < specimenLimit; specIndex++) {
      final Map<String,Integer> offset = Context.offset(
          generationParamName, -1,
          specimenIndexParamName, specIndex
      );

      final Genome value = strategy.createGenome();

      if (value.read(ctx, offset, genKey) == null) {
        break;
      }

      gen.put(new FitnessKey(specIndex, value.getFitness()), value);
    }
  }

  protected void storeToContext(Context ctx, Map<FitnessKey, Genome> genomes) {
    if (genomes.size() == 0) {
      return;
    }

    int specimenIndex = 0;
    for (FitnessKey fKey : children.keySet()) {
      final Genome genome = genomes.get(fKey);

      genome.write(
          fKey.getFitness(), genKey, ctx, Context.offset(specimenIndexParamName, specimenIndex)
      );

      if (specimenIndex == 0) {
        genome.write(
            fKey.getFitness(), genKey + ".best", ctx, Context.offset()
        );
      }

      specimenIndex++;
    }
  }

  public void setCtx(Context ctx) {
    this.ctx = ctx;
  }
}
