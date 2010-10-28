package stephen.ranger.ar.photons;

import java.util.ArrayList;
import java.util.List;

import stephen.ranger.ar.Camera;
import stephen.ranger.ar.IntersectionInformation;
import stephen.ranger.ar.RTStatics;
import stephen.ranger.ar.bounds.AxisAlignedBoundingBox;
import stephen.ranger.ar.bounds.BoundingVolume;

public class PhotonTree {
   public final Photon[] photons;
   public final PhotonTreeNode node;
   public final BoundingVolume bv;

   public PhotonTree(final Photon[] photons) {
      this.photons = photons;

      float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
      float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;

      for (final Photon photon : photons) {
         minX = Math.min(photon.location[0], minX);
         minY = Math.min(photon.location[1], minY);
         minZ = Math.min(photon.location[2], minZ);

         maxX = Math.max(photon.location[0], maxX);
         maxY = Math.max(photon.location[1], maxY);
         maxZ = Math.max(photon.location[2], maxZ);
      }

      bv = new AxisAlignedBoundingBox(null, minX, minY, minZ, maxX, maxY, maxZ);
      node = new PhotonTreeNode(photons, bv.getMinMax(), 0);

      RTStatics.setProgressBarValue(photons.length);
   }

   public Photon[] getPhotonsInRange(final IntersectionInformation info, final float range, final Camera camera) {
      final List<Photon> list = new ArrayList<Photon>();
      list.addAll(node.getPhotonsInRange(info, range, camera));

      return list.toArray(new Photon[list.size()]);
   }
}
