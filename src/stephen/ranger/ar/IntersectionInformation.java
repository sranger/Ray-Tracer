package stephen.ranger.ar;

import javax.vecmath.Vector3d;

import stephen.ranger.ar.bounds.BoundingVolume;

public class IntersectionInformation {
    public final Vector3d intersection;
    public final Vector3d normal;
    public final double w;
    public final BoundingVolume intersectionObject;
    public final Ray ray;

    public IntersectionInformation(final Ray ray, final BoundingVolume intersectionObject, final Vector3d intersection, final Vector3d normal, final double w) {
        this.ray = ray;
        this.intersectionObject = intersectionObject;
        this.intersection = intersection;
        this.normal = normal;
        this.w = w;
    }
}
