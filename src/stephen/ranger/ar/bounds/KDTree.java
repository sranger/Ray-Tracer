package stephen.ranger.ar.bounds;

import java.awt.Color;
import java.util.Arrays;

import stephen.ranger.ar.ColorInformation;
import stephen.ranger.ar.IntersectionInformation;
import stephen.ranger.ar.RTStatics;
import stephen.ranger.ar.Ray;
import stephen.ranger.ar.RTStatics.SeparationAxis;
import stephen.ranger.ar.sceneObjects.TriangleMesh;

public class KDTree extends BoundingVolume {
   private final float[][] minMax = new float[2][];
   private final KDNode rootNode;
   private final float[][] vertices;
   private final float[][] normals;
   private final int[][] indices;
   private final TriangleMesh parentMesh;
   private float shadowDistance;

   private final ColorInformation colorInfo;

   public KDTree(final TriangleMesh parentMesh, final float[][] vertices, final float[][] normals, final int[][] indices, final ColorInformation colorInfo, final boolean computeKDTree) {
      this.parentMesh = parentMesh;
      this.vertices = vertices;
      this.normals = normals;
      this.indices = indices;
      this.colorInfo = colorInfo;
      shadowDistance = RTStatics.EPSILON;

      minMax[0] = new float[] { Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE };
      minMax[1] = new float[] { -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE };

      for (final float[] vertice : vertices) {
         minMax[0][0] = Math.min(vertice[0], minMax[0][0]);
         minMax[1][0] = Math.max(vertice[0], minMax[1][0]);

         minMax[0][1] = Math.min(vertice[1], minMax[0][1]);
         minMax[1][1] = Math.max(vertice[1], minMax[1][1]);

         minMax[0][2] = Math.min(vertice[2], minMax[0][2]);
         minMax[1][2] = Math.max(vertice[2], minMax[1][2]);
      }

      shadowDistance = Math.max(minMax[1][0] - minMax[0][0], Math.max(minMax[1][1] - minMax[0][1], minMax[1][2] - minMax[0][2])) / 2000f;

      System.out.println("creating KD Tree...");
      final long startTime = System.nanoTime();
      rootNode = new KDNode(parentMesh, this.vertices, this.normals, this.indices, minMax, SeparationAxis.X, 0, colorInfo, shadowDistance, computeKDTree);
      final long endTime = System.nanoTime();

      System.out.println("min/max: " + Arrays.toString(minMax[0]) + ", " + Arrays.toString(minMax[1]));

      System.out.println("KD Tree computation duration: " + (endTime - startTime) / 1000000000. + " seconds");
   }

   @Override
   public IntersectionInformation getChildIntersection(final Ray ray) {
      if (intersects(ray)) {
         return rootNode.getChildIntersection(ray);
      } else {
         return null;
      }
   }

   @Override
   public boolean intersects(final Ray ray) {
      return RTStatics.aabbIntersection(ray, getMinMax());
   }

   @Override
   public float[][] getMinMax() {
      return minMax;
   }

   @Override
   public Color getColor(final IntersectionInformation info) {
      return colorInfo.diffuse;
   }

   @Override
   public ColorInformation getColorInformation(final IntersectionInformation info) {
      return colorInfo;
   }
}
