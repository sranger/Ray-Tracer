package stephen.ranger.ar;

import javax.vecmath.Vector3f;

import stephen.ranger.ar.bounds.BoundingVolume;

public class IntersectionInformation {
   public final Vector3f intersection;
   public final Vector3f normal;
   public final float w;
   public final BoundingVolume intersectionObject;
   public final Ray ray;

   public IntersectionInformation(final Ray ray, final BoundingVolume intersectionObject, final Vector3f intersection, final Vector3f normal, final float w) {
      this.ray = ray;
      this.intersectionObject = intersectionObject;
      this.intersection = intersection;
      this.normal = normal;
      this.w = w;
   }
}
