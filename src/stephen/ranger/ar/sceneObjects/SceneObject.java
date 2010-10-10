package stephen.ranger.ar.sceneObjects;

import java.awt.Color;

import javax.vecmath.Vector3f;

import stephen.ranger.ar.ColorInformation;
import stephen.ranger.ar.IntersectionInformation;
import stephen.ranger.ar.Ray;
import stephen.ranger.ar.bounds.BoundingSphere;
import stephen.ranger.ar.bounds.BoundingVolume;

public abstract class SceneObject {
    public final ColorInformation colorInfo;
    public BoundingVolume boundingVolume = new BoundingSphere(this, new Vector3f(), 0);

    /**
     * Creates a new SceneObject instance.
     * 
     */
    public SceneObject(final ColorInformation colorInfo) {
        this.colorInfo = colorInfo;
    }

    public void setBoundingVolume(final BoundingVolume boundingVolume) {
        this.boundingVolume = boundingVolume;
    }

    public SceneObject() {
        colorInfo = new ColorInformation(Color.black, Color.white, Color.white, Color.white, 20, false);
    }

    /**
     * Given the incoming ray, returns the closest intersection as an
     * IntersectionInformation object or null if no intersection occurs.
     * 
     * @param ray
     *            The incoming ray
     * @return The closest point of intersection, its normal, and w value or
     *         null if no intersection occurs
     */
    public abstract IntersectionInformation getIntersection(final Ray ray);

    /**
     * Returns a BoundingVolume object that denotes the bounds of this object in
     * the scene.
     * 
     * @return A BoundingVolume that contains the bounds of this SceneObject
     */
    public BoundingVolume getBoundingVolume() {
        return boundingVolume;
    }

    /**
     * Returns the color at the given intersection location.
     * 
     * @param info  The IntersectionInformation that contains the intersection location.
     * @return  The color to display at the given intersection location
     */
    public Color getColor(final IntersectionInformation info) {
        return info.intersectionObject.getColorInformation(info).emission;
    }
}
