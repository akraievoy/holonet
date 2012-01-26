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

package algores.holonet.core.api.tier0.storage;

import algores.holonet.core.api.Key;
import algores.holonet.core.api.KeySource;
import algores.holonet.core.api.KeySpace;
import algores.holonet.core.api.LocalServiceBase;

import java.util.*;

/**
 * Default implementation.
 *
 * @author Anton Kraievoy
 */
public class StorageServiceBase extends LocalServiceBase implements StorageService {
  //	TODO there's no need for hashmap, two sorted lists would do better and faster
  protected final Map<Key, Object> dataEntries = new HashMap<Key, Object>();

  public Object get(Key key) {
    return dataEntries.get(key);
  }

  public void put(Key key, Object value) {
    dataEntries.put(key, value);

    getMappings().register(key, value, getOwner());
  }

  public void putAll(final Map<Key, Object> newDataEntries) {
    dataEntries.putAll(newDataEntries);

    for (Map.Entry<Key, Object> entry : newDataEntries.entrySet()) {
      getMappings().register(entry.getKey(), entry.getValue(), getOwner());
    }
  }

  public Map<Key, Object> getDataEntries() {
    return Collections.unmodifiableMap(dataEntries);
  }

  public int getEntryCount() {
    return dataEntries.size();
  }

  public int getKeyCountForPath(Key forKey, int forBits) {
    int result = 0;

    final Set<Key> pKeys = dataEntries.keySet();
    for (Key key : pKeys) {
      if (KeySpace.sameKey(key, forKey, forBits)) {
        result++;
      }
    }

    return result;
  }

  public Map<Key, Object> filter(KeySource min, boolean includeMin, KeySource max, boolean includeMax) {
    final Map<Key, Object> migratingEntries = new HashMap<Key, Object>();

    for (Iterator entryIt = dataEntries.keySet().iterator(); entryIt.hasNext();) {
      final Key key = (Key) entryIt.next();

      if (KeySpace.isInRange(min, includeMin, max, includeMax, key)) {
        final Object value = dataEntries.get(key);
        migratingEntries.put(key, value);
        entryIt.remove();

        getMappings().deregister(key, getOwner(), false);
      }
    }

    return migratingEntries;
  }

  public Map<Key, Object> filter(Key filterKey, int filterBits, boolean remove) {
    return filterTo(filterKey, filterBits, remove, null);
  }

  public Map<Key, Object> filterTo(Key filterKey, int filterBits, boolean remove, Map<Key, Object> dest) {
    if (dest == null) {
      dest = new TreeMap<Key, Object>();
    }

    Set<Key> keys = dataEntries.keySet();
    for (Key key : keys) {
      if (KeySpace.sameKey(key, filterKey, filterBits)) {
        dest.put(key, dataEntries.get(key));
      }
    }

    if (remove) {
      dataEntries.keySet().removeAll(dest.keySet());

      for (Map.Entry<Key, Object> entry : dest.entrySet()) {
        getMappings().deregister(
            entry.getKey(), getOwner(), false
        );
      }
    }

    return dest;
  }

  public Collection<Key> getKeys() {
    return dataEntries.keySet();
  }

  public StorageServiceBase copy() {
    return new StorageServiceBase();
  }
}
