package stephen.ranger.ar.bounds;

import java.util.Arrays;

import stephen.ranger.ar.Camera;
import stephen.ranger.ar.IntersectionInformation;
import stephen.ranger.ar.RTStatics;
import stephen.ranger.ar.RTStatics.SeparationAxis;
import stephen.ranger.ar.Ray;
import stephen.ranger.ar.materials.ColorInformation;
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
      this.shadowDistance = RTStatics.EPSILON;

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

      this.shadowDistance = Math.max(this.minMax[1][0] - this.minMax[0][0], Math.max(this.minMax[1][1] - this.minMax[0][1], this.minMax[1][2] - this.minMax[0][2])) / 2000f;

      System.out.println("creating KD Tree...");
      final long startTime = System.nanoTime();
      this.rootNode = new KDNode(parentMesh, this.vertices, this.normals, this.indices, this.minMax, SeparationAxis.X, 0, colorInfo, this.shadowDistance, computeKDTree);
      final long endTime = System.nanoTime();

      System.out.println("min/max: " + Arrays.toString(this.minMax[0]) + ", " + Arrays.toString(this.minMax[1]));

      System.out.println("KD Tree computation duration: " + (endTime - startTime) / 1000000000. + " seconds");
   }

   @Override
   public IntersectionInformation getChildIntersection(final Ray ray, final int depth) {
      if (this.intersects(ray)) {
         return this.rootNode.getChildIntersection(ray, depth);
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
   public float[] getColor(final IntersectionInformation info, final Camera camera, final int depth) {
      return this.colorInfo.getMaterialColor(camera, info, depth);
   }

   @Override
   public float[] getEmission() {
      return this.parentMesh.getEmission();
   }

   @Override
   public float[] getDiffuse() {
      return this.parentMesh.getDiffuse();
   }

   @Override
   public float[] getSpecular() {
      return this.parentMesh.getSpecular();
   }

   @Override
   public float[] getAmbient() {
      return this.parentMesh.getAmbient();
   }

   @Override
   public float getShininess() {
      return this.parentMesh.getShininess();
   }
}
