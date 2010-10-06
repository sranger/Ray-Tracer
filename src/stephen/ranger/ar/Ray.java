package stephen.ranger.ar;

import javax.vecmath.Vector3d;

public class Ray {
    public final Vector3d origin;
    public final Vector3d direction;

    public Ray(final Vector3d origin, final Vector3d direction) {
        this.origin = origin;
        this.direction = direction;
        this.direction.normalize();
    }
}
