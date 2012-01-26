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

package algores.holonet.core;

import algores.holonet.core.api.API;
import algores.holonet.core.api.Address;
import algores.holonet.core.api.Key;
import algores.holonet.core.api.tier0.rpc.NetworkRpc;
import algores.holonet.core.api.tier0.rpc.NetworkRpcBase;
import org.akraievoy.base.Stopwatch;
import org.akraievoy.cnet.gen.vo.EntropySource;

import java.util.Collection;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;

public class Network {
  protected Env env = new EnvSimple();
  protected ServiceFactorySpring factory = new ServiceFactorySpring();

  public Network() {
  }

  public void setEnv(Env env) {
    this.env = env;
  }

  public Env getEnv() {
    return env;
  }

  //	---------------
  //	node management
  //	---------------
  protected final NetworkRpcBase rpc = new NetworkRpcBase(this);

  public NetworkRpc getRpc() {
    return rpc;
  }

  public Node getRandomNode(EntropySource eSource) {
    final Collection<Node> allNodes = getAllNodes();
    if (allNodes.isEmpty()) {
      return null;
    }
    return eSource.randomElement(allNodes);
  }

  public void dispose() {
    rpc.dispose();
  }

  public Map.Entry<Key, Object> selectMapping(EntropySource source) {
    return env.getMappings().select(source);
  }

  /**
   * Generate a node with random address, let it join the DHT, and save it in my hash table.
   *
   * @param newParent          one of the old nodes in the DHT for bootstrapping the new one.
   *                           <code>null</code> if the DHT has no node yet.
   * @param eSource
   * @param nodeFailureCounter swallows (if set) NodeFailureExceptions (if thrown)  @return the newly generated node, <code>null</code> if the generation fails.
   * @throws SimulatorException escalated
   */
  public Node generateNode(Node newParent, EntropySource eSource, AtomicInteger nodeFailureCounter) throws SimulatorException {
    final Address parentAddress = newParent != null ? newParent.getAddress() : null;

    final Node newNode = createNode(eSource);
    if (env.getNode(newNode.getAddress()) != null) {
      if (nodeFailureCounter == null) {
        throw new CommunicationException("address already bound");
      } else {
        nodeFailureCounter.getAndIncrement();
        return null;
      }
    }

    //	normally, node may need its address be correctly resolved immediately after its arrival
    env.putNode(newNode, newNode.getAddress());
    try {

      newNode.getServices().getOverlay().join(parentAddress);
      //	if protocol did not identify any failure for now, arrival is successfull
      getInterceptor().registerNodeArrivals(1, true);
      return newNode;
    } catch (CommunicationException nfe) {
      env.removeNode(newNode.getAddress());
      if (nodeFailureCounter == null) {
        throw nfe;
      } else {
        nodeFailureCounter.getAndIncrement();
        return null;
      }
    } catch (SimulatorException e) {
      env.removeNode(newNode.getAddress());
      getInterceptor().registerNodeArrivals(1, false);
      throw e;
    }
  }

  /**
   * Remove a node from DHT.
   *
   * @param nodeToRemove to be removed.
   * @param forceFailure to emulate failure of the node, leave() would not be called on the node if true.
   * @throws CommunicationException if normal departure will be denied due to network failure.
   */
  public void removeNode(Node nodeToRemove, final boolean forceFailure) throws CommunicationException {
    Collection<Key> keys = nodeToRemove.getServices().getStorage().getKeys();
    if (!forceFailure) {
      getInterceptor().registerNodeDepartures(1);
      nodeToRemove.getServices().getOverlay().leave();
    } else {
      getInterceptor().registerNodeFailures(1);
    }

    env.removeNode(nodeToRemove.getAddress());
    for (Key key : keys) {
      env.getMappings().deregister(key, nodeToRemove, false);
    }
  }

  public Collection<Node> getAllNodes() {
    return env.getAllNodes();
  }

  //	------
  //	timing
  //	------
  protected double elapsedTime;

  public void reset() {
    elapsedTime = 0.0;
  }

  public void registerRpcCall(Node caller, final Address calleeAddress) {
    elapsedTime += 2.0 * caller.getAddress().getDistance(calleeAddress);
  }

  public double getElapsedTime() {
    return elapsedTime;
  }

  //	---------------
  //	factory methods
  //	---------------

  public Node createNode(EntropySource eSource) {
    Node node = new Node();
    node.init(Network.this, factory, env.createNetworkAddress(eSource));
    return node;
  }

  //	-------------
  //	factory setup
  //	-------------

  public void setFactory(ServiceFactorySpring factory) {
    this.factory = factory;
  }

  //	-----------------------
  //	aggregate manipulations
  //	-----------------------

  public void insertNodes(int count, AtomicInteger failCounter, final EntropySource eSource) throws SimulatorException {
    if (count > 50) {
      System.out.println("inserting " + count + " nodes");
    }
    Stopwatch stopwatch = new Stopwatch();
    for (int i = 0; i < count; i++) {
      generateNode(getRandomNode(eSource), eSource, failCounter);
      if (count > 50) {
        System.out.print(".");
        if (i % 25 == 24) {
          System.out.println(" " + (i + 1) + " of " + count + " @ " + stopwatch.diff(i % 25 + 1) + " ms per node");
        }
      }
    }
    if (count > 50) {
      System.out.println("complete");
    }
  }

  public void removeNodes(int count, final boolean forceFailure, final EntropySource eSource) throws CommunicationException {
    for (int i = 0; i < count; i++) {
      removeNode(getRandomNode(eSource), forceFailure);
    }
  }

  public void putDataEntries(int n, final EntropySource eSource) throws SimulatorException {
    final byte[] bytes = new byte[20];

    for (int i = 0; i < n; i++) {
      eSource.nextBytes(bytes);
      final Key key = API.createKey(bytes);

      final Address responsibleAddress = getRandomNode(eSource).getServices().getLookup().lookup(key, false);
      final Node owner = env.getNode(responsibleAddress);
      owner.getServices().getStorage().put(key, bytes);
    }
  }

  //  ----------------------------------------
  //	metrics collection dependency [injected]
  //  ----------------------------------------
  private NetworkInterceptor metrics;

  /**
   * This method should be only package level access
   *
   * @return metrics object
   */
  public NetworkInterceptor getInterceptor() {
    if (metrics == null) {
      metrics = NetworkInterceptor.NOOP;
    }

    return metrics;
  }

  public void setInterceptor(NetworkInterceptor newInterceptor) {
    metrics = newInterceptor;
  }

  public void registerSuccess(final double lookupStartTime, final Stack<Address> route) {
    final double latency = getElapsedTime() - lookupStartTime;
    final double directLatency = route.get(0).getDistance(route.peek().getAddress());

    final NetworkInterceptor interceptor = getInterceptor();
    interceptor.registerLookup(latency, route.size(), directLatency);
    interceptor.registerLookupSuccess(true);
  }
}