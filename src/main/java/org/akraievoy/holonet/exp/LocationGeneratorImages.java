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

package org.akraievoy.holonet.exp;

import com.google.common.base.Throwables;
import org.akraievoy.cnet.gen.domain.LocationGeneratorFractalDLA;
import org.akraievoy.cnet.gen.domain.LocationGeneratorRecursive;
import org.akraievoy.cnet.gen.vo.LocationGenerator;
import org.akraievoy.cnet.gen.vo.Point;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class LocationGeneratorImages implements Runnable {
  protected static final String FORMAT = "PNG";
  protected static final String EXT = "." + FORMAT.toLowerCase();

  protected final LocationGenerator gen;
  protected String imageDestPath = System.getProperty("user.dir") + File.separator + "locGen" + EXT;

  public LocationGeneratorImages(LocationGenerator gen) {
    this.gen = gen;
  }

  public void run() {
    final int gridSize = gen.getGridSize();
    final BufferedImage bufferedImage = new BufferedImage(gridSize, gridSize, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g2d = bufferedImage.createGraphics();

    double max = 0;
    for (int y = 0; y < gridSize; y++) {
      for (int x = 0; x < gridSize; x++) {
        final Point location = new Point(x / (double) gridSize, y / (double) gridSize);
        final double density = gen.getDensity(location);
        max = Math.max(max, density);
      }
    }

    for (int y = 0; y < gridSize; y++) {
      for (int x = 0; x < gridSize; x++) {
        final Point location = new Point(x / (double) gridSize, y / (double) gridSize);
        final double normed = gen.getDensity(location) / max;
        int reds = 0;
        int greens = (int) Math.round(255 * Math.min(1, normed));
        int blues = (int) Math.round(Math.max(64 + Math.log(normed), 0));

        g2d.setColor(new Color(reds, greens, blues));
        g2d.fillRect(x, y, 1, 1);
      }
    }

    String actualDestPath;
    final String baseDestPath = imageDestPath.substring(0, imageDestPath.length() - EXT.length());
    if (gen instanceof LocationGeneratorFractalDLA) {
      final LocationGeneratorFractalDLA genDLA = (LocationGeneratorFractalDLA) gen;

      actualDestPath = baseDestPath +
          "G" + pad(genDLA.getGridSize(), 4) +
          "D" + genDLA.getDimensionRatio() +
          "g" + pad(genDLA.getGenerationNum(), 2) +
          "s" + pad(genDLA.getSeedPointNum(), 2) +
          "d" + genDLA.getDensityRatio() +
          "p" + genDLA.getGenPointsRatio() +
          "t" + pad(genDLA.getTryNum(), 3) + EXT;
    } else if (gen instanceof LocationGeneratorRecursive) {
      final LocationGeneratorRecursive genRecursive = (LocationGeneratorRecursive) gen;
      actualDestPath = baseDestPath + genRecursive.getGridSize() + "_Df_" + genRecursive.getDimensionRatio() + EXT;
    } else {
      actualDestPath = baseDestPath + gen.getGridSize() + EXT;
    }

    try {
      ImageIO.write(bufferedImage, "PNG", new File(actualDestPath));
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  public String pad(int num, final int width) {
    StringBuilder res = new StringBuilder();
    res.append(num);
    while (res.length() < width) {
      res.insert(0, "0");
    }
    return res.toString();
  }
}
