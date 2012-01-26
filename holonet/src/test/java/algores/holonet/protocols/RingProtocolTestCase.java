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
import algores.holonet.protocols.ring.RingRoutingServiceImpl;
import algores.holonet.protocols.ring.RingService;
import junit.framework.TestCase;
import org.akraievoy.base.Stopwatch;
import org.akraievoy.cnet.gen.vo.EntropySourceRandom;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class RingProtocolTestCase extends TestCase {
  private Network network;
  protected AtomicInteger netFailCount;
  protected boolean dataEntryLeaksOnLeavePermitted = false;
  protected EntropySourceRandom eSource;

  public void setUp() {
    final ServiceFactorySpring factory = new ServiceFactorySpring();
    factory.setRouting(new RingRoutingServiceImpl());
    factory.setOverlay(new RingService());

    final Network network = new Network();
    network.setFactory(factory);

    setNetwork(network);

    netFailCount = new AtomicInteger(0);
    eSource = new EntropySourceRandom();
  }

  public void tearDown() {
    System.out.println("net fail count: " + netFailCount.get());
  }

  public void testGeneric() throws Throwable {
    getNetwork().generateNode(null, eSource, null);
    getNetwork().putDataEntries(600, eSource);
    final String testValue = "test1";
    final Key testKey = API.createKey(testValue);
    getNetwork().getRandomNode(eSource).getServices().getStorage().put(testKey, testValue);

    getNetwork().insertNodes(120, netFailCount, eSource);

    int testCount = 100;
    Stopwatch stopwatch = new Stopwatch();
    for (int testIndex = 0; testIndex < testCount; testIndex++) {
      for (int lookupCount = 0; lookupCount < 10; lookupCount++) {
        final Node randomNode = getNetwork().getRandomNode(eSource);
        try {
          randomNode.getServices().getOverlay().stabilize();
          final Address address = randomNode.getServices().getLookup().lookup(testKey, true);
          assertEquals("test: " + testIndex + " loop: " + lookupCount, testValue, getNetwork().getEnv().getNode(address).getServices().getStorage().get(testKey));
        } catch (CommunicationException e) {
          netFailCount.getAndIncrement();
        }
      }

      try {
        getNetwork().putDataEntries(5, eSource);
        getNetwork().removeNodes(3, false, eSource);
        getNetwork().insertNodes(2, netFailCount, eSource);
      } catch (CommunicationException e) {
        netFailCount.getAndIncrement();
      }
      System.out.print(".");
      if (testIndex % 25 == 24) {
        System.out.println(" " + (testIndex + 1) + " of " + testCount + " @ " + stopwatch.diff(testIndex % 25 + 1) + " ms per test");
      }
    }
    System.out.println("complete");
  }

  public void testLeave() throws SimulatorException {
    if (System.getProperty("proto.test") != null) {
      return;
    }

    getNetwork().insertNodes(1, null, eSource);
    getNetwork().putDataEntries(6000, eSource);
    getNetwork().insertNodes(600, netFailCount, eSource);

    final String testValue = "test1";
    final Key testKey = API.createKey(testValue);
    getNetwork().getEnv().getNode(getNetwork().getRandomNode(eSource).getServices().getLookup().lookup(testKey, false)).getServices().getStorage().put(testKey, testValue);

    int mappingCount = countMappings();

    Stopwatch stopwatch = new Stopwatch();
    int testNum = 600 - netFailCount.get();
    for (int testCount = 0; testCount < testNum; testCount++) {
      getNetwork().removeNodes(1, false, eSource);
      for (int lookupCount = 0; lookupCount < 5; lookupCount++) {
        try {
          final Address responsible = getNetwork().getRandomNode(eSource).getServices().getLookup().lookup(testKey, true);
          assertEquals("TestCount: " + testCount, testValue, getNetwork().getEnv().getNode(responsible).getServices().getStorage().get(testKey));
        } catch (CommunicationException e) {
          netFailCount.getAndIncrement();
        }
      }
      System.out.print(".");
      if (testCount % 25 == 24) {
        System.out.println(" " + (testCount + 1) + " of " + testNum + " @ " + stopwatch.diff(testCount % 25 + 1) + " ms per test");
      }
    }
    System.out.println("complete");

    assertEquals(1, getNetwork().getAllNodes().size());
    if (mappingCount != countMappings()) {
      System.err.println("data entries are leaking on leave!");
      assertTrue("data entry leaks not permitted", dataEntryLeaksOnLeavePermitted);
    }
  }

  private int countMappings() {
    final Set<Key> keys = new HashSet<Key>();
    final Collection allNodes = getNetwork().getAllNodes();
    for (Iterator nodeIt = allNodes.iterator(); nodeIt.hasNext();) {
      Node node = (Node) nodeIt.next();
      keys.addAll(node.getServices().getStorage().getDataEntries().keySet());
    }
    return keys.size();
  }

  public void testJoin() throws SimulatorException {
    if (System.getProperty("proto.test") != null) {
      return;
    }

    getNetwork().generateNode(null, eSource, null);

    getNetwork().insertNodes(1, null, eSource);
    getNetwork().putDataEntries(10000, eSource);

    final String testValue = "test1";
    final Key testKey = API.createKey(testValue);

    getNetwork().getEnv().getNode(getNetwork().getRandomNode(eSource).getServices().getLookup().lookup(testKey, false)).getServices().getStorage().put(testKey, testValue);

    Stopwatch stopwatch = new Stopwatch();
    final int testNum = 500;
    for (int testCount = 0; testCount < testNum; testCount++) {
      getNetwork().insertNodes(2, netFailCount, eSource);
      for (int lookupCount = 0; lookupCount < 10; lookupCount++) {
        try {
          final Address responsible = getNetwork().getRandomNode(eSource).getServices().getLookup().lookup(testKey, true);
          assertEquals("TestCount: " + testCount, testValue, getNetwork().getEnv().getNode(responsible).getServices().getStorage().get(testKey));
        } catch (CommunicationException e) {
          netFailCount.getAndIncrement();
        }
      }
      System.out.print(".");
      if (testCount % 25 == 24) {
        System.out.println(" " + (testCount + 1) + " of " + testNum + " @ " + stopwatch.diff(testCount % 25 + 1) + " ms per test");
      }
    }
    System.out.println("complete");
  }

  public void testStabilize() throws SimulatorException {
    if (System.getProperty("proto.test") != null) {
      return;
    }

    getNetwork().generateNode(null, eSource, null);

    getNetwork().insertNodes(1, null, eSource);
    getNetwork().putDataEntries(1000, eSource);

    final String testValue = "test1";
    final Key testKey = API.createKey(testValue);

    getNetwork().getEnv().getNode(getNetwork().getRandomNode(eSource).getServices().getLookup().lookup(testKey, false)).getServices().getStorage().put(testKey, testValue);

    Stopwatch stopwatch = new Stopwatch();
    final int testNum = 50;
    for (int testCount = 0; testCount < testNum; testCount++) {
      getNetwork().insertNodes(2, netFailCount, eSource);
      for (int lookupCount = 0; lookupCount < 10; lookupCount++) {
        final Node randomNode = getNetwork().getRandomNode(eSource);
        randomNode.getServices().getOverlay().stabilize();
        final Address responsible = randomNode.getServices().getLookup().lookup(testKey, true);
        assertEquals("TestCount: " + testCount, testValue, getNetwork().getEnv().getNode(responsible).getServices().getStorage().get(testKey));
      }

      System.out.print(".");
      if (testCount % 25 == 24) {
        System.out.println(" " + (testCount + 1) + " of " + testNum + " @ " + stopwatch.diff(testCount % 25 + 1) + " ms per test");
      }
    }
    System.out.println("complete");
  }

  protected Network getNetwork() {
    return network;
  }

  protected void setNetwork(final Network newNetwork) {
    network = newNetwork;
  }
}
