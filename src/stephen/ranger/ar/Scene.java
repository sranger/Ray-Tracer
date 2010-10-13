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
   }

   @Override
   public String toString() {
      return label;
   }
}
