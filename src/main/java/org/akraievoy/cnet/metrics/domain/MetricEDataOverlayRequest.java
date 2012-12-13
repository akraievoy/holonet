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
import org.akraievoy.cnet.gen.vo.EntropySource;
import org.akraievoy.cnet.metrics.api.MetricEData;
import org.akraievoy.cnet.net.ref.RefVertexData;
import org.akraievoy.cnet.net.vo.EdgeData;
import org.akraievoy.cnet.net.vo.EdgeDataFactory;
import org.akraievoy.cnet.net.vo.VertexData;

public class MetricEDataOverlayRequest extends MetricEData {
  protected final EntropySource eSource;

  protected RefRO<VertexData> source = RefVertexData.forPath("density");

  protected double phi = 0.25;
  protected double psi = 0.75;
  protected double sigma = 1;

  public MetricEDataOverlayRequest(EntropySource eSource) {
    this.eSource = eSource;
  }

  /**
   * @param phi degree-based preference (as power of population density) to emit requests from this node
   */
  public void setPhi(double phi) {
    this.phi = phi;
  }

  /**
   * @param psi degree-based preference (as power of population density) to emit requests to this node
   */
  public void setPsi(double psi) {
    this.psi = psi;
  }

  /**
   * @param sigma request amount deviation
   */
  public void setSigma(double sigma) {
    this.sigma = sigma;
  }

  public void setSource(RefRO<VertexData> source) {
    this.source = source;
  }

  public void run() {
    @SuppressWarnings({"unchecked"})
    final VertexData density = source.getValue();
    final int size = density.getSize();
    final EdgeData requests = EdgeDataFactory.dense(false, 0.0, size);

    for (int i = 1; i < size; i++) {
      final double densityI = density.get(i);

      for (int j = 0; j < i; j++) {
        final double densityJ = density.get(j);

        final double reqIJ = eSource.nextLogGaussian(
            phi * Math.log(densityI) + psi * Math.log(densityJ),
            sigma
        );

        final double reqJI = eSource.nextLogGaussian(
            phi * Math.log(densityJ) + psi * Math.log(densityI),
            sigma
        );

        requests.set(i, j, reqIJ);
        requests.set(j, i, reqJI);
      }
    }

    target.setValue(requests);
  }

  public String getName() {
    return "Requests";
  }
}