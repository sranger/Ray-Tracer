package stephen.ranger.ar.photons;

public class Photon {
   public static enum LightAttribution {
      DIFFUSE(0), SPECULAR(1);
      public final int cell;

      private LightAttribution(final int cell) {
         this.cell = cell;
      }
   };

   public final float[] color;
   public final float[] location;
   public final float[] incomingDir;
   public final float intensity;
   public final LightAttribution value;

   public Photon(final float[] color, final float[] location, final float[] incomingDir, final float intensity, final LightAttribution value) {
      this.color = color;
      this.location = location;
      this.incomingDir = incomingDir;
      this.intensity = intensity;
      this.value = value;
   }
}
