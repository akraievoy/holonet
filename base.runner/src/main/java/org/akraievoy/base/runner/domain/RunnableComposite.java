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

import org.akraievoy.base.Startable;
import org.akraievoy.base.runner.api.Context;
import org.akraievoy.base.runner.api.ContextInjectable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class RunnableComposite implements Startable, ContextInjectable, Runnable {
  private static final Logger log = LoggerFactory.getLogger(RunnableComposite.class);

  protected List<Runnable> group = new ArrayList<Runnable>();

  protected Context ctx;

  public void setGroup(List<Runnable> group) {
    this.group.clear();
    this.group.addAll(group);
  }

  public void start() {
    for (Runnable harness : group) {
      if (harness instanceof ContextInjectable) {
        ((ContextInjectable) harness).setCtx(ctx);
      }
    }
  }

  public void stop() {
    //	nothing to do...
  }

  public void run() {
    runGroup(ctx);
  }

  protected void runGroup(Context ctx) {
    for (int i = 0, groupSize = group.size(); i < groupSize; i++) {
      final Runnable harness = group.get(i);

      log.debug("starting #{} of {}: {}", new Object[]{i, groupSize, harness.toString()});
      runChild(harness, ctx);
    }
  }

  protected void runChild(Runnable runnable, Context ctx) {
    runnable.run();
  }

  public void setCtx(Context ctx) {
    this.ctx = ctx;
  }
}
