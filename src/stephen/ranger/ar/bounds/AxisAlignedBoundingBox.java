package stephen.ranger.ar.bounds;

import stephen.ranger.ar.Camera;
import stephen.ranger.ar.IntersectionInformation;
import stephen.ranger.ar.RTStatics;
import stephen.ranger.ar.Ray;
import stephen.ranger.ar.sceneObjects.SceneObject;

public class AxisAlignedBoundingBox extends BoundingVolume {
   public final SceneObject child;
   public final float[][] minMax;

   public AxisAlignedBoundingBox(final SceneObject child, final float minX, final float minY, final float minZ, final float maxX, final float maxY, final float maxZ) {
      this.child = child;
      this.minMax = new float[2][];
      this.minMax[0] = new float[] { minX, minY, minZ };
      this.minMax[1] = new float[] { maxX, maxY, maxZ };
   }

   public AxisAlignedBoundingBox(final SceneObject child, final float[][] minMax) {
      this.child = child;
      this.minMax = minMax;
   }

   @Override
   public IntersectionInformation getChildIntersection(final Ray ray, final int depth) {
      return this.child.getIntersection(ray, depth);
   }

   @Override
   public boolean intersects(final Ray r) {
      return RTStatics.aabbIntersection(r, this.getMinMax());
   }

   @Override
   public float[][] getMinMax() {
      return this.minMax;
   }

   @Override
   public float[] getColor(final IntersectionInformation info, final Camera camera, final int depth) {
      return this.child.getColor(info, camera, depth);
   }

   public boolean intersects(final AxisAlignedBoundingBox box) {
      return RTStatics.aabbIntersection(this.minMax, box.minMax);
   }

   @Override
   public float[] getEmission() {
      return this.child.getEmission();
   }

   @Override
   public float[] getDiffuse() {
      return this.child.getDiffuse();
   }

   @Override
   public float[] getSpecular() {
      return this.child.getSpecular();
   }

   @Override
   public float[] getAmbient() {
      return this.child.getAmbient();
   }

   @Override
   public float getShininess() {
      return this.child.getShininess();
   }
}
