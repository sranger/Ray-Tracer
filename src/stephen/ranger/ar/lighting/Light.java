package stephen.ranger.ar.lighting;

import java.awt.Color;

import javax.vecmath.Vector3f;

public class Light {
   public final Vector3f origin;
   public final Color emission;
   public final Color ambient;

   public Light(final Vector3f origin, final Color emission, final Color ambient) {
      this.origin = origin;
      this.emission = emission;
      this.ambient = ambient;
   }

   public Light() {
      this(new Vector3f(100, 100, 100), Color.white, Color.white);
   }
}
