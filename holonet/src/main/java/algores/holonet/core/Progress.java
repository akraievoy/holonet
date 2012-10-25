/*
 Copyright 2012 Anton Kraievoy akraievoy@gmail.com
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
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

public class Progress {
  private final static int SILENT = 32;
  private final static int MULTILINE_ITERATIONS = 128;
  public static final int WIDTH = 32;

  private final String description;
  private final Stopwatch stopwatch;
  private final long iterations;
  private final long divider;
  private final boolean silent;

  private long lastNanos = 0;
  private boolean failsSinceLst = false;
  private boolean line = false;

  public Progress(String description, final long iterations) {
    this.description = description;
    this.stopwatch = new Stopwatch();
    this.iterations = iterations;
    long divider = 1;
    while (divider * WIDTH * WIDTH < iterations) {
      divider *= 2;
    }
    this.divider = divider;
    this.silent = iterations <= SILENT;
  }

  public Progress start() {
    if (!silent) {
      if (iterations < MULTILINE_ITERATIONS) {
        System.out.print(String.format("%s[%d] ... ", description, iterations));
      } else {
        System.out.println(String.format("%s[%d]:", description, iterations));
      }
    }
    stopwatch.start();
    return this;
  }

  public void iter(long index) {
    iter(index, true);
  }

  public void iter(long index, boolean success) {
    if (silent || iterations < MULTILINE_ITERATIONS) {
      return;
    }
    failsSinceLst |= !success;
    if (index % divider != 0) {
      return;
    }
    final long nanos = stopwatch.elapsedTime(NANOSECONDS) - lastNanos;
    if (!failsSinceLst) {
      System.out.print(".");
    } else {
      System.out.print("!");
    }
    failsSinceLst = false;
    if (index / divider % WIDTH == 31) {
      lastNanos = nanos;
      line = false;
      System.out.println(
          String.format(
              "%d%%: %d of %d in %s, at %s/op, %6g ops/sec",
              index * 100 / iterations,
              index,
              iterations,
              toString(nanos, 3),
              toString(nanos / WIDTH / divider, 3),
              1.0e9 * WIDTH * divider / nanos
          )
      );
    } else {
      line = true;
    }
  }

  public void stop() {
    stopwatch.stop();
    if (silent) {
      return;
    }
    final long nanos = stopwatch.elapsedTime(NANOSECONDS);
    if (iterations >= MULTILINE_ITERATIONS && line) {
      System.out.print("\n");
    }
    System.out.println(
        String.format(
            "DONE in %s, at %s/op, %6g ops/sec",
            stopwatch.toString(3),
            toString(nanos / iterations, 3),
            1.0e9 * iterations / nanos
        )
    );
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
}
