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

package org.akraievoy.base.runner.domain;

import org.akraievoy.base.Die;
import org.akraievoy.base.runner.api.ParamSetEnumerator;
import org.akraievoy.base.runner.vo.Parameter;

import java.util.*;

public class ParamSetEnumeratorBase implements ParamSetEnumerator {
  protected final List<Parameter> params = new ArrayList<Parameter>();
  protected final List<Long> paramPoses = new ArrayList<Long>();

  public boolean increment(List<String> iteratedParamNames) {
    int overflow = paramPoses.size();
    while (overflow > 0 && hasOverflow(iteratedParamNames, overflow - 1)) {
      overflow--;
      paramPoses.set(overflow, 0L);
    }
    if (overflow == 0) {
      return false;
    }

    int increment = overflow - 1;
    paramPoses.set(increment, paramPoses.get(increment) + 1);
    return true;
  }

  protected boolean hasOverflow(List<String> iterated, final int idx) {
    final Parameter param = params.get(idx);

    if (iterated.indexOf(param.getName()) >= 0) {
      return true;
    }

    final long pos = paramPoses.get(idx);
    final boolean overflow = pos == param.getValueCount() - 1;

    return overflow;
  }

  public void load(final List<Parameter> myParams) {
    Die.ifNull("params", params);

    params.clear();
    paramPoses.clear();

    for (Parameter param : myParams) {
      params.add(param);
      paramPoses.add(0L);
    }

    //  TODO we should not sort anything here: keep param order as it is
    //	this ensures that most crowded parameters are evaluated first
    Collections.sort(params, new ComparatorParameterDepth());
  }

  public Properties getProperties() {
    Properties props = new Properties();

    for (int i = 0, paramsSize = params.size(); i < paramsSize; i++) {
      final Parameter param = params.get(i);
      final long paramPos = paramPoses.get(i);

      props.setProperty(param.getName(), param.getValue(paramPos));
    }

    return props;
  }

  public String[][] getParamValues(String[][] paramValues) {
    final boolean noProvided = paramValues == null || paramValues.length != params.size();
    final String[][] result = noProvided ? new String[params.size()][] : paramValues;

    for (int i = 0, paramsSize = params.size(); i < paramsSize; i++) {
      final Parameter param = params.get(i);
      final long paramPos = paramPoses.get(i);

      if (result[i] == null || result[i].length < 2) {
        result[i] = new String[]{param.getName(), param.getValue(paramPos)};
      } else {
        result[i][0] = param.getName();
        result[i][1] = param.getValue(paramPos);
      }
    }

    return result;
  }

  public ParamSetEnumerator dupe(final boolean restart) {
    final ParamSetEnumeratorBase dupe = new ParamSetEnumeratorBase();

    dupe.params.addAll(params);
    dupe.paramPoses.addAll(paramPoses);
    if (restart) {
      dupe.restart();
    }

    return dupe;
  }

  public void widen(final ParamSetEnumerator someOther) {
    final ParamSetEnumeratorBase other = (ParamSetEnumeratorBase) someOther;

    for (Parameter otherParam : other.params) {
      final int rootIndex = getParameterIndex(otherParam.getName());
      if (rootIndex >= 0) {
        continue;
      }

      params.add(otherParam);
      paramPoses.add(0L);
    }
  }

  public void narrow(ParamSetEnumerator widenedPse) {
    final ParamSetEnumeratorBase widened = (ParamSetEnumeratorBase) widenedPse;

    for (int i = 0, paramsSize = params.size(); i < paramsSize; i++) {
      final int widenedI = widened.getParameterIndex(params.get(i).getName());
      Die.ifTrue("widenedI < 0", widenedI < 0);

      paramPoses.set(i, widened.paramPoses.get(widenedI));
    }
  }

  public long getCount() {
    if (params.size() == 0) {
      return COUNT_EMPTY;
    }

    long result = COUNT_EMPTY;
    for (Parameter param : params) {
      result *= param.getValueCount();
    }

    return result;
  }

  public long getCount(List<String> subsetNames) {
    if (params.size() == 0) {
      return COUNT_EMPTY;
    }

    long result = COUNT_EMPTY;
    for (Parameter param : params) {
      if (subsetNames.contains(param.getName())) {
        result *= param.getValueCount();
      }
    }

    return result;
  }

  public long getIndex() {
    return getIndexForPos(paramPoses);
  }

  public long getIndex(final String paramName, final int offs) {
    final int paramIndex = getParameterIndex(paramName);
    Die.ifFalse("paramIndex >= 0", paramIndex >= 0);

    final List<Long> paramPosesOffset = new ArrayList<Long>(paramPoses);
    paramPosesOffset.set(paramIndex, paramPosesOffset.get(paramIndex) + offs);

    final Parameter param = params.get(paramIndex);
    param.validatePos(paramPosesOffset.get(paramIndex));

    return getIndexForPos(paramPosesOffset);
  }

  public long getIndex(final String[] paramNames, final int[] offsets) {
    final List<Long> paramPosesOffset = new ArrayList<Long>(paramPoses);

    for (int i = 0, paramNamesLength = paramNames.length; i < paramNamesLength; i++) {
      String paramName = paramNames[i];
      final int paramIndex = getParameterIndex(paramName);
      Die.ifFalse("paramIndex >= 0", paramIndex >= 0);

      paramPosesOffset.set(paramIndex, paramPosesOffset.get(paramIndex) + offsets[i]);

      final Parameter param = params.get(paramIndex);
      param.validatePos(paramPosesOffset.get(paramIndex));
    }

    return getIndexForPos(paramPosesOffset);
  }

  public long getIndex(List<String> standaloneIterated) {
    final List<Long> paramPosesIterated = new ArrayList<Long>(paramPoses);

    for (String standalone : standaloneIterated) {
      final int paramIndex = getParameterIndex(standalone);
      Die.ifFalse("paramIndex >= 0", paramIndex >= 0);

      paramPosesIterated.set(paramIndex, params.get(paramIndex).getValueCount() - 1);

      final Parameter param = params.get(paramIndex);
      param.validatePos(paramPosesIterated.get(paramIndex));
    }

    return getIndexForPos(paramPosesIterated);
  }

  public ParamSetEnumerator dupeAt(long psetIndex) {
    final ParamSetEnumeratorBase dupe = new ParamSetEnumeratorBase();

    dupe.params.addAll(params);
    dupe.paramPoses.addAll(paramPoses);

    psetIndex -= COUNT_EMPTY;
    for (int paramI = params.size() - 1; paramI >= 0; paramI--) {
      final long valueCount = params.get(paramI).getValueCount();
      dupe.paramPoses.set(paramI, psetIndex % valueCount);
      psetIndex /= valueCount;
    }

    return dupe;
  }

  public ParamSetEnumerator dupeOffset(final String[] paramNames, final int[] offsets) {
    final List<Long> paramPosesOffset = new ArrayList<Long>(paramPoses);

    for (int i = 0, paramNamesLength = paramNames.length; i < paramNamesLength; i++) {
      String paramName = paramNames[i];
      final int paramIndex = getParameterIndex(paramName);
      Die.ifFalse("paramIndex >= 0", paramIndex >= 0);

      paramPosesOffset.set(paramIndex, paramPosesOffset.get(paramIndex) + offsets[i]);

      final Parameter param = params.get(paramIndex);
      param.validatePos(paramPosesOffset.get(paramIndex));
    }

    final ParamSetEnumeratorBase dupe = new ParamSetEnumeratorBase();

    dupe.params.addAll(params);
    dupe.paramPoses.addAll(paramPosesOffset);

    return dupe;
  }

  public boolean isFirst() {
    return getIndex() == COUNT_EMPTY;
  }

  public long getParameterPos(int parameterIndex) {
    return paramPoses.get(parameterIndex);
  }

  protected long getIndexForPos(final List<Long> paramPoses) {
    if (params.size() == 0) {
      return COUNT_EMPTY;
    }

    long result = COUNT_EMPTY;
    long pow = 1;
    for (int paramI = params.size() - 1; paramI >= 0; paramI--) {
      result += pow * paramPoses.get(paramI);
      pow *= params.get(paramI).getValueCount();
    }

    return result;
  }

  public Parameter getParameter(int index) {
    return index < 0 ? params.get(params.size() + index) : params.get(index);
  }

  public int getParameterIndex(String name) {
    for (int index = 0; index < params.size(); index++) {
      final Parameter param = params.get(index);
      if (param.getName().equals(name)) {
        return index;
      }
    }

    return -1;
  }

  public boolean containsParam(String name) {
    return getParameterIndex(name) >= 0;
  }

  public boolean containsParams(final List<String> names) {
    for (String name : names) {
      if (!containsParam(name)) {
        return false;
      }
    }

    return true;
  }

  public void restart() {
    Collections.fill(paramPoses, 0L);
  }

  public Parameter findCollision(ParamSetEnumeratorBase chainedEnumerator) {
    for (Parameter chainedParam : chainedEnumerator.params) {
      final int rootIndex = getParameterIndex(chainedParam.getName());
      if (rootIndex < 0) {
        continue;
      }

      final Parameter rootParam = getParameter(rootIndex);
      if (!rootParam.hasSameValues(chainedParam)) {
        return rootParam;
      }
    }

    return null;
  }

  public long translateIndex(ParamSetEnumerator chainedEnumerator) {
    if (params.size() == 0) {
      return 0;
    }

    ParamSetEnumeratorBase chained = (ParamSetEnumeratorBase) chainedEnumerator;

    final List<Long> chainedPoses = new ArrayList<Long>(chained.paramPoses);
    for (int i = 0, paramsSize = params.size(); i < paramsSize; i++) {
      Parameter rootParam = params.get(i);
      final int chainedIndex = chainedEnumerator.getParameterIndex(rootParam.getName());
      if (chainedIndex < 0) {
        continue;
      }

      chainedPoses.set(chainedIndex, paramPoses.get(i));
    }

    return chained.getIndexForPos(chainedPoses);
  }

  protected static class ComparatorParameterDepth implements Comparator<Parameter> {
    public int compare(Parameter o1, Parameter o2) {
      return new Long(o1.getValueCount()).compareTo(o2.getValueCount());
    }
  }
}
