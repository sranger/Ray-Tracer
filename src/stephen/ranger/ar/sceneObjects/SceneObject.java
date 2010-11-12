package stephen.ranger.ar.sceneObjects;

import java.awt.Color;

import javax.vecmath.Vector3f;

import stephen.ranger.ar.Camera;
import stephen.ranger.ar.IntersectionInformation;
import stephen.ranger.ar.RTStatics;
import stephen.ranger.ar.Ray;
import stephen.ranger.ar.bounds.BoundingSphere;
import stephen.ranger.ar.bounds.BoundingVolume;
import stephen.ranger.ar.materials.ColorInformation;

public abstract class SceneObject {
   protected final ColorInformation colorInfo;
   protected BoundingVolume boundingVolume = new BoundingSphere(this, new Vector3f(), 0);

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
      this.colorInfo = new ColorInformation(Color.black, Color.white, Color.white, Color.white, 20);
   }

   /**
    * Given the incoming ray, returns the closest intersection as an
    * IntersectionInformation object or null if no intersection occurs.
    * 
    * @param ray
    *            The incoming ray
    * @param depth
    *            The depth of recursion
    * @return The closest point of intersection, its normal, and w value or
    *         null if no intersection occurs
    */
   public abstract IntersectionInformation getIntersection(final Ray ray, final int depth);

   /**
    * Returns a BoundingVolume object that denotes the bounds of this object in
    * the scene.
    * 
    * @return A BoundingVolume that contains the bounds of this SceneObject
    */
   public BoundingVolume getBoundingVolume() {
      return this.boundingVolume;
   }

   /**
    * Returns the color at the given intersection location.
    * 
    * @param info  The IntersectionInformation that contains the intersection location.
    * @return  The color to display at the given intersection location
    */
   public final float[] getColor(final IntersectionInformation info, final Camera camera, final int depth) {
      if (depth >= RTStatics.MAX_RECURSION_DEPTH) {
         return this.getDiffuse();
      }

      return this.colorInfo.getMaterialColor(camera, info, depth);
   }

   public float[] getEmission() {
      return this.colorInfo.emission.getColorComponents(new float[3]);
   }

   public float[] getDiffuse() {
      return this.colorInfo.diffuse.getColorComponents(new float[3]);
   }

   public float[] getSpecular() {
      return this.colorInfo.specular.getColorComponents(new float[3]);
   }

   public float[] getAmbient() {
      return this.colorInfo.ambient.getColorComponents(new float[3]);
   }

   public float getShininess() {
      return this.colorInfo.shininess;
   }
}
