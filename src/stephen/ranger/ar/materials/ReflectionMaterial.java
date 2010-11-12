package stephen.ranger.ar.materials;

import java.awt.Color;

import javax.vecmath.Vector3f;

import stephen.ranger.ar.Camera;
import stephen.ranger.ar.IntersectionInformation;
import stephen.ranger.ar.RTStatics;

public class ReflectionMaterial extends ColorInformation {
   public ReflectionMaterial(final Color diffuse) {
      super(diffuse);
   }

   @Override
   public void getMaterialColor(final float[] returnColor, final Camera camera, final IntersectionInformation info, final int depth) {
      final Vector3f V = new Vector3f(info.intersection);
      V.sub(camera.origin);
      V.normalize();

      final IntersectionInformation mirrorInfo = camera.getClosestIntersection(info.intersectionObject, info.intersection, RTStatics.getReflectionDirection(info, V), depth + 1);

      if (mirrorInfo == null) {
         camera.light.ambient.getColorComponents(returnColor);
      } else {
         mirrorInfo.intersectionObject.getColor(mirrorInfo, camera, depth + 1);
      }
   }
}
