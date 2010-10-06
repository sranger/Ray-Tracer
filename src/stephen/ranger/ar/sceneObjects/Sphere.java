package stephen.ranger.ar.sceneObjects;

import java.awt.Color;

import javax.vecmath.Vector3d;

import stephen.ranger.ar.ColorInformation;
import stephen.ranger.ar.IntersectionInformation;
import stephen.ranger.ar.RTStatics;
import stephen.ranger.ar.Ray;
import stephen.ranger.ar.RayTracer;
import stephen.ranger.ar.bounds.BoundingSphere;
import stephen.ranger.ar.bounds.BoundingVolume;

public class Sphere extends SceneObject {
    public final double radius;
    public final Vector3d origin;

    private final RayTracer rt;

    public Sphere(final double radius, final Vector3d origin, final ColorInformation colorInfo, final RayTracer rt) {
        super(colorInfo);

        this.radius = radius;
        this.origin = origin;
        this.rt = rt;

        setBoundingVolume(new BoundingSphere(this, origin, radius));
    }

    public Sphere(final RayTracer rt) {
        this(1.0, new Vector3d(), new ColorInformation(), rt);
    }

    @Override
    public IntersectionInformation getIntersection(final Ray ray) {
        final Vector3d origin = new Vector3d(ray.origin);
        final Vector3d direction = new Vector3d(ray.direction);
        final Vector3d center = new Vector3d(this.origin);
        IntersectionInformation retVal = null;

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
            // myW = false; // no real root
        }

        else {
            w = RTStatics.leastPositive(wplus, wminus);
            // myW = true;
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
                }
            }
        }

        if (w > 0) {
            final Vector3d intersection = new Vector3d(origin.x + direction.x * w, origin.y + direction.y * w, origin.z + direction.z * w);
            final Vector3d normal = new Vector3d(intersection.x - center.x, intersection.y - center.y, intersection.z - center.z);
            normal.normalize();

            retVal = new IntersectionInformation(ray, boundingVolume, intersection, normal, w);
        }

        return retVal;
    }

    @Override
    public Color getColor(final IntersectionInformation info) {
        if (info.intersectionObject.getColorInformation(info).isMirror) {
            final Vector3d reflectionDirection = RTStatics.getReflectionDirection(info, info.ray.origin);
            reflectionDirection.scale(-1.0);
            final Ray ray = new Ray(info.intersection, reflectionDirection);
            IntersectionInformation temp = null, closest = null;

            for (final BoundingVolume object : rt.objects) {
                if (!object.equals(this)) {
                    temp = object.getChildIntersection(ray);

                    if (temp != null) {
                        closest = closest == null ? temp : closest.w <= temp.w && closest.w > RTStatics.EPSILON ? closest : temp;
                    }
                }
            }

            final Color color = closest == null ? rt.light.ambient : rt.lightingModel.getPixelColor(closest);

            final int r = Math.min(255, Math.max(0, color.getRed() + info.intersectionObject.getColorInformation(info).emission.getRed()));
            final int g = Math.min(255, Math.max(0, color.getGreen() + info.intersectionObject.getColorInformation(info).emission.getGreen()));
            final int b = Math.min(255, Math.max(0, color.getBlue() + info.intersectionObject.getColorInformation(info).emission.getBlue()));

            return new Color(r, g, b);
        } else {
            // TODO: why does this come out black on the reflective sphere? 
            return super.getColor(info);
        }
    }
}
