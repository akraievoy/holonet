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

import com.google.common.base.Optional;
import gnu.trove.TDoubleArrayList;
import gnu.trove.TIntArrayList;
import org.akraievoy.base.Die;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class WeightedEventModel {
  private static final Logger log = LoggerFactory.getLogger(WeightedEventModel.class);

  //  so that you're able to change it in any debug session with no extra bashing
  public static boolean TRACE_HASHES = false;

  public static final double MIN_WEIGHT_DEFAULT = 0.05;
  protected final TIntArrayList events = new TIntArrayList();
  protected final TDoubleArrayList weights = new TDoubleArrayList();
  //	if this is not null then weights've been renormalized and the object is now in the generation phase
  protected TDoubleArrayList sums = null;
  protected final Optional<String> name;

  protected WeightedEventModel(Optional<String> name) {
    this.name = name;
  }

  protected WeightedEventModel() {
    this(null);
  }

  public int generate(EntropySource eSource, final boolean remove, int[] indexRef) {
    Die.ifTrue("please initialize events", events.isEmpty());

    if (sums == null) {
      initSums();
      if (name.isPresent() && TRACE_HASHES) {
        log.debug(
            "WeightedEventModel({}).init() with hash == {}:{}",
            new Object[] {name.get(), events.hashCode(), weights.hashCode()}
        );
      }
    }

    final double eventValue = eSource.nextDouble() * getSum();
    final int searchIndex = sums.binarySearch(eventValue);

    final int eventIndex = searchIndex < 0 ? -(searchIndex + 1) : searchIndex;
    Die.ifTrue("eventIndex >= events.size()", eventIndex >= events.size());

    final int result = events.get(eventIndex);

    if (remove) {
      removeByIndex(eventIndex);
    }
    if (indexRef != null && indexRef.length > 0) {
      indexRef[0] = eventIndex;
    }

    return result;
  }

  public void clear() {
    events.clear();
    weights.clear();

    if (sums != null) {
      sums.clear();  //	assist GC a bit
    }
    sums = null;
  }

  public int getSize() {
    final int size = events.size();

    return size;
  }

  public void add(int e, double weight) {
    final int insPoint = weights.binarySearch(weight);
    final int evtIndex = insPoint < 0 ? -(insPoint + 1) : insPoint;

    events.insert(evtIndex, e);
    weights.insert(evtIndex, weight);
    extendSums(evtIndex, weight);
  }

  protected abstract void extendSums(int evtIndex, double weight);

  public void remove(int val) {
    final int searchIndex = sums.binarySearch(val);

    final int eventIndex = searchIndex < 0 ? -(searchIndex + 1) : searchIndex;
    Die.ifTrue("eventIndex >= events.size()", eventIndex >= events.size());

    removeByIndex(eventIndex);
  }

  public void removeByIndex(int index) {
    Die.ifNull("sums", sums);
    increment(index, -weights.get(index));

    events.remove(index);
    weights.remove(index);
    sums.remove(index);
  }

  protected void increment(int index, double diff) {
    final double oldWeight = weights.get(index);
    final double newWeight = oldWeight + diff;
    if (newWeight < 0) {
      throw new IllegalStateException("weight must be non-negative, but is: " + newWeight);
    }

    weights.set(index, newWeight);
    for (int i = index; i < sums.size(); i++) {
      sums.set(i, sums.get(i) + diff);
    }
  }

  protected double getSum() {
    final double sum = sums.isEmpty() ? 0 : sums.get(sums.size() - 1);

    return sum;
  }

  protected abstract void initSums();
}
