package stephen.ranger.ar.bounds;

import java.awt.Color;

import stephen.ranger.ar.ColorInformation;
import stephen.ranger.ar.IntersectionInformation;
import stephen.ranger.ar.RTStatics;
import stephen.ranger.ar.Ray;
import stephen.ranger.ar.sceneObjects.SceneObject;

public class AxisAlignedBoundingBox extends BoundingVolume {
   public final SceneObject child;
   public final float[][] minMax;

   public AxisAlignedBoundingBox(final SceneObject child, final float minX, final float minY, final float minZ, final float maxX, final float maxY, final float maxZ) {
      this.child = child;
      minMax = new float[2][];
      minMax[0] = new float[] { minX, minY, minZ };
      minMax[1] = new float[] { maxX, maxY, maxZ };
   }

   @Override
   public IntersectionInformation getChildIntersection(final Ray ray) {
      return child.getIntersection(ray);
   }

   @Override
   public boolean intersects(final Ray r) {
      return RTStatics.aabbIntersection(r, getMinMax());
   }

   @Override
   public float[][] getMinMax() {
      return minMax;
   }

   @Override
   public Color getColor(final IntersectionInformation info) {
      return child.getColor(info);
   }

   @Override
   public ColorInformation getColorInformation(final IntersectionInformation info) {
      return child.colorInfo;
   }
}
