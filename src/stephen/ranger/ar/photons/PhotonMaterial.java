package stephen.ranger.ar.photons;

import java.awt.Color;

import stephen.ranger.ar.Camera;
import stephen.ranger.ar.ColorInformation;
import stephen.ranger.ar.IntersectionInformation;
import stephen.ranger.ar.RTStatics;

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
            final double random = (double) i / (double) photons.length;
            final int cell = random < 0.33 ? 1 : random < 0.67 ? 2 : 0;

            total[cell][0] += photons[i].intensity * photons[i].color[0];
            total[cell][1] += photons[i].intensity * photons[i].color[1];
            total[cell][2] += photons[i].intensity * photons[i].color[2];

            counts[cell]++;
         }

         total[0][0] /= counts[0];
         total[0][1] /= counts[0];
         total[0][2] /= counts[0];

         total[1][0] /= counts[1];
         total[1][1] /= counts[1];
         total[1][2] /= counts[1];

         total[2][0] /= counts[2];
         total[2][1] /= counts[2];
         total[2][2] /= counts[2];
      }

      return total;//Math.min(1f, total);
   }
}
