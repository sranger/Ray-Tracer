package stephen.ranger.ar.lighting;

import java.awt.Color;

import stephen.ranger.ar.IntersectionInformation;

public class LightingModel {
   public LightingModel() {
      
   }
   
   public Color getPixelColor(final IntersectionInformation info) {
      return info.intersectionObject.getColor(info);
   }
}
