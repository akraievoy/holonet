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

import com.google.common.primitives.Longs;
import org.akraievoy.base.runner.api.ParamSetEnumerator;

import java.util.ArrayList;
import java.util.List;

public class RunInfo {
  protected final Run run;
  protected final ParamSetEnumerator enumerator;

  public RunInfo(ParamSetEnumerator enumerator, Run run) {
    this.enumerator = enumerator;
    this.run = run;
  }

  public Run getRun() {
    return run;
  }

  public ParamSetEnumerator getEnumerator() {
    return enumerator;
  }

  public static long[] getRunIds(RunInfo[] chainedRuns) {
    final List<Long> chainedRunIds = new ArrayList<Long>(chainedRuns.length);

    for (int i = 0, chainedRunsLength = chainedRuns.length; i < chainedRunsLength; i++) {
      final long id = chainedRuns[i].getRun().getUid();

      if (!chainedRunIds.contains(id)) {
        chainedRunIds.add(id);
      }
    }

    return Longs.toArray(chainedRunIds);
  }
}
