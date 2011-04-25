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

package algores.holonet.capi;

import algores.holonet.core.api.NodeHandle;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Allows applications to access a node’s routing state.
 * <p> All of these operations are strictly local and involve no communication with other nodes. Applications may query
 * the routing state to, for instance, obtain nodes that may be used by the forward upcall as a next hop destination.</p>
 * <p/>
 * <p>Some of the operations return information about a key’s <i>r</i>-root. The <i>r</i>-root is a generalization of a
 * key’s root. A node is an <i>r</i>-root for a key if that node becomes the root for the key if all of the
 * <i>i</i>-roots fail for <i>i</i> < <i>r</i>. The node may be the <i>r</i>-root for keys in one or more contiguous
 * regions of the ID space.</p>
 *
 * @author Frank Dabek
 * @author Ben Zhao
 * @author Peter Druschel
 * @author John Kubiatowicz
 * @author Ion Stoica
 */
public interface RoutingState<NH extends NodeHandle, K extends Key> {
  /**
   * Produces a list of nodes that can be used as next hops on a route towards <code>key</code>.
   * <p>List is such that the resulting route satisfies the overlay protocol’s bounds on the number of hops taken.</p>
   * <p>If safe is true, the expected fraction of faulty nodes in the list is guaranteed to be no higher
   * than the fraction of faulty nodes in the overlay; if false, the set may be chosen to optimize
   * performance at the expense of a potentially higher fraction of faulty nodes.
   * This option allows applications to implement routing in overlays with byzantine node failures. Implementations
   * that assume fail-stop behavior may ignore the safe argument. The fraction of faulty nodes in the returned
   * list may be higher if the safe parameter is not true because, for instance, malicious nodes have caused
   * the local node to build a routing table that is biased towards malicious nodes.</p>
   *
   * @param key  target key
   * @param num  limit on size of returned list
   * @param safe try to filter unsafe hops
   * @return list of next hops for the <code>key</code>
   */
  NH[] localLookup(K key, int num, boolean safe);

  /**
   * Produces an unordered list of nodehandles that are neighbors of the local node in the ID space. Up to num node
   * handles are returned.
   *
   * @param num upper limit on result size
   * @return unordered list of neighbors in the key space
   */
  NH[] neighborSet(int num);

  /**
   * Returns an ordered set of nodehandles on which replicas of the object with key <i>k</i>
   * can be stored.
   * <p>The call returns nodes with a rank up to and including max rank. If max rank exceeds the implementation’s
   * maximum replica set size, then its maximum replica set is returned.</p>
   * <p>Some protocols only support a max rank value of one. With protocols that support a rank value greater
   * than one, the returned nodes may be used for replicating data since they are precisely the nodes which
   * become roots for the <code>key</code> when the local node fails.</p>
   *
   * @param key     key to find replicas for
   * @param maxRank replica rank limit
   * @return ordered set of replica nodehandles
   */
  NH[] replicaSet(K key, byte maxRank);

  /**
   * Invoked to inform the application that node handle has either joined or left the neighbor set of the local
   * node as that set would be returned by the neighborSet call.
   *
   * @param handle node in question
   * @param joined if <code>true</code> - node joined the net, or left if <code>false</code>
   * @deprecated in favor of {@link RoutingState#update(algores.holonet.core.api.NodeHandle, Event)}
   */
  void update(NH handle, boolean joined);

  /**
   * Invoked to inform the application that node handle has either joined or left the neighbor set of the local
   * node as that set would be returned by the neighborSet call.
   *
   * @param handle node in question
   * @param event  respective event related to this node
   */
  void update(NH handle, Event event);

  /**
   * Invoked to inform the application that node handle has either joined or left the neighbor set of the local
   * node as that set would be returned by the neighborSet call.
   *
   * @param handles nodes in question
   * @param event   respective event related to this node
   */
  void update(NH[] handles, Event event);

  /**
   * Provides information about ranges of keys for which the node <code>handle</code> is currently a <i>r</i>-root.
   * <p>If the node referenced by <code>handle</code> is responsible for key <code>lkey</code>,
   * then the resulting range includes <code>lkey</code>. Otherwise, the result is the nearest range clockwise from lkey
   * for which <code>handle</code> is responsible.</p>
   * <p>It is an error to query the range of a node not present in the neighbor set as returned by
   * the {@link RoutingState#update(NodeHandle, boolean)} upcall or the {@link RoutingState#neighborSet} call.</p>
   * <p>Certain implementations may return an error if <code>rank</code> is greater than zero.
   * [<code>lKey</code>, <code>rKey</code>] denotes an inclusive range of key values.
   * Some protocols may have multiple, disjoint ranges of keys for which a given node is responsible.</p>
   *
   * @param handle to inspect
   * @param rank   rank <i>r</i> of the supported range (lesser ranked range may be returned)
   * @param lKey   allows the caller to specify which region should be returned
   * @param rKey   out-parameter, input value ignored
   * @return <code>false</code> if the range could not be determined, <code>true</code>
   *         otherwise.
   */
  boolean range(NH handle, byte rank, AtomicReference<K> lKey, AtomicReference<K> rKey);
}
