package stephen.ranger.ar.photons;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;

import stephen.ranger.ar.Camera;
import stephen.ranger.ar.IntersectionInformation;
import stephen.ranger.ar.RTStatics;
import stephen.ranger.ar.RTStatics.SeparationAxis;
import stephen.ranger.ar.bounds.AxisAlignedBoundingBox;
import stephen.ranger.ar.bounds.BoundingVolume;

public class PhotonTreeNode {
   public final PhotonTreeNode left, right;
   public final float[][] minMax;
   public final Photon[] photons;
   public final BoundingVolume bv;

   public PhotonTreeNode(final Photon[] photons, final float[][] minMax, final int depth) {
      this.minMax = minMax;
      this.photons = photons;
      bv = new AxisAlignedBoundingBox(null, minMax);
      SeparationAxis axis = SeparationAxis.X;

      if (photons.length > RTStatics.MAX_CHILDREN && depth < RTStatics.MAX_DEPTH) {
         final List<Photon> leftChildren = new ArrayList<Photon>();
         final List<Photon> rightChildren = new ArrayList<Photon>();

         final float medianX = (this.minMax[1][0] - this.minMax[0][0]) / 2f + this.minMax[0][0];
         final float medianY = (this.minMax[1][1] - this.minMax[0][1]) / 2f + this.minMax[0][1];
         final float medianZ = (this.minMax[1][2] - this.minMax[0][2]) / 2f + this.minMax[0][2];
         float median = medianX;

         if (medianY > medianX && medianY > medianZ) {
            axis = SeparationAxis.Y;
            median = medianY;
         } else if (medianZ > medianY && medianZ > medianX) {
            axis = SeparationAxis.Z;
            median = medianZ;
         }

         final float[][] leftMinMax = new float[][] { { Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE }, { -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE } };
         final float[][] rightMinMax = new float[][] { { Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE }, { -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE } };
         final float[][] childMinMax = new float[2][3];

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
            left = new PhotonTreeNode(leftPhotons, leftMinMax, depth + 1);
         } else {
            left = null;
         }

         if (rightChildren.size() > 0) {
            final Photon[] rightPhotons = rightChildren.toArray(new Photon[rightChildren.size()]);
            RTStatics.getMinMax(rightPhotons, rightMinMax);
            right = new PhotonTreeNode(rightPhotons, rightMinMax, depth + 1);
         } else {
            right = null;
         }
      } else {
         left = null;
         right = null;

         RTStatics.incrementProgressBarValue(photons.length);
      }
   }

   public Collection<Photon> getPhotonsInRange(final IntersectionInformation info, final float range, final Camera camera) {
      final List<Photon> list = new ArrayList<Photon>();

      if (left != null || right != null) {
         if (left != null) {
            final Collection<Photon> leftMatches = left.getPhotonsInRange(info, range, camera);

            if (leftMatches != null && leftMatches.size() > 0) {
               list.addAll(leftMatches);
            }
         }

         if (right != null) {
            final Collection<Photon> rightMatches = right.getPhotonsInRange(info, range, camera);

            if (rightMatches != null && rightMatches.size() > 0) {
               list.addAll(rightMatches);
            }
         }
      } else {
         final Matrix4f rotation = new Matrix4f();
         final float step = 180f / RTStatics.PHOTON_COLLECTION_GRID_SIZE;
         final float[] location = new float[3];

         for (int x = 0; x <= RTStatics.PHOTON_COLLECTION_GRID_SIZE; x++) {
            for (int y = 0; y <= RTStatics.PHOTON_COLLECTION_GRID_SIZE; y++) {
               final Vector3f dir = new Vector3f(info.normal);
               rotation.set(RTStatics.initializeQuat4f(new float[] { x * step - 90f, y * step - 90f, 0 }));
               rotation.transform(dir);
               dir.normalize();

               final IntersectionInformation closest = camera.getClosestIntersection(info.intersectionObject, info.intersection, dir);

               if (closest != null) {
                  closest.intersection.get(location);
                  for (final Photon photon : photons) {
                     if (photonAABBIntersection(photon, minMax)) {
                        final float distance = RTStatics.getDistance(photon.location, location);

                        if (distance - range - photon.range < 0f) {
                           list.add(photon);
                        }
                     }
                  }
               }
            }
         }
      }

      return list;
   }

   /**
    * http://www.devmaster.net/forums/archive/index.php/t-10324.html
    */
   private boolean photonAABBIntersection(final Photon photon, final float[][] minMax) {
      final float minX = photon.location[0] - photon.intensity;
      final float minY = photon.location[1] - photon.intensity;
      final float minZ = photon.location[2] - photon.intensity;
      final float maxX = photon.location[0] + photon.intensity;
      final float maxY = photon.location[1] + photon.intensity;
      final float maxZ = photon.location[2] + photon.intensity;

      if (minMax[0][0] >= minX && minMax[1][0] <= maxX && minMax[0][1] >= minY && minMax[1][1] <= maxY && minMax[0][2] >= minZ && minMax[1][2] <= maxZ) {
         return true;
      }

      if (maxX < minMax[0][0] || minX > minMax[1][0]) {
         return false;
      }
      if (maxY < minMax[0][1] || minY > minMax[1][1]) {
         return false;
      }
      if (maxZ < minMax[0][2] || minZ > minMax[1][2]) {
         return false;
      }

      return true;
   }
}
