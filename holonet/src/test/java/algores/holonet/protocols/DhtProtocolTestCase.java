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

import algores.holonet.core.*;
import algores.holonet.core.api.API;
import algores.holonet.core.api.Address;
import algores.holonet.core.api.Key;
import algores.holonet.core.api.tier0.routing.RoutingServiceBase;
import algores.holonet.core.api.tier0.storage.StorageService;
import algores.holonet.core.api.tier1.delivery.LookupService;
import algores.holonet.testbench.Metrics;
import junit.framework.TestCase;

public abstract class DhtProtocolTestCase extends TestCase {
  protected abstract ContextMeta createContextMeta();

  protected ProgressMeta progressMeta() { return ProgressMeta.DEFAULT; }

  protected void testHopCount0(final long seed, final int nodes, final int avgHopLimit) {
    System.out.println(String.format("testHopCount0(%d, %d)", seed, nodes));

    final Context ctx = createContextMeta().create(seed);
    final Network net = ctx.net();
    net.generateNode(null, ctx.getEntropy(), null);
    net.insertNodes(nodes - 1, ctx.getNetFailCount(), ctx.getEntropy());

    assertEquals(0, ctx.getNetFailCount().get());

    net.putDataEntries(256, ctx.getEntropy());
    final Progress stabilizeProgress =
        progressMeta().progress("stabilize", nodes).start();
    int nodesStabilized = 0;
    for(Node node : net.getAllNodes()){
      node.getServices().getOverlay().stabilize();
      stabilizeProgress.iter(nodesStabilized++);
    }
    stabilizeProgress.stop();

    final Metrics testMetrics = Metrics.createInstance("test");
    net.setInterceptor(testMetrics);

    int testCount = (int) Math.ceil(nodes * Math.log(nodes) / Math.log(2));
    final Progress lookupProgress =
        progressMeta().progress("lookup", testCount).start();
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
        throw new IllegalStateException("failed at test # " + testIndex, e);
      }
      assertEquals("test: " + testIndex, server.getAddress(), lookup);
      lookupProgress.iter(testIndex);
    }
    lookupProgress.stop();

    final NetworkInterceptor.LookupMetrics getMetrics =
        testMetrics.modeToLookups(LookupService.Mode.GET);
    final double avgHopLimitActual =
        getMetrics.getMeanPathLength();
    assertTrue(
        String.format(
            "average hops for %d nodes should be less than %d, but is %g",
            nodes,
            avgHopLimit,
            avgHopLimitActual
        ),
        avgHopLimitActual < avgHopLimit
    );
    final double redundancyLimit =
        RoutingServiceBase.MAINTENANCE_THRESHOLD * ctx.getNet().getFactory().createRouting().getRedundancy();
    assertTrue(
        String.format(
            "route redundancy for %d nodes should be less than %g, but is %g",
            nodes,
            redundancyLimit,
            getMetrics.getRoutingServiceRedundancyAvg()
        ),
        getMetrics.getRoutingServiceRedundancyAvg() < redundancyLimit
    );
/*
    System.err.println(String.format("hop count (%d nodes) = %g", nodes, getMetrics.getMeanPathLength()));
    System.err.println(String.format("route count (%d nodes) = %g", nodes, getMetrics.getRoutingServiceRouteCountAvg()));
    System.err.println(String.format("route redundancy (%d nodes) = %g", nodes, getMetrics.getRoutingServiceRedundancyAvg()));
    System.err.println(String.format("route redundancy change (%d nodes) = %g", nodes, getMetrics.getRoutingServiceRedundancyChangeAvg()));
    assertEquals(0, ctx.getNetFailCount().get());
*/
  }

  protected void testJoinLeave0(final long seed, final int nodes) {
    final Context ctx = createContextMeta().create(seed);
    final Network net = ctx.net();

    net.generateNode(null, ctx.getEntropy(), null);
    net.putDataEntries(nodes * 5, ctx.getEntropy());
    final String testValue = "test1";
    final Key testKey = API.createKey(testValue);
    net.getRandomNode(ctx.getEntropy()).getServices().getStorage().put(testKey, testValue);

    net.insertNodes(nodes - 1, ctx.getNetFailCount(), ctx.getEntropy());

    final Progress lookupProgress =
        progressMeta().progress("lookup/join/leave", nodes).start();
    for (int testIndex = 0; testIndex < nodes; testIndex++) {
      final String testMessage = "failed at test # " + testIndex;
      //  lookup the test key from random node
      final Node randomNode = net.getRandomNode(ctx.getEntropy());
      assertNotNull(testMessage, randomNode);
      try {
        randomNode.getServices().getOverlay().stabilize();
        final Address address = randomNode.getServices().getLookup().lookup(
            testKey, true, LookupService.Mode.GET
        );
        assertEquals(
            testMessage,
            testValue,
            net.getEnv().getNode(address).getServices().getStorage().get(testKey)
        );
      } catch (CommunicationException e) {
        throw new IllegalStateException(testMessage, e);
      }
      //  lookup a random server key from random client node
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
        throw new IllegalStateException(testMessage, e);
      }
      assertEquals(testMessage, server.getAddress(), lookup);

      try {
        net.putDataEntries(5, ctx.getEntropy());
        net.insertNodes(2, ctx.getNetFailCount(), ctx.getEntropy());
        net.removeNodes(3, false, ctx.getEntropy());
      } catch (CommunicationException e) {
        throw new IllegalStateException(testMessage + " (maintenance)", e);
      }
      assertEquals(testMessage, 0, ctx.getNetFailCount().get());
      lookupProgress.iter(testIndex);
    }
    lookupProgress.stop();

/*
    final NetworkInterceptor.LookupMetrics getMetrics =
        ctx.nameToMetrics().get(Context.METRICS).modeToLookups(LookupService.Mode.GET);
    System.err.println(String.format("hop count (%d nodes) = %g", nodes, getMetrics.getMeanPathLength()));
    System.err.println(String.format("route count (%d nodes) = %g", nodes, getMetrics.getRoutingServiceRouteCountAvg()));
    System.err.println(String.format("route redundancy (%d nodes) = %g", nodes, getMetrics.getRoutingServiceRedundancyAvg()));
    System.err.println(String.format("route redundancy change (%d nodes) = %g", nodes, getMetrics.getRoutingServiceRedundancyChangeAvg()));
*/
  }

}
