package stephen.ranger.ar.bounds;

import javax.vecmath.Vector3f;

import stephen.ranger.ar.Camera;
import stephen.ranger.ar.IntersectionInformation;
import stephen.ranger.ar.RTStatics;
import stephen.ranger.ar.Ray;
import stephen.ranger.ar.sceneObjects.SceneObject;

public class BoundingSphere extends BoundingVolume {
   public final SceneObject child;
   public final Vector3f origin;
   public final float radius;

   public BoundingSphere(final SceneObject child, final Vector3f origin, final float radius) {
      this.child = child;

      this.origin = origin;
      this.radius = radius;
   }

   @Override
   public IntersectionInformation getChildIntersection(final Ray ray, final int depth) {
      return this.intersects(ray) ? this.child.getIntersection(ray, depth) : null;
   }

   @Override
   public boolean intersects(final Ray ray) {
      final Vector3f origin = new Vector3f(ray.origin);
      final Vector3f direction = new Vector3f(ray.direction);
      final Vector3f center = new Vector3f(this.origin);

      final float a = (float) (Math.pow(direction.x, 2) + Math.pow(direction.y, 2) + Math.pow(direction.z, 2));
      final float b = 2 * (direction.x * (origin.x - center.x) + direction.y * (origin.y - center.y) + direction.z * (origin.z - center.z));
      final float c = (float) (Math.pow((origin.x - center.x), 2) + Math.pow((origin.y - center.y), 2) + Math.pow((origin.z - center.z), 2) - Math.pow(this.radius, 2));

      final float b24c = (float) Math.pow(b, 2) - 4 * c;
      final float wplus = (float) (-b + Math.sqrt(b24c)) / (2 * a);
      final float wminus = (float) (-b - Math.sqrt(b24c)) / (2 * a);
      float w = -1;
      // boolean myW = false;

      if (b24c < 0) {
         w = -1;
         return false;
      }

      else {
         w = RTStatics.leastPositive(wplus, wminus);

         if (w <= 0) {
            return false;
         }
      }

      if (w > 0) {
         float xn, yn, zn, xm, ym, zm, xd, yd, zd;

         if (w == wplus) {
            xn = origin.x + direction.x * wplus;
            yn = origin.y + direction.y * wplus;
            zn = origin.z + direction.z * wplus;

            xd = origin.x - xn;
            yd = origin.y - yn;
            zd = origin.z - zn;
            final float nDist = (float) Math.sqrt(xd * xd + yd * yd + zd * zd);

            if ((nDist < RTStatics.EPSILON) && (wminus > 0)) {
               w = wminus;
            } else if ((nDist < RTStatics.EPSILON) && (wminus < 0)) {
               w = -1;
               return false;
            }
         }

         else if (w == wminus) {
            xm = origin.x + direction.x * wminus;
            ym = origin.y + direction.y * wminus;
            zm = origin.z + direction.z * wminus;

            xd = origin.x - xm;
            yd = origin.y - ym;
            zd = origin.z - zm;
            final float mDist = (float) Math.sqrt(xd * xd + yd * yd + zd * zd);

            if ((mDist < 0.01) && (wplus > 0)) {
               w = wplus;
            } else if ((mDist < 0.01) && (wplus < 0)) {
               w = -1;
               return false;
            }
         }
      }

      if (w > 0) {
         return true;
      } else {
         return false;
      }
   }

   @Override
   public float[][] getMinMax() {
      final float[][] retVal = new float[2][];
      retVal[0] = new float[] { this.origin.x - this.radius, this.origin.y - this.radius, this.origin.z - this.radius };
      retVal[1] = new float[] { this.origin.x + this.radius, this.origin.y + this.radius, this.origin.z + this.radius };

      return retVal;
   }

   @Override
   public float[] getColor(final IntersectionInformation info, final Camera camera, final int depth) {
      return this.child.getColor(info, camera, depth);
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
