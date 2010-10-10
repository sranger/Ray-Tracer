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

   public ColorInformation(final Color emission) {
      this(emission, false);
   }

   public ColorInformation(final Color emission, final boolean isMirror) {
      // this(emission, new Color(0.8f, 0.8f, 0.8f, 1.0f), new Color(0.8f, 0.8f, 0.8f, 1.0f), new Color(0.8f, 0.8f, 0.8f, 1.0f), 10, isMirror);
      this(emission, emission, emission, emission, 50, isMirror);
   }
}
