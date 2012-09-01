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

import org.akraievoy.cnet.metrics.api.MetricEData;
import org.akraievoy.cnet.net.vo.EdgeDataFactory;

public class MetricEDataFiller extends MetricEData {
  protected double value = 0.0;
  protected int size = 0;

  public MetricEDataFiller() {
  }

  public MetricEDataFiller(double value) {
    this.value = value;
  }

  public String getName() {
    return "Filler with " + value;
  }

  public void setSize(int size) {
    this.size = size;
  }

  public void setValue(double value) {
    this.value = value;
  }

  public void run() {
    target.setValue(EdgeDataFactory.sparse(true, value, size));
  }
}