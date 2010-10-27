package stephen.ranger.ar;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import stephen.ranger.ar.photons.Photon;

public class RTStatics {

   public static float EPSILON = 1e-15f;

   public static final float nearPlane = 0.01f;
   public static final float farPlane = 3000.0f;

   public static final int MAX_CHILDREN = 5;

   public static boolean ENABLE_BACKFACE_CULLING = true;

   public static final Matrix4f OPENGL_ROTATION = new Matrix4f(RTStatics.initializeQuat4f(new Vector3f(0, 1, 0), 180), new Vector3f(), 0f);

   public static final int MAX_DEPTH = 20;

   public static enum SeparationAxis {
      X(0), Y(1), Z(2);

      public final int pos;

      private SeparationAxis(final int pos) {
         this.pos = pos;
      }

      public SeparationAxis getNextAxis() {
         return equals(X) ? Y : equals(Y) ? Z : X;
      }
   }

   private RTStatics() {
      // only static stuff here
   }

   /**
    * r = L - 2 * N * L.dot(N)
    * 
    * @param info
    * @param dir
    *           direction to reflect through info.normal
    * @return
    */
   public static Vector3f getReflectionDirection(final Vector3f n, final Vector3f dir) {
      final Vector3f R = new Vector3f();
      final Vector3f N = new Vector3f(n);
      N.scale(2f);
      N.scale(dir.dot(n));
      R.sub(dir, N);
      R.normalize();

      return R;
   }

   public static Vector3f getReflectionDirection(final IntersectionInformation info, final Vector3f dir) {
      return getReflectionDirection(info.normal, dir);
   }

   public static float leastPositive(final float i, final float j) {
      float retVal;

      if (i < 0 && j < 0) {
         retVal = -1;
      } else if (i < 0 && j > 0) {
         retVal = j;
      } else if (i > 0 && j < 0) {
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

   public static boolean aabbIntersection(final Ray r, final float[][] minMax) {
      // http://people.csail.mit.edu/amy/papers/box-jgt.pdf
      float txmin, txmax, tymin, tymax, tzmin, tzmax;
      final float divx = 1.0f / r.direction.x;
      final float divy = 1.0f / r.direction.y;
      final float divz = 1.0f / r.direction.z;

      if (divx >= 0) {
         txmin = (minMax[0][0] - r.origin.x) * divx;
         txmax = (minMax[1][0] - r.origin.x) * divx;
      } else {
         txmin = (minMax[1][0] - r.origin.x) * divx;
         txmax = (minMax[0][0] - r.origin.x) * divx;
      }

      if (divy >= 0) {
         tymin = (minMax[0][1] - r.origin.y) * divy;
         tymax = (minMax[1][1] - r.origin.y) * divy;
      } else {
         tymin = (minMax[1][1] - r.origin.y) * divy;
         tymax = (minMax[0][1] - r.origin.y) * divy;
      }

      if (txmin > tymax || tymin > txmax) {
         return false;
      }

      if (tymin > txmin) {
         txmin = tymin;
      }

      if (tymax < txmax) {
         txmax = tymax;
      }

      if (divz >= 0) {
         tzmin = (minMax[0][2] - r.origin.z) * divz;
         tzmax = (minMax[1][2] - r.origin.z) * divz;
      } else {
         tzmin = (minMax[1][2] - r.origin.z) * divz;
         tzmax = (minMax[0][2] - r.origin.z) * divz;
      }

      if (txmin > tzmax || tzmin > txmax) {
         return false;
      }

      if (tzmin > txmin) {
         txmin = tzmin;
      }

      if (tzmax < txmax) {
         txmax = tzmax;
      }

      return txmin < RTStatics.farPlane && txmax > RTStatics.nearPlane;
   }

   /**
    * Returns the distance between the two given vertices.
    * 
    * @param p1    Vertex 1
    * @param p2    Vertex 2
    * @return      The distance between p1 and p2
    */
   public static float getDistance(final float[] p1, final float[] p2) {
      return (float) Math.sqrt((p1[0] - p2[0]) * (p1[0] - p2[0]) + (p1[1] - p2[1]) * (p1[1] - p2[1]) + (p1[2] - p2[2]) * (p1[2] - p2[2]));
   }

   /**
    * Returns the distance between the two given vertices.
    * 
    * @param p1
    *           Vertex 1
    * @param p2
    *           Vertex 2
    * @return The distance between p1 and p2
    */
   public static float getDistance(final Vector3f p1, final Vector3f p2) {
      return (float) Math.sqrt((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y) + (p1.z - p2.z) * (p1.z - p2.z));
   }

   /**
    * Returns the min/max values of the vertices denoted by the given indices.
    * 
    * @param vertices
    *           The complete set of vertices
    * @param indices
    *           The indices to find the min/max for. Arbitrary number of faces
    * @param output
    *           A float matrix of size 2x3 to store the minMax value in. This will be stored as: { minx, miny, minz }, { maxx, maxy, maxz }
    */
   public static void getMinMax(final float[][] vertices, final int[][] indices, final float[][] output) {
      output[0][0] = Float.MAX_VALUE;
      output[0][1] = Float.MAX_VALUE;
      output[0][2] = Float.MAX_VALUE;

      output[1][0] = -Float.MAX_VALUE;
      output[1][1] = -Float.MAX_VALUE;
      output[1][2] = -Float.MAX_VALUE;

      for (final int[] face : indices) {
         for (final int index : face) {
            output[0][0] = Math.min(output[0][0], vertices[index][0]);
            output[0][1] = Math.min(output[0][1], vertices[index][1]);
            output[0][2] = Math.min(output[0][2], vertices[index][2]);

            output[1][0] = Math.max(output[1][0], vertices[index][0]);
            output[1][1] = Math.max(output[1][1], vertices[index][1]);
            output[1][2] = Math.max(output[1][2], vertices[index][2]);
         }
      }
   }

   public static void getMinMax(final Photon[] photons, final float[][] output) {
      output[0][0] = Float.MAX_VALUE;
      output[0][1] = Float.MAX_VALUE;
      output[0][2] = Float.MAX_VALUE;

      output[1][0] = -Float.MAX_VALUE;
      output[1][1] = -Float.MAX_VALUE;
      output[1][2] = -Float.MAX_VALUE;

      for (final Photon photon : photons) {
         output[0][0] = Math.min(output[0][0], photon.location[0]);
         output[0][1] = Math.min(output[0][1], photon.location[1]);
         output[0][2] = Math.min(output[0][2], photon.location[2]);

         output[1][0] = Math.max(output[1][0], photon.location[0]);
         output[1][1] = Math.max(output[1][1], photon.location[1]);
         output[1][2] = Math.max(output[1][2], photon.location[2]);
      }
   }

   /**
    * Returns the min/max values of the vertices denoted by the given indices.
    * 
    * @param vertices
    *           The complete set of vertices
    * @param indices
    *           The indices to find the min/max for. Single face
    * @param output
    *           A float matrix of size 2x3 to store the minMax value in. This will be stored as: { minx, miny, minz }, { maxx, maxy, maxz }
    */
   public static void getMinMax(final float[][] vertices, final int[] indices, final float[][] output) {
      output[0][0] = Float.MAX_VALUE;
      output[0][1] = Float.MAX_VALUE;
      output[0][2] = Float.MAX_VALUE;

      output[1][0] = -Float.MAX_VALUE;
      output[1][1] = -Float.MAX_VALUE;
      output[1][2] = -Float.MAX_VALUE;

      for (final int index : indices) {
         output[0][0] = Math.min(output[0][0], vertices[index][0]);
         output[0][1] = Math.min(output[0][1], vertices[index][1]);
         output[0][2] = Math.min(output[0][2], vertices[index][2]);

         output[1][0] = Math.max(output[1][0], vertices[index][0]);
         output[1][1] = Math.max(output[1][1], vertices[index][1]);
         output[1][2] = Math.max(output[1][2], vertices[index][2]);
      }
   }

   /**
    * Computes the normal from the given vertices.
    * 
    * @param vertices
    *           A array matrix of size 3x3 with three x,y,z triplets in counter-clockwise order
    * @return The normal of the given triangle
    */
   public static float[] computeNormal(final float[][] vertices, final int[] indices) {
      final Vector3f e1 = new Vector3f(vertices[indices[1]][0] - vertices[indices[0]][0], vertices[indices[1]][1] - vertices[indices[0]][1], vertices[indices[1]][2] - vertices[indices[0]][2]);
      final Vector3f e2 = new Vector3f(vertices[indices[2]][0] - vertices[indices[0]][0], vertices[indices[2]][1] - vertices[indices[0]][1], vertices[indices[2]][2] - vertices[indices[0]][2]);
      final Vector3f normal = new Vector3f();
      normal.cross(e1, e2);
      normal.normalize();

      final float[] retVal = new float[3];
      normal.get(retVal);

      return retVal;
   }

   /**
    * Creates a new Quat4f object and initializes it using the given orientation. This will use a right-handed rule.
    * 
    * @param orientation
    *           A Vector3f containing the yaw, pitch, and roll orientation in degrees
    * @return A Quat4f initialized using the given orientation
    */
   public static Quat4f initializeQuat4f(final float[] orientation) {
      final Quat4f qx = initializeQuat4f(new Vector3f(0, 1, 0), -orientation[0]); // yaw
      final Quat4f qy = initializeQuat4f(new Vector3f(1, 0, 0), orientation[1]); // pitch
      final Quat4f qz = initializeQuat4f(new Vector3f(0, 0, 1), -orientation[2]); // roll

      qx.mul(qy);
      qx.mul(qz);

      return qx;
   }

   public static Quat4f initializeQuat4f(final Vector3f axis, final float angle) {
      final Quat4f quat = new Quat4f();
      quat.set(new AxisAngle4f(axis, (float) Math.toRadians(angle)));

      return quat;
   }

   public static float[] computeColorAverage(final float[][] colors) {
      float r = 0, g = 0, b = 0;

      for (final float[] color : colors) {
         r += color[0];
         g += color[1];
         b += color[2];
      }

      return new float[] { r / colors.length, g / colors.length, b / colors.length };
   }

   public static double bound(final double min, final double max, final double value) {
      return Math.min(max, Math.max(min, value));
   }
}
