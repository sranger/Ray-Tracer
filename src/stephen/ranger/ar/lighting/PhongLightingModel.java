package stephen.ranger.ar.lighting;

import javax.vecmath.Vector3f;

import stephen.ranger.ar.IntersectionInformation;
import stephen.ranger.ar.RTStatics;
import stephen.ranger.ar.bounds.BoundingVolume;

public class PhongLightingModel extends LightingModel {
   private final Light light;
   private final BoundingVolume[] objects;

   public PhongLightingModel(final Light light, final BoundingVolume[] objects) {
      this.light = light;
      this.objects = objects;
   }

   @Override
   public float[] getPixelColor(final IntersectionInformation info, final int depth) {
      final float[] color = info.intersectionObject.getColor(info, camera, depth);
      final boolean shadowIntersects = RTStatics.shadowIntersects(light, objects, info, depth);

      final float[] ks = info.intersectionObject.getSpecular();
      final float[] kd = info.intersectionObject.getDiffuse();
      final float[] ka = info.intersectionObject.getAmbient();
      final float a = info.intersectionObject.getShininess();

      final float[] is = light.emission.getColorComponents(new float[3]);
      final float[] id = light.emission.getColorComponents(new float[3]);
      final float[] ia = light.ambient.getColorComponents(new float[3]);

      final Vector3f L = new Vector3f();
      L.sub(light.origin, info.intersection);
      L.normalize();

      final Vector3f N = new Vector3f(info.normal);

      final Vector3f V = new Vector3f();
      V.sub(info.ray.direction);

      // r = L - 2f * N * L.dot(N)
      final Vector3f R = RTStatics.getReflectionDirection(info.normal, L);
      final double LdotN = L.dot(N);
      final double RdotVexpA = Math.pow(V.dot(R), a);

      final double spec = shadowIntersects ? 0f : 1f;
      final double shade = shadowIntersects ? 0.6f : 1f;

      color[0] *= shade * (kd[0] * LdotN * id[0] + spec * ks[0] * RdotVexpA * is[0] + 0.4f * ia[0]);
      color[1] *= shade * (kd[1] * LdotN * id[1] + spec * ks[1] * RdotVexpA * is[1] + 0.4f * ia[1]);
      color[2] *= shade * (kd[2] * LdotN * id[2] + spec * ks[2] * RdotVexpA * is[2] + 0.4f * ia[2]);

      return color;
   }
}
