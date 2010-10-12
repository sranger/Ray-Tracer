package stephen.ranger.ar;

import stephen.ranger.ar.bounds.BoundingVolume;
import stephen.ranger.ar.lighting.Light;
import stephen.ranger.ar.lighting.LightingModel;

public class Scene {
   public final String label;
   public final BoundingVolume[] objects;
   public final Light light;
   public final float[] cameraPos;
   public final float[] cameraOrientation;
   public final LightingModel lightingModel;

   public Scene(final String label, final BoundingVolume[] objects, final Light light, final float[] cameraPos, final float[] cameraOrientation, final LightingModel lightingModel) {
      this.label = label;
      this.objects = objects;
      this.light = light;
      this.cameraPos = cameraPos;
      this.cameraOrientation = cameraOrientation;
      this.lightingModel = lightingModel;
   }

   @Override
   public String toString() {
      return label;
   }
}
