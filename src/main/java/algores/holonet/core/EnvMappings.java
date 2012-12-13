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

import algores.holonet.core.api.Key;
import org.akraievoy.cnet.gen.vo.EntropySource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Tracking DHT key/value mappings for the Network Environment.
 */
public class EnvMappings {
  private static final Logger log = LoggerFactory.getLogger(EnvMappings.class);

  protected final List<SimpleEntry> mappings =
      new ArrayList<SimpleEntry>();
  protected final SimpleEntryComparator mappingComparator =
      new SimpleEntryComparator();

  public void register(
      final Key key,
      final Object value,
      final Node owner
  ) {
    final SimpleEntry newEntry = new SimpleEntry(key, value);
    newEntry.getReplicas().add(owner);

    final int search =
        Collections.binarySearch(
            mappings,
            newEntry,
            mappingComparator
        );
    
    if (search >= 0) {
      //  add all previously registered replicas
      newEntry.getReplicas().addAll(mappings.get(search).getReplicas());
      mappings.set(search, newEntry);
    } else {
      final int insertionPoint= -(search + 1);
      mappings.add(insertionPoint, newEntry);
    }
  }

  public void deregister(
      final Key key,
      final Node owner,
      final boolean remove
  ) {
    final SimpleEntry tempEntry = new SimpleEntry(key, "temp");

    final int search =
        Collections.binarySearch(
            mappings,
            tempEntry,
            mappingComparator
        );

    if (search < 0) {
      //  sanity
      log.warn("deregistering non-registered mapping: {} @ {}", key, owner);
      return;
    }

    final SimpleEntry entry = mappings.get(search);
    entry.getReplicas().remove(owner);
    if (remove && entry.getReplicas().isEmpty()) {
      mappings.remove(search);
    }
  }

  public SimpleEntry select(EntropySource eSource) {
    return eSource.randomElement(mappings);
  }

  protected static class SimpleEntry implements Map.Entry<Key, Object> {
    private final Key key;
    private final Object value;
    //  LATER: ideally this should be a treemap node -> int
    //    which also tracks replica rank
    private final TreeSet<Node> replicas = new TreeSet<Node>();

    public SimpleEntry(Key key, Object value) {
      this.key = key;
      this.value = value;
    }

    public Key getKey() {
      return key;
    }

    public Object getValue() {
      return value;
    }

    public Object setValue(Object value) {
      throw new UnsupportedOperationException(
          "should not have been implemented"
      );
    }

    public SortedSet<Node> getReplicas() {
      return replicas;
    }
  }
  
  protected static class SimpleEntryComparator
      implements Comparator<Map.Entry<Key, Object>> {
    public int compare(Map.Entry<Key, Object> o1, Map.Entry<Key, Object> o2) {
      return o1.getKey().compareTo(o2.getKey());
    }
  }
}
