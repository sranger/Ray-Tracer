package stephen.ranger.ar;

import javax.vecmath.Vector3d;

public class RTStatics {

   public static final double EPSILON = 1e-15;

   public static final double nearPlane = 0.01;
   public static final double farPlane = 30.0;

   public static final int MAX_CHILDREN = 5;

   public static boolean ENABLE_BACKFACE_CULLING = true;

   private RTStatics() {
      // only static stuff here
   }

   public static Vector3d getReflectionDirection(final IntersectionInformation info, final Vector3d p1) {
      final Vector3d L = new Vector3d();
      L.sub(p1, info.intersection);
      L.normalize();

      final Vector3d R = new Vector3d(info.normal);
      R.scale(-2.0);
      R.scale(L.dot(info.normal));
      R.add(R, L);
      R.normalize();

      return R;
   }

   public static double leastPositive(final double i, final double j) {
      double retVal;

      if ((i < 0) && (j < 0)) {
         retVal = -1;
      } else if ((i < 0) && (j > 0)) {
         retVal = j;
      } else if ((i > 0) && (j < 0)) {
         retVal = i;
      } else {
         if (i < j) {
            retVal = i;
         } else {
            retVal = j;
         }
      }

      return retVal;
   }

   public static boolean aabbIntersection(final Ray r, final Vector3d[] minMax) {
      // http://people.csail.mit.edu/amy/papers/box-jgt.pdf
      double txmin, txmax, tymin, tymax, tzmin, tzmax;
      final double divx = 1.0 / r.direction.x;
      final double divy = 1.0 / r.direction.y;
      final double divz = 1.0 / r.direction.z;

      if (divx >= 0) {
         txmin = (minMax[0].x - r.origin.x) * divx;
         txmax = (minMax[1].x - r.origin.x) * divx;
      } else {
         txmin = (minMax[1].x - r.origin.x) * divx;
         txmax = (minMax[0].x - r.origin.x) * divx;
      }

      if (divy >= 0) {
         tymin = (minMax[0].y - r.origin.y) * divy;
         tymax = (minMax[1].y - r.origin.y) * divy;
      } else {
         tymin = (minMax[1].y - r.origin.y) * divy;
         tymax = (minMax[0].y - r.origin.y) * divy;
      }

      if ((txmin > tymax) || (tymin > txmax)) {
         return false;
      }

      if (tymin > txmin) {
         txmin = tymin;
      }

      if (tymax < txmax) {
         txmax = tymax;
      }

      if (divz >= 0) {
         tzmin = (minMax[0].z - r.origin.z) * divz;
         tzmax = (minMax[1].z - r.origin.z) * divz;
      } else {
         tzmin = (minMax[1].z - r.origin.z) * divz;
         tzmax = (minMax[0].z - r.origin.z) * divz;
      }

      if ((txmin > tzmax) || (tzmin > txmax)) {
         return false;
      }

      if (tzmin > txmin) {
         txmin = tzmin;
      }

      if (tzmax < txmax) {
         txmax = tzmax;
      }

      return (txmin < RTStatics.farPlane) && (txmax > RTStatics.nearPlane);
   }

   /**
    * Returns the distance between the two given vertices.
    * 
    * @param p1    Vertex 1
    * @param p2    Vertex 2
    * @return      The distance between p1 and p2
    */
   public static double getDistance(final Vector3d p1, final Vector3d p2) {
      return Math.sqrt((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y) + (p1.z - p2.z) * (p1.z - p2.z));
   }
}
