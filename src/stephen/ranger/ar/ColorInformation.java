package stephen.ranger.ar;

import java.awt.Color;

public class ColorInformation {
   public final Color emission;
   public final Color ambient;
   public final Color diffuse;
   public final Color specular;
   public final float shininess;
   public final boolean isMirror;

   public ColorInformation(final Color emission, final Color ambient, final Color diffuse, final Color specular, final float shininess, final boolean isMirror) {
      this.emission = emission;
      this.ambient = ambient;
      this.diffuse = diffuse;
      this.specular = specular;
      this.shininess = shininess;
      this.isMirror = isMirror;
   }

   public ColorInformation() {
      this(Color.black);
   }

   public ColorInformation(final Color diffuse) {
      this(diffuse, false);
   }

   public ColorInformation(final Color diffuse, final boolean isMirror) {
      this(Color.black, diffuse, diffuse, diffuse, 0, isMirror);
   }
}
