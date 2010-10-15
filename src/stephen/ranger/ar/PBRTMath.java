package stephen.ranger.ar;

public class PBRTMath {
   public static final float F_PI = (float) Math.PI;
   public static final float F_2_PI = 2f * (float) Math.PI;
   public static final float F_HALF_PI = (float) Math.PI / 2f;

   private PBRTMath() {
      // static only
   }

   public static float cosTheta(final float[] w) {
      return (float) Math.cos(w[0]);
   }

   public static float sinTheta(final float[] w) {
      return (float) Math.sin(w[0]);//Math.sqrt(sinTheta2(w));
   }

   public static float sinTheta2(final float[] w) {
      return Math.max(0f, 1f - cosTheta(w) * cosTheta(w));
   }

   public static float sphericalPhi(final float[] v) {
      final float p = (float) Math.atan2(v[1], v[0]);
      return p < 0f ? p + 2f * (float) Math.PI : p;
   }

   public static float[] getRemapedDirection(final float[] in, final float[] out) {
      final float cosi = PBRTMath.cosTheta(in);
      final float coso = PBRTMath.cosTheta(out);
      final float sini = PBRTMath.sinTheta(in);
      final float sino = PBRTMath.sinTheta(out);
      final float phii = PBRTMath.sphericalPhi(in);
      final float phio = PBRTMath.sphericalPhi(out);
      float dphi = phii - phio;

      if (dphi < 0f) {
         dphi += PBRTMath.F_2_PI;
      }

      if (dphi > PBRTMath.F_2_PI) {
         dphi -= PBRTMath.F_2_PI;
      }

      if (dphi > PBRTMath.F_PI) {
         dphi = PBRTMath.F_2_PI - dphi;
      }

      return new float[] { sini * sino, dphi / PBRTMath.F_PI, cosi * coso };
   }

   public static float getDistance2(final float[] p1, final float[] p2) {
      return Math.abs((p1[0] - p2[0]) * (p1[0] - p2[0]) + (p1[1] - p2[1]) * (p1[1] - p2[1]) + (p1[2] - p2[2]) * (p1[2] - p2[2]));
   }
}
