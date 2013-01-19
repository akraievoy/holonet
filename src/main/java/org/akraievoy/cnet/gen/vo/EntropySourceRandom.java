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

import org.akraievoy.base.ref.Ref;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class EntropySourceRandom implements EntropySource {
  private static final Logger log = LoggerFactory.getLogger(EntropySourceRandom.class);

  protected final Random random;
  protected Long seed;
  protected long consumed = 0;

  public EntropySourceRandom() {
    this.random = new Random();
  }

  public void setSeedRef(final Ref<Long> seed) {
    setSeed(seed.getValue());
    consumed = 0;
  }

  public void setSeed(long seed) {
    random.setSeed(seed);

    this.seed = seed;
    consumed = 0;
  }

  public int nextInt(int maxExclusive) {
    consumed += 32;
    this.seed = null;

    return random.nextInt(maxExclusive);
  }

  public double nextDouble() {
    consumed += 23;
    this.seed = null;

    return random.nextDouble();
  }

  public double nextGaussian() {
    consumed += 64;
    this.seed = null;

    return random.nextGaussian();
  }

  public double nextLogGaussian() {
    return Math.exp(nextGaussian());
  }

  public double nextGaussian(double mean, double variance) {
    consumed += 64;
    this.seed = null;

    return random.nextGaussian() * variance + mean;
  }

  public double nextLogGaussian(double mean, double variance) {
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
    consumed += 32 - Integer.numberOfLeadingZeros(size - 1);

    final int targetIndex = nextInt(size);

    if (elements instanceof List) {
      final List<E> elemList = (List<E>) elements;
      return elemList.get(targetIndex);
    }

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
    consumed += 32 - Integer.numberOfLeadingZeros(size - 1);

    return elements[nextInt(size)];
  }

  public void nextBytes(byte[] bytes) {
    consumed += 8 * bytes.length;

    random.nextBytes(bytes);
  }

  public long consumedBits() {
    return consumed;
  }
}
