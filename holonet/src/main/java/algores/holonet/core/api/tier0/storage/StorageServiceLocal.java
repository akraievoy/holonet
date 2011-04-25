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
import algores.holonet.core.api.LocalService;

import java.util.Collection;
import java.util.Map;

/**
 * Base distributed hash-table entry management operation abstractions.
 *
 * @author Anton Kraievoy
 */
public interface StorageServiceLocal extends StorageService, LocalService {
  Object get(Key key);

  void put(Key key, Object value);

  void putAll(Map<Key, Object> newDataEntries);

  Map<Key, Object> getDataEntries();

  Map<Key, Object> filter(KeySource min, boolean includeMin, KeySource max, boolean includeMax);

  int getKeyCountForPath(Key forKey, int forBits);

  Map<Key, Object> filter(Key filterKey, int filterBits, boolean remove);

  Map<Key, Object> filterTo(Key filterKey, int filterBits, boolean remove, Map<Key, Object> dest);

  Collection<Key> getKeys();

  int getEntryCount();
}