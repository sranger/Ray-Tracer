package stephen.ranger.ar.sceneObjects;

import javax.vecmath.Vector3f;

import stephen.ranger.ar.ColorInformation;
import stephen.ranger.ar.IntersectionInformation;
import stephen.ranger.ar.RTStatics;
import stephen.ranger.ar.Ray;
import stephen.ranger.ar.bounds.AxisAlignedBoundingBox;

public class Triangle extends SceneObject {
   public final Vector3f[] p0, p1, p2;
   public final Vector3f faceNormal;
   public final ColorInformation colorInfo;

   public final float minX, maxX, minY, maxY, minZ, maxZ;

   public Triangle(final Vector3f[] p0, final Vector3f[] p1, final Vector3f[] p2, final ColorInformation colorInfo) {
      this.colorInfo = colorInfo;

      faceNormal = new Vector3f();
      final Vector3f e1 = new Vector3f();
      final Vector3f e2 = new Vector3f();

      e1.sub(p1[0], p0[0]);
      e2.sub(p2[0], p0[0]);

      faceNormal.cross(e1, e2);
      faceNormal.normalize();

      if (p0[1] != null && p1[1] != null && p2[1] != null) {
         this.p0 = new Vector3f[] { new Vector3f(p0[0]), new Vector3f(p0[1]) };
         this.p1 = new Vector3f[] { new Vector3f(p1[0]), new Vector3f(p1[1]) };
         this.p2 = new Vector3f[] { new Vector3f(p2[0]), new Vector3f(p2[1]) };
      } else {
         this.p0 = new Vector3f[] { new Vector3f(p0[0]), new Vector3f(faceNormal) };
         this.p1 = new Vector3f[] { new Vector3f(p1[0]), new Vector3f(faceNormal) };
         this.p2 = new Vector3f[] { new Vector3f(p2[0]), new Vector3f(faceNormal) };
      }

      minX = Math.min(p0[0].x, Math.min(p1[0].x, p2[0].x));
      maxX = Math.max(p0[0].x, Math.max(p1[0].x, p2[0].x));
      minY = Math.min(p0[0].y, Math.min(p1[0].y, p2[0].y));
      maxY = Math.max(p0[0].y, Math.max(p1[0].y, p2[0].y));
      minZ = Math.min(p0[0].z, Math.min(p1[0].z, p2[0].z));
      maxZ = Math.max(p0[0].z, Math.max(p1[0].z, p2[0].z));

      setBoundingVolume(new AxisAlignedBoundingBox(this, minX, minY, minZ, maxX, maxY, maxZ));
   }

   @Override
   public IntersectionInformation getIntersection(final Ray ray) {
      final float[] value = getBarycentricIntersection(ray.origin, ray.direction);

      if (value == null) {
         return null;
      } else {
         return new IntersectionInformation(ray, boundingVolume, new Vector3f(value[0], value[1], value[2]), new Vector3f(value[3], value[4], value[5]), value[6]);
      }
   }

   /**
    * Checks the given ray origin and ray direction against the triangle created from the vertex array and the
    * accompanying indices.
    * 
    * @param rO
    *           The ray origin
    * @param rD
    *           The ray direction
    * @param vertices
    *           The complete set of vertices
    * @param normals
    *           The complete set of normals
    * @param indices
    *           The indices that make up this triangle
    * @return An array of floats denoting the intersection, normal, and distance from ray origin in the form of: { x, y,
    *         z, nx, ny, nz, w } or null if no intersection exists
    */
   public static float[] intersectsTriangle(final Vector3f rO, final Vector3f rD, final float[][] vertices, final float[][] normals, final int[] indices) {
      // final float[] normal = RTStatics.computeNormal(vertices, indices);
      final Vector3f[] p0 = new Vector3f[] { new Vector3f(vertices[indices[0]]), new Vector3f(normals[indices[0]]) };
      final Vector3f[] p1 = new Vector3f[] { new Vector3f(vertices[indices[1]]), new Vector3f(normals[indices[1]]) };
      final Vector3f[] p2 = new Vector3f[] { new Vector3f(vertices[indices[2]]), new Vector3f(normals[indices[2]]) };

      final Vector3f e1 = new Vector3f();
      final Vector3f e2 = new Vector3f();
      e1.sub(p1[0], p0[0]);
      e2.sub(p2[0], p0[0]);

      final Vector3f p = new Vector3f();
      p.cross(rD, e2);
      final float divisor = p.dot(e1);
      /*
       * Ray nearly parallel to triangle plane...
       */
      if (divisor < RTStatics.EPSILON && divisor > -RTStatics.EPSILON) {
         return null;
      }

      final Vector3f translatedOrigin = new Vector3f(rO);
      translatedOrigin.sub(p0[0]);
      final Vector3f q = new Vector3f();
      q.cross(translatedOrigin, e1);
      /*
       * Barycentric coords also result from this formulation, which could be useful for interpolating attributes
       * defined at the vertex locations:
       */
      final float u = p.dot(translatedOrigin) / divisor;
      if (u < 0 || u > 1) {
         return null;
      }

      final float v = q.dot(rD) / divisor;
      if (v < 0 || v + u > 1) {
         return null;
      }

      // return q.dot(e2) / divisor;

      final float w = 1.0f - u - v;

      final float[] returnValue = new float[] { w * p0[0].x + u * p1[0].x + v * p2[0].x, w * p0[0].y + u * p1[0].y + v * p2[0].y, w * p0[0].z + u * p1[0].z + v * p2[0].z,
            w * p0[1].x + u * p1[1].x + v * p2[1].x, w * p0[1].y + u * p1[1].y + v * p2[1].y, w * p0[1].z + u * p1[1].z + v * p2[1].z, -1 };
      returnValue[6] = RTStatics.getDistance(new float[] { rO.getX(), rO.getY(), rO.getZ() }, returnValue);

      return returnValue;
   }

   /**
    * Returns an IntersectionInformation for the intersection created from this triangle and the given Ray.
    * 
    * http://www.graphics.cornell.edu/pubs/1997/MT97.html
    * http://www.gamedev.net/community/forums/topic.asp?topic_id=263600
    * 
    * @param ray
    *           The Ray to use for the intersection test
    * @return An array of floats denoting the intersection, normal, and distance from ray origin in the form of: { x, y,
    *         z, nx, ny, nz, w } or null if no intersection exists
    */
   public final float[] getBarycentricIntersection(final Vector3f rO, final Vector3f rD) {
      final Vector3f edge1 = new Vector3f();
      final Vector3f edge2 = new Vector3f();
      final Vector3f qvec = new Vector3f();
      final Vector3f pvec = new Vector3f();
      final Vector3f tvec = new Vector3f();
      float det, inv_det, t, u, v;

      // find edge vectors that share p0
      edge1.sub(p1[0], p0[0]);
      edge2.sub(p2[0], p0[0]);

      // begin calculating determinant - also used to calculate U parameter
      pvec.cross(rD, edge2);

      // if determinant is near zero, ray lies in plane of triangle
      det = edge1.dot(pvec);

      if (RTStatics.ENABLE_BACKFACE_CULLING) {
         if (det < RTStatics.EPSILON) {
            return null;
         }

         // calculate distance from p0 to ray origin
         tvec.sub(rO, p0[0]);

         // calculate U parameter and test bounds
         u = tvec.dot(pvec);

         if (u < 0.0 || u > det) {
            return null;
         }

         // prepare to test V parameter
         qvec.cross(tvec, edge1);

         // calculate V parameter and test bounds
         v = rD.dot(qvec);

         if (v < 0.0 || u + v > det) {
            return null;
         }

         // calculate t, scale parameters, ray intersects triangle
         t = edge2.dot(qvec);
         inv_det = 1.0f / det;
         t *= inv_det;
         u *= inv_det;
         v *= inv_det;
      } else {
         if (det > -RTStatics.EPSILON && det < RTStatics.EPSILON) {
            return null;
         }

         inv_det = 1.0f / det;

         // calculate distance from p0 to ray origin
         tvec.sub(rO, p0[0]);

         // calculate U parameter and test bounds
         u = tvec.dot(pvec) * inv_det;

         if (u < 0.0 || u > 1.0) {
            return null;
         }

         // prepare to test V parameter
         qvec.cross(tvec, edge1);

         // calculate V parameter and test bounds
         v = rD.dot(qvec) * inv_det;

         if (v < 0.0 || u + v > 1.0) {
            return null;
         }

         // calculate t, ray intersects triangle
         t = edge2.dot(qvec) * inv_det;
      }

      // compute w value (as u + v + w must == 1.0)
      final float w = 1.0f - (u + v);

      // compute intersection
      final float[] returnValue = new float[] { w * p0[0].x + u * p1[0].x + v * p2[0].x, w * p0[0].y + u * p1[0].y + v * p2[0].y, w * p0[0].z + u * p1[0].z + v * p2[0].z,
            w * p0[1].x + u * p1[1].x + v * p2[1].x, w * p0[1].y + u * p1[1].y + v * p2[1].y, w * p0[1].z + u * p1[1].z + v * p2[1].z, -1 };
      returnValue[6] = RTStatics.getDistance(new float[] { rO.getX(), rO.getY(), rO.getZ() }, returnValue);

      return returnValue;
   }
}
