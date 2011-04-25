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

package org.akraievoy.cnet.net.vo;

import gnu.trove.TDoubleArrayList;
import org.codehaus.jackson.annotate.JsonIgnore;

public class VertexData implements Resizable {
  protected final TDoubleArrayList data = new TDoubleArrayList();
  protected double nullElement;

  @Deprecated
  public VertexData() {
    this(0);
  }

  public VertexData(int nodes) {
    this(0.0, nodes);
  }

  public VertexData(double nullElement, int size) {
    this.nullElement = nullElement;

    setSize(size);
  }

  public double getNullElement() {
    return nullElement;
  }

  @SuppressWarnings({"UnusedDeclaration"})
  public void setNullElement(double nullElement) {
    this.nullElement = nullElement;
  }

  public double[] getData() {
    return data.toNativeArray();
  }

  @Deprecated
  @SuppressWarnings({"UnusedDeclaration"})
  public void setData(double[] data) {
    this.data.clear();
    this.data.add(data);
  }

  public VertexData proto() {
    return new VertexData(nullElement, 0);
  }

  public boolean isNull(int index) {
    return Double.compare(getNullElement(), get(index)) == 0;
  }

  @JsonIgnore
  public int getSize() {
    return data.size();
  }

  @JsonIgnore
  public void setSize(int size) {
    if (getSize() == size) {
      return; // no op
    }

    data.clear();
    for (int i = 0; i < size; i++) {
      data.add(getNullElement());
    }
  }

  public double get(int index) {
    if (index < 0 || index >= data.size()) {
      return getNullElement();
    }

    return data.get(index);
  }

  public double set(int index, double elem) {
    return data.getSet(index, elem);
  }

  public void insert(int index, double elem) {
    data.insert(index, elem);
  }

  public void insert(int index) {
    data.insert(index, getNullElement());
  }

  public void remove(int index) {
    data.remove(index);
  }

  public String toString() {
    return "VertexData[" + getSize() + "]";
  }
}
