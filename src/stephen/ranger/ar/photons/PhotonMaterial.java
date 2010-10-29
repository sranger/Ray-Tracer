package stephen.ranger.ar.photons;

import java.awt.Color;

import stephen.ranger.ar.Camera;
import stephen.ranger.ar.ColorInformation;
import stephen.ranger.ar.IntersectionInformation;
import stephen.ranger.ar.RTStatics;
import stephen.ranger.ar.photons.Photon.LightAttribution;

public class PhotonMaterial extends ColorInformation {
   public PhotonMaterial(final Color diffuse) {
      super(diffuse);
   }

   public float[][] getPhotonMapLuminocity(final IntersectionInformation info, final Camera camera) {
      final Photon[] photons = camera.photons.getPhotonsInRange(info, RTStatics.COLLECTION_RANGE, camera);
      final float[][] total = new float[3][3];
      final int[] counts = new int[3];

      if (photons.length > 0) {
         for (int i = 0; i < photons.length; i++) {
            final float invDistance = 1f / RTStatics.getDistance(new float[] { camera.origin.x, camera.origin.y, camera.origin.z }, photons[i].location);

            total[photons[i].value.cell][0] += photons[i].intensity * invDistance * photons[i].color[0];
            total[photons[i].value.cell][1] += photons[i].intensity * invDistance * photons[i].color[1];
            total[photons[i].value.cell][2] += photons[i].intensity * invDistance * photons[i].color[2];

            counts[photons[i].value.cell]++;
         }

         total[LightAttribution.DIFFUSE.cell][0] /= counts[LightAttribution.DIFFUSE.cell];
         total[LightAttribution.DIFFUSE.cell][1] /= counts[LightAttribution.DIFFUSE.cell];
         total[LightAttribution.DIFFUSE.cell][2] /= counts[LightAttribution.DIFFUSE.cell];

         total[LightAttribution.SPECULAR.cell][0] /= counts[LightAttribution.SPECULAR.cell];
         total[LightAttribution.SPECULAR.cell][1] /= counts[LightAttribution.SPECULAR.cell];
         total[LightAttribution.SPECULAR.cell][2] /= counts[LightAttribution.SPECULAR.cell];
      }

      return total;//Math.min(1f, total);
   }
}
