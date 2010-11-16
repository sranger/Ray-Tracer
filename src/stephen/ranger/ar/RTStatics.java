package stephen.ranger.ar;

import java.text.DecimalFormat;

import javax.swing.JProgressBar;
import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import stephen.ranger.ar.bounds.BoundingVolume;
import stephen.ranger.ar.lighting.Light;
import stephen.ranger.ar.photons.Photon;

public class RTStatics {

   public static float EPSILON = 1e-15f;
   public static final float NEAR_PLANE = 0.01f;
   public static final float FAR_PLANE = 3000.0f;
   public static final int MAX_RECURSION_DEPTH = 5;

   // KD-Tree settings
   public static final int MAX_CHILDREN = 10;
   public static final int MAX_DEPTH = 20;

   public static boolean ENABLE_BACKFACE_CULLING = true;

   public static final Matrix4f OPENGL_ROTATION = new Matrix4f(RTStatics.initializeQuat4f(new Vector3f(0, 1, 0), 180), new Vector3f(), 0f);

   // photon mapping settings
   public static final float COLLECTION_RANGE = 5f;
   public static final int NUM_REFLECTIONS = 10;
   public static final int NUM_PHOTONS = 2000;
   public static final int COLLECTION_COUNT_THRESHOLD = 50;
   public static final float STARTING_INTENSITY = 10000000f;
   public static final int PHOTON_COLLECTION_RAY_COUNT = 25;

   private static JProgressBar PROGRESS_BAR;

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

   public static Vector3f getReflectionDirection(final IntersectionInformation info) {
      return getReflectionDirection(info.normal, info.ray.direction);
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

      return txmin < RTStatics.FAR_PLANE && txmax > RTStatics.NEAR_PLANE;
   }

   /**
    * Returns the distance between the two given vertices.
    * 
    * @param p1    Vertex 1
    * @param p2    Vertex 2
    * @return      The distance between p1 and p2
    */
   public static float getDistance(final float[] p1, final float[] p2) {
      return (float) Math.sqrt(RTStatics.getDistanceSquared(p1, p2));
   }

   /**
    * Returns the squared distance between the two given vertices.
    * 
    * @param p1
    *           Vertex 1
    * @param p2
    *           Vertex 2
    * @return The distance between p1 and p2
    */
   public static float getDistanceSquared(final float[] p1, final float[] p2) {
      return (p1[0] - p2[0]) * (p1[0] - p2[0]) + (p1[1] - p2[1]) * (p1[1] - p2[1]) + (p1[2] - p2[2]) * (p1[2] - p2[2]);
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
      return (float) Math.sqrt(RTStatics.getDistanceSquared(p1, p2));
   }

   /**
    * Returns the squared distance between the two given vertices.
    * 
    * @param p1
    *           Vertex 1
    * @param p2
    *           Vertex 2
    * @return The distance between p1 and p2
    */
   public static float getDistanceSquared(final Vector3f p1, final Vector3f p2) {
      return (p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y) + (p1.z - p2.z) * (p1.z - p2.z);
   }

   /**
    * Returns the distance between the two given vertices.
    * 
    * @param p1   Vertex 1
    * @param p2   Vertex 2
    * @return     The distance between p1 and p2
    */
   public static float getDistance(final Vector3f p1, final float[] p2) {
      return (float) Math.sqrt(RTStatics.getDistanceSquared(p1, p2));
   }

   /**
    * Returns the squared distance between the two given vertices.
    * 
    * @param p1
    *           Vertex 1
    * @param p2
    *           Vertex 2
    * @return The distance between p1 and p2
    */
   public static float getDistanceSquared(final Vector3f p1, final float[] p2) {
      return (p1.x - p2[0]) * (p1.x - p2[0]) + (p1.y - p2[1]) * (p1.y - p2[1]) + (p1.z - p2[2]) * (p1.z - p2[2]);
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
      final Vector3f e1 = new Vector3f(vertices[indices[1]][0] - vertices[indices[0]][0], vertices[indices[1]][1] - vertices[indices[0]][1],
            vertices[indices[1]][2] - vertices[indices[0]][2]);
      final Vector3f e2 = new Vector3f(vertices[indices[2]][0] - vertices[indices[0]][0], vertices[indices[2]][1] - vertices[indices[0]][1],
            vertices[indices[2]][2] - vertices[indices[0]][2]);
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

   public static float bound(final float min, final float max, final float value) {
      return Math.min(max, Math.max(min, value));
   }

   private static int min = 0;
   private static int max = 0;
   private static long startTime = 0;
   private static long currentTime = 0;
   private static String currentString = "";
   private static final DecimalFormat formatter = new DecimalFormat("0.0");

   public static void setProgressBar(final JProgressBar bar) {
      PROGRESS_BAR = bar;
      PROGRESS_BAR.setStringPainted(true);
      PROGRESS_BAR.setString("");
   }

   public static void setProgressBarMinMax(final int min, final int max) {
      if (PROGRESS_BAR != null) {
         RTStatics.min = min;
         RTStatics.max = max;
         startTime = System.nanoTime();

         PROGRESS_BAR.setMinimum(min);
         PROGRESS_BAR.setMaximum(max);
      }
   }

   public static void setProgressBarValue(final int value) {
      if (PROGRESS_BAR != null) {
         currentTime = System.nanoTime();
         PROGRESS_BAR.setValue(value);

         final double percentageDone = value / ((double) max - (double) min);
         final double seconds = (currentTime - startTime) / 1000000000.;
         PROGRESS_BAR.setString(currentString + " (ETA: " + formatter.format(seconds / percentageDone - seconds) + " seconds)");
      }
   }

   public static void setProgressBarString(final String string) {
      if (PROGRESS_BAR != null) {
         currentString = string;
         PROGRESS_BAR.setString(string);
      }
   }

   public static void incrementProgressBarValue(final int inc) {
      if (PROGRESS_BAR != null) {
         RTStatics.setProgressBarValue(PROGRESS_BAR.getValue() + inc);
      }
   }

   /**
    * http://jgt.akpeters.com/papers/SmithLyons96/hsv_rgb.html
    * 
    * @param rgb
    * @return
    */
   public static float[] convertRGBtoHSV(final float[] rgb) {
      // RGB are each on [0, 1]. S and V are returned on [0, 1] and H is
      // returned on [0, 6]. Exception: H is returned UNDEFINED if S==0.
      final float R = rgb[0], G = rgb[1], B = rgb[2];
      float v, x, f;
      int i;

      x = Math.min(R, Math.min(G, B));
      v = Math.max(R, Math.max(G, B));

      if (v == x) {
         return new float[] { -1, 0, v };
      }

      f = R == x ? G - B : G == x ? B - R : R - G;
      i = R == x ? 3 : G == x ? 5 : 1;

      return new float[] { i - f / (v - x), (v - x) / v, v };
   }

   public static float[] convertHSVtoRGB(final float[] hsv) {
      // H is given on [0, 6] or UNDEFINED. S and V are given on [0, 1].
      // RGB are each returned on [0, 1].
      final float h = hsv[0], s = hsv[1], v = hsv[2];
      float m, n, f;
      int i;

      if (h == -1) {
         return new float[] { v, v, v };
      }

      i = (int) Math.floor(h);
      f = h - i;

      if (i % 2 == 0) {
         f = 1 - f;
      }

      m = v * (1 - s);
      n = v * (1 - s * f);

      float[] rgb = new float[3];

      switch (i) {
      case 6:
      case 0:
         rgb = new float[] { v, n, m };
         break;
      case 1:
         rgb = new float[] { n, v, m };
         break;
      case 2:
         rgb = new float[] { m, v, n };
         break;
      case 3:
         rgb = new float[] { m, n, v };
         break;
      case 4:
         rgb = new float[] { n, m, v };
         break;
      case 5:
         rgb = new float[] { v, m, n };
         break;
      }

      rgb[0] = Math.min(1f, Math.max(0f, rgb[0]));
      rgb[1] = Math.min(1f, Math.max(0f, rgb[1]));
      rgb[2] = Math.min(1f, Math.max(0f, rgb[2]));

      return rgb;
   }

   /**
    * http://www.devmaster.net/forums/archive/index.php/t-10324.html
    * 
    * @param minMax
    * @param minMax2
    * @return
    */
   public static boolean aabbIntersection(final float[][] minMax, final float[][] minMax2) {
      if (minMax[0][0] >= minMax2[0][0] && minMax[1][0] <= minMax2[1][0] && minMax[0][1] >= minMax2[0][1] && minMax[1][1] <= minMax2[1][1]
            && minMax[0][2] >= minMax2[0][2] && minMax[1][2] <= minMax2[1][2]) {
         return true;
      }

      if (minMax2[1][0] < minMax[0][0] || minMax2[0][0] > minMax[1][0]) {
         return false;
      }
      if (minMax2[1][1] < minMax[0][1] || minMax2[0][1] > minMax[1][1]) {
         return false;
      }
      if (minMax2[1][2] < minMax[0][2] || minMax2[0][2] > minMax[1][2]) {
         return false;
      }

      return true;
   }

   public static float[][] getMinMax(final float[] point, final float range) {
      return new float[][] { { point[0] - range, point[1] - range, point[2] - range }, { point[0] + range, point[1] + range, point[2] + range } };
   }

   /**
    * http://en.wikipedia.org/wiki/Quicksort
      <pre>
      {@code
      function quicksort(array, left, right)
         var pivot, leftIdx = left, rightIdx = right
         if right - left > 0
             pivot = (left + right) / 2
             while leftIdx <= pivot and rightIdx >= pivot
                 while array[leftIdx] < array[pivot] and leftIdx <= pivot
                     leftIdx = leftIdx + 1
                 while array[rightIdx] > array[pivot] and rightIdx >= pivot
                     rightIdx = rightIdx - 1;
                 swap array[leftIdx] with array[rightIdx]
                 leftIdx = leftIdx + 1
                 rightIdx = rightIdx - 1
                 if leftIdx - 1 == pivot
                     pivot = rightIdx = rightIdx + 1
                 else if rightIdx + 1 == pivot
                     pivot = leftIdx = leftIdx - 1
             quicksort(array, left, pivot - 1)
             quicksort(array, pivot + 1, right)
      }
      </pre>
    */
   public static void quicksort(final Photon[] photons, final int[] indices, final int left, final int right, final SeparationAxis axis) {
      int pivot, leftIdx = left, rightIdx = right, temp;

      if (right - left > 0) {
         pivot = (left + right) / 2;

         while (leftIdx <= pivot && rightIdx >= pivot) {
            while (compare(photons[indices[leftIdx]], photons[indices[pivot]], axis) < 0 && leftIdx <= pivot) {
               leftIdx++;
            }
            while (compare(photons[indices[rightIdx]], photons[indices[pivot]], axis) > 0 && rightIdx >= pivot) {
               rightIdx--;
            }

            temp = indices[leftIdx];
            indices[leftIdx] = indices[rightIdx];
            indices[rightIdx] = temp;

            leftIdx++;
            rightIdx--;

            if (leftIdx - 1 == pivot) {
               rightIdx++;
               pivot = rightIdx;
            } else if (rightIdx + 1 == pivot) {
               leftIdx--;
               pivot = leftIdx;
            }
         }

         quicksort(photons, indices, left, pivot - 1, axis);
         quicksort(photons, indices, pivot + 1, right, axis);
      }
   }

   public static int compare(final Photon o1, final Photon o2, final SeparationAxis axis) {
      return o1.location[axis.pos] < o2.location[axis.pos] ? -1 : o2.location[axis.pos] < o1.location[axis.pos] ? 1 : 0;
   }

   /**
    * Returns a random vector within the hemisphere with the given normal as elevation = 90 degrees
    * 
    * http://www.gamedev.net/community/forums/viewreply.asp?ID=2823295
    * 
    * @param normal  The center of the hemisphere
    * @return        A random vector in the normal's hemisphere
    */
   public static Vector3f getVectorMarsagliaHemisphere(final Vector3f normal) {
      float a, b, l;

      do {
         a = 1.0f - 2.0f * (float) Math.random();
         b = 1.0f - 2.0f * (float) Math.random();
         l = a * a + b * b;
      } while (1 < l);

      final float s = (float) Math.sqrt(1.0f - l);

      final Vector3f random = new Vector3f(2.0f * a * s, 2.0f * b * s, 1.0f - 2.0f * l);
      random.normalize();

      final Vector3f tangent = PBRTMath.getNormalTangent(normal);
      tangent.normalize();

      return RTStatics.shadingCoordsToWorld(random, normal, tangent);
   }

   public static final Vector3f shadingCoordsToWorld(final Vector3f vec, final Vector3f shadingNormal, final Vector3f shadingX) {
      final Vector3f newVec = new Vector3f();
      final float yAxisX = shadingNormal.y * shadingX.z - shadingNormal.z * shadingX.y;
      final float yAxisY = shadingNormal.z * shadingX.x - shadingNormal.x * shadingX.z;
      final float yAxisZ = shadingNormal.x * shadingX.y - shadingNormal.y * shadingX.x;

      newVec.set(vec.x * shadingX.x + vec.y * yAxisX + vec.z * shadingNormal.x, vec.x * shadingX.y + vec.y * yAxisY + vec.z * shadingNormal.y, vec.x
            * shadingX.z + vec.y * yAxisZ + vec.z * shadingNormal.z);
      newVec.normalize();

      return newVec;
   }

   public static Vector3f offsetPosition(final Vector3f p, final Vector3f n) {
      final Vector3f smallNormal = new Vector3f(n);
      smallNormal.scale(RTStatics.EPSILON);
      final Vector3f intersection = new Vector3f(p);
      intersection.add(smallNormal);

      return intersection;
   }

   public static boolean shadowIntersects(final Light light, final BoundingVolume[] objects, final IntersectionInformation info, final int depth) {
      final Vector3f shadowRayDirection = new Vector3f();
      shadowRayDirection.sub(light.origin, info.intersection);
      shadowRayDirection.normalize();

      final Ray shadowRay = new Ray(RTStatics.offsetPosition(info.intersection, info.normal), shadowRayDirection);
      IntersectionInformation shadowInfo = null;

      for (final BoundingVolume object : objects) {
         shadowInfo = object.getChildIntersection(shadowRay, depth + 1);

         if (shadowInfo != null && shadowInfo.w > RTStatics.EPSILON) {
            final float lightDistance = RTStatics.getDistance(shadowInfo.intersection, light.origin);

            if (shadowInfo.w < lightDistance + RTStatics.EPSILON) {
               return true;
            }
         }
         //         }
      }

      return false;
   }
}
