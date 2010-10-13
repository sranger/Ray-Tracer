package stephen.ranger.ar.lighting;

import java.awt.Color;

import javax.vecmath.Vector3f;

import stephen.ranger.ar.IntersectionInformation;
import stephen.ranger.ar.RTStatics;
import stephen.ranger.ar.Ray;
import stephen.ranger.ar.bounds.BoundingVolume;
import stephen.ranger.ar.bounds.KDTree;

public class PhongLightingModel extends LightingModel {
   private static final Color NO_SHADOW_COMPONENT = Color.white;

   private final Light light;
   private final BoundingVolume[] objects;

   public PhongLightingModel(final Light light, final BoundingVolume[] objects) {
      this.light = light;
      this.objects = objects;
   }

   @Override
   public Color getPixelColor(final IntersectionInformation info) {
      final float[] color = info.intersectionObject.getColor(info).getColorComponents(new float[3]);

      final float[] ks = info.intersectionObject.getColorInformation(info).specular.getColorComponents(new float[3]);
      final float[] kd = info.intersectionObject.getColor(info).getColorComponents(new float[3]);
      final float[] ka = info.intersectionObject.getColorInformation(info).ambient.getColorComponents(new float[3]);
      final float a = info.intersectionObject.getColorInformation(info).shininess;

      final float[] is = this.light.emission.getColorComponents(new float[3]);
      final float[] id = (this.shadowIntersects(info)) ? new float[3] : this.light.emission.getColorComponents(new float[3]);
      final float[] ia = this.light.ambient.getColorComponents(new float[3]);

      final Vector3f L = new Vector3f();
      L.sub(this.light.origin, info.intersection);
      L.normalize();

      final Vector3f N = new Vector3f(info.normal);

      final Vector3f V = new Vector3f();
      V.sub(info.intersection, this.cameraPosition);
      V.normalize();

      // r = L - 2f * N * L.dot(N)
      final Vector3f R = RTStatics.getReflectionDirection(info, L);
      final double LdotN = L.dot(N);
      final double RdotVexpA = Math.pow(R.dot(V), a);

      color[0] = (float) Math.max(0.0, Math.min(1.0, (color[0] * (kd[0] * LdotN * id[0] + ks[0] * RdotVexpA * is[0] + 0.3f * ia[0]))));
      color[1] = (float) Math.max(0.0, Math.min(1.0, (color[1] * (kd[1] * LdotN * id[1] + ks[1] * RdotVexpA * is[1] + 0.3f * ia[1]))));
      color[2] = (float) Math.max(0.0, Math.min(1.0, (color[2] * (kd[2] * LdotN * id[2] + ks[2] * RdotVexpA * is[2] + 0.3f * ia[2]))));

      return new Color(color[0], color[1], color[2], 1f);
   }

   public boolean shadowIntersects(final IntersectionInformation info) {
      final Vector3f shadowRayDirection = new Vector3f();
      shadowRayDirection.sub(this.light.origin, info.intersection);
      shadowRayDirection.normalize();

      final Ray shadowRay = new Ray(info.intersection, shadowRayDirection);
      IntersectionInformation shadowInfo = null;

      for (final BoundingVolume object : this.objects) {
         // TODO: dont check against individual objects but against triangles in a mesh
         if ((object instanceof KDTree) || !object.equals(info.intersectionObject)) {
            shadowInfo = object.getChildIntersection(shadowRay);

            if ((shadowInfo != null) && (shadowInfo.w > 0.5)) {
               return true;
            }
         }
      }

      return false;
   }
}
