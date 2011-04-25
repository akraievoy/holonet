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

package org.akraievoy.gear;

import gnu.trove.TDoubleArrayList;
import gnu.trove.TIntArrayList;

import java.io.*;
import java.lang.reflect.Field;
import java.util.List;

public class G4Trove {
  protected static final Field doubleArrayListAccessor = buildDoubleArrayListAccessor();

  protected G4Trove() {
  }

  protected static Field buildDoubleArrayListAccessor() {
    try {
      final Field field = TDoubleArrayList.class.getDeclaredField("_data");
      field.setAccessible(true);
      return field;
    } catch (NoSuchFieldException e) {
      throw new Error(e);
    }
  }

  public static double[] elements(TDoubleArrayList data) {
    try {
      return (double[]) doubleArrayListAccessor.get(data);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  //	from and into must be sorted for this to work properly
  public static void union(final TIntArrayList from, final TIntArrayList el, final TIntArrayList dest) {
    dest.clear();
    if (from.isEmpty()) {
      return;
    }
    if (el.isEmpty()) {
      dest.add(from.toNativeArray());
      return;
    }

    int fI = 0;
    int eI = 0;
    int fV = from.get(fI);
    int eV = el.get(eI);
    boolean fCache = true;
    boolean eCache = true;

    while (fI < from.size() && eI < el.size()) {
      if (!fCache) {
        fV = from.get(fI);
        fCache = true;
      }
      if (!eCache) {
        eV = el.get(eI);
        eCache = true;
      }

      if (fV == eV) {
        dest.add(fV);
        fI++;
        eI++;
        fCache = eCache = false;
        continue;
      }

      if (fV < eV) {
        fI++;
        fCache = false;
        continue;
      }

      eI++;
      eCache = false;
    }
  }

  //	from and into must be sorted for this to work properly
  public static void remove(final TIntArrayList from, final TIntArrayList el, final TIntArrayList dest) {
    dest.clear();
    if (from.isEmpty()) {
      return;
    }
    if (el.isEmpty()) {
      dest.add(from.toNativeArray());
      return;
    }

    int fI = 0;
    int eI = 0;
    int fV = from.get(fI);
    int eV = el.get(eI);
    boolean fCache = true;
    boolean eCache = true;

    while (fI < from.size() && eI < el.size()) {
      if (!fCache) {
        fV = from.get(fI);
        fCache = true;
      }
      if (!eCache) {
        eV = el.get(eI);
        eCache = true;
      }

      if (fV == eV) {
        fI++;
        eI++;
        fCache = eCache = false;
        continue;
      }

      if (fV < eV) {
        dest.add(fV);
        fI++;
        fCache = false;
        continue;
      }

      eI++;
      eCache = false;
    }
  }

  public static void listAll(TIntArrayList all, int size) {
    for (int i = 0; i < size; i++) {
      all.add(i);
    }
  }

  public static byte[] doublesToBinary(final TDoubleArrayList doubles) {
    try {
      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      final ObjectOutputStream out = new ObjectOutputStream(baos);
      doubles.writeExternal(out);
      out.flush();
      return baos.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException("should not happen", e);
    }
  }

  public static void binaryToDoubles(final byte[] binary, TDoubleArrayList doubles) {
    try {
      final ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(binary));
      doubles.readExternal(in);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("should not happen", e);
    } catch (IOException e) {
      throw new RuntimeException("should not happen", e);
    }
  }

  public static byte[] intsToBinary(final TIntArrayList ints) {
    try {
      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      final ObjectOutputStream out = new ObjectOutputStream(baos);
      ints.writeExternal(out);
      out.flush();
      return baos.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException("should not happen", e);
    }
  }

  public static void binaryToInts(final byte[] binary, TIntArrayList ints) {
    try {
      final ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(binary));
      ints.readExternal(in);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("should not happen", e);
    } catch (IOException e) {
      throw new RuntimeException("should not happen", e);
    }
  }

  public static byte[] intsListToBinary(final List<TIntArrayList> intsList) {
    try {
      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      final ObjectOutputStream out = new ObjectOutputStream(baos);
      out.writeInt(intsList.size());
      for (TIntArrayList ints : intsList) {
        ints.writeExternal(out);
      }
      out.flush();
      return baos.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException("should not happen", e);
    }
  }

  public static void binaryToIntsList(final byte[] binary, final List<TIntArrayList> intsList) {
    try {
      final ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(binary));

      int intsListSize = in.readInt();

      if (intsList.size() > intsListSize) {
        intsList.subList(intsListSize, intsList.size()).clear();
      }

      while (intsList.size() < intsListSize) {
        intsList.add(new TIntArrayList());
      }

      for (int i = 0; i < intsListSize; i++) {
        TIntArrayList ints = intsList.get(i);

        if (ints == null) {
          ints = new TIntArrayList();
          intsList.set(i, ints);
        }

        ints.readExternal(in);
      }
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("should not happen", e);
    } catch (IOException e) {
      throw new RuntimeException("should not happen", e);
    }
  }

  public static byte[] doublesListToBinary(final List<TDoubleArrayList> doublesList) {
    try {
      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      final ObjectOutputStream out = new ObjectOutputStream(baos);
      out.writeInt(doublesList.size());
      for (TDoubleArrayList doubles : doublesList) {
        doubles.writeExternal(out);
      }
      out.flush();
      return baos.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException("should not happen", e);
    }
  }

  public static void binaryToDoublesList(final byte[] binary, final List<TDoubleArrayList> doublesList) {
    try {
      final ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(binary));

      int doublesListSize = in.readInt();

      if (doublesList.size() > doublesListSize) {
        doublesList.subList(doublesListSize, doublesList.size()).clear();
      }

      while (doublesList.size() < doublesListSize) {
        doublesList.add(new TDoubleArrayList());
      }

      for (int i = 0; i < doublesListSize; i++) {
        TDoubleArrayList doubles = doublesList.get(i);

        if (doubles == null) {
          doubles = new TDoubleArrayList();
          doublesList.set(i, doubles);
        }

        doubles.readExternal(in);
      }
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("should not happen", e);
    } catch (IOException e) {
      throw new RuntimeException("should not happen", e);
    }
  }
}
