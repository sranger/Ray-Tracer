package stephen.ranger.ar.sceneObjects;

import java.awt.Color;
import java.security.InvalidParameterException;
import java.util.Arrays;

import javax.vecmath.Vector3f;

import stephen.ranger.ar.ColorInformation;
import stephen.ranger.ar.IntersectionInformation;
import stephen.ranger.ar.RTStatics;
import stephen.ranger.ar.Ray;
import stephen.ranger.ar.bounds.AxisAlignedBoundingBox;

/**
 * Plane equation info: http://local.wasp.uwa.edu.au/~pbourke/geometry/planeeq/
 */
public class Plane extends SceneObject {
   // public final Vector3f[] corners;
   public final float A, B, C, D;
   public final Vector3f normal;

   public final float minX, maxX, minY, maxY, minZ, maxZ;

   public final float width, height;
   public final int horizontalBoxes = 10;
   public final int verticalBoxes = 10;

   private final Color negDiffuse;

   public Plane(Vector3f[] corners, final ColorInformation colorInfo) {
      super(colorInfo);

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

      this.minX = Math.min(corners[0].x, Math.min(corners[1].x, Math.min(corners[2].x, corners[3].x))) - RTStatics.EPSILON;
      this.maxX = Math.max(corners[0].x, Math.max(corners[1].x, Math.max(corners[2].x, corners[3].x))) + RTStatics.EPSILON;
      this.minY = Math.min(corners[0].y, Math.min(corners[1].y, Math.min(corners[2].y, corners[3].y))) - RTStatics.EPSILON;
      this.maxY = Math.max(corners[0].y, Math.max(corners[1].y, Math.max(corners[2].y, corners[3].y))) + RTStatics.EPSILON;
      this.minZ = Math.min(corners[0].z, Math.min(corners[1].z, Math.min(corners[2].z, corners[3].z))) - RTStatics.EPSILON;
      this.maxZ = Math.max(corners[0].z, Math.max(corners[1].z, Math.max(corners[2].z, corners[3].z))) + RTStatics.EPSILON;

      if ((this.maxX - this.minX < this.maxY - this.minY) && (this.maxX - this.minX < this.maxZ - this.minZ)) {
         this.width = RTStatics.getDistance(new float[] { this.minX, this.minY, this.minZ }, new float[] { this.minX, this.minY, this.maxZ });
         this.height = RTStatics.getDistance(new float[] { this.minX, this.minY, this.minZ }, new float[] { this.minX, this.maxY, this.minZ });
      } else if ((this.maxY - this.minY < this.maxX - this.minX) && (this.maxY - this.minY < this.maxZ - this.minZ)) {
         this.width = RTStatics.getDistance(new float[] { this.minX, this.minY, this.minZ }, new float[] { this.maxX, this.minY, this.minZ });
         this.height = RTStatics.getDistance(new float[] { this.minX, this.minY, this.minZ }, new float[] { this.minX, this.minY, this.maxZ });
      } else {
         this.width = RTStatics.getDistance(new float[] { this.minX, this.minY, this.minZ }, new float[] { this.maxX, this.minY, this.minZ });
         this.height = RTStatics.getDistance(new float[] { this.minX, this.minY, this.minZ }, new float[] { this.minX, this.maxY, this.minZ });
      }

      this.negDiffuse = new Color(255 - colorInfo.diffuse.getRed(), 255 - colorInfo.diffuse.getGreen(), 255 - colorInfo.diffuse.getBlue());

      System.out.println("plane bounds: " + this.minX + ", " + this.maxX + "  " + this.minY + ", " + this.maxY + "  " + this.minZ + ", " + this.maxZ);
      this.setBoundingVolume(new AxisAlignedBoundingBox(this, this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ));
   }

   public Plane() {
      this(new Vector3f[] { new Vector3f(-1, 0, -1), new Vector3f(-1, 0, 1), new Vector3f(1, 0, -1), new Vector3f(1, 0, 1) }, new ColorInformation());
   }

   /**
    * http://www.siggraph.org/education/materials/HyperGraph/raytrace/
    * rayplane_intersection.htm
    */
   @Override
   public IntersectionInformation getIntersection(final Ray ray) {
      final Vector3f planeNormal = new Vector3f(this.normal);
      float vD = planeNormal.dot(ray.direction);

      if (vD > RTStatics.EPSILON) {
         planeNormal.scale(-1);
         vD = planeNormal.dot(ray.direction);
      }

      if ((vD <= -RTStatics.EPSILON) || (vD >= RTStatics.EPSILON)) {
         // V0 = -(Pn . R0 + D) and compute t = V0 / Vd. If t < 0 then the
         // ray intersects plane behind origin, i.e. no intersection of
         // interest
         // final float v0 = -(this.normal.dot(ray.origin) + D);
         // final float t = v0 / vD;

         // t = -(AX0 + BY0 + CZ0 + D) / (AXd + BYd + CZd)
         final float t = -(this.A * ray.origin.x + this.B * ray.origin.y + this.C * ray.origin.z + this.D) / (this.A * ray.direction.x + this.B * ray.direction.y + this.C * ray.direction.z);

         if (t > -RTStatics.EPSILON) {
            final Vector3f rD = new Vector3f(ray.direction);

            if (vD > -RTStatics.EPSILON) {
               rD.x = -rD.x;
               rD.y = -rD.y;
               rD.z = -rD.z;
            }

            // Pi = [Xi Yi Zi] = [X0 + Xd * t Y0 + Yd * t Z0 + Zd * t]
            final Vector3f pI = new Vector3f(ray.origin.x + rD.x * t, ray.origin.y + rD.y * t, ray.origin.z + rD.z * t);

            if ((pI.x >= this.minX) && (pI.x <= this.maxX) && (pI.y >= this.minY) && (pI.y <= this.maxY) && (pI.z >= this.minZ) && (pI.z <= this.maxZ)) {
               final Vector3f temp = new Vector3f();
               temp.sub(pI, ray.origin);
               return new IntersectionInformation(ray, this.boundingVolume, pI, new Vector3f(planeNormal), temp.length());
            }
         } else {
            // intersection behind ray origin; ignore
         }
      } else {
         // ray is parallel to plane; ignore
      }

      return null;
   }

   @Override
   public Color getColor(final Vector3f intersection) {
      float wD, hD;
      final float[] intersectionArray = new float[3];
      intersection.get(intersectionArray);

      if ((this.maxX - this.minX < this.maxY - this.minY) && (this.maxX - this.minX < this.maxZ - this.minZ)) {
         //z = width, y = height
         wD = RTStatics.getDistance(intersectionArray, new float[] { intersection.x, intersection.y, this.minZ });
         hD = RTStatics.getDistance(intersectionArray, new float[] { intersection.x, this.minY, intersection.z });
      } else if ((this.maxY - this.minY < this.maxX - this.minX) && (this.maxY - this.minY < this.maxZ - this.minZ)) {
         // x = width, z = height
         wD = RTStatics.getDistance(intersectionArray, new float[] { this.minX, intersection.y, intersection.z });
         hD = RTStatics.getDistance(intersectionArray, new float[] { intersection.x, intersection.y, this.minZ });
      } else {
         // x = width, y = height
         wD = RTStatics.getDistance(intersectionArray, new float[] { this.minX, intersection.y, intersection.z });
         hD = RTStatics.getDistance(intersectionArray, new float[] { intersection.x, this.minY, intersection.z });
      }

      final float widthStep = this.width / this.horizontalBoxes;
      final float heightStep = this.height / this.verticalBoxes;

      final int x = (int) Math.floor(wD / widthStep);
      final int y = (int) Math.floor(hD / heightStep);

      if (((x % 2 == 0) && (y % 2 == 0)) || ((x % 2 != 0) && (y % 2 != 0))) {
         return this.colorInfo.diffuse;
      } else {
         return this.negDiffuse;
      }
   }
}
