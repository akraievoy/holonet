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

import gnu.trove.TDoubleArrayList;
import org.codehaus.jackson.annotate.JsonIgnore;

public class HistogramImpl implements Histogram {
  protected TDoubleArrayList arguments;
  protected TDoubleArrayList values;

  public HistogramImpl(double[] arguments, double[] values) {
    this.arguments = new TDoubleArrayList(arguments);
    this.values = new TDoubleArrayList(values);
  }

  public double getArgumentAt(int at) {
    return arguments.get(at);
  }

  public double getValueAt(int at) {
    return values.get(at);
  }

  public double[] getArguments() {
    return arguments.toNativeArray();
  }

  @SuppressWarnings({"UnusedDeclaration"})
  public void setArguments(TDoubleArrayList arguments) {
    this.arguments = arguments;
  }

  public double[] getValues() {
    return values.toNativeArray();
  }

  @SuppressWarnings({"UnusedDeclaration"})
  public void setValues(TDoubleArrayList values) {
    this.values = values;
  }

  @JsonIgnore
  public int getLength() {
    return values.size();
  }
}
