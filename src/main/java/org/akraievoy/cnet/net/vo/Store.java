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

package org.akraievoy.cnet.net.vo;

import org.akraievoy.holonet.exp.store.Streamable;

import java.io.IOException;
import java.io.InputStream;

@SuppressWarnings("UnusedDeclaration")
public interface Store extends Streamable {
  enum Width {
    BIT{
      @Override StoreBit create() { return new StoreBit(); }
    },
    BYTE {
      @Override StoreByte create() { return new StoreByte(); }
    },
    INT {
      @Override StoreInt create() { return new StoreInt(); }
    },
    LONG {
      @Override StoreLong create() { return new StoreLong(); }
    },
    FLOAT {
      @Override StoreFloat create() { return new StoreFloat(); }
    },
    DOUBLE  {
      @Override StoreDouble create() { return new StoreDouble(); }
    };

    abstract Store create();
  }

  Width width();
  int size();
  void ins(int fromIncl, int uptoExcl, boolean def);
  void ins(int fromIncl, int uptoExcl, byte def);
  void ins(int fromIncl, int uptoExcl, int def);
  void ins(int fromIncl, int uptoExcl, long def);
  void ins(int fromIncl, int uptoExcl, float def);
  void ins(int fromIncl, int uptoExcl, double def);
  void fill(int fromIncl, int uptoExcl, boolean def);
  void fill(int fromIncl, int uptoExcl, byte def);
  void fill(int fromIncl, int uptoExcl, int def);
  void fill(int fromIncl, int uptoExcl, long def);
  void fill(int fromIncl, int uptoExcl, float def);
  void fill(int fromIncl, int uptoExcl, double def);
  int bSearch(int fromIncl, int uptoExcl, byte search);
  int bSearch(int fromIncl, int uptoExcl, int search);
  int bSearch(int fromIncl, int uptoExcl, long search);
  int bSearch(int fromIncl, int uptoExcl, float search);
  int bSearch(int fromIncl, int uptoExcl, double search);
  void rotUp(int fromIncl, int uptoExcl);
  void rotDown(int fromIncl, int uptoExcl);
  void del(int fromIncl, int uptoExcl);

  double get(int pos, double typeHint);
  double set(int pos, double val);
  float get(int pos, float typeHint);
  float set(int pos, float val);
  long get(int pos, long typeHint);
  long set(int pos, long val);
  int get(int pos, int typeHint);
  int set(int pos, int val);
  byte get(int pos, byte typeHint);
  byte set(int pos, byte val);
  boolean get(int pos, boolean typeHint);
  boolean set(int pos, boolean val);

  Store fromStream(InputStream in) throws IOException;
}
