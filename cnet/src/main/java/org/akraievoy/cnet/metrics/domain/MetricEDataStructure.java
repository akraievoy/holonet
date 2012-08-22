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

package org.akraievoy.cnet.metrics.domain;

import org.akraievoy.base.ref.RefRO;
import org.akraievoy.cnet.gen.vo.ConnPreference;
import org.akraievoy.cnet.gen.vo.EntropySource;
import org.akraievoy.cnet.gen.vo.WeightedEventModel;
import org.akraievoy.cnet.gen.vo.WeightedEventModelBase;
import org.akraievoy.cnet.metrics.api.MetricEData;
import org.akraievoy.cnet.net.ref.RefEdgeData;
import org.akraievoy.cnet.net.vo.EdgeData;
import org.akraievoy.cnet.net.vo.RefKeys;

public class MetricEDataStructure extends MetricEData {
  protected final ConnPreference preference;
  protected final EntropySource eSource;

  protected RefRO<EdgeData> distSource = RefEdgeData.forPath(RefKeys.LAYER_DISTANCE);
  protected RefRO<EdgeData> structureSource = RefEdgeData.forPath(RefKeys.LAYER_STRUCTURE);
  protected int baseDegree = 3;

  public MetricEDataStructure(final ConnPreference preference, EntropySource eSource) {
    this.preference = preference;
    this.eSource = eSource;
  }

  public void setBaseDegree(int baseDegree) {
    this.baseDegree = baseDegree;
  }

  public void setDistSource(RefRO<EdgeData> distSource) {
    this.distSource = distSource;
  }

  public void setStructureSource(RefRO<EdgeData> structureSource) {
    this.structureSource = structureSource;
  }

  public void run() {
    final EdgeData dist = distSource.getValue();
    final int size = dist.getSize();

    final EdgeData structureOri = structureSource.getValue();
    final EdgeData structure = structureOri.proto(size);

    structureOri.visitNonDef(new EdgeData.EdgeVisitor() {
      public void visit(int from, int into, double e) {
        structure.set(from, into, e);
      }
    });

    final WeightedEventModel eventModel = new WeightedEventModelBase();

    double[] powers = new double[size];
    int startNode = 0;
    while (startNode < size && (powers[startNode] = structure.power(startNode)) > 0) {
      startNode++;
    }

    for (int i = startNode; i < size; i++) {
      eventModel.clear();

      for (int j = 0; j < i; j++) {
        final double degreeJ = powers[j];
        final double distIJ = dist.get(i, j);

        eventModel.add(j, preference.getPreference(degreeJ, distIJ));
      }

      for (int k = 0; eventModel.getSize() > 0 && k < baseDegree; k++) {
        final Integer conn = eventModel.generate(eSource, true, null);
        structure.set(i, conn, 1.0);
        powers[i] += 1;
        powers[conn] += 1;
      }
    }

    target.setValue(structure);
  }

  public String getName() {
    return "Structure (BA-like)";
  }
}