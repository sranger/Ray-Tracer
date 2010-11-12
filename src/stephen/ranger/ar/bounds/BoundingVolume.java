package stephen.ranger.ar.bounds;

import stephen.ranger.ar.Camera;
import stephen.ranger.ar.IntersectionInformation;
import stephen.ranger.ar.Ray;

public abstract class BoundingVolume {

   public abstract boolean intersects(final Ray ray);

   public abstract IntersectionInformation getChildIntersection(final Ray ray, final int depth);

   public abstract float[][] getMinMax();

   public abstract float[] getColor(final IntersectionInformation info, final Camera camera, final int depth);

   public abstract float[] getEmission();

   public abstract float[] getDiffuse();

   public abstract float[] getSpecular();

   public abstract float[] getAmbient();

   public abstract float getShininess();
}
