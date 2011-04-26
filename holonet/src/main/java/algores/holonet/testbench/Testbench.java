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

package algores.holonet.testbench;

import algores.holonet.core.Network;
import algores.holonet.core.events.Event;
import org.akraievoy.base.runner.api.Context;
import org.akraievoy.base.runner.api.ContextInjectable;
import org.akraievoy.base.runner.api.RefLong;
import org.akraievoy.cnet.gen.vo.EntropySourceRandom;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs collection of <code>Events</code> on a given <code>Network</code>,
 * providing data for gathering different metrics data.
 */
public class Testbench implements Runnable, ContextInjectable {
  private Network network = new Network();

  private Event initialEvent;
  private Event runtimeEvent;

  private Metrics currentMetrics;

  private List<Metrics> periodStats = new ArrayList<Metrics>();
  private List<Snapshot> snapshots = new ArrayList<Snapshot>();

  private Context ctx;
  private RefLong initSeedRef = new RefLong(123456);
  private RefLong runSeedRef = new RefLong(654321);

  private EntropySourceRandom initEntropySource = new EntropySourceRandom();
  private EntropySourceRandom runEntropySource = new EntropySourceRandom();

  public Testbench() {
  }

  public void setInitialEvent(Event initialEvent) {
    this.initialEvent = initialEvent;
  }

  public void setRuntimeEvent(Event runtimeEvent) {
    this.runtimeEvent = runtimeEvent;
  }

  public void setInitSeedRef(RefLong initSeedRef) {
    this.initSeedRef = initSeedRef;
  }

  public void setRunSeedRef(RefLong runSeedRef) {
    this.runSeedRef = runSeedRef;
  }

  public void setNetwork(Network network) {
    this.network = network;
  }

  public void startPeriod(String name) {
    stopCurrentPeriod();
    currentMetrics = Metrics.createInstance(name);
    network.setInterceptor(currentMetrics);
  }

  public void stopCurrentPeriod() {
    if (currentMetrics == null) {
      return;
    }

    periodStats.add(currentMetrics);
    currentMetrics = null;
  }

  protected void storeSnapshot(String name) {
    Snapshot snapshot = new Snapshot(name);
    snapshot.process(network);
    snapshots.add(snapshot);
  }

  public void run() {
    if (false) {
      //  FIXME how do we detect previous experiments with pre-generatied networks?
    }

    this.initEntropySource.setSeed(initSeedRef.getValue());
    this.runEntropySource.setSeed(runSeedRef.getValue());

    initialEvent.execute(network, initEntropySource);

    startPeriod("run");
    storeSnapshot("preRun_");

    runtimeEvent.execute(network, runEntropySource);

    stopCurrentPeriod();
    storeSnapshot("postRun_");

    for (Snapshot snap : snapshots) {
      snap.store(ctx);
    }
    for (Metrics mtx : periodStats) {
      mtx.store(ctx);
    }
  }

  public void setCtx(Context ctx) {
    this.ctx = ctx;
  }
}

