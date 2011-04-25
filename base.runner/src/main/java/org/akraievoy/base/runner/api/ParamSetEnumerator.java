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

package org.akraievoy.base.runner.api;

import org.akraievoy.base.runner.vo.Parameter;

import java.util.List;
import java.util.Properties;

public interface ParamSetEnumerator {
  int COUNT_EMPTY = 1;

  boolean increment(List<String> iteratedParamNames);

  Properties getProperties();

  long getIndex();

  long getCount();

  String[][] getParamValues(String[][] paramValues);

  ParamSetEnumerator dupe(final boolean restart);

  Parameter getParameter(int index);

  void restart();

  long translateIndex(ParamSetEnumerator chainedEnumerator);

  int getParameterIndex(String name);

  void widen(ParamSetEnumerator someOther);

  void narrow(ParamSetEnumerator widenedPse);

  long getIndex(String paramName, int offs);

  long getParameterPos(int parameterIndex);

  long getIndex(String[] paramNames, int[] offsets);

  long getIndex(List<String> standaloneIterated);

  ParamSetEnumerator dupeAt(long psetIndex);

  ParamSetEnumerator dupeOffset(String[] paramNames, int[] offsets);

  boolean isFirst();

  long getCount(List<String> subsetNames);

  boolean containsParam(String name);

  boolean containsParams(List<String> names);
}
