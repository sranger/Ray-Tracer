package stephen.ranger.ar.lighting;

import stephen.ranger.ar.Camera;
import stephen.ranger.ar.IntersectionInformation;

public class LightingModel {
   protected Camera camera;

   public LightingModel() {

   }

   public float[] getPixelColor(final IntersectionInformation info, final int depth) {
      return info.intersectionObject.getColor(info, camera, depth);
   }

   public void setCamera(final Camera camera) {
      this.camera = camera;
   }
}
