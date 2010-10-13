package stephen.ranger.ar.lighting;

import java.awt.Color;

import javax.vecmath.Vector3f;

import stephen.ranger.ar.IntersectionInformation;

public class LightingModel {
   protected Vector3f cameraPosition = new Vector3f();

   public LightingModel() {

   }

   public Color getPixelColor(final IntersectionInformation info) {
      return info.intersectionObject.getColor(info);
   }

   public void setCameraPosition(final float[] position) {
      cameraPosition = new Vector3f(position);
   }
}
