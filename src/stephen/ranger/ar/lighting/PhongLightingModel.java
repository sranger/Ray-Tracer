package stephen.ranger.ar.lighting;

import java.awt.Color;

import javax.vecmath.Vector3f;

import stephen.ranger.ar.Camera;
import stephen.ranger.ar.IntersectionInformation;
import stephen.ranger.ar.RTStatics;
import stephen.ranger.ar.Ray;
import stephen.ranger.ar.bounds.BoundingVolume;

public class PhongLightingModel extends LightingModel {
   private static final Color NO_SHADOW_COMPONENT = Color.white;

   private final Camera camera;
   private final Light light;
   private final BoundingVolume[] objects;

   public PhongLightingModel(final Camera camera, final Light light, final BoundingVolume[] objects) {
      this.camera = camera;
      this.light = light;
      this.objects = objects;
   }

   @Override
   public Color getPixelColor(final IntersectionInformation info) {
      final float[] color = info.intersectionObject.getColor(info).getColorComponents(new float[3]);

      final float[] shadowComponent = getShadowComponent(info).getColorComponents(new float[3]);

      final float[] ks = info.intersectionObject.getColorInformation(info).specular.getColorComponents(new float[3]);
      final float[] kd = info.intersectionObject.getColorInformation(info).diffuse.getColorComponents(new float[3]);
      final float[] ka = info.intersectionObject.getColorInformation(info).ambient.getColorComponents(new float[3]);
      final float a = info.intersectionObject.getColorInformation(info).shininess;

      final float[] is = light.emission.getColorComponents(new float[3]);
      final float[] id = light.emission.getColorComponents(new float[3]);
      final float[] ia = light.ambient.getColorComponents(new float[3]);

      final Vector3f L = new Vector3f();
      L.sub(light.origin, info.intersection);
      L.normalize();

      final Vector3f N = new Vector3f(info.normal);

      final Vector3f V = new Vector3f();
      V.sub(info.intersection, camera.origin);
      V.normalize();

      // r = -2N(L.N)+L
      final Vector3f R = RTStatics.getReflectionDirection(info, light.origin);

      color[0] = color[0] * shadowComponent[0] * (float) Math.min(1f, Math.max(0f, (ka[0] * ia[0] + kd[0] * L.dot(N) * id[0] + ks[0] * Math.pow(R.dot(V), a) * is[0])));
      color[1] = color[1] * shadowComponent[1] * (float) Math.min(1f, Math.max(0f, (ka[1] * ia[1] + kd[1] * L.dot(N) * id[1] + ks[1] * Math.pow(R.dot(V), a) * is[1])));
      color[2] = color[2] * shadowComponent[2] * (float) Math.min(1f, Math.max(0f, (ka[2] * ia[2] + kd[2] * L.dot(N) * id[2] + ks[2] * Math.pow(R.dot(V), a) * is[2])));

      return new Color(color[0], color[1], color[2], 1f);
   }

   public Color getShadowComponent(final IntersectionInformation info) {
      final Vector3f shadowRayDirection = new Vector3f();
      shadowRayDirection.sub(light.origin, info.intersection);
      shadowRayDirection.normalize();

      final Ray shadowRay = new Ray(info.intersection, shadowRayDirection);
      IntersectionInformation shadowInfo = null;

      for (final BoundingVolume object : objects) {
         //         if (!object.equals(info.intersectionObject)) {
         shadowInfo = object.getChildIntersection(shadowRay);

         if (shadowInfo != null) {
            final float[] objectColor = shadowInfo.intersectionObject.getColor(shadowInfo).getColorComponents(new float[3]);
            final float[] ambient = new float[] { 0.6f, 0.6f, 0.6f };//light.ambient.getColorComponents(new float[3]);

            return new Color(ambient[0] * objectColor[0], ambient[1] * objectColor[1], ambient[2] * objectColor[2], 1f);
         }
         //         }
      }

      return PhongLightingModel.NO_SHADOW_COMPONENT;
   }
}
