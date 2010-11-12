package stephen.ranger.ar.photons;

import java.util.ArrayList;
import java.util.Arrays;
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
   public final SeparationAxis splitAxis;
   public final float medianValue;

   public static float maxSearchDistanceSquared = 1f;

   public PhotonTreeNode(final Photon[] photons, final float[][] minMax, final int depth) {
      this.minMax = minMax;
      this.photons = photons;
      this.bv = new AxisAlignedBoundingBox(null, minMax);
      SeparationAxis childSplitAxis = SeparationAxis.X;

      if ((photons.length > RTStatics.MAX_CHILDREN) && (depth < RTStatics.MAX_DEPTH)) {
         final List<Photon> leftChildren = new ArrayList<Photon>();
         final List<Photon> rightChildren = new ArrayList<Photon>();

         final float medianX = (this.minMax[1][0] - this.minMax[0][0]) / 2f + this.minMax[0][0];
         final float medianY = (this.minMax[1][1] - this.minMax[0][1]) / 2f + this.minMax[0][1];
         final float medianZ = (this.minMax[1][2] - this.minMax[0][2]) / 2f + this.minMax[0][2];
         float median = medianX;

         if ((medianY > medianX) && (medianY > medianZ)) {
            childSplitAxis = SeparationAxis.Y;
            median = medianY;
         } else if ((medianZ > medianY) && (medianZ > medianX)) {
            childSplitAxis = SeparationAxis.Z;
            median = medianZ;
         }

         this.splitAxis = childSplitAxis;
         this.medianValue = median;

         final float[][] leftMinMax = new float[][] { { Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE }, { -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE } };
         final float[][] rightMinMax = new float[][] { { Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE }, { -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE } };
         final float[][] childMinMax = new float[2][3];

         for (final Photon photon : photons) {
            RTStatics.getMinMax(new Photon[] { photon }, childMinMax);

            if (childMinMax[1][childSplitAxis.pos] <= median) {
               leftChildren.add(photon);
            } else if (childMinMax[0][childSplitAxis.pos] >= median) {
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
         this.splitAxis = null;
         this.medianValue = 0;

         PhotonTree.leafCount += 1;
         PhotonTree.cumulativeLeafDepth += depth;
         PhotonTree.cumulativeLeafSize += photons.length;

         RTStatics.incrementProgressBarValue(photons.length);
      }
   }

   // public Collection<Photon> getPhotonsInRange(final Vector3f p, final float range, final Camera camera) {
   // final List<Photon> list = new ArrayList<Photon>();
   //
   // if ((this.left != null) || (this.right != null)) {
   // if (this.left != null) {
   // final Collection<Photon> leftMatches = this.left.getPhotonsInRange(p, range, camera);
   //
   // if ((leftMatches != null) && (leftMatches.size() > 0)) {
   // list.addAll(leftMatches);
   // }
   // }
   //
   // if (this.right != null) {
   // final Collection<Photon> rightMatches = this.right.getPhotonsInRange(p, range, camera);
   //
   // if ((rightMatches != null) && (rightMatches.size() > 0)) {
   // list.addAll(rightMatches);
   // }
   // }
   // } else {
   // final float[] location = new float[3];
   // p.get(location);
   // final float rangeSquared = range * range;
   //
   // for (final Photon photon : this.photons) {
   // if (this.photonAABBIntersection(photon, this.minMax)) {
   // final float distanceSquared = RTStatics.getDistanceSquared(photon.location, location);
   //
   // if (distanceSquared - rangeSquared < 0f) {
   // list.add(photon);
   // }
   // }
   // }
   // }
   //
   // return list;
   // }
   //
   // public Collection<Photon> getPhotonsInRange(final IntersectionInformation info, final float range, final Camera camera) {
   // final List<Photon> list = new ArrayList<Photon>();
   //
   // if ((this.left != null) || (this.right != null)) {
   // if (this.left != null) {
   // final Collection<Photon> leftMatches = this.left.getPhotonsInRange(info, range, camera);
   //
   // if ((leftMatches != null) && (leftMatches.size() > 0)) {
   // list.addAll(leftMatches);
   // }
   // }
   //
   // if (this.right != null) {
   // final Collection<Photon> rightMatches = this.right.getPhotonsInRange(info, range, camera);
   //
   // if ((rightMatches != null) && (rightMatches.size() > 0)) {
   // list.addAll(rightMatches);
   // }
   // }
   // } else {
   // final Matrix4f rotation = new Matrix4f();
   // final float step = 180f / RTStatics.PHOTON_COLLECTION_GRID_SIZE;
   // final float[] location = new float[3];
   // final float rangeSquared = range * range;
   //
   // for (int x = 0; x <= RTStatics.PHOTON_COLLECTION_GRID_SIZE; x++) {
   // for (int y = 0; y <= RTStatics.PHOTON_COLLECTION_GRID_SIZE; y++) {
   // final Vector3f dir = new Vector3f(info.normal);
   // rotation.set(RTStatics.initializeQuat4f(new float[] { x * step - 90f, y * step - 90f, 0 }));
   // rotation.transform(dir);
   // dir.normalize();
   //
   // final IntersectionInformation closest = camera.getClosestIntersection(info.intersectionObject, info.intersection, dir);
   //
   // if (closest != null) {
   // closest.intersection.get(location);
   // for (final Photon photon : this.photons) {
   // if (this.photonAABBIntersection(photon, this.minMax)) {
   // final float distanceSquared = RTStatics.getDistanceSquared(photon.location, location);
   //
   // if (distanceSquared - rangeSquared < 0f) {
   // list.add(photon);
   // }
   // }
   // }
   // }
   // }
   // }
   // }
   //
   // return list;
   // }

   /**
    * From: Advanced global illumination using photon mapping
    * http://portal.acm.org/citation.cfm?id=1401136
    * 
    * given the photon map, a position x and a max search distance d2
    * this recursive function returns a heap h with the nearest photons.
    * 
    * Call with locate photons(1) to initiate search at the root of the kd-tree
    * 
      locate photons( p ) {
         if ( 2p + 1 < number of photons ) {
            examine child nodes

            // Compute distance to plane ( just a subtract)
            delta = signed distance to splitting plane of node n

            if (delta < 0) {
               // We are left of the plane - search left subtree first
               locate photons( 2p )

               if ( delta2 < d2 ) {
                  locate photons( 2p + 1 ) check right subtree
               }
            } else {
               // We are right  of the plane - search right subtree first
               locate photons( 2p + 1 )

               if ( delta2 < d2 ) {
                  locate photons( 2p ) check left subtree
               }
            }
         }

         // Compute true squared distance to photon
         delta= squared distance from photon p to x

         if ( delta2 < d2 ) {
            // Check if the photon is close enough?
            insert photon into max heap h

            //  Adjust maximum distance  to prune the search
            d2 = squared distance to photon in root node of h
         }
      }
    */
   public void locatePhotons(final float[] location, final Collection<Photon> list) {
      // if this is the root node, start the search on it's children
      // else if, we don't have the number of photons we need, search itself and it's children
      if (this.splitAxis != null) {
         // if (list.size() < RTStatics.COLLECTION_COUNT_THRESHOLD) {
         if (true) {
            final float delta = location[this.splitAxis.pos] - this.medianValue;

            if (delta < 0) {
               if (this.left != null) {
                  this.left.locatePhotons(location, list);
               }

               if ((this.right != null) && (delta * delta < maxSearchDistanceSquared)) {
                  this.right.locatePhotons(location, list);
               }
            } else {
               if (this.right != null) {
                  this.right.locatePhotons(location, list);
               }

               if ((this.left != null) && (delta * delta < maxSearchDistanceSquared)) {
                  this.left.locatePhotons(location, list);
               }
            }
         }
      }

      for (final Photon photon : this.photons) {
         final float delta = RTStatics.getDistanceSquared(photon.location, location);

         if (delta < maxSearchDistanceSquared) {
            list.add(photon);
            maxSearchDistanceSquared = delta;
         }
      }
   }

   /**
    * http://www.cs.fsu.edu/~lifeifei/cis5930/kdtree.pdf
    * 
      1: if v is a leaf
      2: then  Report the stored at v if it lies in R
      3: else
            if region(lv(c)) is fully contained in R
      4:       then REPORTSUBTREE(lc(v))
      5:    else if region(lc(v)) intersects R
      6:       then SEARCHKDTREE(lc(v),R)

      7:    if region(rv(c)) is fully contained in R
      8:       then REPORTSUBTREE(rc(v))
      9:    else if region(lc(v)) intersects R
      10:      then SEARCHKDTREE(lc(v),R)

    * @param location
    * @param parent
    * @param maxSearchDistanceSquared
    * @param list
    */
   public static void rangeSearch(final float[] location, final PhotonTreeNode parent, final float maxSearchDistanceSquared, final Collection<Photon> list) {
      if(parent.splitAxis == null) {
         for(final Photon photon : parent.photons) {
            if (RTStatics.getDistanceSquared(photon.location, location) < maxSearchDistanceSquared) {
               list.add(photon);
            }
         }
      } else {
         if (parent.left != null) {
            if (parent.left.isNodeContainedInRange(location, maxSearchDistanceSquared)) {
               list.addAll(Arrays.asList(parent.left.photons));
            } else if (parent.left.isNodeIntersectedByRange(location, maxSearchDistanceSquared)) {
               PhotonTreeNode.rangeSearch(location, parent.left, maxSearchDistanceSquared, list);
            }
         }

         if (parent.right != null) {
            if (parent.right.isNodeContainedInRange(location, maxSearchDistanceSquared)) {
               list.addAll(Arrays.asList(parent.right.photons));
            } else if (parent.right.isNodeIntersectedByRange(location, maxSearchDistanceSquared)) {
               PhotonTreeNode.rangeSearch(location, parent.right, maxSearchDistanceSquared, list);
            }
         }
      }
   }

   public boolean isNodeContainedInRange(final float[] point, final float rangeSquared) {
      return ((RTStatics.getDistanceSquared(point, this.minMax[0]) < rangeSquared) && (RTStatics.getDistanceSquared(point, this.minMax[1]) < rangeSquared));
   }

   public boolean isNodeIntersectedByRange(final float[] point, final float rangeSquared) {
      return RTStatics.aabbIntersection(this.minMax, RTStatics.getMinMax(point, (float) Math.sqrt(rangeSquared)));
   }
}
