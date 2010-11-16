package stephen.ranger.ar.materials;

import java.awt.Color;

import javax.vecmath.Vector3f;

import stephen.ranger.ar.Camera;
import stephen.ranger.ar.IntersectionInformation;
import stephen.ranger.ar.RTStatics;

public class RefractionMaterial extends ColorInformation {
   public static final float INDEX_OF_AIR = 1.00029f;
   public static final float INDEX_OF_GLASS = 1.52f;
   public static final float INDEX_OF_ICE = 1.31f;
   public static final float INDEX_OF_WATER = 1.33f;
   public static final float INDEX_OF_QUARTZ = 1.46f;
   public static final float INDEX_OF_SAPPHIRE = 1.77f;
   public static final float INDEX_OF_DIAMOND = 2.417f;

   public final float aetherIndex = INDEX_OF_AIR;
   public final float refractionIndex;

   public RefractionMaterial(final Color diffuse, final float refractionIndex) {
      super(diffuse, 100);

      this.refractionIndex = refractionIndex;
   }

   @Override
   public float[] getMaterialColor(final Camera camera, final IntersectionInformation info, final int depth) {
      if (info == null) {
         return camera.light.ambient.getColorComponents(new float[3]);
      } else {
         final Vector3f refractionDirection = getRefractionDirection(info);
         final IntersectionInformation closest = refractionDirection == null ? null : camera.getClosestIntersection(info.intersectionObject, info.intersection,
               refractionDirection, info.normal, depth + 1);

         if (closest == null) {
            return camera.light.ambient.getColorComponents(new float[3]);
         } else {
            final float[] returnColor = closest.intersectionObject.getColor(closest, camera, depth + 1);

            if (info.normal.dot(info.ray.direction) <= 0f) {
               final float distance = RTStatics.getDistance(info.intersection, closest.intersection);
               final float[] color = info.intersectionObject.getDiffuse();
               color[0] *= 0.15f * -distance;
               color[1] *= 0.15f * -distance;
               color[2] *= 0.15f * -distance;

               returnColor[0] += Math.exp(color[0]);
               returnColor[1] += Math.exp(color[1]);
               returnColor[2] += Math.exp(color[2]);
            }

            return returnColor;
         }
      }
   }

   private Vector3f getRefractionDirection(final IntersectionInformation info) {
      if (info == null) {
         return null;
      }

      final Vector3f inDir = new Vector3f(info.ray.direction);
      final Vector3f surfaceNormal = new Vector3f(info.normal);

      double n;
      double cosI = surfaceNormal.dot(inDir);

      final Vector3f outDir = new Vector3f();

      if (cosI <= 0) {
         n = refractionIndex / aetherIndex;
         cosI = -cosI;
      } else {
         n = aetherIndex / refractionIndex;
         surfaceNormal.scale(-1f);
      }

      final double snellRoot = 1.0 - n * n * (1.0 - cosI * cosI);

      if (snellRoot < 0) {
         outDir.set(RTStatics.getReflectionDirection(surfaceNormal, inDir));
      } else {
         outDir.set(inDir);
         outDir.scale((float) n);
         final Vector3f scaledNormal = new Vector3f(surfaceNormal);
         scaledNormal.scale((float) (n * cosI - Math.sqrt(snellRoot)));
         outDir.add(scaledNormal);
         outDir.normalize();
      }

      return outDir;
   }

   // public static void main(final String[] args) {
   // // create a refraction material
   // final RefractionMaterial material = new RefractionMaterial(Color.blue, INDEX_OF_GLASS);
   //
   // // create our initial ray
   // final Vector3f origin = new Vector3f(0f, -0.5f, -1f);
   // final Vector3f direction = new Vector3f(0, 0, 1);
   // Ray ray = new Ray(origin, direction);
   //
   // // put sphere directly above origin in line with ray
   // final Sphere sphere = new Sphere(1f, new Vector3f(0, 0, 0), new ColorInformation(Color.blue));
   // int ctr = 1;
   // IntersectionInformation info;
   //
   // do {
   // // check intersection
   // info = sphere.getIntersection(ray, 0);
   //
   // if (info != null) {
   // // get refraction direction at given intersection
   // final Vector3f refractionDirection = material.getRefractionDirection(info);
   // System.out.println("\n" + ctr + ". origin:       " + info.ray.origin + "\n   direction:    " + info.ray.direction + "\n   intersection: " + info.intersection + "\n   normal:       "
   // + info.normal + "\n   refraction:   " + refractionDirection + "\n   distance:     " + info.w);
   //
   // // move ray to new intersection location
   // ray = new Ray(info.intersection, refractionDirection);
   // ctr++;
   // }
   // } while (info != null);
   // }
}
