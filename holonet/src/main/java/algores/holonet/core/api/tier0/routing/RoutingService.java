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

package algores.holonet.core.api.tier0.routing;

import algores.holonet.capi.Event;
import algores.holonet.capi.RoutingState;
import algores.holonet.core.CommunicationException;
import algores.holonet.core.api.Address;
import algores.holonet.core.api.Key;
import algores.holonet.core.api.NodeHandle;
import algores.holonet.core.api.Range;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Anton Kraievoy
 */
public interface RoutingService extends RoutingState<RoutingEntry, Key> {
  void update(RoutingEntry handle, Event event);

  void update(RoutingEntry[] routingData, Event event);

  RoutingEntry getOwnRoute() throws CommunicationException;

  /**
   * Provides information about ranges of keys for which the node <code>handle</code> is currently a <i>r</i>-root.
   * <p>If the node referenced by <code>handle</code> is responsible for key <code>lkey</code>,
   * then the resulting range includes <code>lkey</code>. Otherwise, the result is the nearest range clockwise from lkey
   * for which <code>handle</code> is responsible.</p>
   * <p>It is an error to query the range of a node not present in the neighbor set as returned by
   * the {@link RoutingService#update} upcall or the
   * {@link RoutingService#neighborSet} call.</p>
   * <p>Certain implementations may return <code>null</code> if <code>rank</code> is greater than zero.
   * Some protocols may have multiple, disjoint ranges of keys for which a given node is responsible.</p>
   *
   * @param handle to inspect
   * @param rank   rank <i>r</i> of the supported range (lesser ranked range may be returned)
   * @param lKey   allows the caller to specify which region should be returned
   * @return <code>null</code> if the range could not be determined.
   * @throws algores.holonet.core.CommunicationException
   *          if remoting call find node dead for example
   */
  Range getRange(NodeHandle handle, byte rank, Key lKey) throws CommunicationException;

  //	------------------------------------
  //	overriding stuff from the Common API
  //	------------------------------------

  /**
   * @param handle to inspect
   * @param rank   rank <i>r</i> of the supported range (lesser ranked range may be returned)
   * @param lKey   allows the caller to specify which region should be returned
   * @param rKey   out-parameter, input value ignored
   * @return <code>false</code> if the range could not be determined, <code>true</code>
   *         otherwise.
   * @deprecated because ranges are closed (inclusive) on both sides, in-out params are also somewhat troubling,
   *             use {@link RoutingService#getRange(NodeHandle , byte, Key)} instead
   */
  @Deprecated
  boolean range(RoutingEntry handle, byte rank, AtomicReference<Key> lKey, AtomicReference<Key> rKey);

  List<RoutingEntry> localLookup(Key key, int num, boolean safe);

  List<RoutingEntry> neighborSet(int num);

  List<RoutingEntry> replicaSet(Key key, byte maxRank);

  List<RoutingEntry> getRoutes() throws CommunicationException;

  void registerCommunicationFailure(Address calleeAddress);

  double getRedundancy();

  void setRedundancy(double redundancy);
}
