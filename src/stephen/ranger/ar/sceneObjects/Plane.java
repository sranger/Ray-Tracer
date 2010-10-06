package stephen.ranger.ar.sceneObjects;

import java.awt.Color;
import java.security.InvalidParameterException;
import java.util.Arrays;

import javax.vecmath.Vector3d;

import stephen.ranger.ar.ColorInformation;
import stephen.ranger.ar.IntersectionInformation;
import stephen.ranger.ar.RTStatics;
import stephen.ranger.ar.Ray;
import stephen.ranger.ar.bounds.AxisAlignedBoundingBox;

/**
 * Plane equation info: http://local.wasp.uwa.edu.au/~pbourke/geometry/planeeq/
 */
public class Plane extends SceneObject {
    // public final Vector3d[] corners;
    public final double A, B, C, D;
    public final Vector3d normal;

    public final double minX, maxX, minY, maxY, minZ, maxZ;

    public final double width, height;
    public final int horizontalBoxes = 10;
    public final int verticalBoxes = 10;

    public Plane(Vector3d[] corners, final ColorInformation colorInfo) {
        super(colorInfo);

        if (corners.length < 3) {
            throw new InvalidParameterException(
                    "The Vector3d array of corner values must contain three or more vertices that denote the bounds of the given plane.");
        }

        // A = y0 (z1 - z2) + y1 (z2 - z0) + y2 (z0 - z1)
        A = corners[0].y * (corners[1].z - corners[2].z) + corners[1].y * (corners[2].z - corners[0].z) + corners[2].y * (corners[0].z - corners[1].z);

        // B = z0 (x1 - x2) + z1 (x2 - x0) + z2 (x0 - x1)
        B = corners[0].z * (corners[1].x - corners[2].x) + corners[1].z * (corners[2].x - corners[0].x) + corners[2].z * (corners[0].x - corners[1].x);

        // C = x0 (y1 - y2) + x1 (y2 - y0) + x2 (y0 - y1)
        C = corners[0].x * (corners[1].y - corners[2].y) + corners[1].x * (corners[2].y - corners[0].y) + corners[2].x * (corners[0].y - corners[1].y);

        // D = - (x0 (y1 z2 - y2 z1) +
        D = -(corners[0].x * (corners[1].y * corners[2].z - corners[2].y * corners[1].z) +
        // x1 (y2 z0 - y0 z2) +
                corners[1].x * (corners[2].y * corners[0].z - corners[0].y * corners[2].z) +
        // x2 (y0 z1 - y1 z0))
        corners[2].x * (corners[0].y * corners[1].z - corners[1].y * corners[0].z));

        normal = new Vector3d(A, B, C);
        normal.normalize();

        if (A == 0 && B == 0 && C == 0) {
            throw new InvalidParameterException("The first three vertices given for this object are colinear.");
        }

        if (corners.length == 3) {
            corners = Arrays.copyOf(corners, 4);
            corners[3] = new Vector3d(corners[2]);
        }

        minX = Math.min(corners[0].x, Math.min(corners[1].x, Math.min(corners[2].x, corners[3].x))) - RTStatics.EPSILON;
        maxX = Math.max(corners[0].x, Math.max(corners[1].x, Math.max(corners[2].x, corners[3].x))) + RTStatics.EPSILON;
        minY = Math.min(corners[0].y, Math.min(corners[1].y, Math.min(corners[2].y, corners[3].y))) - RTStatics.EPSILON;
        maxY = Math.max(corners[0].y, Math.max(corners[1].y, Math.max(corners[2].y, corners[3].y))) + RTStatics.EPSILON;
        minZ = Math.min(corners[0].z, Math.min(corners[1].z, Math.min(corners[2].z, corners[3].z))) - RTStatics.EPSILON;
        maxZ = Math.max(corners[0].z, Math.max(corners[1].z, Math.max(corners[2].z, corners[3].z))) + RTStatics.EPSILON;

        if (maxX - minX < maxY - minY && maxX - minX < maxZ - minZ) {
            width = RTStatics.getDistance(new Vector3d(minX, minY, minZ), new Vector3d(minX, minY, maxZ));
            height = RTStatics.getDistance(new Vector3d(minX, minY, minZ), new Vector3d(minX, maxY, minZ));
        } else if (maxY - minY < maxX - minX && maxY - minY < maxZ - minZ) {
            width = RTStatics.getDistance(new Vector3d(minX, minY, minZ), new Vector3d(maxX, minY, minZ));
            height = RTStatics.getDistance(new Vector3d(minX, minY, minZ), new Vector3d(minX, minY, maxZ));
        } else {
            width = RTStatics.getDistance(new Vector3d(minX, minY, minZ), new Vector3d(maxX, minY, minZ));
            height = RTStatics.getDistance(new Vector3d(minX, minY, minZ), new Vector3d(minX, maxY, minZ));
        }

        setBoundingVolume(new AxisAlignedBoundingBox(this, minX, minY, minZ, maxX, maxY, maxZ));
    }

    public Plane() {
        this(new Vector3d[] { new Vector3d(-1, 0, -1), new Vector3d(-1, 0, 1), new Vector3d(1, 0, -1), new Vector3d(1, 0, 1) }, new ColorInformation());
    }

    /**
     * http://www.siggraph.org/education/materials/HyperGraph/raytrace/
     * rayplane_intersection.htm
     */
    @Override
    public IntersectionInformation getIntersection(final Ray ray) {
        final double vD = normal.dot(ray.direction);

        if (vD <= -RTStatics.EPSILON || vD >= RTStatics.EPSILON) {
            // V0 = -(Pn . R0 + D) and compute t = V0 / Vd. If t < 0 then the
            // ray intersects plane behind origin, i.e. no intersection of
            // interest
            // final double v0 = -(this.normal.dot(ray.origin) + D);
            // final double t = v0 / vD;

            // t = -(AX0 + BY0 + CZ0 + D) / (AXd + BYd + CZd)
            final double t = -(A * ray.origin.x + B * ray.origin.y + C * ray.origin.z + D) / (A * ray.direction.x + B * ray.direction.y + C * ray.direction.z);

            if (t > -RTStatics.EPSILON) {
                final Vector3d rD = new Vector3d(ray.direction);

                if (vD > -RTStatics.EPSILON) {
                    rD.x = -rD.x;
                    rD.y = -rD.y;
                    rD.z = -rD.z;
                }

                // Pi = [Xi Yi Zi] = [X0 + Xd * t Y0 + Yd * t Z0 + Zd * t]
                final Vector3d pI = new Vector3d(ray.origin.x + rD.x * t, ray.origin.y + rD.y * t, ray.origin.z + rD.z * t);

                if (pI.x >= minX && pI.x <= maxX && pI.y >= minY && pI.y <= maxY && pI.z >= minZ && pI.z <= maxZ) {
                    final Vector3d temp = new Vector3d();
                    temp.sub(pI, ray.origin);

                    return new IntersectionInformation(ray, boundingVolume, pI, new Vector3d(normal), temp.length());
                }
            } else {
                // intersection behind ray origin; ignore
            }
        } else {
            // ray is parallel to plane; ignore
        }

        return null;
    }

    @Override
    public Color getColor(final IntersectionInformation info) {
        double wD, hD;

        if (maxX - minX < maxY - minY && maxX - minX < maxZ - minZ) {
            //z = width, y = height
            wD = RTStatics.getDistance(info.intersection, new Vector3d(info.intersection.x, info.intersection.y, minZ));
            hD = RTStatics.getDistance(info.intersection, new Vector3d(info.intersection.x, minY, info.intersection.z));
        } else if (maxY - minY < maxX - minX && maxY - minY < maxZ - minZ) {
            // x = width, z = height
            wD = RTStatics.getDistance(info.intersection, new Vector3d(minX, info.intersection.y, info.intersection.z));
            hD = RTStatics.getDistance(info.intersection, new Vector3d(info.intersection.x, info.intersection.y, minZ));
        } else {
            // x = width, y = height
            wD = RTStatics.getDistance(info.intersection, new Vector3d(minX, info.intersection.y, info.intersection.z));
            hD = RTStatics.getDistance(info.intersection, new Vector3d(info.intersection.x, minY, info.intersection.z));
        }

        final double widthStep = width / horizontalBoxes;
        final double heightStep = height / verticalBoxes;

        final int x = (int) Math.floor(wD / widthStep);
        final int y = (int) Math.floor(hD / heightStep);

        final Color emissionColor = colorInfo.emission;

        if (x % 2 == 0 && y % 2 == 0 || x % 2 != 0 && y % 2 != 0) {
            return emissionColor;
        } else {
            return new Color(255 - emissionColor.getRed(), 255 - emissionColor.getGreen(), 255 - emissionColor.getBlue());
        }
    }
}
