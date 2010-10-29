package stephen.ranger.ar;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;

import stephen.ranger.ar.bounds.BoundingVolume;
import stephen.ranger.ar.lighting.Light;
import stephen.ranger.ar.lighting.LightingModel;
import stephen.ranger.ar.photons.Photon;
import stephen.ranger.ar.photons.Photon.LightAttribution;
import stephen.ranger.ar.photons.PhotonTree;

public class Camera {
   private static final int nodeSize = 128;

   public final Matrix4f rotation;
   public final BoundingVolume[] objects;
   public final LightingModel lightingModel;
   public final Light light;
   public final int multiSamples, brdfSamples;
   public final Vector3f origin;
   public final float[] orientation;
   public final float nearPlaneDistance;
   public final float viewportWidth, viewportHeight;
   public final int screenWidth, screenHeight;
   public final BufferedImage image;
   public BufferedImage normalizedImage = null;
   public final Set<ActionListener> listeners = new HashSet<ActionListener>();
   public final float[][][] pixels;

   public PhotonTree photons = null;

   public float EPSILON = 1e-15f;

   public Camera(final Scene scene, final int multiSamples, final int brdfSamples, final float nearPlane, final int screenWidth, final int screenHeight) {
      this.objects = scene.objects;
      this.lightingModel = scene.lightingModel;
      this.light = scene.light;
      this.multiSamples = multiSamples;
      this.brdfSamples = brdfSamples;
      this.orientation = scene.cameraOrientation;
      this.nearPlaneDistance = nearPlane;
      this.screenWidth = screenWidth;
      this.screenHeight = screenHeight;
      this.viewportWidth = (screenWidth >= screenHeight ? (float) screenWidth / (float) screenHeight : 1.0f) * this.nearPlaneDistance;
      this.viewportHeight = (screenWidth >= screenHeight ? 1.0f : (float) screenHeight / (float) screenWidth) * this.nearPlaneDistance;

      this.image = new BufferedImage(screenWidth, screenHeight, BufferedImage.TYPE_INT_RGB);
      this.pixels = new float[screenWidth][screenHeight][];

      this.rotation = new Matrix4f();
      this.rotation.set(RTStatics.initializeQuat4f(this.orientation));

      final float[][] minMax = new float[][] { { Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE }, { -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE } };

      for (final BoundingVolume object : this.objects) {
         final float[][] objMinMax = object.getMinMax();
         minMax[0][0] = Math.min(minMax[0][0], objMinMax[0][0]);
         minMax[0][1] = Math.min(minMax[0][1], objMinMax[0][1]);
         minMax[0][2] = Math.min(minMax[0][2], objMinMax[0][2]);

         minMax[1][0] = Math.max(minMax[1][0], objMinMax[1][0]);
         minMax[1][1] = Math.max(minMax[1][1], objMinMax[1][1]);
         minMax[1][2] = Math.max(minMax[1][2], objMinMax[1][2]);
      }

      final float width = minMax[1][0] - minMax[0][0];
      final float height = minMax[1][1] - minMax[0][1];
      final float depth = minMax[1][2] - minMax[0][2];
      final float centerX = width / 2f + minMax[0][0];
      final float centerY = height / 2f + minMax[0][1];
      final float centerZ = depth / 2f + minMax[0][2];
      final float distance = width / 2f / (float) Math.tan(Math.toRadians(scene.fov));
      this.origin = new Vector3f(centerX, centerY, centerZ + distance);

      this.rotation.transform(this.origin);
      final Vector3f viewportDirection = new Vector3f(0, 0, -this.nearPlaneDistance);
      this.rotation.transform(viewportDirection);
      viewportDirection.normalize();

      //      if (viewportDirection.z < 0) {
      //         origin.z = -origin.z;
      //      }

      System.out.println("camera location:  " + this.origin);
      System.out.println("camera direction: " + viewportDirection);

      this.lightingModel.setCameraPosition(new float[] { this.origin.x, this.origin.y, this.origin.z });
   }

   public void setPixel(final int x, final int y, final float[] color) {
      if ((color == null) || (Float.valueOf(color[0]).equals(Float.NaN)) || (Float.valueOf(color[1]).equals(Float.NaN)) || (Float.valueOf(color[2]).equals(Float.NaN))) {
         this.pixels[x][y] = new float[3];
      } else {
         this.pixels[x][y] = color;
      }

      this.image.setRGB(
            x,
            y,
            (int) RTStatics.bound(0, 255, (this.pixels[x][y][0] * 255)) * 65536 + (int) RTStatics.bound(0, 255, (this.pixels[x][y][1] * 255)) * 256
            + (int) RTStatics.bound(0, 255, (this.pixels[x][y][2] * 255)));
   }

   public BufferedImage getNormalizedImage() {
      this.updateNormalizedImage();
      return this.normalizedImage;
   }

   public BufferedImage getImage() {
      return this.image;
   }

   public void createImage() {
      new Thread() {
         @Override
         public void run() {
            final long startTimePhotonMap = System.nanoTime();
            Camera.this.photons = Camera.this.computePhotonMap();
            final long endTimePhotonMap = System.nanoTime();
            System.out.println("Created Photon Map in " + (endTimePhotonMap - startTimePhotonMap) / 1000000000. + " seconds");

            final float xStart = -(Camera.this.viewportWidth / 2.0f);
            final float yStart = Camera.this.viewportHeight / 2.0f;
            final float xInc = Camera.this.viewportWidth / Camera.this.screenWidth;
            final float yInc = Camera.this.viewportHeight / Camera.this.screenHeight;
            final long startTime = System.nanoTime();

            final List<int[]> nodes = new ArrayList<int[]>();

            for (int x = 0; x < Camera.this.image.getWidth(); x = x + nodeSize) {
               for (int y = 0; y < Camera.this.image.getHeight(); y = y + nodeSize) {
                  nodes.add(new int[] { x, y, x + nodeSize > Camera.this.image.getWidth() ? Camera.this.image.getWidth() - x : nodeSize, y + nodeSize > Camera.this.image.getHeight() ? Camera.this.image.getHeight() - y : nodeSize });
               }
            }

            int temp = Runtime.getRuntime().availableProcessors();
            temp = Math.max(1, temp);
            temp = Math.min(nodes.size(), temp);
            final int cpus = temp;

            RTStatics.setProgressBarMinMax(0, Camera.this.image.getWidth() * Camera.this.image.getHeight());
            RTStatics.setProgressBarValue(0);
            RTStatics.setProgressBarString("Rendering image...");

            final Set<RenderThread> threads = new HashSet<RenderThread>();
            final ActionListener threadListener = new ActionListener() {
               int nodePosition = cpus;
               double totalTime = 0;

               @Override
               public synchronized void actionPerformed(final ActionEvent event) {
                  threads.remove(event.getSource());
                  final double seconds = Double.parseDouble(event.getActionCommand());
                  this.totalTime += seconds;
                  System.out.println("node #" + event.getID() + ": " + seconds + " seconds,  threads running: " + threads.size());

                  for (final ActionListener listener : Camera.this.listeners) {
                     listener.actionPerformed(new ActionEvent(this, 2, "update"));
                  }

                  if (this.nodePosition < nodes.size()) {
                     final int[] node = nodes.get(this.nodePosition);
                     System.out.println("creating node for: " + Arrays.toString(node));
                     this.nodePosition++;
                     final RenderThread thread = new RenderThread(Camera.this, this, this.nodePosition, node[0], node[1], node[2], node[3], xStart, yStart, xInc, yInc);
                     threads.add(thread);
                     thread.start();
                  } else if ((this.nodePosition == nodes.size()) && (threads.size() == 0)) {
                     final long endTime = System.nanoTime();

                     System.out.println("total elapsed time: " + (endTime - startTime) / 1000000000. + " seconds");
                     System.out.println("total cpu time:     " + this.totalTime + " seconds");

                     RTStatics.setProgressBarValue(Camera.this.image.getWidth() * Camera.this.image.getHeight());
                     RTStatics.setProgressBarString("Rendered image completed!");

                     for (final ActionListener listener : Camera.this.listeners) {
                        listener.actionPerformed(new ActionEvent(this, 1, "finished"));
                     }
                     Camera.this.listeners.clear();
                  }
               }
            };

            for (int i = 0; i < cpus; i++) {
               final int[] node = nodes.get(i);
               System.out.println("creating node for: " + Arrays.toString(node));
               final RenderThread thread = new RenderThread(Camera.this, threadListener, i, node[0], node[1], node[2], node[3], xStart, yStart, xInc, yInc);
               threads.add(thread);
               thread.start();
            }
         }
      }.start();
   }

   protected PhotonTree computePhotonMap() {
      final Vector3f originDirection = new Vector3f();
      originDirection.sub(this.light.origin);
      originDirection.normalize();
      final List<Photon> photons = new ArrayList<Photon>();

      RTStatics.setProgressBarString("Computing Photon Map...");
      RTStatics.setProgressBarMinMax(0, RTStatics.NUM_PHOTONS);

      final int axisCount = (int) Math.cbrt(RTStatics.NUM_PHOTONS);
      final float step = 360f / axisCount;
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
         Vector3f intersection = new Vector3f(this.light.origin);
         float intensity = RTStatics.STARTING_INTENSITY;
         final float[] emissionColor = this.light.emission.getColorComponents(new float[3]);

         for (int m = 0; m < RTStatics.NUM_REFLECTIONS; m++) {
            final float chance = random.nextFloat();
            final LightAttribution value = (chance < 0.33f) ? LightAttribution.DIFFUSE : (chance < 0.67f) ? LightAttribution.SPECULAR : null;

            if (value != null) {
               final IntersectionInformation info = this.getClosestIntersection(null, intersection, dir);
               if (info != null) {
                  final float[] color = info.intersectionObject.getColor(info).getColorComponents(new float[3]);
                  emissionColor[0] *= color[0];
                  emissionColor[1] *= color[1];
                  emissionColor[2] *= color[2];

                  photons.add(new Photon(emissionColor, new float[] { info.intersection.x, info.intersection.y, info.intersection.z }, intensity, value));
                  intensity *= RTStatics.PHOTON_FALLOFF;

                  intersection = info.intersection;
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

      RTStatics.incrementProgressBarValue(axisCount);

      /**
       * Grid of photons
       */
      // for (int x = 0; x < axisCount; x++) {
      // for (int y = 0; y < axisCount; y++) {
      // for (int z = 0; z < axisCount; z++) {
      // rotation.set(RTStatics.initializeQuat4f(new float[] { step * x, step * y, step * z }));
      // Vector3f dir = new Vector3f(0, 0, 1);
      // rotation.transform(dir);
      // dir.normalize();
      // Vector3f intersection = new Vector3f(this.light.origin);
      // float intensity = RTStatics.STARTING_INTENSITY;
      // final float[] emissionColor = this.light.emission.getColorComponents(new float[3]);
      //
      // for (int m = 0; m < RTStatics.NUM_REFLECTIONS; m++) {
      // final IntersectionInformation info = this.getClosestIntersection(null, intersection, dir);
      // if (info != null) {
      // final float[] color = info.intersectionObject.getColor(info).getColorComponents(new float[3]);
      // emissionColor[0] *= color[0];
      // emissionColor[1] *= color[1];
      // emissionColor[2] *= color[2];
      // photons.add(new Photon(emissionColor, new float[] { info.intersection.x, info.intersection.y, info.intersection.z }, intensity));
      // intensity *= RTStatics.PHOTON_FALLOFF;
      //
      // intersection = info.intersection;
      // dir = RTStatics.getReflectionDirection(info.normal, dir);
      // } else {
      // m = RTStatics.NUM_REFLECTIONS;
      // }
      //
      // ctr++;
      // }
      // }
      //
      // RTStatics.incrementProgressBarValue(axisCount);
      // }
      // }

      System.out.println("num photons: " + ctr);

      RTStatics.setProgressBarValue(RTStatics.NUM_PHOTONS);
      RTStatics.setProgressBarMinMax(0, photons.size());
      RTStatics.setProgressBarValue(0);
      RTStatics.setProgressBarString("Creating Photon Map KD-Tree");

      return new PhotonTree(photons.toArray(new Photon[photons.size()]));
   }

   public IntersectionInformation getClosestIntersection(final BoundingVolume mirrorObject, final Vector3f origin, final Vector3f direction) {
      final Ray ray = new Ray(origin, direction);
      IntersectionInformation closest = null;
      IntersectionInformation temp = null;

      for (final BoundingVolume object : this.objects) {
         if (((mirrorObject == null) || !mirrorObject.equals(object)) && object.intersects(ray)) {
            temp = object.getChildIntersection(ray);

            if ((temp != null) && (temp.w > RTStatics.EPSILON)) {
               closest = closest == null ? temp : closest.w <= temp.w ? closest : temp;
            }
         }
      }

      return closest;
   }

   private void updateNormalizedImage() {
      if ((this.normalizedImage == null) || (this.normalizedImage.getWidth() != this.image.getWidth()) || (this.normalizedImage.getHeight() != this.normalizedImage.getHeight())) {
         this.normalizedImage = new BufferedImage(this.image.getWidth(), this.image.getHeight(), BufferedImage.TYPE_INT_RGB);
      }

      float min = Float.MAX_VALUE, max = -Float.MAX_VALUE;
      // Y' = 0.299 r + 0.587 g + 0.114 b

      for (int i = 0; i < 2; i++) {
         for (int x = 0; x < this.normalizedImage.getWidth(); x++) {
            for (int y = 0; y < this.normalizedImage.getHeight(); y++) {
               if (i == 0) {
                  if ((this.pixels[x][y] != null) && !(Float.valueOf(this.pixels[x][y][0]).equals(Float.NaN)) && !(Float.valueOf(this.pixels[x][y][1]).equals(Float.NaN))
                        && !(Float.valueOf(this.pixels[x][y][2]).equals(Float.NaN))) {
                     // luma
                     // min = Math.min(min, 0.299f * pixels[x][y][0] + 0.587f * pixels[x][y][1] + 0.114f * pixels[x][y][2]);
                     // max = Math.max(max, 0.299f * pixels[x][y][0] + 0.587f * pixels[x][y][1] + 0.114f * pixels[x][y][2]);

                     // brightness
                     final float[] hsv = RTStatics.convertRGBtoHSV(this.pixels[x][y]);

                     min = Math.min(min, hsv[2]);
                     max = Math.max(max, hsv[2]);
                  }
               } else {
                  if ((this.pixels[x][y] == null) || (this.pixels[x][y][0] == Float.NaN) || (this.pixels[x][y][1] == Float.NaN) || (this.pixels[x][y][2] == Float.NaN)) {
                     this.normalizedImage.setRGB(x, y, 0);
                  } else {
                     final float[] hsv = RTStatics.convertRGBtoHSV(this.pixels[x][y]);
                     // final float oldV = hsv[2];
                     hsv[2] = (hsv[2] - min) / (max - min);
                     final float[] rgb = RTStatics.convertHSVtoRGB(hsv);

                     // System.err.println("(" + oldV + " - " + min + ") / " + "(" + max + " - " + min + ") == " + hsv[2]);

                     this.normalizedImage.setRGB(x, y, new Color(rgb[0], rgb[1], rgb[2]).getRGB());
                  }
               }
            }
         }
      }
   }

   public void writeOutputFile(final String outputFile) {
      try {
         final File output = new File(outputFile);

         this.updateNormalizedImage();

         if (output.createNewFile() || output.canWrite()) {
            final String[] split = outputFile.split("\\.");
            ImageIO.write(this.normalizedImage, split[split.length - 1], output);
            System.out.println("Image saved to " + outputFile + " successfully");
         }
      } catch (final Exception e) {
         e.printStackTrace();
      }
   }

   public void addActionListener(final ActionListener actionListener) {
      this.listeners.add(actionListener);
   }
}
