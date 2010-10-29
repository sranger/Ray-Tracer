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
      this.bv = new AxisAlignedBoundingBox(null, minMax);
      SeparationAxis axis = SeparationAxis.X;

      if ((photons.length > RTStatics.MAX_CHILDREN) && (depth < RTStatics.MAX_DEPTH)) {
         final List<Photon> leftChildren = new ArrayList<Photon>();
         final List<Photon> rightChildren = new ArrayList<Photon>();

         final float medianX = (this.minMax[1][0] - this.minMax[0][0]) / 2f + this.minMax[0][0];
         final float medianY = (this.minMax[1][1] - this.minMax[0][1]) / 2f + this.minMax[0][1];
         final float medianZ = (this.minMax[1][2] - this.minMax[0][2]) / 2f + this.minMax[0][2];
         float median = medianX;

         if ((medianY > medianX) && (medianY > medianZ)) {
            axis = SeparationAxis.Y;
            median = medianY;
         } else if ((medianZ > medianY) && (medianZ > medianX)) {
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
            this.left = new PhotonTreeNode(leftPhotons, leftMinMax, depth + 1);
         } else {
            this.left = null;
         }

         if (rightChildren.size() > 0) {
            final Photon[] rightPhotons = rightChildren.toArray(new Photon[rightChildren.size()]);
            RTStatics.getMinMax(rightPhotons, rightMinMax);
            this.right = new PhotonTreeNode(rightPhotons, rightMinMax, depth + 1);
         } else {
            this.right = null;
         }
      } else {
         this.left = null;
         this.right = null;

         PhotonTree.leafCount += 1;
         PhotonTree.cumulativeLeafDepth += depth;
         PhotonTree.cumulativeLeafSize += photons.length;

         RTStatics.incrementProgressBarValue(photons.length);
      }
   }

   public Collection<Photon> getPhotonsInRange(final Vector3f p, final float range, final Camera camera) {
      final List<Photon> list = new ArrayList<Photon>();

      if ((this.left != null) || (this.right != null)) {
         if (this.left != null) {
            final Collection<Photon> leftMatches = this.left.getPhotonsInRange(p, range, camera);

            if ((leftMatches != null) && (leftMatches.size() > 0)) {
               list.addAll(leftMatches);
            }
         }

         if (this.right != null) {
            final Collection<Photon> rightMatches = this.right.getPhotonsInRange(p, range, camera);

            if ((rightMatches != null) && (rightMatches.size() > 0)) {
               list.addAll(rightMatches);
            }
         }
      } else {
         final float[] location = new float[3];
         p.get(location);
         final float rangeSquared = range * range;

         for (final Photon photon : this.photons) {
            if (this.photonAABBIntersection(photon, this.minMax)) {
               final float distanceSquared = RTStatics.getDistanceSquared(photon.location, location);

               if (distanceSquared - rangeSquared < 0f) {
                  list.add(photon);
               }
            }
         }
      }

      return list;
   }

   public Collection<Photon> getPhotonsInRange(final IntersectionInformation info, final float range, final Camera camera) {
      final List<Photon> list = new ArrayList<Photon>();

      if ((this.left != null) || (this.right != null)) {
         if (this.left != null) {
            final Collection<Photon> leftMatches = this.left.getPhotonsInRange(info, range, camera);

            if ((leftMatches != null) && (leftMatches.size() > 0)) {
               list.addAll(leftMatches);
            }
         }

         if (this.right != null) {
            final Collection<Photon> rightMatches = this.right.getPhotonsInRange(info, range, camera);

            if ((rightMatches != null) && (rightMatches.size() > 0)) {
               list.addAll(rightMatches);
            }
         }
      } else {
         final Matrix4f rotation = new Matrix4f();
         final float step = 180f / RTStatics.PHOTON_COLLECTION_GRID_SIZE;
         final float[] location = new float[3];
         final float rangeSquared = range * range;

         for (int x = 0; x <= RTStatics.PHOTON_COLLECTION_GRID_SIZE; x++) {
            for (int y = 0; y <= RTStatics.PHOTON_COLLECTION_GRID_SIZE; y++) {
               final Vector3f dir = new Vector3f(info.normal);
               rotation.set(RTStatics.initializeQuat4f(new float[] { x * step - 90f, y * step - 90f, 0 }));
               rotation.transform(dir);
               dir.normalize();

               final IntersectionInformation closest = camera.getClosestIntersection(info.intersectionObject, info.intersection, dir);

               if (closest != null) {
                  closest.intersection.get(location);
                  for (final Photon photon : this.photons) {
                     if (this.photonAABBIntersection(photon, this.minMax)) {
                        final float distanceSquared = RTStatics.getDistanceSquared(photon.location, location);

                        if (distanceSquared - rangeSquared < 0f) {
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
    * From: Advanced global illumination using photon mapping
    * http://portal.acm.org/citation.cfm?id=1401136
    * 
      locate photons( p ) {
         if ( 2p + 1 < number of photons ) {
            examine child nodes

            // Compute distance to plane (final just a subtract)
            delta = signed distance to splitting plane of node n

            if (delta < 0) {
               // We are left of the plane - search left subtree first
               locate photons( 2p )

               if ( delta2 < d2 ) {
                  locate photons( 2p + 1 ) check right subtree
               }
            } else {
               // We are right final of the plane - search right subtree first
               locate photons( 2p + 1 )

               if ( delta2 < d2 ) {
                  locate photons( 2p ) check left subtree
               }
            }
         }

         // Compute true squared distance to photon
         delta= squared distance from photon p to x

         if ( delta2 < d2 ) {
            // Check if the photon is final close enough?
            insert photon into final max heap h

            // final Adjust maximum distance final to prune the search
            d2 = squared distance final to photon in final root node final of h
         }
      }
    */
   public Collection<Photon> locatePhotons(final float[] p) {
      final List<Photon> list = new ArrayList<Photon>();

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

      if ((minMax[0][0] >= minX) && (minMax[1][0] <= maxX) && (minMax[0][1] >= minY) && (minMax[1][1] <= maxY) && (minMax[0][2] >= minZ) && (minMax[1][2] <= maxZ)) {
         return true;
      }

      if ((maxX < minMax[0][0]) || (minX > minMax[1][0])) {
         return false;
      }
      if ((maxY < minMax[0][1]) || (minY > minMax[1][1])) {
         return false;
      }
      if ((maxZ < minMax[0][2]) || (minZ > minMax[1][2])) {
         return false;
      }

      return true;
   }
}
