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

import org.akraievoy.holonet.exp.store.RefObject;
import org.akraievoy.cnet.metrics.api.MetricResultFetcher;
import org.akraievoy.cnet.metrics.domain.MetricEDataVertexDiff;
import org.akraievoy.cnet.metrics.domain.MetricVDataEigenGap;
import org.akraievoy.cnet.net.vo.EdgeData;
import org.akraievoy.cnet.stat.domain.MedianClustering;

public class MutatorSooRewireExpand extends MutatorSooRewire {
  protected final MetricVDataEigenGap metricVDataEigenGap;
  protected final MetricEDataVertexDiff metricEDataVertexDiff;

  public MutatorSooRewireExpand() {
    metricVDataEigenGap = new MetricVDataEigenGap();
    metricEDataVertexDiff = new MetricEDataVertexDiff(metricVDataEigenGap);

    metricEDataVertexDiff.setMedian(new MedianClustering());
  }

  protected EdgeData buildLinkFitness(GeneticStrategySoo strategySoo, GenomeSoo genome) {
    final EdgeData linkFitness;

    metricVDataEigenGap.setSource(new RefObject<EdgeData>(genome.getSolution()));
    linkFitness = (EdgeData) MetricResultFetcher.fetch(metricEDataVertexDiff);

    return linkFitness;
  }

  protected boolean isFavoringMinimal() {
    return true;
  }
}