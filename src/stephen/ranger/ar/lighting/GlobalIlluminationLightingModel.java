package stephen.ranger.ar.lighting;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.vecmath.Vector3f;

import stephen.ranger.ar.Camera;
import stephen.ranger.ar.IntersectionInformation;
import stephen.ranger.ar.RTStatics;
import stephen.ranger.ar.photons.Photon;
import stephen.ranger.ar.photons.PhotonTree;
import stephen.ranger.ar.photons.Photon.LightAttribution;

public class GlobalIlluminationLightingModel extends LightingModel {

   public PhotonTree photons = null;
   private PhongLightingModel phong = null;

   public GlobalIlluminationLightingModel() {
   }

   @Override
   public void setCamera(final Camera camera) {
      this.camera = camera;
      System.out.println("camera set: " + camera);
      phong = new PhongLightingModel(camera.light, camera.objects);

      final long startTimePhotonMap = System.nanoTime();
      photons = computePhotonMap();
      final long endTimePhotonMap = System.nanoTime();
      System.out.println("Created Photon Map in " + (endTimePhotonMap - startTimePhotonMap) / 1000000000. + " seconds");
   }

   @Override
   public float[] getPixelColor(final IntersectionInformation info, final int depth) {
      final float[][] luminocity = collectPhotons(info);
      final float[] color = info.intersectionObject.getColor(info, camera, depth);

      final float[] diffuse = new float[3];
      diffuse[0] = luminocity[LightAttribution.DIFFUSE.cell][0] * color[0];
      diffuse[1] = luminocity[LightAttribution.DIFFUSE.cell][1] * color[1];
      diffuse[2] = luminocity[LightAttribution.DIFFUSE.cell][2] * color[2];

      final float[] specular = new float[3];
      specular[0] = luminocity[LightAttribution.SPECULAR.cell][0] * color[0];
      specular[1] = luminocity[LightAttribution.SPECULAR.cell][1] * color[1];
      specular[2] = luminocity[LightAttribution.SPECULAR.cell][2] * color[2];

      final float[] phongColor = new float[] { 0, 0, 0 };

      //      if (phong != null) {
      //         phongColor = phong.getPixelColor(info, depth);
      //      }

      return new float[] { diffuse[0] + specular[0] + phongColor[0], diffuse[1] + specular[1] + phongColor[1], diffuse[2] + specular[2] + phongColor[2] };
   }

   private float[][] collectPhotons(final IntersectionInformation info) {
      final float[][] values = new float[][] { { 0, 0, 0 }, { 0, 0, 0 } };
      float[][] temp;

      for (int x = 0; x < RTStatics.PHOTON_COLLECTION_GRID_SIZE; x++) {
         for (int y = 0; y < RTStatics.PHOTON_COLLECTION_GRID_SIZE; y++) {
            final Vector3f dir = RTStatics.getVectorMarsagliaHemisphere(info.normal);

            final IntersectionInformation collectionInfo = camera.getClosestIntersection(null, info.intersection, dir, 0);

            if (collectionInfo != null) {
               temp = getPhotonMapLuminocity(collectionInfo);
               final float falloff = (float) Math.toDegrees(Math.cos(RTStatics.getAngle(collectionInfo.ray.direction, info.normal)))
                     / RTStatics.getDistance(info.intersection, collectionInfo.intersection);
               //               final float falloff = 1f;

               values[0][0] += temp[0][0] * falloff;
               values[0][1] += temp[0][1] * falloff;
               values[0][2] += temp[0][2] * falloff;

               values[1][0] += temp[1][0] * falloff;
               values[1][1] += temp[1][1] * falloff;
               values[1][2] += temp[1][2] * falloff;
            }
         }
      }

      final int rayCount = RTStatics.PHOTON_COLLECTION_GRID_SIZE * RTStatics.PHOTON_COLLECTION_GRID_SIZE;

      values[0][0] /= rayCount;
      values[0][1] /= rayCount;
      values[0][2] /= rayCount;

      values[1][0] /= rayCount;
      values[1][1] /= rayCount;
      values[1][2] /= rayCount;

      return values;
   }

   private float[][] getPhotonMapLuminocity(final IntersectionInformation info) {
      final float[][] total = new float[][] { { 0, 0, 0 }, { 0, 0, 0 } };

      if (info != null) {
         final int[] indices = photons.rangeSearch(new float[] { info.intersection.x, info.intersection.y, info.intersection.z }, RTStatics.COLLECTION_RANGE);
         // final int[] indices = this.photons.kNearest(new float[] { info.intersection.x, info.intersection.y, info.intersection.z }, RTStatics.COLLECTION_COUNT_THRESHOLD);
         final int[] counts = new int[3];
         Photon photon;

         if (indices.length > 0) {
            for (final int index : indices) {
               photon = photons.get(index);
               // take into consideration falloff from intersection to photon location
               //               final float invDistance = (float) Math.toDegrees(Math.cos(RTStatics.getAngle(info.normal, new Vector3f(photon.incomingDir))))
               //                     / RTStatics.getDistance(info.intersection, photons.get(index).location);
               final float invDistance = 1f;

               total[photon.value.cell][0] += photon.intensity * invDistance * photon.color[0];
               total[photon.value.cell][1] += photon.intensity * invDistance * photon.color[1];
               total[photon.value.cell][2] += photon.intensity * invDistance * photon.color[2];

               counts[photon.value.cell]++;
            }

            //            if (counts[LightAttribution.DIFFUSE.cell] > 0) {
            //               total[LightAttribution.DIFFUSE.cell][0] /= counts[LightAttribution.DIFFUSE.cell];
            //               total[LightAttribution.DIFFUSE.cell][1] /= counts[LightAttribution.DIFFUSE.cell];
            //               total[LightAttribution.DIFFUSE.cell][2] /= counts[LightAttribution.DIFFUSE.cell];
            //            }
            //
            //            if (counts[LightAttribution.SPECULAR.cell] > 0) {
            //               total[LightAttribution.SPECULAR.cell][0] /= counts[LightAttribution.SPECULAR.cell];
            //               total[LightAttribution.SPECULAR.cell][1] /= counts[LightAttribution.SPECULAR.cell];
            //               total[LightAttribution.SPECULAR.cell][2] /= counts[LightAttribution.SPECULAR.cell];
            //            }
         }
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

      final Random random = new Random();
      final Vector3f lightDir = new Vector3f(camera.light.origin);
      lightDir.scale(-1f);
      lightDir.normalize();

      System.out.println("light direction: " + lightDir);
      /**
       * Random photons
       */
      for (int i = 0; i < RTStatics.NUM_PHOTONS; i++) {
         Vector3f dir = RTStatics.getVectorMarsagliaHemisphere(lightDir);
         Vector3f origin = new Vector3f(camera.light.origin);
         float intensity = RTStatics.STARTING_INTENSITY;
         final float[] emissionColor = camera.light.emission.getColorComponents(new float[3]);

         for (int m = 0; m < RTStatics.NUM_REFLECTIONS; m++) {
            final float chance = random.nextFloat();
            final LightAttribution value = chance < 0.5f ? LightAttribution.DIFFUSE : chance < 0.75f ? LightAttribution.SPECULAR : null;

            if (value != null) {
               final IntersectionInformation info = camera.getClosestIntersection(null, origin, dir, 0);
               if (info != null) {
                  final float[] color = info.intersectionObject.getColor(info, camera, 0);
                  emissionColor[0] *= color[0];
                  emissionColor[1] *= color[1];
                  emissionColor[2] *= color[2];

                  intensity *= 0.4f;//(float) Math.toDegrees(Math.cos(RTStatics.getAngle(info.normal, dir))) / RTStatics.getDistanceSquared(info.intersection, origin);
                  photons.add(new Photon(emissionColor, new float[] { info.intersection.x, info.intersection.y, info.intersection.z }, new float[] { dir.x,
                        dir.y, dir.z }, intensity, value));

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
