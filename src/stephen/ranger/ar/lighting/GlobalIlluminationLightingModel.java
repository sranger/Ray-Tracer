package stephen.ranger.ar.lighting;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;

import stephen.ranger.ar.Camera;
import stephen.ranger.ar.IntersectionInformation;
import stephen.ranger.ar.RTStatics;
import stephen.ranger.ar.photons.Photon;
import stephen.ranger.ar.photons.PhotonTree;
import stephen.ranger.ar.photons.Photon.LightAttribution;

public class GlobalIlluminationLightingModel extends LightingModel {

   public PhotonTree photons = null;

   public GlobalIlluminationLightingModel() {

   }

   @Override
   public void setCamera(final Camera camera) {
      this.camera = camera;

      final long startTimePhotonMap = System.nanoTime();
      photons = computePhotonMap();
      final long endTimePhotonMap = System.nanoTime();
      System.out.println("Created Photon Map in " + (endTimePhotonMap - startTimePhotonMap) / 1000000000. + " seconds");
   }

   @Override
   public float[] getPixelColor(final IntersectionInformation info, final int depth) {
      final float[][] luminocity = getPhotonMapLuminocity(info);
      final float[] color = info.intersectionObject.getColor(info, camera, depth);

      final float[] diffuse = new float[3];
      diffuse[0] = luminocity[LightAttribution.DIFFUSE.cell][0] * color[0];
      diffuse[1] = luminocity[LightAttribution.DIFFUSE.cell][1] * color[1];
      diffuse[2] = luminocity[LightAttribution.DIFFUSE.cell][2] * color[2];

      final float[] specular = new float[3];
      specular[0] = luminocity[LightAttribution.SPECULAR.cell][0] * color[0];
      specular[1] = luminocity[LightAttribution.SPECULAR.cell][1] * color[1];
      specular[2] = luminocity[LightAttribution.SPECULAR.cell][2] * color[2];

      return new float[] { diffuse[0] + specular[0], diffuse[1] + specular[1], diffuse[2] + specular[2] };
   }

   private float[][] getPhotonMapLuminocity(final IntersectionInformation info) {
      final int[] indices = photons.rangeSearch(new float[] { info.intersection.x, info.intersection.y, info.intersection.z }, RTStatics.COLLECTION_RANGE);
      //      final int[] indices = photons.kNearest(new float[] { info.intersection.x, info.intersection.y, info.intersection.z }, RTStatics.COLLECTION_COUNT_THRESHOLD);
      final float[][] total = new float[3][3];
      final int[] counts = new int[3];

      if (indices.length > 0) {
         for (final int indice : indices) {
            // take into consideration falloff from intersection to photon location
            final float invDistance = 1f;// / RTStatics.getDistance(new float[] { info.intersection.x, info.intersection.y, info.intersection.z }, photons[i].location);
            final Photon photon = photons.get(indice);

            total[photon.value.cell][0] += photon.intensity * invDistance * photon.color[0];
            total[photon.value.cell][1] += photon.intensity * invDistance * photon.color[1];
            total[photon.value.cell][2] += photon.intensity * invDistance * photon.color[2];

            counts[photon.value.cell]++;
         }

         total[LightAttribution.DIFFUSE.cell][0] /= counts[LightAttribution.DIFFUSE.cell];
         total[LightAttribution.DIFFUSE.cell][1] /= counts[LightAttribution.DIFFUSE.cell];
         total[LightAttribution.DIFFUSE.cell][2] /= counts[LightAttribution.DIFFUSE.cell];

         total[LightAttribution.SPECULAR.cell][0] /= counts[LightAttribution.SPECULAR.cell];
         total[LightAttribution.SPECULAR.cell][1] /= counts[LightAttribution.SPECULAR.cell];
         total[LightAttribution.SPECULAR.cell][2] /= counts[LightAttribution.SPECULAR.cell];
      }

      return total;
   }

   protected PhotonTree computePhotonMap() {
      final Vector3f originDirection = new Vector3f();
      originDirection.sub(camera.light.origin);
      originDirection.normalize();
      final List<Photon> photons = new ArrayList<Photon>();

      RTStatics.setProgressBarString("Computing Photon Map...");
      RTStatics.setProgressBarMinMax(0, RTStatics.NUM_PHOTONS);

      int ctr = 0;

      final Matrix4f rotation = new Matrix4f();
      final Random random = new Random();

      /**
       * Random photons
       */
      for (int i = 0; i < RTStatics.NUM_PHOTONS; i++) {
         rotation.set(RTStatics.initializeQuat4f(new float[] { random.nextFloat() * 360f, random.nextFloat() * 360f, random.nextFloat() * 360f }));
         Vector3f dir = new Vector3f(0, 0, 1);
         rotation.transform(dir);
         dir.normalize();
         Vector3f origin = new Vector3f(camera.light.origin);
         float intensity = RTStatics.STARTING_INTENSITY;
         final float[] emissionColor = camera.light.emission.getColorComponents(new float[3]);

         for (int m = 0; m < RTStatics.NUM_REFLECTIONS; m++) {
            final float chance = random.nextFloat();
            final LightAttribution value = chance < 0.5f ? LightAttribution.DIFFUSE : chance < 0.75f ? LightAttribution.SPECULAR : null;

            if (value != null) {
               final IntersectionInformation info = camera.getClosestIntersection(null, origin, dir, 0);
               if (info != null) {
                  // final float[] color = info.intersectionObject.getColor(info).getColorComponents(new float[3]);
                  // emissionColor[0] *= color[0];
                  // emissionColor[1] *= color[1];
                  // emissionColor[2] *= color[2];

                  intensity *= Math.toDegrees(info.normal.dot(dir)) / RTStatics.getDistance(info.intersection, origin);
                  photons.add(new Photon(emissionColor, new float[] { info.intersection.x, info.intersection.y, info.intersection.z }, new float[] { dir.x, dir.y, dir.z }, intensity, value));

                  origin = info.intersection;
                  dir = RTStatics.getReflectionDirection(info.normal, dir);
               } else {
                  m = RTStatics.NUM_REFLECTIONS;
               }

               ctr++;
            } else {
               m = RTStatics.NUM_REFLECTIONS;
            }
         }
      }

      System.out.println("num photons: " + ctr);

      RTStatics.setProgressBarValue(RTStatics.NUM_PHOTONS);
      RTStatics.setProgressBarMinMax(0, photons.size());
      RTStatics.setProgressBarValue(0);
      RTStatics.setProgressBarString("Creating Photon Map KD-Tree");

      return new PhotonTree(photons.toArray(new Photon[photons.size()]));
   }
}
