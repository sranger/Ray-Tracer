package stephen.ranger.ar;

import javax.vecmath.Vector3f;

public class Ray {
    public final Vector3f origin;
    public final Vector3f direction;

    public Ray(final Vector3f origin, final Vector3f direction) {
        this.origin = origin;
        this.direction = direction;
        this.direction.normalize();
    }
}
