package stephen.ranger.ar.sceneObjects;

import javax.vecmath.Vector3f;

import stephen.ranger.ar.ColorInformation;
import stephen.ranger.ar.IntersectionInformation;
import stephen.ranger.ar.RTStatics;
import stephen.ranger.ar.Ray;
import stephen.ranger.ar.bounds.BoundingSphere;

public class Sphere extends SceneObject {
   public final float radius;
   public final Vector3f origin;

   public Sphere(final float radius, final Vector3f origin, final ColorInformation colorInfo) {
      super(colorInfo);

      this.radius = radius;
      this.origin = origin;

      setBoundingVolume(new BoundingSphere(this, origin, radius));
   }

   public Sphere() {
      this(1.0f, new Vector3f(), new ColorInformation());
   }

   @Override
   public IntersectionInformation getIntersection(final Ray ray) {
      final Vector3f origin = new Vector3f(ray.origin);
      final Vector3f direction = new Vector3f(ray.direction);
      final Vector3f center = new Vector3f(this.origin);
      IntersectionInformation retVal = null;

      final float a = (float) (Math.pow(direction.x, 2) + Math.pow(direction.y, 2) + Math.pow(direction.z, 2));
      final float b = 2 * (direction.x * (origin.x - center.x) + direction.y * (origin.y - center.y) + direction.z * (origin.z - center.z));
      final float c = (float) (Math.pow((origin.x - center.x), 2) + Math.pow((origin.y - center.y), 2) + Math.pow((origin.z - center.z), 2) - Math.pow(radius, 2));

      final float b24c = (float) (Math.pow(b, 2) - 4 * c);
      final float wplus = (float) ((-b + Math.sqrt(b24c)) / (2 * a));
      final float wminus = (float) ((-b - Math.sqrt(b24c)) / (2 * a));
      float w = -1;
      // boolean myW = false;

      if (b24c < 0) {
         w = -1;
         // myW = false; // no real root
      } else {
         w = RTStatics.leastPositive(wplus, wminus);
         // myW = true;
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

            if (nDist < RTStatics.EPSILON && wminus > 0) {
               w = wminus;
            } else if (nDist < RTStatics.EPSILON && wminus < 0) {
               w = -1;
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

            if (mDist < 0.01 && wplus > 0) {
               w = wplus;
            } else if (mDist < 0.01 && wplus < 0) {
               w = -1;
            }
         }
      }

      if (w > 0) {
         final Vector3f intersection = new Vector3f(origin.x + direction.x * w, origin.y + direction.y * w, origin.z + direction.z * w);
         final Vector3f normal = new Vector3f(intersection);
         normal.sub(center);
         normal.normalize();
         retVal = new IntersectionInformation(ray, boundingVolume, intersection, normal, w);
      }

      return retVal;
   }
}
