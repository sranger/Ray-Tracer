package stephen.ranger.ar.sceneObjects;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;

import stephen.ranger.ar.ColorInformation;
import stephen.ranger.ar.IntersectionInformation;
import stephen.ranger.ar.RTStatics;
import stephen.ranger.ar.Ray;
import stephen.ranger.ar.bounds.KDTree;

public class TriangleMesh extends SceneObject {
   private int numVertices, numFaces;
   private float[][] vertices;
   private float[][] normals;
   private int[][] indices;

   boolean notDone = true;

   public TriangleMesh(final File modelLocation, final ColorInformation colorInfo, final boolean computeKDTree) {
      super(colorInfo);

      final long startTime = System.nanoTime();

      try {
         final BufferedReader reader = new BufferedReader(new FileReader(modelLocation));
         String temp = null;
         int ctr = 0;
         boolean body = false;
         numVertices = 0;
         numFaces = 0;
         boolean hasNormals = false;
         boolean isASCII = true;
         int propertyCount = 0;
         int xpos = -1, ypos = -1, zpos = -1, nxpos = -1, nypos = -1, nzpos = -1;

         while (!body && (temp = reader.readLine()) != null) {
            if (temp.startsWith("element vertex")) {
               numVertices = Integer.parseInt(temp.split(" ")[2]);
               vertices = new float[numVertices][3];
               normals = new float[numVertices][3];
            } else if (temp.startsWith("element face")) {
               numFaces = Integer.parseInt(temp.split(" ")[2]);
               indices = new int[numFaces][3];
            } else if (temp.startsWith("property")) {
               if (temp.endsWith(" nx")) {
                  nxpos = propertyCount;
               } else if (temp.endsWith(" ny")) {
                  nypos = propertyCount;
               } else if (temp.endsWith(" nz")) {
                  nzpos = propertyCount;
               } else if (temp.endsWith(" x")) {
                  xpos = propertyCount;
               } else if (temp.endsWith(" y")) {
                  ypos = propertyCount;
               } else if (temp.endsWith(" z")) {
                  zpos = propertyCount;
               }

               if (!temp.startsWith("property list")) {
                  propertyCount++;
               }
            } else if (temp.startsWith("format binary")) {
               isASCII = false;
            } else if (temp.equals("end_header")) {
               body = true;
            }

            System.out.println(temp);
            ctr++;
         }
         System.out.println("position locations: " + xpos + ", " + ypos + ", " + zpos + "\nnormal locations: " + nxpos + ", " + nypos + ", " + nzpos);
         hasNormals = nxpos != -1 && nypos != -1 && nzpos != -1;

         if (isASCII) {
            readASCII(reader, new int[] { xpos, ypos, zpos }, new int[] { nxpos, nypos, nzpos }, hasNormals);
         } else {
            readBinary(modelLocation, propertyCount, new int[] { xpos, ypos, zpos }, new int[] { nxpos, nypos, nzpos }, hasNormals);
         }

         final long endTime = System.nanoTime();
         System.out.println("model parsed in " + (endTime - startTime) / 1000000000. + " seconds");

         computeNormals(hasNormals);

         setBoundingVolume(new KDTree(TriangleMesh.this, vertices, normals, indices, colorInfo, computeKDTree));
      } catch (final Exception e) {
         e.printStackTrace();
         System.exit(1);
      }
   }

   private void computeNormals(final boolean hasNormals) {
      if (!hasNormals) {
         final long startTime = System.nanoTime();
         float[] normal = new float[3];

         for (final int[] face : indices) {
            normal = RTStatics.computeNormal(vertices, face);

            for (final int i : face) {
               normals[i][0] += normal[0];
               normals[i][1] += normal[1];
               normals[i][2] += normal[2];
            }
         }

         final float[] zero = new float[] { 0, 0, 0 };
         float length = 0;

         // average and normalize
         for (int i = 0; i < normals.length; i++) {
            length = RTStatics.getDistance(zero, normals[i]);

            normals[i][0] /= length;
            normals[i][1] /= length;
            normals[i][2] /= length;
         }

         final long endTime = System.nanoTime();
         System.out.println("computed normals in: " + (endTime - startTime) / 1000000000. + " seconds");
      }
   }

   private void readASCII(final BufferedReader reader, final int[] pos, final int[] normal, final boolean hasNormals) throws IOException {
      String[] split;
      String temp;

      for (int i = 0; i < numVertices; i++) {
         split = reader.readLine().split(" ");

         vertices[i][0] = Float.parseFloat(split[pos[0]]);
         vertices[i][1] = Float.parseFloat(split[pos[1]]);
         vertices[i][2] = Float.parseFloat(split[pos[2]]);

         normals[i][0] = hasNormals ? Float.parseFloat(split[normal[0]]) : 0;
         normals[i][1] = hasNormals ? Float.parseFloat(split[normal[1]]) : 0;
         normals[i][2] = hasNormals ? Float.parseFloat(split[normal[2]]) : 0;
      }

      int ctr = 0;

      while ((temp = reader.readLine()) != null) {
         split = temp.split(" ");
         indices[ctr][0] = Integer.parseInt(split[1]);
         indices[ctr][1] = Integer.parseInt(split[2]);
         indices[ctr][2] = Integer.parseInt(split[3]);
         ctr++;
      }
   }

   private void readBinary(final File modelLocation, final int propertyCount, final int[] pos, final int[] normal, final boolean hasNormals) throws IOException {
      final DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(modelLocation)));
      StringBuilder builder = new StringBuilder();
      String temp = "";
      byte c = 0;

      while (!temp.startsWith("end_header")) {
         while ((c = (byte) dis.read()) != '\n') {
            builder.append((char) c);
         }

         temp = builder.toString();
         builder = new StringBuilder();
      }

      System.out.println("property count: " + propertyCount);
      final float[] properties = new float[propertyCount];

      for (int i = 0; i < numVertices; i++) {
         for (int j = 0; j < propertyCount; j++) {
            properties[j] = dis.readFloat();
         }

         vertices[i][0] = properties[pos[0]];
         vertices[i][1] = properties[pos[1]];
         vertices[i][2] = properties[pos[2]];

         normals[i][0] = hasNormals ? properties[normal[0]] : 0;
         normals[i][1] = hasNormals ? properties[normal[1]] : 0;
         normals[i][2] = hasNormals ? properties[normal[2]] : 0;
      }

      System.out.println("vertices read: " + numVertices);

      for (int i = 0; i < numFaces; i++) {
         dis.read(); // only supporting triangles
         indices[i][0] = dis.readInt();
         indices[i][1] = dis.readInt();
         indices[i][2] = dis.readInt();
      }

      System.out.println("faces read: " + numFaces);
   }

   @Override
   public IntersectionInformation getIntersection(final Ray ray) {
      if (boundingVolume.intersects(ray)) {
         return boundingVolume.getChildIntersection(ray);
      }

      return null;
   }
}
