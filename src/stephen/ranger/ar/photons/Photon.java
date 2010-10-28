package stephen.ranger.ar.photons;

public class Photon {
   public final float[] color;
   public final float[] location;
   public final float intensity;
   public final float range;

   public Photon(final float[] color, final float[] location, final float intensity, final float range) {
      this.color = color;
      this.location = location;
      this.intensity = intensity;
      this.range = range;
   }
}
