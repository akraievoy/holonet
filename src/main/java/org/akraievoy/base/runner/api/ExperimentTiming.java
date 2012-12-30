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

import com.google.common.base.Throwables;
import org.akraievoy.base.Format;
import org.akraievoy.base.ref.Ref;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * Simple decorator to track Experiment execution times.
 */
public class ExperimentTiming implements Runnable, ContextInjectable {
  private static final Logger log = LoggerFactory.getLogger(ExperimentTiming.class);

  protected final int durationReportThresh = 10000;

  protected Context ctx;
  protected Runnable wrapped;
  protected Ref<Long> durationRef = new RefLong();
  protected Ref<String> durationTextRef = new RefString();

  public ExperimentTiming(Runnable wrapped) {
    this.wrapped = wrapped;
  }

  public void setDurationRef(Ref<Long> durationRef) {
    this.durationRef = durationRef;
  }

  public void setDurationTextRef(Ref<String> durationTextRef) {
    this.durationTextRef = durationTextRef;
  }

  public void run() {
    final Date start = new Date();
    log.trace("Started {} on {}", wrapped.toString(), Format.format(start, true));

    try {
      wrapped.run();
    } catch (Throwable e) {
      log.warn("failed: " + Throwables.getRootCause(e).toString());
      throw Throwables.propagate(e);
    } finally {
      final long runMillis = System.currentTimeMillis() - start.getTime();
      if (runMillis > durationReportThresh) {
        log.info("Completed {} in {}", wrapped.toString(), Format.formatDuration(runMillis));
      } else {
        log.debug("Completed {} in {}", wrapped.toString(), Format.formatDuration(runMillis));
      }

      durationRef.setValue(runMillis);
      durationTextRef.setValue(Format.formatDuration(runMillis));
    }
  }

  public void setCtx(Context ctx) {
    if (wrapped instanceof ContextInjectable) {
      ((ContextInjectable) wrapped).setCtx(ctx);
    }

    this.ctx = ctx;
  }
}