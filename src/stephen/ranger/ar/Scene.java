package stephen.ranger.ar;

import stephen.ranger.ar.bounds.BoundingVolume;
import stephen.ranger.ar.lighting.Light;
import stephen.ranger.ar.lighting.LightingModel;

public class Scene {
   public final String label;
   public final BoundingVolume[] objects;
   public final Light light;
   public final float[] cameraOrientation;
   public final LightingModel lightingModel;
   public final float fov;

   public Scene(final String label, final BoundingVolume[] objects, final Light light, final float[] cameraOrientation, final LightingModel lightingModel, final float fov) {
      this.label = label;
      this.objects = objects;
      this.light = light;
      this.cameraOrientation = cameraOrientation;
      this.lightingModel = lightingModel;
      this.fov = fov;

      float minx = Float.MAX_VALUE, miny = Float.MAX_VALUE, minz = Float.MAX_VALUE;
      float maxx = -Float.MAX_VALUE, maxy = -Float.MAX_VALUE, maxz = -Float.MAX_VALUE;

      for (final BoundingVolume v : objects) {
         final float[][] minmax = v.getMinMax();
         minx = Math.min(minx, minmax[0][0]);
         miny = Math.min(miny, minmax[0][1]);
         minz = Math.min(minz, minmax[0][2]);

         maxx = Math.max(maxx, minmax[1][0]);
         maxy = Math.max(maxy, minmax[1][1]);
         maxz = Math.max(maxz, minmax[1][2]);
      }

      final float minSpan = Math.min(maxx - minx, Math.min(maxy - miny, maxz - minz));
      RTStatics.EPSILON = minSpan / (65536f * 32f);
      System.out.println("epsilon: " + RTStatics.EPSILON);
   }

   @Override
   public String toString() {
      return label;
   }
}
