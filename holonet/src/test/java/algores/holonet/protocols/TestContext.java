/*
 Copyright 2012 Anton Kraievoy akraievoy@gmail.com
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

package algores.holonet.protocols;

import algores.holonet.core.Network;
import algores.holonet.testbench.Metrics;
import org.akraievoy.cnet.gen.vo.EntropySource;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class TestContext {
  public static final String METRICS = "test";

  private final Network net;
  private final AtomicLong netFailCount = new AtomicLong(0);
  private final EntropySource entropy;
  private final Map<String, Metrics> nameToMetrics = new HashMap<String,Metrics>();

  public TestContext(EntropySource entropy, Network net) {
    this.entropy = entropy;
    this.net = net;

    injectMetrics(METRICS);
  }

  public void injectMetrics(String newMetricsName) {
    final Metrics newMetrics = Metrics.createInstance(newMetricsName);
    this.net.setInterceptor(newMetrics);
    nameToMetrics.put(newMetricsName, newMetrics);
  }

  public EntropySource getEntropy() {
    return entropy;
  }

  public Network net() {
    return getNet();
  }

  public Network getNet() {
    return net;
  }

  public AtomicLong getNetFailCount() {
    return netFailCount;
  }

  public Map<String, Metrics> nameToMetrics() {
    return Collections.unmodifiableMap(nameToMetrics);
  }
}

