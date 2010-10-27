package stephen.ranger.ar.photons;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import stephen.ranger.ar.RTStatics;
import stephen.ranger.ar.RTStatics.SeparationAxis;
import stephen.ranger.ar.bounds.AxisAlignedBoundingBox;
import stephen.ranger.ar.bounds.BoundingVolume;

public class PhotonTreeNode {
   public final PhotonTreeNode left, right;
   public final float[][] minMax;
   public final Photon[] photons;
   public final BoundingVolume bv;

   public PhotonTreeNode(final Photon[] photons, final float[][] minMax, final SeparationAxis axis, final int depth) {
      this.minMax = minMax;
      this.photons = photons;
      bv = new AxisAlignedBoundingBox(null, minMax);

      if (photons.length > RTStatics.MAX_CHILDREN && depth < RTStatics.MAX_DEPTH) {
         float median;
         final List<Photon> leftChildren = new ArrayList<Photon>();
         final List<Photon> rightChildren = new ArrayList<Photon>();

         if (axis.equals(SeparationAxis.X)) {
            median = (this.minMax[1][0] - this.minMax[0][0]) / 2f + this.minMax[0][0];
         } else if (axis.equals(SeparationAxis.Y)) {
            median = (this.minMax[1][1] - this.minMax[0][1]) / 2f + this.minMax[0][1];
         } else {
            median = (this.minMax[1][2] - this.minMax[0][2]) / 2f + this.minMax[0][2];
         }

         final float[][] leftMinMax = new float[][] { { Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE }, { -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE } };
         final float[][] rightMinMax = new float[][] { { Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE }, { -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE } };
         final float[][] childMinMax = new float[3][2];

         for (final Photon photon : photons) {
            RTStatics.getMinMax(new Photon[] { photon }, childMinMax);

            if (childMinMax[1][axis.pos] <= median) {
               leftChildren.add(photon);
            } else if (childMinMax[0][axis.pos] >= median) {
               rightChildren.add(photon);
            } else {
               leftChildren.add(photon);
               rightChildren.add(photon);
            }
         }

         if (leftChildren.size() > 0) {
            final Photon[] leftPhotons = leftChildren.toArray(new Photon[leftChildren.size()]);
            RTStatics.getMinMax(leftPhotons, leftMinMax);
            left = new PhotonTreeNode(leftPhotons, leftMinMax, axis.getNextAxis(), depth + 1);
         } else {
            left = null;
         }

         if (rightChildren.size() > 0) {
            final Photon[] rightPhotons = rightChildren.toArray(new Photon[rightChildren.size()]);
            RTStatics.getMinMax(rightPhotons, rightMinMax);
            right = new PhotonTreeNode(rightPhotons, rightMinMax, axis.getNextAxis(), depth + 1);
         } else {
            right = null;
         }
      } else {
         left = null;
         right = null;
      }
   }

   public Collection<Photon> getPhotonsInRange(final float[] location, final float range) {
      final List<Photon> list = new ArrayList<Photon>();

      if (left != null || right != null) {

      } else {
         for (final Photon photon : photons) {
            // TODO: check if BV's intersect
         }
      }

      return list;
   }
}
