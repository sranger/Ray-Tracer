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
import stephen.ranger.ar.photons.Photon.LightAttribution;
import stephen.ranger.ar.photons.PhotonTree;

public class GlobalIlluminationLightingModel extends LightingModel {

   public PhotonTree photons = null;
   private PhongLightingModel phong = null;

   public GlobalIlluminationLightingModel() {
   }

   @Override
   public void setCamera(final Camera camera) {
      this.camera = camera;
      System.out.println("camera set: " + camera);
      this.phong = new PhongLightingModel(camera.light, camera.objects);

      final long startTimePhotonMap = System.nanoTime();
      this.photons = this.computePhotonMap();
      final long endTimePhotonMap = System.nanoTime();
      System.out.println("Created Photon Map in " + (endTimePhotonMap - startTimePhotonMap) / 1000000000. + " seconds");
   }

   @Override
   public float[] getPixelColor(final IntersectionInformation info, final int depth) {
      final float[] irradiance = this.collectPhotons(info);
      final float[] color = info.intersectionObject.getColor(info, this.camera, depth);

      final float[] diffuse = new float[3];
      diffuse[0] = irradiance[LightAttribution.DIFFUSE.cell] * color[0];
      diffuse[1] = irradiance[LightAttribution.DIFFUSE.cell] * color[1];
      diffuse[2] = irradiance[LightAttribution.DIFFUSE.cell] * color[2];

      final float[] specular = new float[3];
      specular[0] = irradiance[LightAttribution.SPECULAR.cell] * color[0];
      specular[1] = irradiance[LightAttribution.SPECULAR.cell] * color[1];
      specular[2] = irradiance[LightAttribution.SPECULAR.cell] * color[2];

      final float[] ks = info.intersectionObject.getSpecular();
      final float[] kd = info.intersectionObject.getDiffuse();
      final float a = info.intersectionObject.getShininess();

      final Vector3f L = new Vector3f();
      L.sub(this.camera.light.origin, info.intersection);
      L.normalize();

      final Vector3f N = new Vector3f(info.normal);

      final Vector3f V = new Vector3f();
      V.sub(info.ray.direction);

      // r = L - 2f * N * L.dot(N)
      final Vector3f R = RTStatics.getReflectionDirection(info.normal, L);
      final double LdotN = L.dot(N);
      final double RdotVexpA = Math.pow(V.dot(R), a);

      color[0] *= (kd[0] * LdotN * diffuse[0] + ks[0] * RdotVexpA * specular[0]);
      color[1] *= (kd[1] * LdotN * diffuse[1] + ks[1] * RdotVexpA * specular[1]);
      color[2] *= (kd[2] * LdotN * diffuse[2] + ks[2] * RdotVexpA * specular[2]);

      return new float[] { irradiance[0], irradiance[0], irradiance[0] };
   }

   private float[] collectPhotons(final IntersectionInformation info) {
      final float[] values = new float[] { 0, 0 };
      float[] temp;

      final int rayCount = RTStatics.PHOTON_COLLECTION_RAY_COUNT;

      for (int x = 0; x < rayCount; x++) {
         final Vector3f dir = RTStatics.getVectorMarsagliaHemisphere(info.normal);

         final IntersectionInformation collectionInfo = this.camera.getClosestIntersection(null, info.intersection, dir, 0);

         if (collectionInfo != null) {
            temp = this.getPhotonMapLuminocity(collectionInfo);
            final Vector3f invDir = new Vector3f(collectionInfo.ray.direction);
            invDir.scale(-1f);
            final float falloff = invDir.dot(info.normal);// / RTStatics.getDistanceSquared(info.intersection, collectionInfo.intersection);

            values[0] += temp[0] * falloff;
            values[1] += temp[1] * falloff;
         }
      }

      values[0] /= rayCount;
      values[1] /= rayCount;

      return values;
   }

   private float[] getPhotonMapLuminocity(final IntersectionInformation info) {
      final List<Photon> diffusePhotons = new ArrayList<Photon>();
      final List<Photon> specularPhotons = new ArrayList<Photon>();

      if (info != null) {
         // final int[] indices = this.photons.rangeSearch(new float[] { info.intersection.x, info.intersection.y, info.intersection.z }, RTStatics.COLLECTION_RANGE);
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

      final float[] output = new float[2];

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


         final float prefix = (1f / (photons.size() * maxDistanceSquared)) * (3f / PBRTMath.F_PI);
         float temp;

         for (final Photon p : photons) {
            temp = 1f - RTStatics.getDistanceSquared(info.intersection, p.location) / maxDistanceSquared;
            total += (temp * temp * Math.abs(info.ray.direction.dot(info.normal)) * p.intensity);
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
         float intensity = RTStatics.STARTING_INTENSITY;
         final float[] emissionColor = this.camera.light.emission.getColorComponents(new float[3]);

         for (int m = 0; m < RTStatics.NUM_REFLECTIONS; m++) {
            final float chance = random.nextFloat();
            final LightAttribution value = chance < 0.5f ? LightAttribution.DIFFUSE : chance < 0.75f ? LightAttribution.SPECULAR : null;

            if (value != null) {
               final IntersectionInformation info = this.camera.getClosestIntersection(null, origin, dir, 0);
               if (info != null) {
                  final float[] color = info.intersectionObject.getColor(info, this.camera, 0);
                  emissionColor[0] *= color[0];
                  emissionColor[1] *= color[1];
                  emissionColor[2] *= color[2];

                  final Vector3f invDir = new Vector3f(dir);
                  invDir.scale(-1f);
                  intensity *= info.normal.dot(invDir);

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
