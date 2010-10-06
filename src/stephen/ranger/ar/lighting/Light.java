package stephen.ranger.ar.lighting;

import java.awt.Color;

import javax.vecmath.Vector3d;

public class Light {
   public final Vector3d origin;
   public final Color emission;
   public final Color ambient;

   public Light(final Vector3d origin, final Color emission, final Color ambient) {
      this.origin = origin;
      this.emission = emission;
      this.ambient = ambient;
   }

   public Light() {
      this(new Vector3d(100, 100, 100), Color.white, Color.white);
   }
}
