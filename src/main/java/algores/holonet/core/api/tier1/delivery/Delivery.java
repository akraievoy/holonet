/*
 Copyright 2013 Anton Kraievoy akraievoy@gmail.com
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

import algores.holonet.core.api.Address;
import com.google.common.base.Optional;

import java.util.*;

import static algores.holonet.core.api.tier0.routing.Routing.*;

public final class Delivery {
  private Delivery() { /* sealed */ }

  public static Iterable<RoutingEntry> mapToRoutes(Iterable<Traversal> traversals) {
    final List<RoutingEntry> res = new ArrayList<RoutingEntry>();
    for (Traversal t : traversals) {
      res.add(t.re);
    }
    return res;
  }

  public static class RecursiveLookupState {
    private static final SortedMap<Address, Traversal> EMPTY_MAP =
        Collections.unmodifiableSortedMap(new TreeMap<Address, Traversal>());

    public final Optional<Address> replicaOpt;
    public final List<RoutingEntry> replicaPath;
    public final SortedMap<Address, Traversal> traversed;
    public final SortedMap<Address, Traversal> failed;
    public final SortedMap<Address, Traversal> pending;
    public final int hopCount;
    public final double hopDistance;

    public RecursiveLookupState(
        final Optional<Address> replicaOpt,
        final List<RoutingEntry> replicaPath,
        final SortedMap<Address, Traversal> traversed,
        final SortedMap<Address, Traversal> failed,
        final SortedMap<Address, Traversal> pending,
        final int hopCount,
        final double hopDistance
    ) {
      if (traversed.isEmpty()) {
        throw new IllegalStateException("traversals.isEmpty");
      }

      this.replicaOpt = replicaOpt;
      this.replicaPath =
          Collections.unmodifiableList(
              new ArrayList<RoutingEntry>(replicaPath)
          );
      this.traversed =
          Collections.unmodifiableSortedMap(
              new TreeMap<Address, Traversal>(traversed)
          );
      this.failed =
          Collections.unmodifiableSortedMap(
              new TreeMap<Address, Traversal>(failed)
          );
      this.pending =
          Collections.unmodifiableSortedMap(
              new TreeMap<Address, Traversal>(pending)
          );
      this.hopCount = hopCount;
      this.hopDistance = hopDistance;
    }

    public RecursiveLookupState(
      final RoutingEntry ownerEntry
    ) {
      this(
          Optional.<Address>absent(),
          Arrays.asList(ownerEntry),
          singleton(ownerEntry),
          EMPTY_MAP,
          EMPTY_MAP,
          0,
          Double.MAX_VALUE
      );
    }

    private static SortedMap<Address, Traversal> singleton(
        RoutingEntry ownerEntry
    ) {
      final TreeMap<Address, Traversal> singleton =
          new TreeMap<Address, Traversal>();
      singleton.put(
          ownerEntry.getAddress(),
          new Traversal(ownerEntry, 0, 0)
      );
      return singleton;
    }

    public RecursiveLookupState withReplica(Address replica) {
      return new RecursiveLookupState(
          Optional.of(replica),
          replicaPath,
          traversed,
          failed,
          pending,
          hopCount,
          hopDistance
      );
    }

    public StatsTuple getStats() {
      int called = 0;
      int failed = 0;
      for (Traversal t : this.traversed.values()) {
        if (!t.called()) {
          throw new IllegalStateException("t.called == false");
        }
        called++;
      }
      for (Traversal t : this.traversed.values()) {
        if (!t.called()) {
          throw new IllegalStateException("t.called == false");
        }
        called++;
        failed++;
      }
      for (Traversal t : pending.values()) {
        if (t.called()) {
          throw new IllegalStateException("t.called == true");
        }
      }
      if (called == 0) {
        throw new IllegalStateException("called == 0");
      }
      return new StatsTuple(traversed.size() + pending.size(), called, failed);
    }

    //  LATER it's now possible to create nice dump method for debug purposes
  }

  public static class Traversal {
    public final RoutingEntry re;
    public final int hopAdded;
    public final int hopCalled;

    public Traversal(
        final RoutingEntry re,
        final int hopAdded,
        final int hopCalled
    ) {
      this.re = re;
      this.hopAdded = hopAdded;
      this.hopCalled = hopCalled;
    }

    public boolean called() {
      return hopCalled >= 0;
    }

    public Traversal calledAtHop(final int hopCalled) {
      return new Traversal(re, hopAdded, hopCalled);
    }

    @Override
    public String toString() {
      return re.getAddress() + " : " + hopAdded + " --> " + hopCalled;
    }
  }

  public static class StatsTuple {
    public final int traversalsAdded;
    public final int traversalsCalled;
    public final int traversalsFailed;
    public final int traversalsSucceeded;

    public StatsTuple(
        int traversalsAdded,
        int traversalsCalled,
        int traversalsFailed
    ) {
      this.traversalsAdded = traversalsAdded;
      this.traversalsCalled = traversalsCalled;
      this.traversalsFailed = traversalsFailed;
      this.traversalsSucceeded = this.traversalsCalled - this.traversalsFailed;
    }
  }
}
