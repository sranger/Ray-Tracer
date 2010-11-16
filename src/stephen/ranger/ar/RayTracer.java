package stephen.ranger.ar;

import java.awt.Color;
import java.io.File;

import javax.vecmath.Vector3f;

import stephen.ranger.ar.bounds.BoundingVolume;
import stephen.ranger.ar.lighting.GlobalIlluminationLightingModel;
import stephen.ranger.ar.lighting.Light;
import stephen.ranger.ar.lighting.LightingModel;
import stephen.ranger.ar.lighting.PhongLightingModel;
import stephen.ranger.ar.materials.BRDFMaterial;
import stephen.ranger.ar.materials.CheckerboardMaterial;
import stephen.ranger.ar.materials.ColorInformation;
import stephen.ranger.ar.materials.ReflectionMaterial;
import stephen.ranger.ar.materials.RefractionMaterial;
import stephen.ranger.ar.sceneObjects.Plane;
import stephen.ranger.ar.sceneObjects.Sphere;
import stephen.ranger.ar.sceneObjects.TriangleMesh;

public class RayTracer {
   public static String baseDir = "D:/";

   public static enum Scenes {
      CORNELL_BOX_SPHERES("Cornell Box (spheres)"), CORNELL_BOX("Cornell Box"), CORNELL_BOX_PHONG("Cornell Box (Phong)"), WHITTED_SCENE("Whitted Scene"), WHITTED_SCENE_BRDF(
            "Whitted Scene (BRDF)"), STANFORD_BUNNY("Stanford Bunny"), STANFORD_DRAGON("Stanford Dragon"), STANFORD_BUDDHA("Stanford Buddha"), XYZ_DRAGON(
            "XYZ RGB Dragon"), XYZ_THAI_STATUE("XYZ RGB Thai Statue"), STANFORD_LUCY("Stanford Lucy");

      public final String title;
      private Scene scene = null;

      private Scenes(final String title) {
         this.title = title;
      }

      public Scene getScene(final boolean useKDTree) {
         if (scene == null) {
            scene = RayTracer.getScene(this, useKDTree);
         }

         return scene;
      }

      @Override
      public String toString() {
         return title;
      }

      public static void resetKDTreeMeshes() {
         Scenes.STANFORD_BUNNY.scene = null;
         Scenes.STANFORD_DRAGON.scene = null;
         Scenes.STANFORD_BUDDHA.scene = null;
         Scenes.STANFORD_LUCY.scene = null;
         Scenes.XYZ_DRAGON.scene = null;
         Scenes.XYZ_THAI_STATUE.scene = null;
      }
   }

   public Scene currentScene = null;

   public Camera camera = null;

   public RayTracer(final String baseDir, final int width, final int height, final int x, final int y, final int imageWidth, final int imageHeight) {
      RayTracer.baseDir = baseDir;
      new RayTracerInterface(this, width, height, x, y, imageWidth, imageHeight);
   }

   public static void main(final String[] args) {
      if (args != null && args.length > 0 && !args[0].equals("")) {
         final File baseDir = new File(args[0]);
         int width = 1500;
         int height = 1000;
         int x = 100;
         int y = 100;
         int imageWidth = 512;
         int imageHeight = 512;

         if (baseDir.exists() && baseDir.isDirectory()) {
            if (args.length >= 3) {
               width = Integer.parseInt(args[1]);
               height = Integer.parseInt(args[2]);

               if (args.length >= 5) {
                  x = Integer.parseInt(args[3]);
                  y = Integer.parseInt(args[4]);

                  if (args.length >= 7) {
                     imageWidth = Integer.parseInt(args[5]);
                     imageHeight = Integer.parseInt(args[6]);
                  }
               }
            }
            new RayTracer(args[0], width, height, x, y, imageWidth, imageHeight);
         } else {
            System.err.println(baseDir.getAbsolutePath() + " must be a directory.");
         }
      } else {
         System.err.println("The directory where the Stanford models reside must be set.");
      }
   }

   private static final Scene getScene(final Scenes scene, final boolean useKDTree) {
      System.out.println("\n\n----------------------------------------\nRendering " + scene.title + "\n----------------------------------------\n\n");

      final Light light = new Light(new Vector3f(0, 100, 100), new Color(0.3f, 0.3f, 0.3f, 1.0f), new Color(0.5f, 0.5f, 0.9f, 1.0f));
      final Light light2 = new Light(new Vector3f(0, 100, -100), new Color(0.3f, 0.3f, 0.3f, 1.0f), new Color(0.5f, 0.5f, 0.9f, 1.0f));
      // final Light cornellLight = new Light(new Vector3f(278, 540, 0.5f), new Color(1f, 0.85f, 0.43f), new Color(1f, 0.85f, 0.43f));
      final Light cornellLight = new Light(new Vector3f(0, 270, 0), new Color(0.75f, 0.75f, 0.75f), new Color(0.75f, 0.75f, 0.75f));

      if (scene.equals(Scenes.WHITTED_SCENE)) {
         final BoundingVolume[] volumes = RayTracer.getWhittedObjects(false);
         return new Scene(volumes, light, new float[] { 0, 0, 0 }, new PhongLightingModel(light, volumes), 35f);
      } else if (scene.equals(Scenes.WHITTED_SCENE_BRDF)) {
         final BoundingVolume[] volumes = RayTracer.getWhittedObjects(true);
         return new Scene(volumes, light, new float[] { 0, 0, 0 }, new LightingModel(), 35f);
      } else if (scene.equals(Scenes.STANFORD_BUNNY)) {
         final BoundingVolume[] volumes = new BoundingVolume[] { new TriangleMesh(new File(baseDir + "models/bunny/reconstruction/bun_zipper.ply"),
               new ColorInformation(Color.white), useKDTree).getBoundingVolume() };
         return new Scene(volumes, light, new float[] { 0, 0, 0 }, new PhongLightingModel(light, volumes), 15f);
      } else if (scene.equals(Scenes.STANFORD_DRAGON)) {
         final BoundingVolume[] volumes = new BoundingVolume[] { new TriangleMesh(new File(baseDir + "models/dragon_recon/dragon_vrip.ply"),
               new ColorInformation(new Color(0.9f, 0.9f, 0.9f, 1f)), useKDTree).getBoundingVolume() };
         return new Scene(volumes, light, new float[] { 0, 0, 0 }, new PhongLightingModel(light, volumes), 23f);
      } else if (scene.equals(Scenes.STANFORD_BUDDHA)) {
         final BoundingVolume[] volumes = new BoundingVolume[] { new TriangleMesh(new File(baseDir + "models/happy_recon/happy_vrip.ply"),
               new ColorInformation(Color.white), useKDTree).getBoundingVolume() };
         return new Scene(volumes, light, new float[] { 180, 0, 0 }, new PhongLightingModel(light, volumes), 10f);
      } else if (scene.equals(Scenes.STANFORD_LUCY)) {
         final BoundingVolume[] volumes = new BoundingVolume[] { new TriangleMesh(new File(baseDir + "models/lucy.ply"), new ColorInformation(Color.white),
               useKDTree).getBoundingVolume() };
         return new Scene(volumes, light, new float[] { 180, 0, 0 }, new PhongLightingModel(light, volumes), 10f);
      } else if (scene.equals(Scenes.XYZ_DRAGON)) {
         final BoundingVolume[] volumes = new BoundingVolume[] { new TriangleMesh(new File(baseDir + "models/xyzrgb_dragon.ply"), new ColorInformation(
               Color.white), useKDTree).getBoundingVolume() };
         return new Scene(volumes, light2, new float[] { 220, 0, 0 }, new PhongLightingModel(light2, volumes), 20f);
      } else if (scene.equals(Scenes.XYZ_THAI_STATUE)) {
         final BoundingVolume[] volumes = new BoundingVolume[] { new TriangleMesh(new File(baseDir + "models/xyzrgb_statuette.ply"), new ColorInformation(
               Color.white), useKDTree).getBoundingVolume() };
         return new Scene(volumes, light, new float[] { 0, 0, 0 }, new PhongLightingModel(light, volumes), 10f);
      } else if (scene.equals(Scenes.CORNELL_BOX)) {
         final BoundingVolume[] volumes = RayTracer.getCornellBox(false, false);
         return new Scene(volumes, cornellLight, new float[] { 180, 0, 0 }, new GlobalIlluminationLightingModel(), 15f);
      } else if (scene.equals(Scenes.CORNELL_BOX_SPHERES)) {
         final BoundingVolume[] volumes = RayTracer.getCornellBox(true, false);
         return new Scene(volumes, cornellLight, new float[] { 180, 0, 0 }, new GlobalIlluminationLightingModel(), 15f);
      } else if (scene.equals(Scenes.CORNELL_BOX_PHONG)) {
         final BoundingVolume[] volumes = RayTracer.getCornellBox(true, true);
         return new Scene(volumes, cornellLight, new float[] { 180, 0, 0 }, new PhongLightingModel(cornellLight, volumes), 15f);
      }

      return null;
   }

   private static final BoundingVolume[] getWhittedObjects(final boolean useBRDFs) {
      final Plane plane = new Plane(new Vector3f[] { new Vector3f(-50, 0, -100), new Vector3f(-50, -40, 25), new Vector3f(50, -40, 25),
            new Vector3f(50, 0, -100) }, new CheckerboardMaterial(Color.yellow, Color.red, 10f, 10f, 10f));

      final ReflectionMaterial blue = new ReflectionMaterial(Color.blue);
      final RefractionMaterial glass = new RefractionMaterial(Color.gray, RefractionMaterial.INDEX_OF_GLASS);

      final Sphere sphere1 = new Sphere(5, new Vector3f(0, -12, 0), useBRDFs ? new BRDFMaterial(15, Color.green) : glass);// new ColorInformation(Color.blue));
      final Sphere sphere2 = new Sphere(3, new Vector3f(5, -15, -10), useBRDFs ? new BRDFMaterial(16, Color.cyan) : blue);// new ColorInformation(Color.green));

      return new BoundingVolume[] { plane.getBoundingVolume(), sphere1.getBoundingVolume(), sphere2.getBoundingVolume() };

   }

   /**
    * http://www.cs.uiowa.edu/~cwyman/classes/spring07-22C251/code/cornellBoxScene.txt
    * 
    * http://www.graphics.cornell.edu/online/box/data.html
    * 
    * @return
    */
   private static final BoundingVolume[] getCornellBox(final boolean useSpheres, final boolean useWhittedMaterials) {
      // final ColorInformation white = new ColorInformation(new Color(0.76f, 0.75f, 0.5f), 0);
      final ColorInformation white = new ColorInformation(new Color(0.75f, 0.75f, 0.75f));
      final ColorInformation white2 = new ColorInformation(new Color(0.65f, 0.65f, 0.65f));
      final ColorInformation red = new ColorInformation(new Color(0.63f, 0.06f, 0.04f));
      final ColorInformation green = new ColorInformation(new Color(0.15f, 0.48f, 0.09f));
      final ColorInformation blue = new ColorInformation(new Color(0.392f, 0.584f, 0.93f));
      final ReflectionMaterial mirror = new ReflectionMaterial(Color.white);
      final RefractionMaterial glass = new RefractionMaterial(Color.blue, RefractionMaterial.INDEX_OF_GLASS);

      final float[][] box = new float[][] { { -278, -275, -800 }, { 278, 275, 280 } };

      final Plane floor = new Plane(new Vector3f[] { new Vector3f(box[1][0], box[0][1], box[0][2]), new Vector3f(box[0][0], box[0][1], box[0][2]),
            new Vector3f(box[0][0], box[0][1], box[1][2]), new Vector3f(box[1][0], box[0][1], box[1][2]) }, white);
      final Plane ceiling = new Plane(new Vector3f[] { new Vector3f(box[1][0], box[1][1], box[0][2]), new Vector3f(box[1][0], box[1][1], box[1][2]),
            new Vector3f(box[0][0], box[1][1], box[1][2]), new Vector3f(box[0][0], box[1][1], box[0][2]) }, white);
      final Plane back = new Plane(new Vector3f[] { new Vector3f(box[1][0], box[0][1], box[1][2]), new Vector3f(box[0][0], box[0][1], box[1][2]),
            new Vector3f(box[0][0], box[1][1], box[1][2]), new Vector3f(box[1][0], box[1][1], box[1][2]) }, white);
      final Plane front = new Plane(new Vector3f[] { new Vector3f(box[1][0], box[0][1], box[0][2]), new Vector3f(box[0][0], box[0][1], box[0][2]),
            new Vector3f(box[0][0], box[1][1], box[0][2]), new Vector3f(box[1][0], box[1][1], box[0][2]) }, white);
      final Plane left = new Plane(new Vector3f[] { new Vector3f(box[1][0], box[0][1], box[0][2]), new Vector3f(box[1][0], box[0][1], box[1][2]),
            new Vector3f(box[1][0], box[1][1], box[1][2]), new Vector3f(box[1][0], box[1][1], box[0][2]) }, red);
      final Plane right = new Plane(new Vector3f[] { new Vector3f(box[0][0], box[0][1], box[1][2]), new Vector3f(box[0][0], box[0][1], box[0][2]),
            new Vector3f(box[0][0], box[1][1], box[0][2]), new Vector3f(box[0][0], box[1][1], box[1][2]) }, green);

      if (useSpheres) {
         final Sphere s1 = new Sphere(82.5f, new Vector3f(-92f, -192.5f, -111.5f), useWhittedMaterials ? glass : blue);
         final Sphere s2 = new Sphere(82.5f, new Vector3f(116.5f, -192.5f, 71.5f), useWhittedMaterials ? mirror : blue);

         return new BoundingVolume[] { floor.getBoundingVolume(), ceiling.getBoundingVolume(), back.getBoundingVolume(), front.getBoundingVolume(),
               left.getBoundingVolume(), right.getBoundingVolume(), s1.getBoundingVolume(), s2.getBoundingVolume() };
      } else {
         final Plane small1 = new Plane(new Vector3f[] { new Vector3f(-148, -110, -215), new Vector3f(-196, -110, -55), new Vector3f(-32, -110, -8),
               new Vector3f(12, -110, -166) }, white2);
         final Plane small2 = new Plane(new Vector3f[] { new Vector3f(12, -275, -166), new Vector3f(12, -110, -166), new Vector3f(-32, -110, -8),
               new Vector3f(-32, -275, -8) }, white2);
         final Plane small3 = new Plane(new Vector3f[] { new Vector3f(-148, -275, -215), new Vector3f(-148, -110, -215), new Vector3f(12, -110, -166),
               new Vector3f(12, -275, -166) }, white2);
         final Plane small4 = new Plane(new Vector3f[] { new Vector3f(-196, -275, -55), new Vector3f(-196, -110, -55), new Vector3f(-148, -110, -215),
               new Vector3f(-148, -275, -215) }, white2);
         final Plane small5 = new Plane(new Vector3f[] { new Vector3f(-32, -275, -8), new Vector3f(-32, -110, -8), new Vector3f(-196, -110, -55),
               new Vector3f(-196, -275, -55) }, white2);

         final Plane large1 = new Plane(new Vector3f[] { new Vector3f(145, 55, -33), new Vector3f(-13, 55, 16), new Vector3f(36, 55, 176),
               new Vector3f(194, 55, 126) }, white2);
         final Plane large2 = new Plane(new Vector3f[] { new Vector3f(145, -275, -33), new Vector3f(145, 55, -33), new Vector3f(194, 55, 126),
               new Vector3f(194, -275, 126) }, white2);
         final Plane large3 = new Plane(new Vector3f[] { new Vector3f(194, -275, 126), new Vector3f(194, 55, 126), new Vector3f(36, 55, 176),
               new Vector3f(36, -275, 176) }, white2);
         final Plane large4 = new Plane(new Vector3f[] { new Vector3f(36, -275, 176), new Vector3f(36, 55, 176), new Vector3f(-13, 55, 16),
               new Vector3f(-13, -275, 16) }, white2);
         final Plane large5 = new Plane(new Vector3f[] { new Vector3f(-13, -275, 16), new Vector3f(-13, 55, 16), new Vector3f(145, 55, -33),
               new Vector3f(145, -275, -33) }, white2);

         return new BoundingVolume[] { floor.getBoundingVolume(), ceiling.getBoundingVolume(), back.getBoundingVolume(), front.getBoundingVolume(),
               left.getBoundingVolume(), right.getBoundingVolume(), small1.getBoundingVolume(), small2.getBoundingVolume(), small3.getBoundingVolume(),
               small4.getBoundingVolume(), small5.getBoundingVolume(), large1.getBoundingVolume(), large2.getBoundingVolume(), large3.getBoundingVolume(),
               large4.getBoundingVolume(), large5.getBoundingVolume() };
      }
   }
}
