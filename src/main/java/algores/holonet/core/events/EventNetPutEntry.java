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
import algores.holonet.core.SimulatorException;
import org.akraievoy.base.runner.api.RefLong;
import org.akraievoy.cnet.gen.vo.EntropySource;

/**
 * Adding a data entry to a node.
 */
class EventNetPutEntry extends Event {
  protected RefLong count = new RefLong(1);

  public void setCountRef(final RefLong count) {
    this.count = count;
  }

  public void setCount(final long count) {
    this.count.setValue(count);
  }

  public EventComposite.Result executeInternal(Network network, final EntropySource eSource) {
    try {
      network.putDataEntries(count.getValue().intValue(), eSource);
      return EventComposite.Result.SUCCESS;
    } catch (SimulatorException e) {
      return handleEventFailure(e, null);
    }
  }
}
