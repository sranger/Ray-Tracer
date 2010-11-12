package stephen.ranger.ar.lighting;

import stephen.ranger.ar.Camera;
import stephen.ranger.ar.IntersectionInformation;

public class GlobalPhongLightingModel extends LightingModel {
   private final PhongLightingModel phong;
   private final GlobalIlluminationLightingModel global;

   public GlobalPhongLightingModel(final GlobalIlluminationLightingModel global, final PhongLightingModel phong) {
      this.phong = phong;
      this.global = global;
   }

   @Override
   public float[] getPixelColor(final IntersectionInformation info, final int depth) {
      if (Math.random() < 075f) {
         return this.global.getPixelColor(info, depth);
      } else {
         return this.phong.getPixelColor(info, depth);
      }
   }

   @Override
   public void setCamera(final Camera camera) {
      super.setCamera(camera);

      this.phong.setCamera(camera);
      this.global.setCamera(camera);
   }

}
