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

import algores.holonet.core.CommunicationException;
import algores.holonet.core.Network;
import org.akraievoy.base.ref.Ref;
import org.akraievoy.holonet.exp.store.RefObject;
import org.akraievoy.cnet.gen.vo.EntropySource;

/**
 * Node stabilize event.
 */
public class EventNodeStabilize extends Event<EventNodeStabilize> {
  protected Ref<Long> count = new RefObject<Long>(1L);

  public void setCountRef(Ref<Long> nodeCount) {
    this.count = nodeCount;
  }

  public void setCount(int nodeCount) {
    this.count.setValue((long) nodeCount);
  }

  public EventNodeStabilize withCounthRef(Ref<Long> nodeCountRef) {
    setCountRef(nodeCountRef);
    return this;
  }

  public Result executeInternal(Network targetNetwork, final EntropySource eSource) {
    final long count = this.count.getValue().intValue();
    for (int callCount = 0; callCount < count; callCount++) {
      try {
        targetNetwork.getRandomNode(eSource).getServices().getOverlay().stabilize();
      } catch (CommunicationException e) {
        return handleEventFailure(e, null);
      }
    }

    return Result.SUCCESS;
  }
}