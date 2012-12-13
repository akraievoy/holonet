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

import algores.holonet.core.CommunicationException;
import algores.holonet.core.api.Key;
import algores.holonet.core.api.KeySource;

import java.util.Collection;
import java.util.Map;

/**
 * Base distributed hash-table entry management operation abstractions.
 *
 * @author Anton Kraievoy
 */
public interface StorageService {
  Object get(Key key) throws CommunicationException;

  void put(Key key, Object value) throws CommunicationException;

  void putAll(Map<Key, Object> newDataEntries) throws CommunicationException;

  Map<Key, Object> getDataEntries() throws CommunicationException;

  Map<Key, Object> filter(KeySource min, boolean includeMin, KeySource max, boolean includeMax) throws CommunicationException;

  int getKeyCountForPath(Key forKey, int forBits) throws CommunicationException;

  Map<Key, Object> filter(Key filterKey, int filterBits, boolean remove) throws CommunicationException;

  Map<Key, Object> filterTo(Key filterKey, int filterBits, boolean remove, Map<Key, Object> dest) throws CommunicationException;

  Collection<Key> getKeys() throws CommunicationException;

  int getEntryCount() throws CommunicationException;
}
