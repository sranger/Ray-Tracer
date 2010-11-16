package stephen.ranger.ar.materials;

import java.awt.Color;

import stephen.ranger.ar.Camera;
import stephen.ranger.ar.IntersectionInformation;
import stephen.ranger.ar.RTStatics;

public class ReflectionMaterial extends ColorInformation {
   public ReflectionMaterial(final Color diffuse) {
      super(diffuse, 100);
   }

   @Override
   public float[] getMaterialColor(final Camera camera, final IntersectionInformation info, final int depth) {
      final IntersectionInformation mirrorInfo = camera.getClosestIntersection(info.intersectionObject, info.intersection, RTStatics
            .getReflectionDirection(info), info.normal, depth + 1);

      if (mirrorInfo == null) {
         return camera.light.ambient.getColorComponents(new float[3]);
      } else {
         return mirrorInfo.intersectionObject.getColor(mirrorInfo, camera, depth + 1);
      }
   }
}
