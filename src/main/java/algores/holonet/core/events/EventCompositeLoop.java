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

import org.akraievoy.base.ref.Ref;
import org.akraievoy.base.runner.api.RefLong;

/**
 * Composite event consisting of repetitions of nested event.
 */
public class EventCompositeLoop extends EventComposite<EventCompositeLoop> {
  private final Event nestedEvent;
  int currentLoop;
  Ref<Long> count = new RefLong(1);

  public EventCompositeLoop(final Event<?> newNestedEvent) {
    nestedEvent = newNestedEvent;
  }

  public void setCountRef(final Ref<Long> loopCountRef) {
    this.count = loopCountRef;
  }

  public EventCompositeLoop withCountRef(Ref<Long> loopCountRef) {
    setCountRef(loopCountRef);
    return this;
  }

  public void setCount(final int loopCount) {
    this.count.setValue((long) loopCount);
  }

  public Event<?> generateNextEvent() {
    if (currentLoop < count.getValue()) {
      currentLoop++;
      return nestedEvent;
    }

    throw new AssertionError("Querying exhausted eventLoop");
  }

  public boolean isExhausted() {
    return currentLoop >= count.getValue();
  }

  public void reset() {
    currentLoop = 0;
  }

}
