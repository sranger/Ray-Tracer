package stephen.ranger.ar.bounds;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Vector3f;

import stephen.ranger.ar.Camera;
import stephen.ranger.ar.IntersectionInformation;
import stephen.ranger.ar.RTStatics;
import stephen.ranger.ar.Ray;
import stephen.ranger.ar.RTStatics.SeparationAxis;
import stephen.ranger.ar.materials.ColorInformation;
import stephen.ranger.ar.sceneObjects.Triangle;
import stephen.ranger.ar.sceneObjects.TriangleMesh;

public class KDNode extends BoundingVolume {
   public KDNode left;
   public KDNode right;
   public final SeparationAxis axis;
   public final float[][] minMax;
   public final int depth;
   private final float[][] vertices;
   private final float[][] normals;
   private final int[][] indices;
   private final TriangleMesh parentMesh;
   private final float shadowDistance;

   public KDNode(final TriangleMesh parentMesh, final float[][] vertices, final float[][] normals, final int[][] indices, final float[][] minMax, final SeparationAxis axis, final int depth,
         final ColorInformation colorInfo, final float shadowDistance, final boolean computeKDTree) {
      this.axis = axis;
      this.minMax = minMax;
      this.depth = depth;
      this.vertices = vertices;
      this.normals = normals;
      this.indices = indices;
      this.parentMesh = parentMesh;
      this.shadowDistance = shadowDistance;

      if (computeKDTree && depth < RTStatics.MAX_DEPTH && indices.length > RTStatics.MAX_CHILDREN) {
         float median;
         final List<int[]> leftChildren = new ArrayList<int[]>();
         final List<int[]> rightChildren = new ArrayList<int[]>();

         if (axis.equals(SeparationAxis.X)) {
            median = (this.minMax[1][0] - this.minMax[0][0]) / 2f + this.minMax[0][0];
         } else if (axis.equals(SeparationAxis.Y)) {
            median = (this.minMax[1][1] - this.minMax[0][1]) / 2f + this.minMax[0][1];
         } else {
            median = (this.minMax[1][2] - this.minMax[0][2]) / 2f + this.minMax[0][2];
         }

         final float[][] leftMinMax = new float[][] { { Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE }, { -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE } };
         final float[][] rightMinMax = new float[][] { { Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE }, { -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE } };

         final float[][] childMinMax = new float[2][3];

         for (final int[] face : indices) {
            RTStatics.getMinMax(vertices, face, childMinMax);

            if (childMinMax[1][axis.pos] <= median) {
               leftChildren.add(face);
            } else if (childMinMax[0][axis.pos] >= median) {
               rightChildren.add(face);
            } else {
               leftChildren.add(face);
               rightChildren.add(face);
            }
         }

         if (leftChildren.size() > 0) {
            final int[][] leftChildrenFaces = leftChildren.toArray(new int[leftChildren.size()][3]);
            RTStatics.getMinMax(vertices, leftChildrenFaces, leftMinMax);
            left = new KDNode(parentMesh, vertices, normals, leftChildrenFaces, leftMinMax, axis.getNextAxis(), depth + 1, colorInfo, shadowDistance, computeKDTree);
         } else {
            left = null;
         }

         if (rightChildren.size() > 0) {
            final int[][] rightChildrenFaces = rightChildren.toArray(new int[rightChildren.size()][3]);
            RTStatics.getMinMax(vertices, rightChildrenFaces, rightMinMax);
            right = new KDNode(parentMesh, vertices, normals, rightChildrenFaces, rightMinMax, axis.getNextAxis(), depth + 1, colorInfo, shadowDistance, computeKDTree);
         } else {
            right = null;
         }
      } else {
         left = null;
         right = null;
      }
   }

   @Override
   public IntersectionInformation getChildIntersection(final Ray ray, final int depth) {
      if (left != null || right != null) {
         IntersectionInformation tempLeft = null, tempRight = null;

         if (left != null && left.intersects(ray)) {
            tempLeft = left.getChildIntersection(ray, depth);
         }

         if (right != null && right.intersects(ray)) {
            tempRight = right.getChildIntersection(ray, depth);
         }

         return tempLeft == null && tempRight == null ? null : tempLeft == null ? tempRight : tempRight == null ? tempLeft : tempLeft.w < tempRight.w ? tempLeft : tempRight;
      } else {
         float[] temp;
         float[] closest = null;

         for (final int[] face : indices) {
            if (RTStatics.aabbIntersection(ray, minMax)) {
               temp = Triangle.intersectsTriangle(ray.origin, ray.direction, vertices, normals, face);

               if (temp != null && temp[6] > shadowDistance && (closest == null || temp[6] < closest[6])) {
                  closest = temp;
               }
            }
         }

         return closest == null ? null : new IntersectionInformation(ray, parentMesh.getBoundingVolume(), new Vector3f(closest[0], closest[1], closest[2]), new Vector3f(closest[3], closest[4],
               closest[5]), closest[6]);
      }
   }

   @Override
   public float[][] getMinMax() {
      return minMax;
   }

   @Override
   public boolean intersects(final Ray ray) {
      return RTStatics.aabbIntersection(ray, minMax);
   }

   @Override
   public float[] getColor(final IntersectionInformation info, final Camera camera, final int depth) {
      return parentMesh.getColor(info, camera, depth);
   }

   @Override
   public float[] getEmission() {
      return parentMesh.getEmission();
   }

   @Override
   public float[] getDiffuse() {
      return parentMesh.getDiffuse();
   }

   @Override
   public float[] getSpecular() {
      return parentMesh.getSpecular();
   }

   @Override
   public float[] getAmbient() {
      return parentMesh.getAmbient();
   }

   @Override
   public float getShininess() {
      return parentMesh.getShininess();
   }
}
