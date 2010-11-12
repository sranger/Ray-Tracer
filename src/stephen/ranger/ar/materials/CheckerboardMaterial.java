package stephen.ranger.ar.materials;

import java.awt.Color;

import stephen.ranger.ar.Camera;
import stephen.ranger.ar.IntersectionInformation;

public class CheckerboardMaterial extends ColorInformation {
   private final Color color1;
   private final Color color2;
   private final float cellWidth, cellHeight, cellDepth;

   public CheckerboardMaterial(final Color c1, final Color c2, final float cellWidth, final float cellHeight, final float cellDepth) {
      this.color1 = c1;
      this.color2 = c2;
      this.cellWidth = cellWidth;
      this.cellHeight = cellHeight;
      this.cellDepth = cellDepth;
   }

   @Override
   public float[] getMaterialColor(final Camera camera, final IntersectionInformation info, final int depth) {
      final float[] returnColor = new float[3];
      final float[][] minMax = info.intersectionObject.getMinMax();

      final float xSpan = minMax[1][0] - minMax[0][0];
      final float ySpan = minMax[1][1] - minMax[0][1];
      final float zSpan = minMax[1][2] - minMax[0][2];

      final float xDist = info.intersection.x - minMax[0][0];
      final float yDist = info.intersection.y - minMax[0][1];
      final float zDist = info.intersection.z - minMax[0][2];

      if ((xDist < 0) || (yDist < 0) || (zDist < 0) || (xDist > xSpan) || (yDist > ySpan) || (zDist > zSpan)) {
         return new float[3];
      }

      final int xCell = (int) Math.floor(xDist / this.cellWidth);
      final int yCell = (int) Math.floor(yDist / this.cellHeight);
      final int zCell = (int) Math.floor(zDist / this.cellDepth);

      if (((xCell % 2 == 0) && (yCell % 2 == 0) && (zCell % 2 == 0)) || ((xCell % 2 == 1) && (yCell % 2 == 0) && (zCell % 2 == 1)) || ((xCell % 2 == 0) && (yCell % 2 == 1) && (zCell % 2 == 1))) {
         this.color1.getColorComponents(returnColor);
      } else {
         this.color2.getColorComponents(returnColor);
      }

      return returnColor;
   }
}
