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
import org.akraievoy.base.ref.Ref;
import org.akraievoy.holonet.exp.store.RefObject;
import org.akraievoy.cnet.gen.vo.EntropySourceRandom;
import org.akraievoy.holonet.exp.store.StoreLens;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs collection of <code>Events</code> on a given <code>Network</code>,
 * providing data for gathering different metrics data.
 */
public class Testbench implements Runnable {
  private Network network = new Network();

  private Event initialEvent;
  private Event runtimeEvent;

  private Metrics currentMetrics;

  private List<Metrics> periodStats = new ArrayList<Metrics>();
  private List<Snapshot> snapshots = new ArrayList<Snapshot>();

  private StoreLens<Double> reportLens;
  private Ref<Long> initSeedRef = new RefObject<Long>(123456L);
  private Ref<Long> runSeedRef = new RefObject<Long>(654321L);

  private EntropySourceRandom initEntropySource = new EntropySourceRandom();
  private EntropySourceRandom runEntropySource = new EntropySourceRandom();

  public Testbench() {
  }

  public void setInitialEvent(Event<?> initialEvent) {
    this.initialEvent = initialEvent;
  }

  public void setRuntimeEvent(Event<?> runtimeEvent) {
    this.runtimeEvent = runtimeEvent;
  }

  public void setInitSeedRef(Ref<Long> initSeedRef) {
    this.initSeedRef = initSeedRef;
  }

  public void setRunSeedRef(Ref<Long> runSeedRef) {
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
    try {
      network.getEnv().init();

      this.initEntropySource.setSeed(initSeedRef.getValue());
      this.runEntropySource.setSeed(runSeedRef.getValue());

      initialEvent.execute(network, initEntropySource);

      startPeriod("run");
      storeSnapshot("preRun");

      runtimeEvent.execute(network, runEntropySource);

      stopCurrentPeriod();
      storeSnapshot("postRun");

      for (Snapshot snap : snapshots) {
        snap.store(reportLens);
      }
      for (Metrics mtx : periodStats) {
        mtx.store(reportLens);
      }
    } finally {
      network.dispose();
    }
  }

  public void setReportLens(StoreLens<Double> reportLens) {
    this.reportLens = reportLens;
  }
}

