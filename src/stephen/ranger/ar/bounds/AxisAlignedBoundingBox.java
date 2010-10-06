package stephen.ranger.ar.bounds;

import java.awt.Color;

import javax.vecmath.Vector3d;

import stephen.ranger.ar.ColorInformation;
import stephen.ranger.ar.IntersectionInformation;
import stephen.ranger.ar.RTStatics;
import stephen.ranger.ar.Ray;
import stephen.ranger.ar.sceneObjects.SceneObject;

public class AxisAlignedBoundingBox extends BoundingVolume {
   public final SceneObject child;
   public final Vector3d[] minMax;
   
   public AxisAlignedBoundingBox(final SceneObject child, final double minX, final double minY, final double minZ, final double maxX, final double maxY,
         final double maxZ) {
      this.child = child;
      minMax = new Vector3d[] { new Vector3d(minX, minY, minZ), new Vector3d(maxX, maxY, maxZ) };
   }
   
   @Override
   public IntersectionInformation getChildIntersection(final Ray ray) {
      return intersects(ray) ? child.getIntersection(ray) : null;
   }
   
   @Override
   public boolean intersects(final Ray r) {
      return RTStatics.aabbIntersection(r, getMinMax());
   }
   
   @Override
   public Vector3d[] getMinMax() {
      return new Vector3d[] { new Vector3d(minMax[0]), new Vector3d(minMax[1]) };
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
