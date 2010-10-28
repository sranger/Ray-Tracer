package stephen.ranger.ar.lighting;

import java.awt.Color;

import javax.vecmath.Vector3f;

import stephen.ranger.ar.Camera;
import stephen.ranger.ar.ColorInformation;
import stephen.ranger.ar.IntersectionInformation;
import stephen.ranger.ar.RTStatics;
import stephen.ranger.ar.Ray;
import stephen.ranger.ar.bounds.BoundingVolume;
import stephen.ranger.ar.photons.PhotonMaterial;

public class PhongLightingModel extends LightingModel {
   private final Light light;
   private final BoundingVolume[] objects;

   public PhongLightingModel(final Light light, final BoundingVolume[] objects) {
      this.light = light;
      this.objects = objects;
   }

   @Override
   public Color getPixelColor(final IntersectionInformation info, final Camera camera) {
      final ColorInformation colorInfo = info.intersectionObject.getColorInformation(info);
      final float[] color = info.intersectionObject.getColor(info).getColorComponents(new float[3]);
      final boolean shadowIntersects = shadowIntersects(info);

      final float[] ks = shadowIntersects ? new float[3] : info.intersectionObject.getColorInformation(info).specular.getColorComponents(new float[3]);
      final float[] kd = shadowIntersects ? new float[3] : info.intersectionObject.getColor(info).getColorComponents(new float[3]);
      final float[] ka = info.intersectionObject.getColorInformation(info).ambient.getColorComponents(new float[3]);
      final float a = info.intersectionObject.getColorInformation(info).shininess;

      final float[] is = shadowIntersects ? new float[3] : light.emission.getColorComponents(new float[3]);
      final float[] id = shadowIntersects ? new float[3] : light.emission.getColorComponents(new float[3]);
      final float[] ia = light.ambient.getColorComponents(new float[3]);

      if (colorInfo instanceof PhotonMaterial) {
         final float[][] luminance = ((PhotonMaterial) colorInfo).getPhotonMapLuminocity(info, camera);

         ia[0] *= luminance[0][0];
         ia[1] *= luminance[0][1];
         ia[2] *= luminance[0][2];

         id[0] *= luminance[1][0];
         id[1] *= luminance[1][1];
         id[2] *= luminance[1][2];

         is[0] *= luminance[2][0];
         is[1] *= luminance[2][1];
         is[2] *= luminance[2][2];
      }

      final Vector3f L = new Vector3f();
      L.sub(light.origin, info.intersection);
      L.normalize();

      final Vector3f N = new Vector3f(info.normal);

      final Vector3f V = new Vector3f();
      V.sub(info.intersection, cameraPosition);
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
      shadowRayDirection.sub(light.origin, info.intersection);
      shadowRayDirection.normalize();

      final Ray shadowRay = new Ray(info.intersection, shadowRayDirection);
      IntersectionInformation shadowInfo = null;

      for (final BoundingVolume object : objects) {
         // TODO: dont check against individual objects but against triangles in a mesh
         //         if (object instanceof KDTree || !object.equals(info.intersectionObject)) {
         shadowInfo = object.getChildIntersection(shadowRay);

         if (shadowInfo != null && shadowInfo.w > RTStatics.EPSILON) {
            final float lightDistance = RTStatics.getDistance(shadowInfo.intersection, light.origin);

            if (shadowInfo.w < lightDistance + RTStatics.EPSILON) {
               return true;
            }
         }
         //         }
      }

      return false;
   }
}
