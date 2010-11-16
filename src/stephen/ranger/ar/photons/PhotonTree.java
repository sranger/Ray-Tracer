package stephen.ranger.ar.photons;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import stephen.ranger.ar.RTStatics;
import stephen.ranger.ar.RTStatics.SeparationAxis;
import stephen.ranger.ar.photons.Photon.LightAttribution;

/**
 * A KDTree is a special case of Oct-Tree where the split axis and location are determined
 * by the axis with the max distance between its min and max value and split where the left
 * and right trees will contain the same number of children regardless of spatial location.
 * 
 * @param <E> A class that implements the PointLocation interface
 */
public class PhotonTree {
   /** Array position for the x-axis. */
   public static final int X_AXIS = 0;
   /** Array position for the y-axis. */
   public static final int Y_AXIS = 1;
   /** Array position for the z-axis. */
   public static final int Z_AXIS = 2;

   private final Photon[] photons;
   private final int[] indices;
   private final PhotonTree left, right;
   private final SeparationAxis splitAxis;
   private final float medianValue;

   /** Running count of total leaf nodes created. Only for debugging; will not work correctly if multiple KDTrees are being created concurrently. */
   public static int leafNodes = 0;
   /** Running count of total children in all leaf nodes. Only for debugging; will not work correctly if multiple KDTrees are being created concurrently. */
   public static int leafNodeChildrenCount = 0;
   /** Updated value of current max depth of nodes created. Only for debugging; will not work correctly if multiple KDTrees are being created concurrently. */
   public static int maxTreeDepth = 0;

   /**
    * Creates a new KDTree with the given PointLocation array using log2(points.length) as the max depth of the tree.
    * 
    * @param points  The points to create the KDTree from
    */
   public PhotonTree(final Photon[] photons) {
      // use the log base 2 value of the points length to get a balanced tree with one node per leaf.
      // Multiply by 0.8 to decrease depth so it isn't too deep
      this(photons, null, 1, (int) (Math.log10(photons.length) / Math.log10(2) * 0.8), 0);
   }

   /**
    * Creates a new KDTree with the given PointLocation array and using the given settings for depth and children count for leaf nodes.
    * 
    * @param points        The points to create the KDTree from
    * @param minChildren   The minimum number of children in a node before no split will be performed
    * @param maxDepth      The max depth for a leaf node before no split will be performed
    */
   public PhotonTree(final Photon[] photons, final int minChildren, final int maxDepth) {
      this(photons, null, minChildren, maxDepth, 0);
   }

   /**
    * Creates a new KDTree with the given PointLocation array only using the points denoted by the indices array given.
    * Uses the given settings for leaf node depth and children count.
    * 
    * @param points        The points to create the KDTree from
    * @param indices       The indices denoting the points to use; all other points are ignored
    * @param minChildren   The minimum number of children in a node before no split will be performed
    * @param maxDepth      The max depth for a leaf node before no split will be performed
    */
   public PhotonTree(final Photon[] photons, final int[] indices, final int minChildren, final int maxDepth) {
      this(photons, indices, minChildren, maxDepth, 0);
   }

   /**
    * Creates a new KDTree with the given PointLocation array only using the points denoted by the indices array given.
    * Uses the given settings for leaf node depth and children count. If indices is null, will use all points.
    * 
    * @param points        The points to create the KDTree from
    * @param indices       The indices denoting the points to use; all other points are ignored
    * @param minChildren   The minimum number of children in a node before no split will be performed
    * @param maxDepth      The max depth for a leaf node before no split will be performed
    * @param currentDepth  The current depth of the tree. The root node will have a depth of 0
    */
   private PhotonTree(final Photon[] photons, final int[] indices, final int minChildren, final int maxDepth, final int currentDepth) {
      // if the depth is 0, reset the static debug values
      if (currentDepth == 0) {
         leafNodes = 0;
         leafNodeChildrenCount = 0;
         maxTreeDepth = 0;
      }

      // store the given points and indices. if indices are null, create an array containing a reference to all points in the points array
      this.photons = photons;
      this.indices = indices == null ? createIndicesArray(photons.length) : indices;

      // create children nodes if the number of indices for this node is greater than the minChildren value and the current depth is less than the max depth
      if (this.indices.length > minChildren && currentDepth < maxDepth) {
         // get the minMax values for this set of indices
         final float minMax[][] = this.getMinMax(photons, this.indices);
         // get the distance between the min and max for each axis
         final float xSpan = minMax[1][0] - minMax[0][0];
         final float ySpan = minMax[1][1] - minMax[0][1];
         final float zSpan = minMax[1][2] - minMax[0][2];

         // determine the split axis based on the max span distance for each axis
         splitAxis = xSpan >= ySpan && xSpan >= zSpan ? SeparationAxis.X : ySpan >= xSpan && ySpan >= zSpan ? SeparationAxis.Y : SeparationAxis.Z;

         // sort our indices array based on the selected split axis
         RTStatics.quicksort(photons, this.indices, 0, this.indices.length - 1, splitAxis);

         // set the split index as the center value for the indices array
         final int medianIndex = this.indices.length / 2;
         // store the location of the center point
         final float[] medianPointLocation = photons[this.indices[medianIndex]].location;
         // set the split value as the value of the center point's splitAxis value
         medianValue = medianPointLocation[splitAxis.pos];

         // create the left and right children nodes based on the split position
         left = medianIndex == 0 ? null : new PhotonTree(photons, Arrays.copyOfRange(this.indices, 0, medianIndex), minChildren, maxDepth, currentDepth + 1);
         right = medianIndex >= this.indices.length ? null : new PhotonTree(photons, Arrays.copyOfRange(this.indices, medianIndex, this.indices.length),
               minChildren, maxDepth, currentDepth + 1);
      } else {
         // invalidate children info if this node is a leaf node
         left = null;
         right = null;
         splitAxis = null;
         medianValue = 0;

         // increment debugging values
         leafNodes++;
         leafNodeChildrenCount += this.indices.length;
         maxTreeDepth = Math.max(maxTreeDepth, currentDepth);
      }
   }

   /**
    * Returns the closest object to the given location.
    * 
    * @param location   The location to search for its nearest neighbor
    * @return           Index of the nearest neighbor found to given location or -1 if none were found
    */
   public int nearestNeighbor(final float[] location) {
      // create a temp map to store our heap of found points
      final SortedMap<Integer, Float> heap = new TreeMap<Integer, Float>();
      // call our search method with a count of 1
      this.kNearest(heap, location, 1);

      // if our search didn't find any points, return null
      // else, return the first object in the heap (should only be one)
      if (heap.size() == 0) {
         return -1;
      } else {
         return heap.firstKey();
      }
   }

   /**
    * Returns the indices of the closest objects to the given location.
    * The count (k) is the max number of neighbors to find.
    * 
    * @param location   The location to use as the central location
    * @param k          The max number of points to return
    * @return           An array of indices for the points found
    */
   public int[] kNearest(final float[] location, final int k) {
      // temp map for our heap
      final SortedMap<Integer, Float> heap = new TreeMap<Integer, Float>();
      // call our search method with a count of k
      this.kNearest(heap, location, k);

      // create our return int array
      final int[] foundIndices = new int[heap.size()];
      int ctr = 0;

      // fill the return array with the indices in the heap
      for (final Entry<Integer, Float> entry : heap.entrySet()) {
         foundIndices[ctr] = entry.getKey();
         ctr++;
      }

      return foundIndices;
   }

   /**
    * Returns the closest objects to the given location. The count is the max number of neighbors to find.
    * 
    * @param heap       The sorted map that will be used to store the found points
    * @param location   The location to search for its nearest neighbors
    * @param k          The maximum number of points to search for
    * @return           The k nearest neighbors to the given location
    */
   private void kNearest(final SortedMap<Integer, Float> heap, final float[] location, final int k) {
      final float m2 = RTStatics.COLLECTION_RANGE * RTStatics.COLLECTION_RANGE;

      // if the current node is not a leaf node, search its children
      if (splitAxis != null) {
         // get the distance from the search location to this nodes' median value
         final float distanceToMedian = location[splitAxis.pos] - medianValue;
         final float d2 = distanceToMedian * distanceToMedian;
         final float last2 = heap.size() > 0 ? heap.get(heap.lastKey()) : m2;

         // if either left or right are not null, do search (should always be true if this is not a leaf node)
         if (left != null || right != null) {
            // if the location is on the left side of the median, search the left node first
            // else, search right first
            if (distanceToMedian < 0) {
               // if left is not null, search left
               if (left != null) {
                  left.kNearest(heap, location, k);
               }
               // right is not null and if the heap isn't full or the distance from median to given location is less than the distance to the last point in the heap
               if (right != null && (heap.size() < k || d2 < last2) && Math.abs(distanceToMedian) <= m2) {
                  right.kNearest(heap, location, k);
               }
            } else {
               // if right is not null, search right
               if (right != null) {
                  right.kNearest(heap, location, k);
               }
               // if right is not null and if the heap isn't full or the distance from median to given location is less than the distance to the last point in the heap
               if (left != null && (heap.size() < k || d2 < last2) && Math.abs(distanceToMedian) <= m2) {
                  left.kNearest(heap, location, k);
               }
            }
         }
      }
      // else, check the leaf node's children
      else {
         // create some temp variables and store some temp values so we don't have to re-compute them too much
         float distanceSquared, x, y, z;
         float[] tempPos;
         int heapSize = heap.size();
         float currentMax = heapSize == 0 ? RTStatics.COLLECTION_RANGE : heap.get(heap.lastKey());

         // iterate over all the children indices
         for (final int index : indices) {
            // get current point's location
            tempPos = photons[index].location;
            x = tempPos[0] - location[0];
            y = tempPos[1] - location[1];
            z = tempPos[2] - location[2];
            // get distance from current point to the search location
            distanceSquared = x * x + y * y + z * z;

            // if the heap isn't full, insert the current point
            if (heapSize < k && distanceSquared <= m2) {
               heap.put(index, distanceSquared);
               heapSize++;
            }
            // else if the distance to the current point is less than the furthest point in the heap,
            // remove the furthest point and insert the current point
            else if (distanceSquared < currentMax && distanceSquared <= m2) {
               heap.remove(heap.lastKey());
               heap.put(index, distanceSquared);
               currentMax = heap.get(heap.lastKey());
            }
         }
      }
   }

   public int[] rangeSearch(final float[] location, final float range) {
      // create a temp collection to store the results
      final List<Integer> list = new ArrayList<Integer>();
      // call our search method
      this.rangeSearch(list, location, range);

      // create a return array
      final int[] foundIndices = new int[list.size()];

      // fill the return array with the indices found by our range search method
      for (int i = 0; i < foundIndices.length; i++) {
         foundIndices[i] = list.get(i);
      }

      return foundIndices;
   }

   /**
    * Returns any objects within the given range from the given location.
    * 
    * @param location   The location to search for
    * @param range      The range from the location to search in
    * @return           An array of objects within the given range of the given location
    */
   private void rangeSearch(final Collection<Integer> list, final float[] location, final float range) {
      // if this node is not a leaf node
      if (splitAxis != null) {
         // get distance from search location to current node's split value
         final float distanceToMedian = location[splitAxis.pos] - medianValue;

         // if left or right are not null (should always be true for a non-leaf node)
         if (left != null || right != null) {
            // if the search location is on the left of the split axis, search left first
            if (distanceToMedian < 0) {
               // if left is not null, search on it
               if (left != null) {
                  left.rangeSearch(list, location, range);
               }
               // if right is not null and the distance from the search location is less than or equal to the search range
               if (right != null && distanceToMedian * distanceToMedian <= range * range) {
                  right.rangeSearch(list, location, range);
               }
            }
            // else, the search location is on the right, so search right first
            else {
               // if right is not null
               if (right != null) {
                  right.rangeSearch(list, location, range);
               }
               // if left is not null and the distance from the search location is less than or equal to the search range
               if (left != null && distanceToMedian * distanceToMedian <= range * range) {
                  left.rangeSearch(list, location, range);
               }
            }
         }
      }
      // check children indices to see if they're within the range
      else {
         // create some temp variables
         float distanceSquared, x, y, z;
         float[] tempPos;

         // iterate over children indices and check them against the range
         for (final int index : indices) {
            tempPos = photons[index].location;
            x = tempPos[0] - location[0];
            y = tempPos[1] - location[1];
            z = tempPos[2] - location[2];
            distanceSquared = x * x + y * y + z * z;

            // if the current point is within the range, add its index to the list
            if (distanceSquared <= range * range) {
               list.add(index);
            }
         }
      }
   }

   /**
    * Return the minMax for the given point locations denoted by the given index array for the given axis.
    * 
    * @param points  The master set of points
    * @param indices The index locations of the points in the points array to find the minMax for
    * @param axis    The axis to get the minMax value for
    * @return        float array containing { min, max } for the given axis
    */
   private float[] getMinMax(final Photon[] points, final int[] indices, final int axis) {
      final float[] minMax = new float[] { Float.MAX_VALUE, -Float.MAX_VALUE };
      float[] location;

      // get min/max values for all the points denoted by the indices for the given axis
      for (final int index : indices) {
         location = points[index].location;
         minMax[0] = Math.min(minMax[0], location[axis]);
         minMax[1] = Math.max(minMax[1], location[axis]);
      }

      return minMax;
   }

   /**
    * Return the minMax for the given point locations denoted by the given index array for all three axes.
    * 
    * @param points  The master set of points
    * @param indices The index locations of the points in the points array to find the minMax for
    * @return        float matrix containing { {minX, minY, minZ}, {maxX, maxY, maxZ} }
    */
   private float[][] getMinMax(final Photon[] points, final int[] indices) {
      final float[][] minMax = new float[2][3];

      // get the min/max for each axis
      final float[] x = this.getMinMax(points, indices, X_AXIS);
      final float[] y = this.getMinMax(points, indices, Y_AXIS);
      final float[] z = this.getMinMax(points, indices, Z_AXIS);

      // set the values in a matrix
      minMax[0][0] = x[0];
      minMax[1][0] = x[1];

      minMax[0][1] = y[0];
      minMax[1][1] = y[1];

      minMax[0][2] = z[0];
      minMax[1][2] = z[1];

      return minMax;
   }

   /**
    * Create a default indices array.
    * 
    * @param numPoints  The number of points to add to the indices array
    * @return           An array of indices containing 0, 1, 2, ..., numPoints-2, numPoints-1
    */
   private int[] createIndicesArray(final int numPoints) {
      final int[] indices = new int[numPoints];

      for (int i = 0; i < numPoints; i++) {
         indices[i] = i;
      }

      return indices;
   }

   /**
    * Returns the point at the given index.
    * 
    * @param index   The index of the point requested
    * @return        The point requested at the given index
    */
   public final Photon get(final int index) {
      // if index is valid, return the point at the given index, else, return null
      if (index >= 0 && index < photons.length) {
         return photons[index];
      } else {
         return null;
      }
   }

   /**
    * Test implementation that shows the creation time, five nearest points, the max depth,
    * number of leaf nodes, and the average indices per leaf node.
    * 
    * @param args Command line arguments; not used
    */
   public static void main(final String[] args) {
      // create vert array with a power of two length (to show off default max depth balancing)
      final Photon[] verts = new Photon[(int) Math.pow(2, 17)];
      // create random object used to create verts
      final Random r = new Random(1234567890);

      // create random vertices
      for (int i = 0; i < verts.length; i++) {
         verts[i] = new Photon(new float[] { 0, 0, 0 }, new float[] { r.nextInt(1000), r.nextInt(1000), r.nextInt(1000) }, new float[] { 0, 0, 0 }, 10,
               LightAttribution.DIFFUSE);
      }

      System.out.println("vert count: " + verts.length);

      // create random location
      final float[] location = new float[] { r.nextInt(1000), r.nextInt(1000), r.nextInt(1000) };

      // create kdtree and keep track of time it takes to build
      long start = System.nanoTime();
      final PhotonTree kdtree = new PhotonTree(verts);
      long end = System.nanoTime();

      // print out build duration
      System.out.println("creation time: " + (end - start) / 1000000000. + " seconds");

      // search for the five nearest points in the kdtree and keep track of duration
      start = System.nanoTime();
      final SortedMap<Integer, Float> heap = new TreeMap<Integer, Float>();
      kdtree.kNearest(heap, location, 5);
      end = System.nanoTime();

      // print out the five nearest points and the search duration
      System.out.println("5 nearest at: " + Arrays.toString(location) + "\n----------------------------------------");
      for (final Entry<Integer, Float> entry : heap.entrySet()) {
         System.out.println(kdtree.get(entry.getKey()));
      }

      System.out.println("----------------------------------------\nsearch duration: " + (end - start) / 1000000000. + " seconds");

      // print out debugging stats for the kdtree
      System.out.println("max tree depth:           " + kdtree.maxTreeDepth);
      System.out.println("number of leaf nodes:     " + kdtree.leafNodes);
      System.out.println("average indices per leaf: " + (float) kdtree.leafNodeChildrenCount / (float) kdtree.leafNodes);
   }
}
