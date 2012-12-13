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

package org.akraievoy.cnet.metrics.vo;

public class StatProps {
  protected static final double THRESH = 1e-12;

  public final double max;
  public final double mid;
  public final double min;
  public final double diff;

  public StatProps(double min, double max) {
    this.max = max;
    this.min = min;
    this.mid = (min + max) / 2;
    this.diff = max - min;
  }

  public boolean isSingleValued() {
    return diff < THRESH;
  }
}
