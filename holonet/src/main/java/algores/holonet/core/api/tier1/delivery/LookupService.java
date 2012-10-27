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

package algores.holonet.core.api.tier1.delivery;

import algores.holonet.capi.Event;
import algores.holonet.core.CommunicationException;
import algores.holonet.core.api.Address;
import algores.holonet.core.api.Key;
import algores.holonet.core.api.tier0.routing.RoutingEntry;
import com.google.common.base.Optional;

import java.util.*;

/**
 * Delivery service spec.
 */
public interface LookupService {
  long HOP_LIMIT = 65535;

  enum Mode {JOIN, LEAVE, FIXFINGERS, GET, PUT}

  Address lookup(Key key, boolean mustExist, Mode mode) throws CommunicationException;

  RecursiveLookupState recursiveLookup(
      Key key,
      boolean mustExist,
      RecursiveLookupState state
  ) throws CommunicationException;

  public static class RecursiveLookupState {
    public final Optional<Address> replicaOpt;
    public final List<RoutingEntry> replicaPath;
    public final List<Traversal> traversals;
    public final int hopCount;
    public final double hopDistance;

    public RecursiveLookupState(
        final Optional<Address> replicaOpt,
        final List<RoutingEntry> replicaPath,
        final List<Traversal> traversals,
        final int hopCount,
        final double hopDistance
    ) {
      this.replicaOpt = replicaOpt;
      this.traversals = Collections.unmodifiableList(traversals);
      this.hopCount = hopCount;
      this.replicaPath = Collections.unmodifiableList(replicaPath);
      this.hopDistance = hopDistance;
    }

    public RecursiveLookupState(
      final RoutingEntry ownerEntry
    ) {
      this(
          Optional.<Address>absent(),
          Arrays.asList(ownerEntry), Arrays.asList(
              new Traversal(ownerEntry, 0, 0, Event.HEART_BEAT)
          ),
          0,
          Double.MAX_VALUE
      );
    }

    //  LATER it's now possible to create nice dump method for debug purposes
  }

  public static class Traversal {
    public final RoutingEntry re;
    public final int hopAdded;
    public final int hopCalled;
    public final Event event;

    public Traversal(
        final RoutingEntry re,
        final int hopAdded,
        final int hopCalled,
        final Event event
    ) {
      this.event = event;
      this.re = re;
      this.hopAdded = hopAdded;
      this.hopCalled = hopCalled;
    }

    public boolean called() {
      return hopCalled >= 0;
    }
  }
}
