package stephen.ranger.ar.lighting;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.vecmath.Vector3f;

import stephen.ranger.ar.Camera;
import stephen.ranger.ar.IntersectionInformation;
import stephen.ranger.ar.PBRTMath;
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
      final float[] color = new float[] { 0, 0, 0 };

      final Vector3f origin = new Vector3f(info.intersection);
      final Vector3f dir = new Vector3f();
      final Random random = new Random();
      float weight;
      IntersectionInformation newInfo;
      final float[] location = new float[3];
      int[] indices;

      //      final Vector3f invDir = new Vector3f(info.ray.direction);
      //      invDir.scale(-1f);
      //      final float falloff = invDir.dot(info.normal);
      int ctr = 0;

      for (int i = 0; i < RTStatics.PHOTON_COLLECTION_RAY_COUNT; i++) {
         weight = RTStatics.cosSampleHemisphere(dir, info.normal, random);
         newInfo = camera.getClosestIntersection(null, origin, dir, info.normal, depth);
         final float falloff = dir.dot(info.normal);

         if (newInfo != null) {
            ctr++;
            newInfo.intersection.get(location);
            indices = photons.kNearest(location, RTStatics.COLLECTION_COUNT_THRESHOLD);
            final float[] spawnedColor = radialBasisPhotonAverageIrradiance(newInfo, indices);

            color[0] += spawnedColor[0] * falloff / weight;
            color[1] += spawnedColor[1] * falloff / weight;
            color[2] += spawnedColor[2] * falloff / weight;
         }
      }

      if (ctr > 0) {
         final float[] diffuseColor = info.intersectionObject.getDiffuse();

         color[0] = color[0] / ctr * diffuseColor[0];
         color[1] = color[1] / ctr * diffuseColor[1];
         color[2] = color[2] / ctr * diffuseColor[2];
      }

      return color;
   }

   private float[] radialBasisPhotonAverageIrradiance(final IntersectionInformation info, final int[] indices) {
      float maxDistanceSquared = -Float.MAX_VALUE;
      final float[] averageColor = new float[] { 1, 1, 1 };
      float total = 0f;

      if (indices.length > 0) {
         for (final int index : indices) {
            final Photon p = photons.get(index);
            maxDistanceSquared = Math.max(maxDistanceSquared, RTStatics.getDistanceSquared(info.intersection, p.location));
         }

         final float prefix = 1f / (indices.length * maxDistanceSquared) * 3f / PBRTMath.F_PI;
         float temp;

         for (final int index : indices) {
            final Photon p = photons.get(index);
            final Vector3f invDir = new Vector3f(p.incomingDir);
            invDir.scale(-1f);
            invDir.normalize();
            final float cosTerm = Math.abs(invDir.dot(info.normal));

            if (cosTerm > 0f) {
               temp = 1f - RTStatics.getDistanceSquared(info.intersection, p.location) / maxDistanceSquared;
               total += temp * temp * cosTerm * p.intensity;

               averageColor[0] += p.color[0];
               averageColor[1] += p.color[1];
               averageColor[2] += p.color[2];
            }
         }

         total *= prefix;

         averageColor[0] *= total;
         averageColor[1] *= total;
         averageColor[2] *= total;
      }

      return averageColor;
   }

   //      @Override
   //      public float[] getPixelColor(final IntersectionInformation info, final int depth) {
   //      final float[] color = new float[] { 0, 0, 0 };
   //      final float[] diffuse, specular;
   //
   //      if (info != null) {
   //         diffuse = info.intersectionObject.getColor(info, camera, depth);
   //         // diffuse = info.intersectionObject.getDiffuse();
   //         specular = info.intersectionObject.getSpecular();
   //         final Vector3f invDir = new Vector3f(info.ray.direction);
   //         invDir.scale(-1f);
   //         final float falloff = invDir.dot(info.normal);
   //
   //         //         final float[] temp = getPhotonMapLuminocity(info);
   //         //
   //         //         color[0] += temp[0] * falloff * diffuse[0]; // direct diffuse
   //         //         color[1] += temp[0] * falloff * diffuse[1];
   //         //         color[2] += temp[0] * falloff * diffuse[2];
   //
   //         final float[] irradiance = collectPhotons(info);
   //
   //         color[0] += irradiance[0] * falloff * diffuse[0]; // indirect diffuse
   //         color[1] += irradiance[0] * falloff * diffuse[1];
   //         color[2] += irradiance[0] * falloff * diffuse[2];
   //
   //         //         color[0] += irradiance[1] * falloff * specular[0]; // indirect specular
   //         //         color[1] += irradiance[1] * falloff * specular[1];
   //         //         color[2] += irradiance[1] * falloff * specular[2];
   //
   //         // get photons and their colors
   //         // color = getPhotonLocations(info);
   //      }
   //
   //      return color;
   //   }
   //
   //   private float[] collectPhotons(final IntersectionInformation info) {
   //      final float[] values = new float[] { 0, 0 };
   //      float[] temp;
   //
   //      final int rayCount = RTStatics.PHOTON_COLLECTION_RAY_COUNT;
   //      final Random random = new Random();
   //
   //      for (int x = 0; x < rayCount; x++) {
   //         final Vector3f dir = new Vector3f();
   //         final float weight = RTStatics.cosSampleHemisphere(dir, info.normal, random);
   //         // final Vector3f dir = RTStatics.getReflectionDirection(info);
   //         // final float weight = 1f;
   //
   //         final IntersectionInformation collectionInfo = camera.getClosestIntersection(null, info.intersection, dir, info.normal, 0);
   //
   //         if (collectionInfo != null) {
   //            temp = getPhotonMapLuminocity(collectionInfo);
   //
   //            final Vector3f invDir = new Vector3f(info.ray.direction);
   //            invDir.scale(-1f);
   //            final float falloff = invDir.dot(info.normal);
   //
   //            values[0] += temp[0] * falloff / weight;
   //            values[1] += temp[1] * falloff / weight;
   //         }
   //      }
   //
   //      values[0] /= rayCount;
   //      values[1] /= rayCount;
   //
   //      return values;
   //   }
   //
   //   private float[] getPhotonLocations(final IntersectionInformation info) {
   //      final int[] indices = photons.kNearest(new float[] { info.intersection.x, info.intersection.y, info.intersection.z }, 1);
   //
   //      if (indices.length > 0 && indices[0] >= 0) {
   //         final Photon p = photons.get(indices[0]);
   //         final float[] color = Arrays.copyOf(p.color, 3);
   //         color[0] *= p.intensity;
   //         color[1] *= p.intensity;
   //         color[2] *= p.intensity;
   //         return color;
   //      } else {
   //         return new float[] { 0, 0, 0 };
   //      }
   //   }
   //
   //   private float[] getPhotonMapLuminocity(final IntersectionInformation info) {
   //      final List<Photon> diffusePhotons = new ArrayList<Photon>();
   //      final List<Photon> specularPhotons = new ArrayList<Photon>();
   //
   //      if (info != null) {
   //         final int[] indices = photons.kNearest(new float[] { info.intersection.x, info.intersection.y, info.intersection.z },
   //               RTStatics.COLLECTION_COUNT_THRESHOLD);
   //         // final int[] indices = new int[] { this.photons.nearestNeighbor(new float[] { info.intersection.x, info.intersection.y, info.intersection.z }) };
   //         Photon photon;
   //
   //         if (indices.length > 0) {
   //            for (final int index : indices) {
   //               photon = photons.get(index);
   //
   //               if (photon.value.cell == LightAttribution.DIFFUSE.cell) {
   //                  diffusePhotons.add(photon);
   //               } else {
   //                  specularPhotons.add(photon);
   //               }
   //            }
   //         }
   //      }
   //
   //      final float[] output = new float[] { 0, 0 };
   //
   //      output[0] = radialBasisPhotonAverageIrradiance(info, diffusePhotons);
   //      output[1] = radialBasisPhotonAverageIrradiance(info, specularPhotons);
   //
   //      // output[0] = average(diffusePhotons);
   //      // output[1] = average(specularPhotons);
   //
   //      return output;
   //   }
   //
   //   private float average(final List<Photon> photons) {
   //      float intensity = 0;
   //
   //      for (final Photon p : photons) {
   //         intensity += p.intensity;
   //      }
   //
   //      return photons.size() == 0 ? 0 : intensity / photons.size();
   //   }
   //
   //
   //   private float radialBasisPhotonAverageIrradiance(final IntersectionInformation info, final List<Photon> photons) {
   //      float maxDistanceSquared = -Float.MAX_VALUE;
   //      float total = 0;
   //
   //      if (photons.size() > 0) {
   //         for (final Photon p : photons) {
   //            maxDistanceSquared = Math.max(maxDistanceSquared, RTStatics.getDistanceSquared(info.intersection, p.location));
   //         }
   //
   //         final float prefix = 1f / (photons.size() * maxDistanceSquared) * 3f / PBRTMath.F_PI;
   //         float temp;
   //         final Vector3f invDir = new Vector3f();
   //
   //         for (final Photon p : photons) {
   //            invDir.set(p.incomingDir);
   //            invDir.scale(-1f);
   //            final float dot = invDir.dot(info.normal);
   //
   //            if (dot > 0f) {
   //               temp = 1f - RTStatics.getDistanceSquared(info.intersection, p.location) / maxDistanceSquared;
   //               total += temp * temp * dot * p.intensity;
   //            }
   //         }
   //
   //         total *= prefix;
   //      }
   //
   //      return total;
   //   }

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
         // Vector3f dir = RTStatics.getVectorMarsagliaHemisphere(lightDir);
         Vector3f dir = new Vector3f();
         RTStatics.cosSampleHemisphere(dir, lightDir, new Random());
         Vector3f origin = new Vector3f(camera.light.origin);
         Vector3f normal = null;
         float intensity = RTStatics.STARTING_INTENSITY;
         final float weight = 1f;
         final float[] emissionColor = camera.light.emission.getColorComponents(new float[3]);

         for (int m = 0; m < RTStatics.NUM_REFLECTIONS; m++) {
            final float chance = random.nextFloat();
            final LightAttribution value = chance < 0.8f ? LightAttribution.DIFFUSE : chance < 0.8f ? LightAttribution.SPECULAR : null;

            if (value != null && intensity > 0f) {
               final IntersectionInformation info = camera.getClosestIntersection(null, origin, dir, normal, 0);
               if (info != null) {
                  final float[] color = info.intersectionObject.getColor(info, camera, 0);
                  emissionColor[0] *= color[0];
                  emissionColor[1] *= color[1];
                  emissionColor[2] *= color[2];

                  photons.add(new Photon(emissionColor, new float[] { info.intersection.x, info.intersection.y, info.intersection.z }, new float[] { dir.x,
                        dir.y, dir.z }, new float[] { info.normal.x, info.normal.y, info.normal.z }, intensity * weight, value));

                  origin = info.intersection;
                  dir = RTStatics.getReflectionDirection(info.normal, dir);
                  // weight = RTStatics.cosSampleHemisphere(dir, info.normal, random);
                  normal = info.normal;

                  final Vector3f invDir = new Vector3f(dir);
                  invDir.scale(-1f);

                  intensity *= Math.max(0f, info.normal.dot(invDir));
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
