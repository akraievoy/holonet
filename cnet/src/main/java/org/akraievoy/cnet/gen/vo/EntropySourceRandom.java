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

package org.akraievoy.cnet.gen.vo;

import org.akraievoy.base.runner.api.RefLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Iterator;
import java.util.Random;

public class EntropySourceRandom implements EntropySource {
  private static final Logger log = LoggerFactory.getLogger(EntropySourceRandom.class);

  protected final Random random;
  protected Long seed;

  public EntropySourceRandom() {
    this.random = new Random();
  }

  public void setSeedRef(final RefLong seed) {
    setSeed(seed.getValue());
  }

  public void setSeed(long seed) {
    random.setSeed(seed);

    this.seed = seed;
  }

  public int nextInt(int maxExclusive) {
    this.seed = null;

    return random.nextInt(maxExclusive);
  }

  public double nextDouble() {
    this.seed = null;

    return random.nextDouble();
  }

  public double nextGaussian() {
    this.seed = null;

    return random.nextGaussian();
  }

  public double nextLogGaussian() {
    this.seed = null;

    return Math.exp(nextGaussian());
  }

  public double nextGaussian(double mean, double variance) {
    this.seed = null;

    return random.nextGaussian() * variance + mean;
  }

  public double nextLogGaussian(double mean, double variance) {
    this.seed = null;

    return Math.exp(nextGaussian(mean, variance));
  }

  public void diagnoseSeed(String targetName) {
    if (seed != null) {
      log.info("seed for {} is {}", targetName, seed);
    } else {
      log.warn("seed for {} not set or already consumed", targetName);
    }
  }

  public <E> E randomElement(final Collection<E> elements) {
    final int size = elements.size();

    if (size == 0) {
      return null;
    }

    final int targetIndex = nextInt(size);
    int index = 0;
    for (Iterator<E> iterator = elements.iterator(); iterator.hasNext(); index++) {
      E target = iterator.next();
      if (index == targetIndex) {
        return target;
      }
    }

    throw new IllegalStateException("Should be unreachable");
  }

  public <E> E randomElement(final E[] elements) {
    final int size = elements.length;
    if (size == 0) {
      return null;
    }

    return elements[nextInt(size)];
  }

  public void nextBytes(byte[] bytes) {
    random.nextBytes(bytes);
  }
}
