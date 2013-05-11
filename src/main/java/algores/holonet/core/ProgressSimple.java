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

package algores.holonet.core;

import com.google.common.base.Stopwatch;

import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.*;

public class ProgressSimple {
  private final Stopwatch stopwatch;
  private final long iterations;

  private long lastElapsed = 0;

  public ProgressSimple(final long iterations) {
    this.stopwatch = new Stopwatch();
    this.iterations = iterations;
  }

  public ProgressSimple start() {
    stopwatch.reset();
    stopwatch.start();
    return this;
  }

  public String iter(long index) {
    final long elapsed = stopwatch.elapsedTime(NANOSECONDS);
    final long nanos = elapsed - lastElapsed;
    lastElapsed = elapsed;

    final String progressFormatted = String.format(
        "%d%%: %d of %d in %s, at %s/op %3g/sec (avg. %s/op %3g/sec)",
        index * 100 / iterations,
        index,
        iterations,
        toString(elapsed, 3),
        toString(nanos, 3),
        1.0e9 / nanos,
        toString(elapsed / (index + 1), 3),
        1.0e9 / (elapsed / (index + 1))
    );

    return progressFormatted;
  }

  public String stop() {
    stopwatch.stop();
    final long nanos = stopwatch.elapsedTime(NANOSECONDS);
    final String timeFormatted = String.format(
        "%s, at %s/op, %3g ops/sec",
        stopwatch.toString(3),
        toString(nanos / iterations, 3),
        1.0e9 * iterations / nanos
    );
    return timeFormatted;
  }

  //  =============================================================
  //   rude copy-paste of formatting code from the Guava Stopwatch
  //  >> too bad this functionality is not exposed as a method call
  //  =============================================================
  private String toString(final long nanos, final int significantDigits) {
    TimeUnit unit = chooseUnit(nanos);
    double value = (double) nanos / NANOSECONDS.convert(1, unit);

    // Too bad this functionality is not exposed as a regular method call
    return String.format("%." + significantDigits + "g %s",
        value, abbreviate(unit));
  }

  private static TimeUnit chooseUnit(long nanos) {
    if (SECONDS.convert(nanos, NANOSECONDS) > 0) {
      return SECONDS;
    }
    if (MILLISECONDS.convert(nanos, NANOSECONDS) > 0) {
      return MILLISECONDS;
    }
    if (MICROSECONDS.convert(nanos, NANOSECONDS) > 0) {
      return MICROSECONDS;
    }
    return NANOSECONDS;
  }

  private static String abbreviate(TimeUnit unit) {
    switch (unit) {
      case NANOSECONDS:
        return "ns";
      case MICROSECONDS:
        return "\u03bcs"; // μs
      case MILLISECONDS:
        return "ms";
      case SECONDS:
        return "s";
      default:
        throw new AssertionError();
    }
  }

  public static void main(String[] args) throws InterruptedException {
    final ProgressSimple sleep = new ProgressSimple(300).start();
    for (int i = 0; i < 300; i++) {
      sleep.iter(i);
      Thread.sleep(100);
    }
    sleep.stop();
  }
}
