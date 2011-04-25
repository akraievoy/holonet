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

package org.akraievoy.db;

import java.io.IOException;
import java.io.Reader;
import java.sql.SQLException;

/**
 * Brought in from H2 db, org.h2.util.ScriptReader.
 */
public class ScriptReader {
  private Reader reader;
  private boolean end;
  private boolean insideRemark;
  private boolean blockRemark;
  private boolean skipRemarks;

  public ScriptReader(Reader reader) {
    this.reader = reader;
  }

  private int read() throws SQLException {
    try {
      return reader.read();
    } catch (IOException e) {
      throw new SQLException(e.getMessage());
    }
  }

  public String readStatement() throws SQLException {
    if (end) {
      return null;
    }

    StringBuffer buff = new StringBuffer();
    int c = read();
    while (true) {
      if (c < 0) {
        end = true;
        return buff.length() == 0 ? null : buff.toString();
      } else if (c == ';') {
        break;
      }
      switch (c) {
        case '\'':
          buff.append((char) c);
          while (true) {
            c = read();
            if (c < 0) {
              break;
            }
            buff.append((char) c);
            if (c == '\'') {
              break;
            }
          }
          c = read();
          break;
        case '"':
          buff.append((char) c);
          while (true) {
            c = read();
            if (c < 0) {
              break;
            }
            buff.append((char) c);
            if (c == '\"') {
              break;
            }
          }
          c = read();
          break;
        case '/': {
          int last = c;
          c = read();
          if (c == '*') {
            // block comment
            insideRemark = true;
            blockRemark = true;
            if (!skipRemarks) {
              buff.append((char) last);
              buff.append((char) c);
            }
            while (true) {
              c = read();
              if (c < 0) {
                break;
              }
              if (!skipRemarks) {
                buff.append((char) c);
              }
              if (c == '*') {
                c = read();
                if (c < 0) {
                  break;
                }
                if (!skipRemarks) {
                  buff.append((char) c);
                }
                if (c == '/') {
                  insideRemark = false;
                  break;
                }
              }
            }
            c = read();
          } else if (c == '/') {
            // single line comment
            insideRemark = true;
            blockRemark = false;
            if (!skipRemarks) {
              buff.append((char) last);
              buff.append((char) c);
            }
            while (true) {
              c = read();
              if (c < 0) {
                break;
              }
              if (!skipRemarks) {
                buff.append((char) c);
              }
              if (c == '\r' || c == '\n') {
                insideRemark = false;
                break;
              }
            }
            c = read();
          } else {
            buff.append((char) last);
          }
          break;
        }
        case '-': {
          int last = c;
          c = read();
          if (c == '-') {
            // single line comment
            insideRemark = true;
            blockRemark = false;
            if (!skipRemarks) {
              buff.append((char) last);
              buff.append((char) c);
            }
            while (true) {
              c = read();
              if (c < 0) {
                break;
              }
              if (!skipRemarks) {
                buff.append((char) c);
              }
              if (c == '\r' || c == '\n') {
                insideRemark = false;
                break;
              }
            }
            c = read();
          } else {
            buff.append((char) last);
          }
          break;
        }
        default:
          buff.append((char) c);
          c = read();
      }
    }
    return buff.toString();
  }

  public boolean isInsideRemark() {
    return insideRemark;
  }

  public boolean isBlockRemark() {
    return blockRemark;
  }

  public void setSkipRemarks(boolean skipRemarks) {
    this.skipRemarks = skipRemarks;
  }
}