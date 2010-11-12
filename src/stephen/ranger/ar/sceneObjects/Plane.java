package stephen.ranger.ar.sceneObjects;

import java.awt.Color;
import java.security.InvalidParameterException;
import java.util.Arrays;

import javax.vecmath.Vector3f;

import stephen.ranger.ar.IntersectionInformation;
import stephen.ranger.ar.RTStatics;
import stephen.ranger.ar.Ray;
import stephen.ranger.ar.bounds.AxisAlignedBoundingBox;
import stephen.ranger.ar.materials.CheckerboardMaterial;
import stephen.ranger.ar.materials.ColorInformation;

/**
 * Plane equation info: http://local.wasp.uwa.edu.au/~pbourke/geometry/planeeq/
 */
public class Plane extends SceneObject {
   // public final Vector3f[] corners;
   private final float A, B, C, D;
   private final Vector3f normal;

   private final float minX, maxX, minY, maxY, minZ, maxZ;

   private final Vector3f[] corners;

   public Plane(Vector3f[] corners, final ColorInformation colorInfo) {
      super(colorInfo);

      this.corners = corners;

      if (corners.length < 3) {
         throw new InvalidParameterException("The Vector3f array of corner values must contain three or more vertices that denote the bounds of the given plane.");
      }

      // A = y0 (z1 - z2) + y1 (z2 - z0) + y2 (z0 - z1)
      this.A = corners[0].y * (corners[1].z - corners[2].z) + corners[1].y * (corners[2].z - corners[0].z) + corners[2].y * (corners[0].z - corners[1].z);

      // B = z0 (x1 - x2) + z1 (x2 - x0) + z2 (x0 - x1)
      this.B = corners[0].z * (corners[1].x - corners[2].x) + corners[1].z * (corners[2].x - corners[0].x) + corners[2].z * (corners[0].x - corners[1].x);

      // C = x0 (y1 - y2) + x1 (y2 - y0) + x2 (y0 - y1)
      this.C = corners[0].x * (corners[1].y - corners[2].y) + corners[1].x * (corners[2].y - corners[0].y) + corners[2].x * (corners[0].y - corners[1].y);

      // D = - (x0 (y1 z2 - y2 z1) +
      this.D = -(corners[0].x * (corners[1].y * corners[2].z - corners[2].y * corners[1].z) +
            // x1 (y2 z0 - y0 z2) +
            corners[1].x * (corners[2].y * corners[0].z - corners[0].y * corners[2].z) +
            // x2 (y0 z1 - y1 z0))
            corners[2].x * (corners[0].y * corners[1].z - corners[1].y * corners[0].z));

      this.normal = new Vector3f(this.A, this.B, this.C);
      this.normal.normalize();

      if ((this.A == 0) && (this.B == 0) && (this.C == 0)) {
         throw new InvalidParameterException("The first three vertices given for this object are colinear.");
      }

      if (corners.length == 3) {
         corners = Arrays.copyOf(corners, 4);
         corners[3] = new Vector3f(corners[2]);
      }

      this.minX = Math.min(corners[0].x, Math.min(corners[1].x, Math.min(corners[2].x, corners[3].x)));
      this.maxX = Math.max(corners[0].x, Math.max(corners[1].x, Math.max(corners[2].x, corners[3].x)));
      this.minY = Math.min(corners[0].y, Math.min(corners[1].y, Math.min(corners[2].y, corners[3].y)));
      this.maxY = Math.max(corners[0].y, Math.max(corners[1].y, Math.max(corners[2].y, corners[3].y)));
      this.minZ = Math.min(corners[0].z, Math.min(corners[1].z, Math.min(corners[2].z, corners[3].z)));
      this.maxZ = Math.max(corners[0].z, Math.max(corners[1].z, Math.max(corners[2].z, corners[3].z)));

      // System.out.println("\nplane bounds: x = (" + minX + ", " + maxX + ")  y = (" + minY + ", " + maxY + ")  z = (" + minZ + ", " + maxZ + ")");
      // System.out.println("plane normal: " + normal);

      this.setBoundingVolume(new AxisAlignedBoundingBox(this, this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ));
   }

   public Plane() {
      this(new Vector3f[] { new Vector3f(-1, 0, -1), new Vector3f(-1, 0, 1), new Vector3f(1, 0, -1), new Vector3f(1, 0, 1) }, new CheckerboardMaterial(Color.yellow, Color.blue, 1f, 1f, 1f));
   }

   /**
    * http://www.siggraph.org/education/materials/HyperGraph/raytrace/
    * rayplane_intersection.htm
    */
   @Override
   public IntersectionInformation getIntersection(final Ray ray, final int depth) {
      final Vector3f origin = new Vector3f(ray.origin);
      final Vector3f dir = new Vector3f(ray.direction);
      dir.scale(RTStatics.EPSILON * 2);
      origin.add(dir);
      dir.set(ray.direction);
      final float offsetDistance = 0;// RTStatics.getDistance(ray.origin, origin);

      final Vector3f planeNormal = new Vector3f(this.normal);
      float vD = planeNormal.dot(dir);

      if (vD > RTStatics.EPSILON) {
         planeNormal.scale(-1);
         vD = planeNormal.dot(dir);
      }

      if ((vD <= -RTStatics.EPSILON) || (vD >= RTStatics.EPSILON)) {
         // V0 = -(Pn . R0 + D) and compute t = V0 / Vd. If t < 0 then the
         // ray intersects plane behind origin, i.e. no intersection of
         // interest
         // final float v0 = -(this.normal.dot(origin) + D);
         // final float t = v0 / vD;

         // t = -(AX0 + BY0 + CZ0 + D) / (AXd + BYd + CZd)
         final float t = -(this.A * origin.x + this.B * origin.y + this.C * origin.z + this.D) / (this.A * dir.x + this.B * dir.y + this.C * dir.z);

         if (t > -RTStatics.EPSILON) {
            final Vector3f rD = new Vector3f(dir);

            if (vD > -RTStatics.EPSILON) {
               rD.x = -rD.x;
               rD.y = -rD.y;
               rD.z = -rD.z;
            }

            // Pi = [Xi Yi Zi] = [X0 + Xd * t Y0 + Yd * t Z0 + Zd * t]
            final Vector3f pI = new Vector3f(origin.x + rD.x * t, origin.y + rD.y * t, origin.z + rD.z * t);

            if (this.pointInPolygon(pI)) {
               final Vector3f temp = new Vector3f();
               temp.sub(pI, origin);

               return new IntersectionInformation(ray, this.boundingVolume, pI, new Vector3f(planeNormal), temp.length() + offsetDistance);
            } else {
               // outside plane bounds
            }
         } else {
            // intersection behind ray origin; ignore
         }
      } else {
         // ray is parallel to plane; ignore
      }

      return null;
   }

   private boolean pointInPolygon(final Vector3f p) {
      return (p.x >= this.minX - RTStatics.EPSILON) && (p.x <= this.maxX + RTStatics.EPSILON) && (p.y >= this.minY - RTStatics.EPSILON) && (p.y <= this.maxY + RTStatics.EPSILON) && (p.z >= this.minZ - RTStatics.EPSILON)
      && (p.z <= this.maxZ + RTStatics.EPSILON);

      //      final float a = (corners[0].x - p.x) * (corners[1].y - p.y) - (corners[1].x - p.x) * (corners[0].y - p.y);
      //      final float b = (corners[1].x - p.x) * (corners[2].y - p.y) - (corners[2].x - p.x) * (corners[1].y - p.y);
      //      final float c = (corners[2].x - p.x) * (corners[3].y - p.y) - (corners[3].x - p.x) * (corners[2].y - p.y);
      //      final float d = (corners[3].x - p.x) * (corners[0].y - p.y) - (corners[0].x - p.x) * (corners[3].y - p.y);
      //
      //      return Math.signum(a) == Math.signum(b) && Math.signum(b) == Math.signum(c) && Math.signum(c) == Math.signum(d);

   }
}
