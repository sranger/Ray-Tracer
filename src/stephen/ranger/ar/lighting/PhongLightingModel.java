package stephen.ranger.ar.lighting;

import javax.vecmath.Vector3f;

import stephen.ranger.ar.IntersectionInformation;
import stephen.ranger.ar.RTStatics;
import stephen.ranger.ar.Ray;
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
      final float[] color = info.intersectionObject.getColor(info, this.camera, depth);
      final boolean shadowIntersects = this.shadowIntersects(info, depth);

      final float[] ks = info.intersectionObject.getSpecular();
      final float[] kd = info.intersectionObject.getDiffuse();
      final float[] ka = info.intersectionObject.getAmbient();
      final float a = info.intersectionObject.getShininess();

      final float[] is = this.light.emission.getColorComponents(new float[3]);
      final float[] id = this.light.emission.getColorComponents(new float[3]);
      final float[] ia = this.light.ambient.getColorComponents(new float[3]);


      final Vector3f L = new Vector3f();
      L.sub(this.light.origin, info.intersection);
      L.normalize();

      final Vector3f N = new Vector3f(info.normal);

      final Vector3f V = new Vector3f();
      V.sub(info.ray.direction);

      // r = L - 2f * N * L.dot(N)
      final Vector3f R = RTStatics.getReflectionDirection(info, L);
      final double LdotN = L.dot(N);
      final double RdotVexpA = Math.pow(V.dot(R), a);

      final double shade = (shadowIntersects) ? 0.3f : 1f;

      color[0] *= (shade * (kd[0] * LdotN * id[0] + ks[0] * RdotVexpA * is[0] + 0.4f * ia[0]));
      color[1] *= (shade * (kd[1] * LdotN * id[1] + ks[1] * RdotVexpA * is[1] + 0.4f * ia[1]));
      color[2] *= (shade * (kd[2] * LdotN * id[2] + ks[2] * RdotVexpA * is[2] + 0.4f * ia[2]));

      return color;
   }

   public boolean shadowIntersects(final IntersectionInformation info, final int depth) {
      final Vector3f shadowRayDirection = new Vector3f();
      shadowRayDirection.sub(this.light.origin, info.intersection);
      shadowRayDirection.normalize();

      final Ray shadowRay = new Ray(info.intersection, shadowRayDirection);
      IntersectionInformation shadowInfo = null;

      for (final BoundingVolume object : this.objects) {
         // TODO: dont check against individual objects but against triangles in a mesh
         //         if (object instanceof KDTree || !object.equals(info.intersectionObject)) {
         shadowInfo = object.getChildIntersection(shadowRay, depth + 1);

         if ((shadowInfo != null) && (shadowInfo.w > RTStatics.EPSILON)) {
            final float lightDistance = RTStatics.getDistance(shadowInfo.intersection, this.light.origin);

            if (shadowInfo.w < lightDistance + RTStatics.EPSILON) {
               return true;
            }
         }
         //         }
      }

      return false;
   }
}
