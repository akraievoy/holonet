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
import org.akraievoy.cnet.metrics.api.Metric;
import org.akraievoy.holonet.exp.store.RefObject;
import org.akraievoy.cnet.gen.domain.MetricEDataGenStructural;
import org.akraievoy.cnet.metrics.domain.MetricScalarEigenGap;
import org.akraievoy.cnet.metrics.domain.MetricVDataEigenGap;
import org.akraievoy.cnet.net.vo.EdgeData;
import org.akraievoy.cnet.net.vo.VertexData;
import org.akraievoy.cnet.opt.api.GeneticStrategy;
import org.akraievoy.cnet.opt.api.SeedSource;
import org.akraievoy.cnet.stat.api.Median;
import org.akraievoy.cnet.stat.domain.MedianClustering;
import org.apache.log4j.BasicConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/*
 * 0.7752695142636543 - star, power amp
 * 0.7752695142636543 - star, no power amp
 * crash* - cycle, power amp, thresh = 1e-9
 * crash* - cycle, power amp, thresh = 1e-12
 * 0.6966333393226684 - cycle, no power amp
 * 0.6966333393226684 - cycle, no power amp, thresh = 1e-12
 */
public class SeedSourceSooSpectral implements SeedSource<GenomeSoo> {
  private static final Logger log = LoggerFactory.getLogger(SeedSourceSooSpectral.class);

  protected boolean debug = true;
  protected double thresh = 1e-12;

  protected MetricEDataGenStructural gen = new MetricEDataGenStructural(MetricEDataGenStructural.TYPE_STAR);
  protected Median median = new MedianClustering();

  protected long reportPeriod = 10000;
  protected long lastReport = 0;

  public List<GenomeSoo> getSeeds(GeneticStrategy strategy) {
    final GeneticStrategySoo strategySoo = (GeneticStrategySoo) strategy;


    final int limit = (int) Math.floor(strategySoo.getTotalLinkUpperLimit());
    final int size = strategySoo.getDistSource().getValue().getSize();

    final GenomeSoo genome = generateSeed(limit, size);

    return Collections.singletonList(genome);
  }

  public void setDebug(boolean debug) {
    SeedSourceSooSpectral.this.debug = debug;
  }

  public void setThresh(double thresh) {
    SeedSourceSooSpectral.this.thresh = thresh;
  }

  public void setMedian(Median median) {
    this.median = median;
  }

  public void setGen(MetricEDataGenStructural gen) {
    this.gen = gen;
  }

  protected GenomeSoo generateSeed(int limit, int size) {
    gen.setNetNodeNum(size);

    EdgeData solution = Metric.fetch(gen);

    final MetricVDataEigenGap eigenVData = new MetricVDataEigenGap();
    eigenVData.setSource(new RefObject<EdgeData>(solution));

    for (int addedEdges = solution.getNonDefCount() / 2; addedEdges < limit; addedEdges++) {
      final VertexData eigenVector = Metric.fetch(eigenVData);

      double median = this.median.computeMedian(eigenVector.getData());
      double maxDiff = Double.NaN;
      int from = -1;
      int into = -1;

      for (int curFrom = 0; curFrom < size - 1; curFrom++) {
        final double fromVal = eigenVector.get(curFrom);
        for (int curInto = curFrom + 1; curInto < size; curInto++) {
          if (solution.conn(curFrom, curInto)) {
            continue;
          }

          final double intoVal = eigenVector.get(curInto);
          if (
              fromVal < median + thresh && intoVal < median + thresh ||
                  fromVal > median - thresh && intoVal > median - thresh
              ) {
            continue;
          }

          final double curDiff = Math.abs(fromVal - intoVal);
          if (Double.isNaN(maxDiff) || (curDiff > maxDiff)) {
            maxDiff = curDiff;
            from = curFrom;
            into = curInto;
          }
        }
      }

      if (Double.isNaN(maxDiff)) {
        throw new IllegalStateException("failed to add link #" + addedEdges);
      }

      solution.set(from, into, 1.0);

      final long currentTime = System.currentTimeMillis();
      if (currentTime - lastReport > reportPeriod) {
        lastReport = currentTime;
        log.debug(
            "generated {} of {} links ({}%)...",
            new Object[]{addedEdges, limit, Format.format(100.0 * addedEdges / limit)
            });
      }
    }

    final MetricScalarEigenGap eigenGapScalar = new MetricScalarEigenGap();
    eigenGapScalar.setSource(new RefObject<EdgeData>(solution));
    final double eigenGap = Metric.fetch(eigenGapScalar);
    log.info("seed eigengap = {}", eigenGap);

    return new GenomeSoo(solution);
  }

  public static void main(String[] args) {
    BasicConfigurator.configure();

    final SeedSourceSooSpectral sooSpectral = new SeedSourceSooSpectral();
    sooSpectral.gen.setType(MetricEDataGenStructural.TYPE_CYCLE);
    final GenomeSoo genomeSoo = sooSpectral.generateSeed(2000, 240);

//		final GenomeSoo genomeSoo = sooSpectral.generateSeed(7 * 64, 128);

    genomeSoo.getSolution().visitNonDef(new EdgeData.EdgeVisitor() {
      public void visit(int from, int into, double e) {
        System.out.println(from + " <-> " + into);
      }
    });
  }

}
