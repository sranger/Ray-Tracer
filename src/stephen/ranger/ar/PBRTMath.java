package stephen.ranger.ar;

import javax.vecmath.Vector3f;

public class PBRTMath {
   public static final float F_PI = (float) Math.PI;
   public static final float F_2_PI = 2f * (float) Math.PI;
   public static final float F_HALF_PI = (float) Math.PI / 2f;

   private PBRTMath() {
      // static only
   }

   // public static float cosTheta(final float[] w) {
   // return w[2];
   // }
   //
   // public static float sinTheta(final float[] w) {
   // return (float) Math.sqrt(sinTheta2(w));
   // }
   //
   // public static float sinTheta2(final float[] w) {
   // return Math.max(0f, 1f - cosTheta(w) * cosTheta(w));
   // }
   //
   // public static float sphericalPhi(final float[] v) {
   // final float p = (float) Math.atan2(v[1], v[0]);
   // return p < 0f ? p + 2f * (float) Math.PI : p;
   // }
   //
   // public static float[] getRemapedDirection(final float[] in, final float[] out) {
   // final float cosi = PBRTMath.cosTheta(in);
   // final float coso = PBRTMath.cosTheta(out);
   // final float sini = PBRTMath.sinTheta(in);
   // final float sino = PBRTMath.sinTheta(out);
   // final float phii = PBRTMath.sphericalPhi(in);
   // final float phio = PBRTMath.sphericalPhi(out);
   // float dphi = phii - phio;
   //
   // if (dphi < 0f) {
   // dphi += PBRTMath.F_2_PI;
   // }
   //
   // if (dphi > PBRTMath.F_2_PI) {
   // dphi -= PBRTMath.F_2_PI;
   // }
   //
   // if (dphi > PBRTMath.F_PI) {
   // dphi = PBRTMath.F_2_PI - dphi;
   // }
   //
   // return new float[] { sini * sino, dphi / PBRTMath.F_PI, cosi * coso };
   // }

   public static float getDistance2(final float[] p1, final float[] p2) {
      return Math.abs((p1[0] - p2[0]) * (p1[0] - p2[0]) + (p1[1] - p2[1]) * (p1[1] - p2[1]) + (p1[2] - p2[2]) * (p1[2] - p2[2]));
   }

   public static float[] getRemappedDirection(final Vector3f normal, final Vector3f tangent, final Vector3f inDir, final Vector3f outDir) {
      final Vector3f n = new Vector3f(normal);
      final Vector3f t = new Vector3f(tangent); // x
      final Vector3f s = new Vector3f(); // y
      n.normalize();
      t.normalize();
      s.cross(n, t);
      s.normalize();

      final float[] in = PBRTMath.getThetaPhi(n, s, t, inDir);
      final float[] out = PBRTMath.getThetaPhi(n, s, t, outDir);

      return PBRTMath.getRemappedDirection(in, out);
   }

   public static float[] getRemappedDirection(final float[] in, final float[] out) {
      final float cosi = (float) Math.cos(in[0]);
      final float coso = (float) Math.cos(out[0]);
      final float sini = (float) Math.sin(in[0]);
      final float sino = (float) Math.sin(out[0]);
      final float phii = PBRTMath.normalizePhi(in[1]);
      final float phio = PBRTMath.normalizePhi(out[1]);

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

   public static float normalizePhi(final float phi) {
      return phi < Math.PI / 2f ? phi + 2f * (float) Math.PI : phi;
   }

   public static float[] getThetaPhi(final Vector3f n, final Vector3f s, final Vector3f t, final Vector3f dir) {
      final float z = dir.dot(n);
      final float y = dir.dot(s);
      final float x = dir.dot(t);

      final float theta = (float) Math.acos(z);
      final float phi = (float) Math.atan2(y, x);

      return new float[] { theta, phi };
   }

   /**
    * @param normal
    * @return
    */
   public static Vector3f getNormalTangent(final Vector3f normal) {
      final Vector3f randomVector = new Vector3f(0, 1, 0);
      final float dot = Math.abs(normal.dot(randomVector));

      if (dot > 0.9f) {
         randomVector.set(-1, 0, 0);
      }

      final Vector3f tangent = new Vector3f();
      tangent.cross(normal, randomVector);

      return tangent;
   }
}
