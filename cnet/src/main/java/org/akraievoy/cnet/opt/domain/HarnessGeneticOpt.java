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

import gnu.trove.TDoubleArrayList;
import org.akraievoy.base.Die;
import org.akraievoy.base.Format;
import org.akraievoy.base.ref.Ref;
import org.akraievoy.base.ref.RefSimple;
import org.akraievoy.base.runner.api.Context;
import org.akraievoy.base.runner.api.ContextInjectable;
import org.akraievoy.base.runner.api.SkipTrigger;
import org.akraievoy.cnet.gen.vo.EntropySource;
import org.akraievoy.cnet.gen.vo.EntropySourceRandom;
import org.akraievoy.cnet.gen.vo.WeightedEventModelRenorm;
import org.akraievoy.cnet.opt.api.*;
import org.akraievoy.gear.G4Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class HarnessGeneticOpt implements Runnable, ContextInjectable {
  private static final Logger log = LoggerFactory.getLogger(HarnessGeneticOpt.class);

  protected Context ctx;

  protected final List<Specimen> gen = new ArrayList<Specimen>();
  protected final List<Genome> newGen = new ArrayList<Genome>();
  protected final TDoubleArrayList fitnesses = new TDoubleArrayList();
  protected final SpecimenFitnessComparator fitnessComparator = new SpecimenFitnessComparator();

  protected final GeneticStrategy<Genome> strategy;
  protected final EntropySource eSource;

  protected final CompositeMutator mutators = new CompositeMutator();
  protected final CompositeBreeder breeders = new CompositeBreeder();
  protected final CompositeCondition conditions = new CompositeCondition();
  protected final WeightedEventModelRenorm events = new WeightedEventModelRenorm();

  protected SeedSource<Genome> seedSource;
  protected GeneticState state = new GeneticState();

  protected double eliteRatio = 0.05;
  protected double mutationRatio = 0.05;
  protected double crossoverRatio = 0.25;
  protected double missLimitRatio = 12.0;
  protected boolean terminate;

  protected int generation = 0;
  protected int specimenIndex = 0;

  protected String genKey = "gen";
  protected String generationParamName = "main.generation";
  protected String specimenIndexParamName = "main.specimenIndex";

  protected long specimenLimit;
  protected long missLimit;
  protected int eliteLimit;

  protected long reportPeriod = 30000;

  public HarnessGeneticOpt(GeneticStrategy<Genome> strategy, final EntropySourceRandom eSource) {
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
      storeGen(ctx, seedSource.getSeeds(strategy), fitnesses);
      log.info("Initialization complete; fitness = {}", fitnessReport(fitnesses));
      return;
    }

    loadGen(ctx, gen);

    if (gen.isEmpty()) {
      log.info("Generation #{} empty: specimens died-out", generation);
      return;
    }

    state.setCompleteness((generation + 1.0) / ctx.getCount(generationParamName));
    state.setFitnessDeviation(fitnessDeviation(fitnesses));
    state.setSimilarityMean(similarityMean(gen));

    state.calibrate(ctx);

    breeders.calibrate(ctx, generationParamName);
    mutators.calibrate(ctx, generationParamName);

    events.setMinWeight(state.getMinElemFitness());
    events.setAmp(state.getElemFitPow());

    for (int i = 0, genSize = gen.size(); i < genSize; i++) {
      Specimen spec = gen.get(i);
      events.add(i, spec.getFitness());
    }

    Ref<Long> lastReport = new RefSimple<Long>(System.currentTimeMillis());

    final int eliteSize = Math.min(eliteLimit, gen.size());

    Ref<Integer> eliteIndex = new RefSimple<Integer>(0);
    int generateCount = 0;
    while (newGen.size() < specimenLimit && (generateCount < missLimit)) {
      report(lastReport);

      final Ref<Breeder<Genome>> breeder = new RefSimple<Breeder<Genome>>(null);
      final Ref<Mutator<Genome>> mutator = new RefSimple<Mutator<Genome>>(null);
      generateCount++;

      final Genome child = generateNew(state, eliteSize, eliteIndex, breeder, mutator);

      if (!validate(child)) {
        mutators.onFailure(mutator.getValue());
        breeders.onFailure(breeder.getValue());
        continue;
      }

      newGen.add(child);
    }

    report(null);

    storeGen(ctx, newGen, fitnesses);
    mutators.storeRatios(ctx);
    breeders.storeRatios(ctx);
    log.info("Generation #{} filled; fitness {}", generation, fitnessReport(fitnesses));

    terminate = newGen.isEmpty();
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

  protected static double similarityMean(final List<Specimen> gen) {
    final Genome bestFit = gen.get(0).getGenome();
    double similarity = 1;

    for (int i = 1; i < gen.size(); i++) {
      similarity += bestFit.similarity(gen.get(i).getGenome());
    }

    return similarity / gen.size();
  }

  protected void initOnContext() {
    //	assert iterated params are set to initial values
    Die.ifFalse("specimenIndex == 0", specimenIndex == 0);

    strategy.init(ctx);

    specimenLimit = ctx.getCount(specimenIndexParamName);
    eliteLimit = (int) Math.ceil(specimenLimit * eliteRatio);
    missLimit = (long) Math.ceil(missLimitRatio * specimenLimit);
  }

  protected Genome generateNew(GeneticState state, int eliteSize, Ref<Integer> eliteIndex, Ref<Breeder<Genome>> breeder, Ref<Mutator<Genome>> mutator) {
    final Genome child;
    final Integer eliteIndexInt = eliteIndex.getValue();

    if (newGen.size() > eliteSize || eliteIndexInt >= gen.size()) {
      Specimen parentA = gen.get(events.generate(eSource, false, null));
      Specimen parentB = gen.get(events.generate(eSource, false, null));
      if (parentA.getFitness() > parentB.getFitness()) {
        Specimen temp = parentB;
        parentB = parentA;
        parentA = temp;
      }

      breeder.setValue(breeders.select(eSource));
      child = breeder.getValue().crossover(strategy, parentA.getGenome(), parentB.getGenome(), state, eSource);

      mutator.setValue(mutators.select(eSource));
      mutator.getValue().mutate(strategy, child, state, eSource);
    } else {
      child = gen.get(eliteIndexInt).getGenome();
      eliteIndex.setValue(eliteIndexInt + 1);
    }

    return child;
  }

  protected static String fitnessReport(TDoubleArrayList fitnesses) {
    if (fitnesses.isEmpty()) {
      return "empty";
    }

    final double max = G4Stat.max(fitnesses);
    final double min = G4Stat.min(fitnesses);

    return Format.format6(min) + " .. " + max;
  }

  protected static double fitnessDeviation(TDoubleArrayList fitnesses) {
    final double mean = G4Stat.mean(fitnesses);
    final double dev = G4Stat.meanDeviation(fitnesses, mean);

    return dev;
  }

  protected static void fitnessSetOrAdd(final TDoubleArrayList fitnesses, int specIndex, double fitness) {
    if (fitnesses.size() == specIndex) {
      fitnesses.insert(specIndex, fitness);
    } else {
      fitnesses.set(specIndex, fitness);
    }
  }

  protected boolean validate(Genome child) {
    for (int i = 0, conditionsSize = conditions.size(); i < conditionsSize; i++) {
      final Condition<Genome> cond = conditions.wrap(i);

      final boolean valid = cond.isValid(strategy, child, newGen, generation);

      if (!valid) {
        conditions.onFailure(cond);
        return false;
      }
    }

    return true;
  }

  protected void loadGen(Context ctx, final List<Specimen> gen) {
    fitnesses.clear();
    gen.clear();

    final List<Object> genome = new ArrayList<Object>();

    for (int specIndex = 0; specIndex < specimenLimit; specIndex++) {
      final Double fitness = ctx.get(
          getKeyFitness(), Double.class,
          new String[]{generationParamName, specimenIndexParamName},
          new int[]{-1, specIndex}
      );

      if (fitness == null) {
        break;
      }

      fitnessSetOrAdd(fitnesses, specIndex, fitness);

      genome.clear();
      for (int i = 0; i == genome.size(); i++) {
        final Object genomeComponent = ctx.get(
            getKeyGenomeIndexed(i), Object.class,
            new String[]{generationParamName, specimenIndexParamName},
            new int[]{-1, specIndex}
        );

        if (genomeComponent != null) {
          genome.add(genomeComponent);
        }
      }
      final Object[] genomeArr = genome.toArray(new Object[genome.size()]);

      final Specimen specimen = new Specimen<Genome>(fitness, strategy.createGenome(genomeArr));

      gen.add(specimen);
    }

    Collections.sort(gen, fitnessComparator);
  }

  protected String getKeyGenomeIndexed(int i) {
    return genKey + ".genome" + i;
  }

  protected String getKeyFitness() {
    return genKey + ".fitness";
  }

  protected String getKeyGenomeBestIndexed(int i) {
    return genKey + ".best.genome" + i;
  }

  protected String getKeyFitnessBest() {
    return genKey + ".best.fitness";
  }

  protected void storeGen(Context ctx, List<Genome> genomes, final TDoubleArrayList fitnesses) {
    fitnesses.clear();
    if (genomes.size() == 0) {
      return;
    }

    int bestIndex = -1;
    for (int gIndex = 0, specNum = genomes.size(); gIndex < specNum && gIndex < specimenLimit; gIndex++) {
      final Genome genome = genomes.get(gIndex);

      final double fitness = strategy.computeFitness(genome);
      if (fitnesses.size() == gIndex) {
        fitnesses.insert(gIndex, fitness);
      } else {
        fitnesses.set(gIndex, fitness);
      }

      if (bestIndex < 0 || fitnesses.get(bestIndex) < fitness) {
        bestIndex = gIndex;
      }

      ctx.put(
          getKeyFitness(), fitness,
          new String[]{specimenIndexParamName},
          new int[]{gIndex}
      );

      final Object[] genomeArr = genome.getGenomeData();
      for (int i = 0, genomeLength = genomeArr.length; i < genomeLength; i++) {
        final Object genomeItem = genomeArr[i];
        ctx.put(
            getKeyGenomeIndexed(i), genomeItem,
            new String[]{specimenIndexParamName},
            new int[]{gIndex}
        );
      }
    }

    if (bestIndex >= 0) {
      ctx.put(getKeyFitnessBest(), fitnesses.get(bestIndex), false);

      final Object[] genomeArr = genomes.get(bestIndex).getGenomeData();
      for (int i = 0, genomeLength = genomeArr.length; i < genomeLength; i++) {
        final Object genomeItem = genomeArr[i];
        ctx.put(getKeyGenomeBestIndexed(i), genomeItem, false);
      }
    }
  }

  protected boolean feasible(int genSize, final long sum) {
    if (sum >= missLimit) {
      log.warn("stopping generation: misses overflow");
      return false;
    }

    return true;
  }

  protected long computeTotalMisses(int[] misses) {
    long sum = 0;
    for (int i = 0; i < misses.length; i++) {
      sum += misses[i];
    }
    return sum;
  }

  public void setCtx(Context ctx) {
    this.ctx = ctx;
  }

  static class SpecimenFitnessComparator implements Comparator<Specimen> {
    public int compare(Specimen o1, Specimen o2) {
      return Double.compare(0, o1.getFitness() - o2.getFitness());
    }
  }
}
