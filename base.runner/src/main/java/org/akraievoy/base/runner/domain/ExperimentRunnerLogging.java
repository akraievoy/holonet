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

import com.google.common.base.Throwables;
import org.akraievoy.base.Format;
import org.akraievoy.base.runner.api.Context;
import org.akraievoy.base.runner.persist.ExperimentRegistry;
import org.akraievoy.base.runner.persist.RunnerDao;
import org.akraievoy.base.runner.vo.Experiment;
import org.akraievoy.base.runner.vo.Parameter;
import org.akraievoy.base.runner.vo.RunInfo;

import java.sql.SQLException;
import java.util.SortedMap;

//	LATER this looks like a listener, not a subclass
public class ExperimentRunnerLogging extends ExperimentRunnerImpl {
  protected long startMillis;

  public ExperimentRunnerLogging(final RunnerDao dao, final ExperimentRegistry expDao) {
    super(dao, expDao);
  }

  protected void runIterative(
      long runId, Experiment exp,
      ParamSetEnumerator paramSetEnumerator, ParamSetEnumerator widened,
      SortedMap<Long, RunInfo> runChain
  ) {
    log.info("Starting {} with runId = {}", exp.getDesc(), runId);
    startMillis = System.currentTimeMillis();

    try {
      super.runIterative(runId, exp, paramSetEnumerator, widened, runChain);

      log.info("Completed experiment {}, runId = {}", exp.getDesc(), runId);
    } catch (Exception e) {
      log.error("Failed {} ", Throwables.getRootCause(e).toString());
      throw Throwables.propagate(e);
    }
  }

  protected void runForPoses(
      long runId, Experiment exp,
      ParamSetEnumerator widenedPse, ParamSetEnumerator pse, 
      final Context ctx) {
    final long paramSetCount = widenedPse.getCount();
    final Runtime runtime = Runtime.getRuntime();

    super.runForPoses(runId, exp, widenedPse, pse, ctx);

    final long totalMem = runtime.totalMemory();
    final long freeMem = runtime.freeMemory();
    if (paramSetCount > 0) {
      final long index = widenedPse.getIndex();
      log.info("Parameter set {} / {}, mem used {} / {}, ETA: {} ",
          new Object[]{
              index, paramSetCount,
              Format.formatMem(totalMem - freeMem), Format.formatMem(totalMem),
              Format.formatDuration((System.currentTimeMillis() - startMillis) * (paramSetCount - index) / index)
          });
    } else {
      log.info("Mem used {} / {} ", Format.formatMem(totalMem - freeMem), Format.formatMem(totalMem));
    }
  }

  protected void reportUpdateCompleteFailed(SQLException e) {
    ExperimentRunner.log.warn("failed to update complete marker: {}", Throwables.getRootCause(e).toString());
  }

  protected void reportChainedCollision(RunInfo runI, RunInfo runJ, Parameter collision) {
    log.warn(
        "param set for run {} collides with param set for run {} at parameter {}",
        new Object[]{runI, runJ, collision.getName()}
    );
  }

  protected void reportRootCollision(RunInfo run, Parameter collision) {
    log.warn("param set for run {} collides with root at parameter {}", run.getRun(), collision.getName());
  }

}
