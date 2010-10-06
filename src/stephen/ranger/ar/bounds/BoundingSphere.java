package stephen.ranger.ar.bounds;

import java.awt.Color;

import javax.vecmath.Vector3d;

import stephen.ranger.ar.ColorInformation;
import stephen.ranger.ar.IntersectionInformation;
import stephen.ranger.ar.RTStatics;
import stephen.ranger.ar.Ray;
import stephen.ranger.ar.sceneObjects.SceneObject;

public class BoundingSphere extends BoundingVolume {
    public final SceneObject child;
    public final Vector3d origin;
    public final double radius;

    public BoundingSphere(final SceneObject child, final Vector3d origin, final double radius) {
        this.child = child;

        this.origin = origin;
        this.radius = radius;
    }

    @Override
    public IntersectionInformation getChildIntersection(final Ray ray) {
        return intersects(ray) ? child.getIntersection(ray) : null;
    }

    @Override
    public boolean intersects(final Ray ray) {
        final Vector3d origin = new Vector3d(ray.origin);
        final Vector3d direction = new Vector3d(ray.direction);
        final Vector3d center = new Vector3d(this.origin);

        final double a = Math.pow(direction.x, 2) + Math.pow(direction.y, 2) + Math.pow(direction.z, 2);
        final double b = 2 * (direction.x * (origin.x - center.x) + direction.y * (origin.y - center.y) + direction.z * (origin.z - center.z));
        final double c = Math.pow((origin.x - center.x), 2) + Math.pow((origin.y - center.y), 2) + Math.pow((origin.z - center.z), 2) - Math.pow(radius, 2);

        final double b24c = Math.pow(b, 2) - 4 * c;
        final double wplus = (-b + Math.sqrt(b24c)) / (2 * a);
        final double wminus = (-b - Math.sqrt(b24c)) / (2 * a);
        double w = -1;
        // boolean myW = false;

        if (b24c < 0) {
            w = -1;
            return false;
        }

        else {
            w = RTStatics.leastPositive(wplus, wminus);

            if (w <= 0) {
                return false;
            }
        }

        if (w > 0) {
            double xn, yn, zn, xm, ym, zm, xd, yd, zd;

            if (w == wplus) {
                xn = origin.x + direction.x * wplus;
                yn = origin.y + direction.y * wplus;
                zn = origin.z + direction.z * wplus;

                xd = origin.x - xn;
                yd = origin.y - yn;
                zd = origin.z - zn;
                final double nDist = Math.sqrt(xd * xd + yd * yd + zd * zd);

                if (nDist < RTStatics.EPSILON && wminus > 0) {
                    w = wminus;
                } else if (nDist < RTStatics.EPSILON && wminus < 0) {
                    w = -1;
                    return false;
                }
            }

            else if (w == wminus) {
                xm = origin.x + direction.x * wminus;
                ym = origin.y + direction.y * wminus;
                zm = origin.z + direction.z * wminus;

                xd = origin.x - xm;
                yd = origin.y - ym;
                zd = origin.z - zm;
                final double mDist = Math.sqrt(xd * xd + yd * yd + zd * zd);

                if (mDist < 0.01 && wplus > 0) {
                    w = wplus;
                } else if (mDist < 0.01 && wplus < 0) {
                    w = -1;
                    return false;
                }
            }
        }

        if (w > 0) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Vector3d[] getMinMax() {
        return new Vector3d[] { new Vector3d(origin.x - radius, origin.y - radius, origin.z - radius),
                new Vector3d(origin.x + radius, origin.y + radius, origin.z + radius) };
    }

    @Override
    public Color getColor(final IntersectionInformation info) {
        return child.getColor(info);
    }

    @Override
    public ColorInformation getColorInformation(final IntersectionInformation info) {
        return child.colorInfo;
    }
}
