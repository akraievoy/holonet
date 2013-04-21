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
import algores.holonet.core.events.EventNetDiscover;
import algores.holonet.core.events.EventNetStabilize;
import algores.holonet.testbench.Metrics;
import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Collection;

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

    final Metrics testMetrics = Metrics.createInstance(net, "test");
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
            request, false, LookupService.Mode.GET,
            Optional.of(server.getAddress())
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
        1.15 * RoutingServiceBase.MAINTENANCE_THRESHOLD * ctx.getNet().getFactory().createRouting().getRedundancy();
    assertTrue(
        String.format(
            "route redundancy for %d nodes should be less than %g, but is %g",
            nodes,
            redundancyLimit,
            getMetrics.getRoutingServiceRedundancyAvg()
        ),
        getMetrics.getRoutingServiceRedundancyAvg() < redundancyLimit
    );
    if (nodes >= 64) {
      System.err.println(String.format("hop count (%d nodes) = %g", nodes, getMetrics.getMeanPathLength()));
      System.err.println(String.format("route count (%d nodes) = %g", nodes, getMetrics.getRoutingServiceRouteCountAvg()));
      System.err.println(String.format("route redundancy (%d nodes) = %g", nodes, getMetrics.getRoutingServiceRedundancyAvg()));
      System.err.println(String.format("route redundancy change (%d nodes) = %g", nodes, getMetrics.getRoutingServiceRedundancyChangeAvg()));
    }
    assertEquals(0, ctx.getNetFailCount().get());
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
            testKey, true, LookupService.Mode.GET,
            Optional.<Address>absent()
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
            request, false, LookupService.Mode.GET,
            Optional.of(server.getAddress())
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

    final NetworkInterceptor.LookupMetrics getMetrics =
        ctx.nameToMetrics().get(Context.METRICS).modeToLookups(LookupService.Mode.GET);
    if (nodes >= 64) {
      System.err.println(String.format("hop count (%d nodes) = %g", nodes, getMetrics.getMeanPathLength()));
      System.err.println(String.format("route count (%d nodes) = %g", nodes, getMetrics.getRoutingServiceRouteCountAvg()));
      System.err.println(String.format("route redundancy (%d nodes) = %g", nodes, getMetrics.getRoutingServiceRedundancyAvg()));
      System.err.println(String.format("route redundancy change (%d nodes) = %g", nodes, getMetrics.getRoutingServiceRedundancyChangeAvg()));
    }
    assertEquals(0, ctx.getNetFailCount().get());
  }

  protected void testFail0(final long seed, final int nodes) {
    final Context ctx = createContextMeta().create(seed);
    final Network net = ctx.net();

    net.insertNodes(nodes, ctx.getNetFailCount(), ctx.getEntropy());
    new EventNetDiscover().execute(net, ctx.getEntropy());

    Node nodeFrom = null;
    Node nodeInto = null;
    final Collection<Node> allNodes = new ArrayList<Node>(net.getAllNodes());
    for (Node nFrom : allNodes) {
      for (Node nInto: allNodes) {
        if (nFrom.getAddress().equals(nInto.getAddress())) {
          continue;
        }

        if (
            nFrom.getServices().getRouting().hasRouteFor(nInto.getAddress(), true, true) &&
            !nInto.getServices().getRouting().hasRouteFor(nFrom.getAddress(), true, true)
        ) {
          nodeFrom = nFrom;
          nodeInto = nInto;
        }
      }
    }

    if (nodeFrom == null) {
      System.err.printf("no assymetric link for seed %d%n", seed);
      return;
    }

    for (Node n : allNodes) {
      if (nodeFrom.equals(n) || nodeInto.equals(n)) {
        continue;
      }
      net.removeNode(n, true);
    }

    try {
      nodeInto.getServices().getLookup().lookup(
          nodeFrom.getAddress().getKey(),
          false,
          LookupService.Mode.GET,
          Optional.<Address>absent()
      );
      fail("should have failed");
    } catch (CommunicationException e) {
      //  it should fail for the first time
    }

    final Address fromIntoRes = nodeFrom.getServices().getLookup().lookup(
        nodeInto.getAddress().getKey(),
        false,
        LookupService.Mode.GET,
        Optional.<Address>absent()
    );
    assertEquals(nodeInto.getAddress(), fromIntoRes);

    final Address intoFromRes = nodeInto.getServices().getLookup().lookup(
        nodeFrom.getAddress().getKey(),
        false,
        LookupService.Mode.GET,
        Optional.<Address>absent()
    );
    assertEquals(nodeFrom.getAddress(), intoFromRes);
  }

  protected void testFailStabilize0(final long seed, final int nodes) {
    final Context ctx = createContextMeta().routingRedundancy(1).maxFingerFlavorNum(1).create(seed);
    final Network net = ctx.net();

    net.insertNodes(nodes, ctx.getNetFailCount(), ctx.getEntropy());
    new EventNetDiscover().execute(net, ctx.getEntropy());

    final double failRatio = 0.125;
    final Collection<Node> allNodes = new ArrayList<Node>(net.getAllNodes());
    int failedCount = 0;
    for (Node n : allNodes) {
      if (ctx.getEntropy().nextDouble() > failRatio || failedCount > allNodes.size() * failRatio) {
        continue;
      }

      net.removeNode(n, true);
      failedCount++;
    }

    new EventNetStabilize().execute(net, ctx.getEntropy());

    final EventNetDiscover secondDiscover = new EventNetDiscover();
    secondDiscover.execute(net, ctx.getEntropy());
    assertEquals(1.0f, secondDiscover.successRatio());
  }

  protected void testFailStabilizePerf0(final long seed, final int nodes, final int dataElems, final double failRatio) {
    final Context ctx = createContextMeta().routingRedundancy(1).maxFingerFlavorNum(1).create(seed);
    final Network net = ctx.net();

    Stopwatch sw = new Stopwatch(); sw.start();
    net.insertNodes(nodes, ctx.getNetFailCount(), ctx.getEntropy());
    System.out.println("insert nodes: " + sw.toString()); sw.reset(); sw.start();

    for (int node = 0; node < nodes; node++) {
      net.putDataEntries(dataElems, ctx.getEntropy());
    }
    System.out.println("put data entries: " + sw.toString()); sw.reset(); sw.start();

    new EventNetDiscover().execute(net, ctx.getEntropy());
    System.out.println("global discover: " + sw.toString()); sw.reset(); sw.start();

    final Collection<Node> allNodes = new ArrayList<Node>(net.getAllNodes());
    int failedCount = 0;
    for (Node n : allNodes) {
      if (ctx.getEntropy().nextDouble() > failRatio || failedCount > allNodes.size() * failRatio) {
        continue;
      }

      net.removeNode(n, true);
      failedCount++;
    }
    System.out.println("fail: " + sw.toString()); sw.reset(); sw.start();

    new EventNetStabilize().execute(net, ctx.getEntropy());
    System.out.println("stabilize: " + sw.toString()); sw.reset(); sw.start();

    new EventNetDiscover().mode(LookupService.Mode.FIXFINGERS).execute(net, ctx.getEntropy());
    System.out.println("fix@discover: " + sw.toString()); sw.reset(); sw.start();

    final EventNetDiscover secondDiscover = new EventNetDiscover();
    secondDiscover.execute(net, ctx.getEntropy());
    System.out.println("get@discover: " + sw.toString()); sw.reset(); sw.start();
  }

}
