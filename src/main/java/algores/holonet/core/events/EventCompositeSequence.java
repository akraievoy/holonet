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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * Represents sequence of events.
 */
public class EventCompositeSequence extends EventComposite<EventCompositeSequence> {
  protected final Collection<Event<?>> events = new ArrayList<Event<?>>();
  protected Iterator eventIterator;

  public EventCompositeSequence(final Collection<Event<?>> newEvents) {
    events.addAll(newEvents);
  }

  public Event<?> generateNextEvent() {
    return (Event) getEventIterator().next();
  }

  public boolean isExhausted() {
    return !getEventIterator().hasNext();
  }

  public void reset() {
    eventIterator = null;
  }

  protected Iterator getEventIterator() {
    if (eventIterator == null) {
      eventIterator = events.iterator();
    }
    return eventIterator;
  }
}
