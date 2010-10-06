package stephen.ranger.ar.bounds;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Vector3d;

import stephen.ranger.ar.ColorInformation;
import stephen.ranger.ar.IntersectionInformation;
import stephen.ranger.ar.RTStatics;
import stephen.ranger.ar.Ray;
import stephen.ranger.ar.bounds.KDTree.SeparationAxis;

public class KDNode extends BoundingVolume {
    public final BoundingVolume[] children;
    public KDNode left;
    public KDNode right;
    public final SeparationAxis axis;
    public final Vector3d[] minMax;
    public final int depth;

    public KDNode(final BoundingVolume[] children, final Vector3d[] minMax, final SeparationAxis axis, final int depth) {
        this.children = children;
        this.axis = axis;
        this.minMax = minMax;
        this.depth = depth;

        if (depth < KDTree.MAX_DEPTH && children.length > RTStatics.MAX_CHILDREN) {
            double median;
            final List<BoundingVolume> leftChildren = new ArrayList<BoundingVolume>();
            final List<BoundingVolume> rightChildren = new ArrayList<BoundingVolume>();

            if (axis.equals(SeparationAxis.X)) {
                median = (minMax[1].x - minMax[0].x) / 2. + minMax[0].x;
            } else if (axis.equals(SeparationAxis.Y)) {
                median = (minMax[1].y - minMax[0].y) / 2. + minMax[0].y;
            } else {
                median = (minMax[1].z - minMax[0].z) / 2. + minMax[0].z;
            }

            final Vector3d[] leftMinMax = new Vector3d[] { new Vector3d(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE),
                    new Vector3d(-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE) };
            final Vector3d[] rightMinMax = new Vector3d[] { new Vector3d(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE),
                    new Vector3d(-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE) };

            for (final BoundingVolume child : children) {
                final Vector3d[] childMinMax = child.getMinMax();
                int isLeftRightBoth = 0; // -1 for left, 0 for both, 1 for right

                if (axis.equals(SeparationAxis.X)) {
                    if (childMinMax[1].x <= median) {
                        leftChildren.add(child);
                        isLeftRightBoth = -1;
                    } else if (childMinMax[0].x >= median) {
                        rightChildren.add(child);
                        isLeftRightBoth = 1;
                    } else {
                        leftChildren.add(child);
                        rightChildren.add(child);
                    }
                } else if (axis.equals(SeparationAxis.Y)) {
                    if (childMinMax[1].y <= median) {
                        leftChildren.add(child);
                        isLeftRightBoth = -1;
                    } else if (childMinMax[0].y >= median) {
                        rightChildren.add(child);
                        isLeftRightBoth = 1;
                    } else {
                        leftChildren.add(child);
                        rightChildren.add(child);
                    }
                } else {
                    if (childMinMax[1].z <= median) {
                        leftChildren.add(child);
                        isLeftRightBoth = -1;
                    } else if (childMinMax[0].z >= median) {
                        rightChildren.add(child);
                        isLeftRightBoth = 1;
                    } else {
                        leftChildren.add(child);
                        rightChildren.add(child);
                    }
                }

                if (leftChildren.size() > 0 && (isLeftRightBoth == -1 || isLeftRightBoth == 0)) {
                    leftMinMax[0].set(Math.min(leftMinMax[0].x, childMinMax[0].x), Math.min(leftMinMax[0].y, childMinMax[0].y), Math.min(leftMinMax[0].z,
                            childMinMax[0].z));
                    leftMinMax[1].set(Math.max(leftMinMax[1].x, childMinMax[1].x), Math.max(leftMinMax[1].y, childMinMax[1].y), Math.max(leftMinMax[1].z,
                            childMinMax[1].z));
                }

                if (rightChildren.size() > 0 && (isLeftRightBoth == 1 || isLeftRightBoth == 0)) {
                    rightMinMax[0].set(Math.min(rightMinMax[0].x, childMinMax[0].x), Math.min(rightMinMax[0].y, childMinMax[0].y), Math.min(rightMinMax[0].z,
                            childMinMax[0].z));
                    rightMinMax[1].set(Math.max(rightMinMax[1].x, childMinMax[1].x), Math.max(rightMinMax[1].y, childMinMax[1].y), Math.max(rightMinMax[1].z,
                            childMinMax[1].z));
                }
            }

            if (leftChildren.size() > 0) {
                left = new KDNode(leftChildren.toArray(new BoundingVolume[leftChildren.size()]), leftMinMax, axis.getNextAxis(), depth + 1);
            } else {
                left = null;
            }

            if (rightChildren.size() > 0) {
                right = new KDNode(rightChildren.toArray(new BoundingVolume[rightChildren.size()]), rightMinMax, axis.getNextAxis(), depth + 1);
            } else {
                right = null;
            }
        } else {
            left = null;
            right = null;
        }
    }

    @Override
    public IntersectionInformation getChildIntersection(final Ray ray) {
        if (left != null || right != null) {
            IntersectionInformation tempLeft = null, tempRight = null;

            if (left != null && left.intersects(ray)) {
                tempLeft = left.getChildIntersection(ray);
            }

            if (right != null && right.intersects(ray)) {
                tempRight = right.getChildIntersection(ray);
            }

            return tempLeft == null && tempRight == null ? null : tempLeft == null ? tempRight : tempRight == null ? tempLeft
                    : tempLeft.w < tempRight.w ? tempLeft : tempRight;
        } else {
            IntersectionInformation temp, closest = null;

            for (final BoundingVolume child : children) {
                if (child.intersects(ray)) {
                    temp = child.getChildIntersection(ray);

                    if (temp != null && (closest == null || temp.w < closest.w)) {
                        closest = temp;
                    }
                }
            }

            return closest;
        }
    }

    @Override
    public Vector3d[] getMinMax() {
        return new Vector3d[] { new Vector3d(minMax[0]), new Vector3d(minMax[1]) };
    }

    @Override
    public boolean intersects(final Ray ray) {
        return RTStatics.aabbIntersection(ray, minMax);
    }

    @Override
    public Color getColor(final IntersectionInformation info) {
        return info.intersectionObject.getColor(info);
    }

    @Override
    public ColorInformation getColorInformation(final IntersectionInformation info) {
        if (info.intersectionObject instanceof AxisAlignedBoundingBox) {
            return ((AxisAlignedBoundingBox) info.intersectionObject).child.colorInfo;
        } else if (info.intersectionObject instanceof BoundingSphere) {
            return ((BoundingSphere) info.intersectionObject).child.colorInfo;
        } else {
            // shouldn't happen
            return null;
        }
    }
}
