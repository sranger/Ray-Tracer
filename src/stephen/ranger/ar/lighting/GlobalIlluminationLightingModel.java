package stephen.ranger.ar.lighting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.vecmath.Vector3f;

import stephen.ranger.ar.Camera;
import stephen.ranger.ar.IntersectionInformation;
import stephen.ranger.ar.PBRTMath;
import stephen.ranger.ar.RTStatics;
import stephen.ranger.ar.photons.Photon;
import stephen.ranger.ar.photons.Photon.LightAttribution;
import stephen.ranger.ar.photons.PhotonTree;

public class GlobalIlluminationLightingModel extends LightingModel {

   public PhotonTree photons = null;

   public GlobalIlluminationLightingModel() {
   }

   @Override
   public void setCamera(final Camera camera) {
      this.camera = camera;

      final long startTimePhotonMap = System.nanoTime();
      this.photons = this.computePhotonMap();
      final long endTimePhotonMap = System.nanoTime();
      System.out.println("Created Photon Map in " + (endTimePhotonMap - startTimePhotonMap) / 1000000000. + " seconds");
   }

   @Override
   public float[] getPixelColor(final IntersectionInformation info, final int depth) {
      float[] color = new float[] { 0, 0, 0 };
      final float[] temp;
      final float[] diffuse, specular;

      if (info != null) {
         diffuse = info.intersectionObject.getColor(info, this.camera, depth);
         // diffuse = info.intersectionObject.getDiffuse();
         specular = info.intersectionObject.getSpecular();

         // final float[] irradiance = this.collectPhotons(info);
         //
         // color[0] = irradiance[0] * diffuse[0]; // indirect diffuse
         // color[1] = irradiance[0] * diffuse[1];
         // color[2] = irradiance[0] * diffuse[2];
         //
         // color[0] += irradiance[1] * specular[0]; // indirect specular
         // color[1] += irradiance[1] * specular[1];
         // color[2] += irradiance[1] * specular[2];

         // temp = this.getPhotonMapLuminocity(info);
         // final Vector3f invDir = new Vector3f(info.ray.direction);
         // invDir.scale(-1f);
         // final float falloff = invDir.dot(info.normal);
         //
         // color[0] += temp[0] * falloff * diffuse[0]; // direct diffuse
         // color[1] += temp[0] * falloff * diffuse[1];
         // color[2] += temp[0] * falloff * diffuse[2];
         //
         // color[0] += temp[1] * falloff * specular[0]; // direct specular
         // color[1] += temp[1] * falloff * specular[1];
         // color[2] += temp[1] * falloff * specular[2];

         // get photons and their colors
         color = this.getPhotonLocations(info);
      }

      return color;
   }

   private float[] collectPhotons(final IntersectionInformation info) {
      final float[] values = new float[] { 0, 0 };
      float[] temp;

      final int rayCount = RTStatics.PHOTON_COLLECTION_RAY_COUNT;
      final Random random = new Random();

      for (int x = 0; x < rayCount; x++) {
         final Vector3f dir = new Vector3f();
         final float weight = RTStatics.cosSampleHemisphere(dir, info.normal, random);
         // final Vector3f dir = RTStatics.getReflectionDirection(info);
         // final float weight = 1f;

         final IntersectionInformation collectionInfo = this.camera.getClosestIntersection(null, info.intersection, dir, info.normal, 0);

         if (collectionInfo != null) {
            temp = this.getPhotonMapLuminocity(collectionInfo);

            final Vector3f invDir = new Vector3f(collectionInfo.ray.direction);
            invDir.scale(-1f);
            final float falloff = invDir.dot(collectionInfo.normal);

            values[0] += temp[0] * falloff * weight;
            values[1] += temp[1] * falloff * weight;
         }
      }

      values[0] /= rayCount;
      values[1] /= rayCount;

      return values;
   }

   public float[] getPhotonLocations(final IntersectionInformation info) {
      final int[] indices = this.photons.kNearest(new float[] { info.intersection.x, info.intersection.y, info.intersection.z }, 1);

      if ((indices.length > 0) && (indices[0] >= 0)) {
         final Photon p = this.photons.get(indices[0]);
         final float[] color = Arrays.copyOf(p.color, 3);
         color[0] *= p.intensity;
         color[1] *= p.intensity;
         color[2] *= p.intensity;
         return color;
      } else {
         return new float[] { 0, 0, 0 };
      }
   }

   private float[] getPhotonMapLuminocity(final IntersectionInformation info) {
      final List<Photon> diffusePhotons = new ArrayList<Photon>();
      final List<Photon> specularPhotons = new ArrayList<Photon>();

      if (info != null) {
         final int[] indices = this.photons.kNearest(new float[] { info.intersection.x, info.intersection.y, info.intersection.z }, RTStatics.COLLECTION_COUNT_THRESHOLD);
         Photon photon;

         if (indices.length > 0) {
            for (final int index : indices) {
               photon = this.photons.get(index);

               if (photon.value.cell == LightAttribution.DIFFUSE.cell) {
                  diffusePhotons.add(photon);
               } else {
                  specularPhotons.add(photon);
               }
            }
         }
      }

      final float[] output = new float[] { 0, 0 };

      output[0] = this.radialBasisPhotonAverageIrradiance(info, diffusePhotons);
      output[1] = this.radialBasisPhotonAverageIrradiance(info, specularPhotons);

      return output;
   }

   private float radialBasisPhotonAverageIrradiance(final IntersectionInformation info, final List<Photon> photons) {
      float maxDistanceSquared = -Float.MAX_VALUE;
      float total = 0;

      if (photons.size() > 0) {
         for (final Photon p : photons) {
            maxDistanceSquared = Math.max(maxDistanceSquared, RTStatics.getDistanceSquared(info.intersection, p.location));
         }

         final float prefix = 1f / (photons.size() * maxDistanceSquared) * 3f / PBRTMath.F_PI;
         float temp;
         final Vector3f invDir = new Vector3f();

         for (final Photon p : photons) {
            temp = 1f - RTStatics.getDistanceSquared(info.intersection, p.location) / maxDistanceSquared;
            invDir.set(p.incomingDir);
            invDir.scale(-1f);
            total += temp * temp * Math.abs(invDir.dot(info.normal)) * p.intensity;
         }

         total *= prefix;
      }

      return total;
   }

   protected PhotonTree computePhotonMap() {
      final Vector3f originDirection = new Vector3f();
      originDirection.sub(this.camera.light.origin);
      originDirection.normalize();
      final List<Photon> photons = new ArrayList<Photon>();

      RTStatics.setProgressBarString("Computing Photon Map...");
      RTStatics.setProgressBarMinMax(0, RTStatics.NUM_PHOTONS);

      int ctr = 0;

      final Random random = new Random();
      final Vector3f lightDir = new Vector3f(this.camera.light.origin);
      lightDir.scale(-1f);
      lightDir.normalize();

      System.out.println("light direction: " + lightDir);
      /**
       * Random photons
       */
      for (int i = 0; i < RTStatics.NUM_PHOTONS; i++) {
         Vector3f dir = RTStatics.getVectorMarsagliaHemisphere(lightDir);
         Vector3f origin = new Vector3f(this.camera.light.origin);
         Vector3f normal = null;
         float intensity = RTStatics.STARTING_INTENSITY;
         final float weight = 1f;
         final float[] emissionColor = this.camera.light.emission.getColorComponents(new float[3]);

         for (int m = 0; m < RTStatics.NUM_REFLECTIONS; m++) {
            final float chance = random.nextFloat();
            final LightAttribution value = chance < 0.5f ? LightAttribution.DIFFUSE : chance < 0.8f ? LightAttribution.SPECULAR : null;

            if (value != null) {
               final IntersectionInformation info = this.camera.getClosestIntersection(null, origin, dir, normal, 0);
               if (info != null) {
                  final float[] color = info.intersectionObject.getColor(info, this.camera, 0);
                  emissionColor[0] *= color[0];
                  emissionColor[1] *= color[1];
                  emissionColor[2] *= color[2];

                  photons.add(new Photon(emissionColor, new float[] { info.intersection.x, info.intersection.y, info.intersection.z }, new float[] { dir.x,
                        dir.y, dir.z }, intensity * weight, value));

                  origin = info.intersection;
                  dir = RTStatics.getReflectionDirection(info.normal, dir);
                  // weight = RTStatics.cosSampleHemisphere(dir, info.normal, random);
                  normal = info.normal;

                  final Vector3f invDir = new Vector3f(dir);
                  //                  invDir.scale(-1f);

                  intensity *= info.normal.dot(invDir);
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
