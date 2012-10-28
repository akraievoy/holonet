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
import algores.holonet.core.api.tier0.routing.RoutingEntry;
import algores.holonet.core.api.tier0.rpc.NetworkRpc;
import algores.holonet.core.api.tier0.rpc.NetworkRpcBase;
import algores.holonet.core.api.tier1.delivery.LookupService;
import org.akraievoy.cnet.gen.vo.EntropySource;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicLong;

public class Network {

  protected Env env = new EnvSimple();
  protected ServiceFactorySpring factory = new ServiceFactorySpring();
  protected ProgressMeta progressMeta =
      ProgressMeta.DEFAULT;

  public Network() {
  }

  public void setEnv(Env env) {
    this.env = env;
  }

  public Env getEnv() {
    return env;
  }

  public void setProgressMeta(ProgressMeta progressMeta) {
    this.progressMeta = progressMeta;
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

  public RequestPair generateRequestPair(EntropySource entropy) {
    return getEnv().generateRequestPair(entropy);
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
   *
   * @param newParent          one of the old nodes in the DHT for bootstrapping the new one.
   *                           <code>null</code> if the DHT has no node yet.
   * @param eSource
   * @param nodeFailureCounter swallows (if set) NodeFailureExceptions (if thrown)  @return the newly generated node, <code>null</code> if the generation fails.
   * @throws SimulatorException escalated
   */
  public Node generateNode(Node newParent, EntropySource eSource, AtomicLong nodeFailureCounter) throws SimulatorException {
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

  public ServiceFactorySpring getFactory() {
    return factory;
  }

  //	-----------------------
  //	aggregate manipulations
  //	-----------------------

  public void insertNodes(int count, AtomicLong failCounter, final EntropySource eSource) throws SimulatorException {
    final Progress progress =
        progressMeta.progress("inserting nodes", count).start();
    for (int i = 0; i < count; i++) {
      generateNode(getRandomNode(eSource), eSource, failCounter);
      progress.iter(i);
    }
    progress.stop();
  }

  public void removeNodes(int count, final boolean forceFailure, final EntropySource eSource) throws CommunicationException {
    for (int i = 0; i < count; i++) {
      removeNode(getRandomNode(eSource), forceFailure);
    }
  }

  public void putDataEntries(int n, final EntropySource eSource) throws SimulatorException {
    final byte[] bytes = new byte[20];
    final Progress progress =
        progressMeta.progress("inserting data", n).start();
    for (int i = 0; i < n; i++) {
      eSource.nextBytes(bytes);
      final Key key = API.createKey(bytes);

      final Address responsibleAddress =
          getRandomNode(eSource).getServices().getLookup().lookup(
              key, false, LookupService.Mode.PUT
          );
      final Node owner = env.getNode(responsibleAddress);
      owner.getServices().getStorage().put(key, bytes.clone());
      progress.iter(i);
    }
    progress.stop();
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

  public void registerLookupSuccess(
      final LookupService.Mode mode,
      final double lookupStartTime,
      final List<RoutingEntry> route,
      final LookupService.RecursiveLookupState.StatsTuple stats,
      final boolean success
  ) {
    final double latency = getElapsedTime() - lookupStartTime;
    final Address firstAddr = route.get(0).getAddress();
    final Address lastAddr = route.get(route.size() - 1).getAddress();
    final double directLatency = 2 * firstAddr.getDistance(lastAddr);

    final NetworkInterceptor interceptor = getInterceptor();
    final double routeRedundancy =
        (double) stats.traversalSucceeded / route.size();
    final double routeRetraction =
        (double) stats.traversalFailed / route.size();
    final double routeExhaustion =
        (double) stats.traversalCalled / stats.traversalAdded;
    final double routeRpcFailRatio =
        (double) stats.traversalFailed / stats.traversalCalled;
    interceptor.modeToLookups(mode).registerLookup(
        latency,
        route.size() - 1,
        routeRedundancy,
        routeRetraction,
        routeExhaustion,
        routeRpcFailRatio,
        directLatency,
        success
    );
  }
}