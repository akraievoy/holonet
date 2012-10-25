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

package algores.holonet.protocols;

import algores.holonet.core.*;
import algores.holonet.core.api.API;
import algores.holonet.core.api.Address;
import algores.holonet.core.api.Key;
import algores.holonet.core.api.tier1.delivery.LookupService;
import algores.holonet.protocols.ring.RingRoutingServiceImpl;
import algores.holonet.protocols.ring.RingService;
import org.akraievoy.base.Stopwatch;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class RingProtocolTestCase extends DhtProtocolTestCase {
  @Override
  protected ContextMeta createContextMeta() {
    return new ContextMeta()
        .withRouting(new RingRoutingServiceImpl())
        .withOverlay(new RingService());
  }

  public void testGeneric() throws Throwable {
    testJoinLeave0(-123L, 120);
  }

  public void testHopCount() throws Throwable {
    testHopCount0(135930, 4, 1024);
  }

  public void testLeave() throws SimulatorException {
    if (System.getProperty("proto.test") != null) {
      return;
    }

    final Context ctx = createContextMeta().create(-123L);
    final Network net = ctx.net();

    net.insertNodes(1, null, ctx.getEntropy());
    net.putDataEntries(6000, ctx.getEntropy());
    net.insertNodes(600, ctx.getNetFailCount(), ctx.getEntropy());

    final String testValue = "test1";
    final Key testKey = API.createKey(testValue);
    net.getEnv().getNode(
      net.getRandomNode(ctx.getEntropy()).getServices().getLookup().lookup(
        testKey, false, LookupService.Mode.GET
      )
    ).getServices().getStorage().put(testKey, testValue);

    int mappingCount = countMappings(net);

    long testNum = 600 - ctx.getNetFailCount().get();
    final Progress lookupProgress =
        progressMeta().progress("lookup/join/leave", testNum).start();
    for (int testCount = 0; testCount < testNum; testCount++) {
      net.removeNodes(1, false, ctx.getEntropy());
      for (int lookupCount = 0; lookupCount < 5; lookupCount++) {
        try {
          final Address responsible =
              net.getRandomNode(ctx.getEntropy()).getServices().getLookup().lookup(
                  testKey, true, LookupService.Mode.GET
              );

          assertEquals(
              "TestCount: " + testCount,
              testValue,
              net.getEnv().getNode(responsible).getServices().getStorage().get(testKey)
          );
        } catch (CommunicationException e) {
          ctx.getNetFailCount().getAndIncrement();
        }
      }
      lookupProgress.iter(testCount);
    }
    lookupProgress.stop();

    assertEquals(1, net.getAllNodes().size());
    if (mappingCount != countMappings(net)) {
      System.err.println("data entries are leaking on leave!");
      fail("data entry leaks not permitted");
    }
  }

  private int countMappings(Network net) {
    final Set<Key> keys = new HashSet<Key>();
    final Collection allNodes = net.getAllNodes();
    for (Object allNode : allNodes) {
      Node node = (Node) allNode;
      keys.addAll(node.getServices().getStorage().getDataEntries().keySet());
    }
    return keys.size();
  }

  public void testJoin() throws SimulatorException {
    if (System.getProperty("proto.test") != null) {
      return;
    }

    final Context ctx = createContextMeta().create(-123L);
    final Network net = ctx.net();

    net.generateNode(null, ctx.getEntropy(), null);

    net.insertNodes(1, null, ctx.getEntropy());
    net.putDataEntries(10000, ctx.getEntropy());

    final String testValue = "test1";
    final Key testKey = API.createKey(testValue);

    net.getEnv().getNode(
        net.getRandomNode(ctx.getEntropy()).getServices().getLookup().lookup(
            testKey, false, LookupService.Mode.GET
        )
    ).getServices().getStorage().put(testKey, testValue);

    final int testNum = 500;
    final Progress lookupProgress =
        progressMeta().progress("lookup/join/leave", testNum).start();
    for (int testCount = 0; testCount < testNum; testCount++) {
      net.insertNodes(2, ctx.getNetFailCount(), ctx.getEntropy());
      for (int lookupCount = 0; lookupCount < 10; lookupCount++) {
        try {
          final Address responsible =
              net.getRandomNode(ctx.getEntropy()).getServices().getLookup().lookup(
                  testKey, true, LookupService.Mode.GET
              );
          assertEquals(
              "TestCount: " + testCount,
              testValue,
              net.getEnv().getNode(responsible).getServices().getStorage().get(testKey)
          );
        } catch (CommunicationException e) {
          ctx.getNetFailCount().getAndIncrement();
        }
      }
      lookupProgress.iter(testCount);
    }
    lookupProgress.stop();
  }

  public void testStabilize() throws SimulatorException {
    if (System.getProperty("proto.test") != null) {
      return;
    }

    final Context ctx = createContextMeta().create(-123L);
    final Network net = ctx.net();

    net.generateNode(null, ctx.getEntropy(), null);

    net.insertNodes(1, null, ctx.getEntropy());
    net.putDataEntries(1000, ctx.getEntropy());

    final String testValue = "test1";
    final Key testKey = API.createKey(testValue);

    net.getEnv().getNode(
        net.getRandomNode(ctx.getEntropy()).getServices().getLookup().lookup(
            testKey, false, LookupService.Mode.GET
        )
    ).getServices().getStorage().put(testKey, testValue);

    final int testNum = 50;
    final Progress lookupProgress =
        progressMeta().progress("lookup/join/leave", testNum).start();
    for (int testCount = 0; testCount < testNum; testCount++) {
      net.insertNodes(2, ctx.getNetFailCount(), ctx.getEntropy());
      for (int lookupCount = 0; lookupCount < 10; lookupCount++) {
        final Node randomNode = net.getRandomNode(ctx.getEntropy());
        randomNode.getServices().getOverlay().stabilize();
        final Address responsible =
            randomNode.getServices().getLookup().lookup(
                testKey, true, LookupService.Mode.GET
            );
        assertEquals(
            "TestCount: " + testCount,
            testValue,
            net.getEnv().getNode(responsible).getServices().getStorage().get(testKey)
        );
      }
      lookupProgress.iter(testCount);
    }
    lookupProgress.stop();
  }
}
