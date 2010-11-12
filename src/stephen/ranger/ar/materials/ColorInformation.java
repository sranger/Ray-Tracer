package stephen.ranger.ar.materials;

import java.awt.Color;

import stephen.ranger.ar.Camera;
import stephen.ranger.ar.IntersectionInformation;

public class ColorInformation {
   public final Color emission;
   public final Color ambient;
   public final Color diffuse;
   public final Color specular;
   public final float shininess;

   public ColorInformation(final Color emission, final Color ambient, final Color diffuse, final Color specular, final float shininess) {
      this.emission = emission;
      this.ambient = ambient;
      this.diffuse = diffuse;
      this.specular = specular;
      this.shininess = shininess;
   }

   public ColorInformation() {
      this(Color.black);
   }

   public ColorInformation(final Color diffuse) {
      this(diffuse, 20);
   }

   public ColorInformation(final Color diffuse, final float shininess) {
      this(Color.black, diffuse, diffuse, diffuse, shininess);
   }

   public void getMaterialColor(final float[] returnColor, final Camera camera, final IntersectionInformation info, final int depth) {
      this.diffuse.getColorComponents(returnColor);
      return;
   }
}
