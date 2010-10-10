package stephen.ranger.ar.bounds;

import java.awt.Color;
import java.util.Arrays;

import stephen.ranger.ar.ColorInformation;
import stephen.ranger.ar.IntersectionInformation;
import stephen.ranger.ar.RTStatics;
import stephen.ranger.ar.Ray;
import stephen.ranger.ar.sceneObjects.TriangleMesh;

public class KDTree extends BoundingVolume {

   public static final int MAX_DEPTH = 20;

   public static enum SeparationAxis {
      X(0), Y(1), Z(2);

      public final int pos;

      private SeparationAxis(final int pos) {
         this.pos = pos;
      }

      public SeparationAxis getNextAxis() {
         return this.equals(X) ? Y : this.equals(Y) ? Z : X;
      }
   }

   private final float[][] minMax = new float[2][];
   private final KDNode rootNode;
   private final float[][] vertices;
   private final float[][] normals;
   private final int[][] indices;
   private final TriangleMesh parentMesh;

   private final ColorInformation colorInfo;

   public KDTree(final TriangleMesh parentMesh, final float[][] vertices, final float[][] normals, final int[][] indices, final ColorInformation colorInfo) {
      this.parentMesh = parentMesh;
      this.vertices = vertices;
      this.normals = normals;
      this.indices = indices;
      this.colorInfo = colorInfo;

      this.minMax[0] = new float[] { Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE };
      this.minMax[1] = new float[] { -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE };

      for (final float[] vertice : vertices) {
         this.minMax[0][0] = Math.min(vertice[0], this.minMax[0][0]);
         this.minMax[1][0] = Math.max(vertice[0], this.minMax[1][0]);

         this.minMax[0][1] = Math.min(vertice[1], this.minMax[0][1]);
         this.minMax[1][1] = Math.max(vertice[1], this.minMax[1][1]);

         this.minMax[0][2] = Math.min(vertice[2], this.minMax[0][2]);
         this.minMax[1][2] = Math.max(vertice[2], this.minMax[1][2]);
      }

      System.out.println("creating KD Tree...");
      final long startTime = System.nanoTime();
      this.rootNode = new KDNode(parentMesh, this.vertices, this.normals, this.indices, this.minMax, SeparationAxis.X, 0, colorInfo);
      final long endTime = System.nanoTime();

      System.out.println("min/max: " + Arrays.toString(this.minMax[0]) + ", " + Arrays.toString(this.minMax[1]));

      System.out.println("KD Tree computation duration: " + (endTime - startTime) / 1000000000. + " seconds");
   }

   @Override
   public IntersectionInformation getChildIntersection(final Ray ray) {
      if (this.intersects(ray)) {
         return this.rootNode.getChildIntersection(ray);
      } else {
         return null;
      }
   }

   @Override
   public boolean intersects(final Ray ray) {
      return RTStatics.aabbIntersection(ray, this.getMinMax());
   }

   @Override
   public float[][] getMinMax() {
      return this.minMax;
   }

   @Override
   public Color getColor(final IntersectionInformation info) {
      return this.colorInfo.emission;
   }

   @Override
   public ColorInformation getColorInformation(final IntersectionInformation info) {
      return this.colorInfo;
   }
}
