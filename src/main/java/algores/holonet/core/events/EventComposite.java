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
import org.akraievoy.cnet.gen.vo.EntropySource;

/**
 * The way to treat EventComposite as a regular Event.
 * <p/>
 * If a nested event once fails, no further attempts will be performed,
 * so composite event sequence will be truncated on upon first failure.
 */
public abstract class EventComposite<SelfType extends Event<?>> extends Event<SelfType> {
  private boolean stopOnFailure = false;

  /**
   * Generates next event for execution.
   *
   * @return <code>null</code> if and only if {@link EventComposite#isExhausted()} returns true.
   */
  public abstract Event<?> generateNextEvent();

  public void setStopOnFailure(boolean stopOnFailure) {
    this.stopOnFailure = stopOnFailure;
  }

  @SuppressWarnings("unchecked")
  public SelfType withStopOnFailure(boolean stopOnFailure) {
    setStopOnFailure(stopOnFailure);
    return (SelfType) this;
  }

  /**
   * Returns <code>true</code> if no more events will be available.
   */
  public abstract boolean isExhausted();

  /**
   * Resets event generation state.
   * This will effectively &quot;rewind&quot; event sequence to the very beginning.
   */
  public abstract void reset();

  /**
   * Will stop execution if one of events fails.
   *
   * @param targetNetwork to be affected by this event sequence.
   * @return true if the sequence succeeded as a whole
   */
  public Result executeInternal(final Network targetNetwork, final EntropySource eSource) {
    reset();
    Result aggregateResult = Result.PASSIVE;

    while (!isExhausted()) {
      final Event event = generateNextEvent();

      final Result result = event.execute(targetNetwork, eSource);

      if (Result.FAILURE.equals(result)) {
        //  FIXME redundamcy, remove Composite in favor of Iterable and couple of iteration strategies?
        aggregateResult = Result.FAILURE;
        if (stopOnFailure) {
          return handleEventFailure(null, "Failure on nested level");
        }
      }

      if (Result.SUCCESS.equals(result)) {
        aggregateResult = Result.SUCCESS;
      }
    }

    return aggregateResult;
  }
}
