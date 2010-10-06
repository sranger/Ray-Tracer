package stephen.ranger.ar.sceneObjects;

import java.awt.Color;

import javax.vecmath.Vector3d;

import stephen.ranger.ar.ColorInformation;
import stephen.ranger.ar.IntersectionInformation;
import stephen.ranger.ar.RTStatics;
import stephen.ranger.ar.Ray;
import stephen.ranger.ar.bounds.AxisAlignedBoundingBox;

public class Triangle extends SceneObject {
   public final Vector3d[] p0, p1, p2;
   public final Vector3d faceNormal;
   public final ColorInformation colorInfo;

   public final double minX, maxX, minY, maxY, minZ, maxZ;

   public Triangle(final Vector3d[] p0, final Vector3d[] p1, final Vector3d[] p2, final ColorInformation colorInfo) {
      this.colorInfo = colorInfo;

      this.faceNormal = new Vector3d();
      final Vector3d e1 = new Vector3d();
      final Vector3d e2 = new Vector3d();

      e1.sub(p1[0], p0[0]);
      e2.sub(p2[0], p0[0]);

      this.faceNormal.cross(e1, e2);
      this.faceNormal.normalize();

      if ((p0[1] != null) && (p1[1] != null) && (p2[1] != null)) {
         this.p0 = new Vector3d[] { new Vector3d(p0[0]), new Vector3d(p0[1]) };
         this.p1 = new Vector3d[] { new Vector3d(p1[0]), new Vector3d(p1[1]) };
         this.p2 = new Vector3d[] { new Vector3d(p2[0]), new Vector3d(p2[1]) };
      } else {
         this.p0 = new Vector3d[] { new Vector3d(p0[0]), new Vector3d(this.faceNormal) };
         this.p1 = new Vector3d[] { new Vector3d(p1[0]), new Vector3d(this.faceNormal) };
         this.p2 = new Vector3d[] { new Vector3d(p2[0]), new Vector3d(this.faceNormal) };
      }

      this.minX = Math.min(p0[0].x, Math.min(p1[0].x, p2[0].x));
      this.maxX = Math.max(p0[0].x, Math.max(p1[0].x, p2[0].x));
      this.minY = Math.min(p0[0].y, Math.min(p1[0].y, p2[0].y));
      this.maxY = Math.max(p0[0].y, Math.max(p1[0].y, p2[0].y));
      this.minZ = Math.min(p0[0].z, Math.min(p1[0].z, p2[0].z));
      this.maxZ = Math.max(p0[0].z, Math.max(p1[0].z, p2[0].z));

      this.setBoundingVolume(new AxisAlignedBoundingBox(this, this.minX, this.minY, this.minZ, this.maxX, this.maxY,
            this.maxZ));
   }

   @Override
   public IntersectionInformation getIntersection(final Ray ray) {
      // return this.getBarycentricIntersection(ray);

      return this.intersectsTriangle(ray);
   }

   @Override
   public Color getColor(final IntersectionInformation info) {
      return this.colorInfo.emission;
   }

   public IntersectionInformation intersectsTriangle(final Ray ray) {
      final Vector3d e1 = new Vector3d();
      final Vector3d e2 = new Vector3d();
      e1.sub(this.p1[0], this.p0[0]);
      e2.sub(this.p2[0], this.p0[0]);

      final Vector3d p = new Vector3d();
      p.cross(ray.direction, e2);
      final double divisor = p.dot(e1);
      /*
       * Ray nearly parallel to triangle plane...
       */
      if ((divisor < RTStatics.EPSILON) && (divisor > -RTStatics.EPSILON)) {
         return null;
      }

      final Vector3d translatedOrigin = new Vector3d(ray.origin);
      translatedOrigin.sub(this.p0[0]);
      final Vector3d q = new Vector3d();
      q.cross(translatedOrigin, e1);
      /*
       * Barycentric coords also result from this formulation, which could be useful for interpolating attributes
       * defined at the vertex locations:
       */
      final double u = p.dot(translatedOrigin) / divisor;
      if ((u < 0) || (u > 1)) {
         return null;
      }

      final double v = q.dot(ray.direction) / divisor;
      if ((v < 0) || (v + u > 1)) {
         return null;
      }

      // return q.dot(e2) / divisor;

      final double w = 1.0 - u - v;

      final Vector3d intersection = new Vector3d(w * this.p0[0].x + u * this.p1[0].x + v * this.p2[0].x, w
            * this.p0[0].y + u * this.p1[0].y + v * this.p2[0].y, w * this.p0[0].z + u * this.p1[0].z + v
            * this.p2[0].z);
      final Vector3d normal = new Vector3d(w * this.p0[1].x + u * this.p1[1].x + v * this.p2[1].x, w * this.p0[1].y + u
            * this.p1[1].y + v * this.p2[1].y, w * this.p0[1].z + u * this.p1[1].z + v * this.p2[1].z);
      normal.normalize();

      return new IntersectionInformation(ray, this.boundingVolume, intersection, normal,
            RTStatics.getDistance(ray.origin, intersection));
   }

   /**
    * Returns an IntersectionInformation for the intersection created from this triangle and the given Ray.
    * 
    * http://www.graphics.cornell.edu/pubs/1997/MT97.html
    * http://www.gamedev.net/community/forums/topic.asp?topic_id=263600
    * 
    * @param ray   The Ray to use for the intersection test
    * @return      An IntersectionInformation object containing the intersection or null if no intersection exists
    */
   public final IntersectionInformation getBarycentricIntersection(final Ray ray) {
      final Vector3d edge1 = new Vector3d();
      final Vector3d edge2 = new Vector3d();
      final Vector3d qvec = new Vector3d();
      final Vector3d pvec = new Vector3d();
      final Vector3d tvec = new Vector3d();
      double det, inv_det, t, u, v;

      // find edge vectors that share p0
      edge1.sub(this.p1[0], this.p0[0]);
      edge2.sub(this.p2[0], this.p0[0]);

      // begin calculating determinant - also used to calculate U parameter
      pvec.cross(ray.direction, edge2);

      // if determinant is near zero, ray lies in plane of triangle
      det = edge1.dot(pvec);

      if (RTStatics.ENABLE_BACKFACE_CULLING) {
         if (det < RTStatics.EPSILON) {
            return null;
         }

         // calculate distance from p0 to ray origin
         tvec.sub(ray.origin, this.p0[0]);

         // calculate U parameter and test bounds
         u = tvec.dot(pvec);

         if ((u < 0.0) || (u > det)) {
            return null;
         }

         // prepare to test V parameter
         qvec.cross(tvec, edge1);

         // calculate V parameter and test bounds
         v = ray.direction.dot(qvec);

         if ((v < 0.0) || (u + v > det)) {
            return null;
         }

         // calculate t, scale parameters, ray intersects triangle
         t = edge2.dot(qvec);
         inv_det = 1.0 / det;
         t *= inv_det;
         u *= inv_det;
         v *= inv_det;
      } else {
         if ((det > -RTStatics.EPSILON) && (det < RTStatics.EPSILON)) {
            return null;
         }

         inv_det = 1.0 / det;

         // calculate distance from p0 to ray origin
         tvec.sub(ray.origin, this.p0[0]);

         // calculate U parameter and test bounds
         u = tvec.dot(pvec) * inv_det;

         if ((u < 0.0) || (u > 1.0)) {
            return null;
         }

         // prepare to test V parameter
         qvec.cross(tvec, edge1);

         // calculate V parameter and test bounds
         v = ray.direction.dot(qvec) * inv_det;

         if ((v < 0.0) || (u + v > 1.0)) {
            return null;
         }

         // calculate t, ray intersects triangle
         t = edge2.dot(qvec) * inv_det;
      }

      // compute w value (as u + v + w must == 1.0)
      final double w = 1.0 - (u + v);

      // compute intersection
      final Vector3d intersection = new Vector3d(u * this.p0[0].x + v * this.p1[0].x + w * this.p2[0].x, u
            * this.p0[0].y + v * this.p1[0].y + w * this.p2[0].y, u * this.p0[0].z + v * this.p1[0].z + w
            * this.p2[0].z);
      //      final Vector3d normal = new Vector3d(u * this.p0[1].x + v * this.p1[1].x + w * this.p2[1].x, u * this.p0[1].y + v
      //            * this.p1[1].y + w * this.p2[1].y, u * this.p0[1].z + v * this.p1[1].z + w * this.p2[1].z);
      //      normal.normalize();

      final Vector3d normal = new Vector3d(this.faceNormal);

      return new IntersectionInformation(ray, this.boundingVolume, intersection, normal,
            RTStatics.getDistance(ray.origin, intersection));
   }
}
