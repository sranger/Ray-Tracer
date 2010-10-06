package stephen.ranger.ar.bounds;

import java.awt.Color;

import javax.vecmath.Vector3d;

import stephen.ranger.ar.ColorInformation;
import stephen.ranger.ar.IntersectionInformation;
import stephen.ranger.ar.RTStatics;
import stephen.ranger.ar.Ray;

public class KDTree extends BoundingVolume {

    public static final int MAX_DEPTH = 20;

    public static enum SeparationAxis {
        X, Y, Z;

        public SeparationAxis getNextAxis() {
            return equals(X) ? Y : equals(Y) ? Z : X;
        }
    }

    public final BoundingVolume[] children;
    public final Vector3d[] minMax = new Vector3d[] { new Vector3d(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE), new Vector3d(-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE) };

    public final KDNode rootNode;

    public KDTree(final BoundingVolume[] children) {
        this.children = children;

        for (final BoundingVolume child : children) {
            final Vector3d[] minMax = child.getMinMax();

            this.minMax[0].x = Math.min(minMax[0].x, this.minMax[0].x);
            this.minMax[1].x = Math.max(minMax[1].x, this.minMax[1].x);

            this.minMax[0].y = Math.min(minMax[0].y, this.minMax[0].y);
            this.minMax[1].y = Math.max(minMax[1].y, this.minMax[1].y);

            this.minMax[0].z = Math.min(minMax[0].z, this.minMax[0].z);
            this.minMax[1].z = Math.max(minMax[1].z, this.minMax[1].z);
        }

        System.out.println("creating KD Tree...");
        final long startTime = System.nanoTime();
        rootNode = new KDNode(children, minMax, SeparationAxis.X, 0);
        final long endTime = System.nanoTime();

        System.out.println("min: " + minMax[0]);
        System.out.println("max: " + minMax[1]);

        System.out.println("KD Tree computation duration: " + (endTime - startTime) / 1000000000. + " seconds");
    }

    @Override
    public IntersectionInformation getChildIntersection(final Ray ray) {
        return rootNode.getChildIntersection(ray);
    }

    @Override
    public boolean intersects(final Ray ray) {
        return RTStatics.aabbIntersection(ray, getMinMax());
    }

    @Override
    public Vector3d[] getMinMax() {
        return minMax;
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
