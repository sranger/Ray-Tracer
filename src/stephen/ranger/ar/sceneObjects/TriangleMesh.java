package stephen.ranger.ar.sceneObjects;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import javax.vecmath.Vector3d;

import stephen.ranger.ar.ColorInformation;
import stephen.ranger.ar.IntersectionInformation;
import stephen.ranger.ar.Ray;
import stephen.ranger.ar.bounds.BoundingVolume;
import stephen.ranger.ar.bounds.KDTree;

public class TriangleMesh extends SceneObject {
   private int numVertices, numFaces;
   private Vector3d[][] vertices;
   private BoundingVolume[] faces;

   public TriangleMesh(final File modelLocation, final ColorInformation colorInfo) {
      super(colorInfo);

      try {
         final BufferedReader reader = new BufferedReader(new FileReader(modelLocation));
         String temp = null;
         String[] split;
         int ctr = 0;
         boolean body = false;
         this.numVertices = 0;
         this.numFaces = 0;
         boolean hasNormals = false;
         int propertyCount = 0;
         int xpos = -1, ypos = -1, zpos = -1, nxpos = -1, nypos = -1, nzpos = -1;

         while (!body && ((temp = reader.readLine()) != null)) {
            if (temp.startsWith("element vertex")) {
               this.numVertices = Integer.parseInt(temp.split(" ")[2]);
               this.vertices = new Vector3d[this.numVertices][2];
            } else if (temp.startsWith("element face")) {
               this.numFaces = Integer.parseInt(temp.split(" ")[2]);
               this.faces = new BoundingVolume[this.numFaces];
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
               propertyCount++;
            } else if (temp.equals("end_header")) {
               body = true;
            }

            System.out.println(temp);
            ctr++;
         }
         System.out.println("position locations: " + xpos + ", " + ypos + ", " + zpos + "\nnormal locations: " + nxpos + ", " + nypos + ", " + nzpos);
         hasNormals = (nxpos != -1) && (nypos != -1) && (nzpos != -1);

         for (int i = 0; i < this.numVertices; i++) {
            split = reader.readLine().split(" ");
            final double x = Double.parseDouble(split[xpos]);
            final double y = Double.parseDouble(split[ypos]);
            final double z = -Double.parseDouble(split[zpos]);
            double nx = 0, ny = 0, nz = 0;

            if (hasNormals) {
               nx = Double.parseDouble(split[nxpos]);
               ny = Double.parseDouble(split[nypos]);
               nz = -Double.parseDouble(split[nzpos]);
            }

            this.vertices[i][0] = new Vector3d(x, y, z);
            this.vertices[i][1] = hasNormals ? new Vector3d(nx, ny, nz) : null;

            if (hasNormals) {
               this.vertices[i][1].normalize();
            }
         }

         ctr = 0;

         while ((temp = reader.readLine()) != null) {
            split = temp.split(" ");
            final Vector3d[] v0 = this.vertices[Integer.parseInt(split[1])];
            final Vector3d[] v1 = this.vertices[Integer.parseInt(split[2])];
            final Vector3d[] v2 = this.vertices[Integer.parseInt(split[3])];

            final Vector3d p0 = new Vector3d(v0[0]);
            final Vector3d p1 = new Vector3d(v1[0]);
            final Vector3d p2 = new Vector3d(v2[0]);

            Vector3d n0 = null, n1 = null, n2 = null;

            if (hasNormals) {
               n0 = new Vector3d(v0[1]);
               n1 = new Vector3d(v1[1]);
               n2 = new Vector3d(v2[1]);
            }

            if ((p0 != null) && (p1 != null) && (p2 != null)) {
               this.faces[ctr] = new Triangle(new Vector3d[] { p0, n0 }, new Vector3d[] { p2, n2 }, new Vector3d[] {
                     p1, n1 }, colorInfo).getBoundingVolume();
            } else {
               System.out.println("null");
               this.faces[ctr] = null;
            }

            ctr++;
         }

         this.setBoundingVolume(new KDTree(this.faces));
      } catch (final Exception e) {
         e.printStackTrace();
         System.exit(1);
      }
   }

   @Override
   public IntersectionInformation getIntersection(final Ray ray) {
      if (this.boundingVolume.intersects(ray)) {
         return this.boundingVolume.getChildIntersection(ray);
      }

      return null;
   }

   @Override
   public Color getColor(final IntersectionInformation info) {
      return this.colorInfo.emission;
   }
}
