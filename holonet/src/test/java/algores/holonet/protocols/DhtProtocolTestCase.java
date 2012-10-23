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

import algores.holonet.core.CommunicationException;
import algores.holonet.core.Network;
import algores.holonet.core.Node;
import algores.holonet.core.api.Address;
import algores.holonet.core.api.Key;
import algores.holonet.core.api.tier0.storage.StorageService;
import algores.holonet.core.api.tier1.delivery.LookupService;
import algores.holonet.testbench.Metrics;
import junit.framework.TestCase;
import org.akraievoy.base.Stopwatch;

public abstract class DhtProtocolTestCase extends TestCase {
  protected abstract TextContextMeta createContextMeta();

  protected void testHopCount0(final long seed, final int nodes) {
    System.out.println(String.format("testHopCount0(%d, %d)", seed, nodes));

    final Context ctx = createContextMeta().create(seed);
    final Network net = ctx.net();
    net.generateNode(null, ctx.getEntropy(), null);
    net.insertNodes(nodes - 1, ctx.getNetFailCount(), ctx.getEntropy());
    net.putDataEntries(256, ctx.getEntropy());

    Stopwatch stopwatch = new Stopwatch();
    System.out.println("stabilizing:");
    int nodesStabilized = 0;
    for(Node node : net.getAllNodes()){
      node.getServices().getOverlay().stabilize();

      nodesStabilized += 1;
      System.out.print(".");
      if (nodesStabilized % 25 == 24) {
        System.out.println(" " + nodesStabilized + " of " + nodes + " @ " + stopwatch.diff(nodesStabilized % 25 + 1) + " ms per stabilize");
      }
    }
    System.out.println("COMPLETE"); //  FIXME collapse all this reporting

    final Metrics testMetrics = Metrics.createInstance("test");
    net.setInterceptor(testMetrics);

    int testCount = nodes * nodes;
    stopwatch = new Stopwatch();
    for (int testIndex = 0; testIndex < testCount; testIndex++) {
        final Node client = net.getRandomNode(ctx.getEntropy());
        final Node server = net.getRandomNode(ctx.getEntropy());
        final Key request;
        final StorageService serverStorage = server.getServices().getStorage();
        if (serverStorage.getEntryCount() > 0) {
          request = ctx.getEntropy().randomElement(serverStorage.getKeys());
        } else {
          request = server.getKey();
        }
      final Address lookup;
      try {
        lookup = client.getServices().getLookup().lookup(
            request, false, LookupService.Mode.GET
        );
      } catch (CommunicationException e) {
        throw new AssertionError("failed at test # " + testIndex);
      }
      assertEquals("test: " + testIndex, server.getAddress(), lookup);

        System.out.print(".");
        if (testIndex % 25 == 24) {
          System.out.println(" " + (testIndex + 1) + " of " + testCount + " @ " + stopwatch.diff(testIndex % 25 + 1) + " ms per test");
        }
    }
    System.out.println("COMPLETE");
    System.out.println("average hops for " + nodes + " nodes = " + testMetrics.modeToLookups(LookupService.Mode.GET).getMeanPathLength());
    System.out.println("net fail count: " + ctx.getNetFailCount().get());

  }
}
