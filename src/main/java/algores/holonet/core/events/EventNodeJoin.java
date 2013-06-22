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

package algores.holonet.core.events;

import algores.holonet.core.Network;
import algores.holonet.core.Node;
import algores.holonet.core.SimulatorException;
import algores.holonet.core.api.Key;
import org.akraievoy.base.ref.Ref;
import org.akraievoy.holonet.exp.store.RefObject;
import org.akraievoy.cnet.gen.vo.EntropySource;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * Node join event.
 */
public class EventNodeJoin extends Event<EventNodeJoin> {
  protected Ref<Long> count = new RefObject<Long>(1L);

  public void setCountRef(Ref<Long> nodeCount) {
    this.count = nodeCount;
  }

  public EventNodeJoin withCountRef(Ref<Long> nodeCountRef) {
    setCountRef(nodeCountRef);
    return this;
  }

  public void setCount(int nodeCount) {
    this.count.setValue((long) nodeCount);
  }

  public Result executeInternal(Network network, final EntropySource eSource) {
    try {
      network.insertNodes(count.getValue().intValue(), null, eSource);

      //  HOUSTON deactivate this debugging printout
      final TreeMap<Key, Integer> keyToEntryCount = new TreeMap<Key, Integer>();
      for (Node node : network.getAllNodes()) {
        keyToEntryCount.put(node.getKey(), node.getServices().getStorage().getEntryCount());
      }
      log.info("keyToEntryCount = " + String.valueOf(keyToEntryCount));

      return Result.SUCCESS;
    } catch (SimulatorException e) {
      return handleEventFailure(e, null);
    }
  }
}
