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

import junit.framework.TestCase;
import org.akraievoy.cnet.gen.vo.EntropySourceRandom;
import org.akraievoy.cnet.metrics.domain.MetricRoutesFloydWarshall;
import org.akraievoy.cnet.net.ref.RefEdgeData;
import org.akraievoy.cnet.net.vo.EdgeData;
import org.akraievoy.cnet.net.vo.EdgeDataDense;
import org.akraievoy.cnet.opt.api.GeneticState;
import org.akraievoy.cnet.opt.api.Genome;

import java.util.List;

public class BreederSooTest extends TestCase {
  protected BreederSoo breederSoo;
  protected GeneticStrategySoo strategy;
  protected GeneticState state;
  protected EntropySourceRandom esr;
  protected EdgeDataDense dist;
  protected EdgeDataDense req;

  public void setUp() {
    dist = new EdgeDataDense();
    for (int i = 0; i < 5; i++) {
      for (int j = 0; j < 5; j++) {
        if (i != j) {
          dist.set(i, j, (i + 1) * (j + 1));
        }
      }
    }

    req = new EdgeDataDense();
    for (int i = 0; i < 5; i++) {
      for (int j = 0; j < 5; j++) {
        if (i != j) {
          dist.set(i, j, (i + 5) * (j + 6));
        }
      }
    }

    strategy = new GeneticStrategySoo(new MetricRoutesFloydWarshall());
    strategy.setDistSource(new RefEdgeData(dist));
    strategy.setRequestSource(new RefEdgeData(req));
    strategy.setTheta(1);

    state = new GeneticState();
    state.setCompleteness(0.0);
    state.setMaxCrossover(0.5);

    esr = new EntropySourceRandom();
    esr.setSeed(53124);

    breederSoo = new BreederSoo() {
      @Override
      protected EdgeData buildLinkFitness(GeneticStrategySoo strategySoo, GenomeSoo child) {
        final EdgeData resEdgeData = new EdgeDataDense();

        for (int i = 0; i < 5; i++) {
          for (int j = 0; j < 5; j++) {
            if (i != j) {
              resEdgeData.set(i, j, 1);
            }
          }
        }

        return resEdgeData;
      }

      @Override
      protected boolean isFavoringMinimal() {
        return false;
      }
    };
  }

  public void testCrossover() {
    testCrossover(1);
  }

  public void testCrossover_forFiveSteps() {
    testCrossover(5);
  }

  public void testCrossover(final int steps) {
    strategy.setSteps(steps);

    final List<GenomeSoo> spectralSeeds = new SeedSourceSooSpectral().getSeeds(strategy);
    assertFalse(spectralSeeds.isEmpty());

    final GenomeSoo seedA = spectralSeeds.get(0);

    final EdgeData solutionA = seedA.getSolution();
    final int size = solutionA.getSize();
    final EdgeData solutionB = solutionA.proto();
    for (int i = 0; i < 5; i++) {
      for (int j = 0; j < 5; j++) {
        if (i != j) {
          solutionB.set(i, j, solutionA.get((i + 3) % size, (j + 3) % size));
        }
      }
    }

    final GenomeSoo seedB = strategy.createGenome(new Object[]{solutionB});

    state.calibrate(null);
    final Genome child = breederSoo.crossover(strategy, seedA, seedB, state, esr);

    System.out.println("\nseedA = \n" + EdgeData.Util.dump(seedA.getSolution()));
    System.out.println("\nseedB = \n" + EdgeData.Util.dump(seedB.getSolution()));
    System.out.println("\nchild = \n" + EdgeData.Util.dump(((GenomeSoo) child).getSolution()));

    //	TODO add asserts on the actual values
/*
		for (int i = 0; i < ((GenomeSoo) child).getSolution().getSize(); i++) {
			assertEquals(0.0, ((GenomeSoo) child).getSolution().get(i, i));
		}

		for (int[] fi : new int[][]{{0, 2}, {0, 4}, {1, 2}, {1, 4}, {2, 3}, {2, 4}}) {
			assertEquals(1.0, ((GenomeSoo) child).getSolution().get(fi[0], fi[1]));
			assertEquals(1.0, ((GenomeSoo) child).getSolution().get(fi[1], fi[0]));
		}

		for (int[] fi : new int[][]{{0, 1}, {0, 3}, {1, 3}, {3, 4}}) {
			assertEquals(0.0, ((GenomeSoo) child).getSolution().get(fi[0], fi[1]));
			assertEquals(0.0, ((GenomeSoo) child).getSolution().get(fi[1], fi[0]));
		}
*/
  }
}
