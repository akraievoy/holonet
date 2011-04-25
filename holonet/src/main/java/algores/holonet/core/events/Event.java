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
import org.akraievoy.base.runner.api.RefDouble;
import org.akraievoy.cnet.gen.vo.EntropySource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An abstract implementor.
 */
public abstract class Event {
  protected static final Logger log = LoggerFactory.getLogger(Event.class);

  /**
   * Various event execution results.
   */
  public static enum Result {
    SUCCESS, FAILURE, PASSIVE
  }

  private static boolean reporting = false;

  protected boolean failOnError;
  protected RefDouble probability = new RefDouble(1.0);

  public void setFailOnError(boolean failOnError) {
    this.failOnError = failOnError;
  }

  public final EventComposite.Result execute(Network targetNetwork, EntropySource eSource) {
    if (shouldIDoThis(eSource)) {
      return executeInternal(targetNetwork, eSource);
    } else {
      return EventComposite.Result.PASSIVE;
    }
  }

  protected boolean shouldIDoThis(EntropySource eSource) {
    final double prob = probability.getValue();
    return prob == 1.0 || eSource.nextDouble() < prob;
  }

  protected abstract EventComposite.Result executeInternal(Network targetNetwork, final EntropySource eSource);

  public void setProbability(final double newProbability) {
    probability.setValue(newProbability);
  }

  public void setProbabilityRef(final RefDouble newProbability) {
    probability = newProbability;
  }

  public EventComposite.Result handleEventFailure(final SimulatorException anException, final String aMessage) {
    final Result result = failOnError ? Result.FAILURE : Result.PASSIVE;

    if (reporting) {
      if (anException != null) {
        log.info(aMessage, anException);
      } else {
        log.info("{}", aMessage);
      }
    }

    return result;
  }
}
