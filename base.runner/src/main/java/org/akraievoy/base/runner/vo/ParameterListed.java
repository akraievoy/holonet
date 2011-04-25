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

package org.akraievoy.base.runner.vo;

import com.google.common.base.Objects;
import org.akraievoy.base.Die;

import java.util.Arrays;

public class ParameterListed implements Parameter {
  private static final String SEPARATOR_REGEX = ";";

  private final String name;
  private final String[] values;
  private String desc;
  private boolean internal;

  public ParameterListed(final String name, final String valueSpec) {
    this(name, valueSpec.split(SEPARATOR_REGEX));
  }

  protected ParameterListed(final String newName, final String[] newValues) {
    name = newName;
    values = newValues.clone();
  }

  public String getName() {
    return name;
  }

  public boolean isInternal() {
    return internal;
  }

  public String getDesc() {
    return desc;
  }

  public void setDesc(String desc) {
    this.desc = desc;
  }

  public void setInternal(boolean internal) {
    this.internal = internal;
  }

  public long getValueCount() {
    return values.length;
  }

  public String getValue(final long index) {
    return values[(int) index];
  }

  public boolean hasSameValues(Parameter param) {
    if (param instanceof ParameterListed) {
      return Arrays.deepEquals(values, ((ParameterListed) param).values);
    }

    if (getValueCount() != param.getValueCount()) {
      return false;
    }

    for (int i = 0; i < getValueCount(); i++) {
      if (!Objects.equal(getValue(i), param.getValue(i))) {
        return false;
      }
    }

    return true;
  }

  public void validatePos(long pos) {
    Die.ifTrue("pos < 0", pos < 0);
    Die.ifTrue("pos >= valueCount", pos >= getValueCount());
  }

  public String[] getValues() {
    return values.clone();
  }

  public String toString() {
    if (values == null) {
      return String.valueOf(name);
    }

    return String.valueOf(name) + "[" + getValueCount() + "] {" + Arrays.deepToString(values) + "}";
  }
}