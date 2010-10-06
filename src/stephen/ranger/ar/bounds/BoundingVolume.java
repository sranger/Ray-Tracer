package stephen.ranger.ar.bounds;

import java.awt.Color;

import javax.vecmath.Vector3d;

import stephen.ranger.ar.ColorInformation;
import stephen.ranger.ar.IntersectionInformation;
import stephen.ranger.ar.Ray;

public abstract class BoundingVolume {

    public abstract boolean intersects(final Ray ray);

    public abstract IntersectionInformation getChildIntersection(final Ray ray);

    public abstract Vector3d[] getMinMax();

    public abstract Color getColor(final IntersectionInformation info);

    public abstract ColorInformation getColorInformation(final IntersectionInformation info);
}
