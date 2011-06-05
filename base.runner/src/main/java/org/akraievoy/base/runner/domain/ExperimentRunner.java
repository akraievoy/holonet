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

import org.akraievoy.base.runner.vo.Experiment;
import org.akraievoy.base.runner.vo.RunInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.SortedMap;

public interface ExperimentRunner {
  Logger log = LoggerFactory.getLogger(ExperimentRunner.class);

  void run(Experiment info, final long confUid, final List<Long> chainedRunIds);

  void setListener(RunStateListener listener);

  RunContext loadRunContext(final Long runUid, long confUid, final List<Long> chainedRunIds);

  interface RunContext {
    SortedMap<Long, RunInfo> getChainedRuns();

    ParamSetEnumerator getRootParams();

    ParamSetEnumerator getWideParams();

    boolean isValid();

    long[] getChainedRunIds();

    long getRunId();
  }
}
