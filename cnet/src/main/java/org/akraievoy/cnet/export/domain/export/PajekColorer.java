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

package org.akraievoy.cnet.export.domain.export;

public class PajekColorer {
  protected String[] colorNames;

  protected double min = 0.0;
  protected double max = 1.0;
  protected double thresh = 1e-9;

  public PajekColorer() {
    this("bw");
  }

  public PajekColorer(final String palette) {
    if ("bw".equalsIgnoreCase(palette)) {
      colorNames = new String[]{
          "Black",
          "Gray95", "Gray90", "Gray85", "Gray80", "Gray75", "Gray70",
          "Gray65", "Gray60", "Gray55", "Gray50", "Gray45", "Gray40",
          "Gray35", "Gray30", "Gray25", "Gray20", "Gray15", "Gray10",
          "Gray05", "White"
      };
    } else {
      colorNames = new String[]{
          "BlueViolet",
          "RoyalPurple",
          "Violet",
          "Plum",
          "Purple",
          "DarkOrchid",
          "Orchid",
          "Thistle",
          "Lavender"
      };
    }
  }

  public void setMax(double max) {
    this.max = max;
  }

  public void setMin(double min) {
    this.min = min;
  }

  public void setColorNames(String[] colorNames) {
    this.colorNames = colorNames;
  }

  public String getName(final double intensity) {
    if (colorNames == null || colorNames.length == 0) {
      return "BrickRed";
    }

    if (intensity < min - thresh) {
      return "Tan";
    }

    if (intensity > max + thresh) {
      return "RedViolet";
    }

    if (intensity <= min) {
      return colorNames[0];
    }

    if (intensity >= max) {
      return colorNames[colorNames.length - 1];
    }

    final double step = (max - min) / colorNames.length;
    final int index = (int) Math.floor((intensity - min) / step);

    return colorNames[index];
  }
}
