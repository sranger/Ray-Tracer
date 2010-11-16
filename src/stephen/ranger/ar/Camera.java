package stephen.ranger.ar;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
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

      this.lightingModel.setCamera(this);
   }

   public void setPixel(final int x, final int y, final float[] color) {
      if ((color == null) || Float.valueOf(color[0]).equals(Float.NaN) || Float.valueOf(color[1]).equals(Float.NaN) || Float.valueOf(color[2]).equals(Float.NaN)) {
         this.pixels[x][y] = new float[3];
      } else {
         this.pixels[x][y] = color;
      }

      final float r = RTStatics.bound(0f, 1f, this.pixels[x][y][0]);
      final float g = RTStatics.bound(0f, 1f, this.pixels[x][y][1]);
      final float b = RTStatics.bound(0f, 1f, this.pixels[x][y][2]);

      this.image.setRGB(x, y, new Color(r, g, b).getRGB());
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
            final float xStart = -(Camera.this.viewportWidth / 2.0f);
            final float yStart = Camera.this.viewportHeight / 2.0f;
            final float xInc = Camera.this.viewportWidth / Camera.this.screenWidth;
            final float yInc = -Camera.this.viewportHeight / Camera.this.screenHeight;
            final long startTime = System.nanoTime();

            final int temp = Runtime.getRuntime().availableProcessors();
            final int cpus = Math.max(1, temp - 1);
            final List<RenderThread> threads = new ArrayList<RenderThread>();
            final Random random = new Random();

            final List<List<int[]>> pixels = new ArrayList<List<int[]>>();

            for (int i = 0; i < cpus; i++) {
               pixels.add(new ArrayList<int[]>());
            }

            for (int x = 0; x < Camera.this.image.getWidth(); x++) {
               for (int y = 0; y < Camera.this.image.getHeight(); y++) {
                  pixels.get(random.nextInt(cpus)).add(new int[] { x, y });
               }
            }

            RTStatics.setProgressBarMinMax(0, Camera.this.image.getWidth() * Camera.this.image.getHeight());
            RTStatics.setProgressBarValue(0);
            RTStatics.setProgressBarString("Rendering image...");

            final ActionListener threadListener = new ActionListener() {
               double totalTime = 0;

               @Override
               public synchronized void actionPerformed(final ActionEvent event) {
                  threads.remove(event.getSource());
                  final double seconds = Double.parseDouble(event.getActionCommand());
                  this.totalTime += seconds;
                  System.out.println("node #" + event.getID() + ": " + seconds + " seconds,  threads running: " + threads.size());

                  Camera.this.sendUpdate();

                  if (threads.size() == 0) {
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
               System.out.println("creating thread #" + i + " with " + pixels.get(i).size() + " pixels");
               threads.add(new RenderThread(Camera.this, pixels.get(i), xStart, yStart, xInc, yInc, threadListener, i));
               threads.get(i).start();
            }
         }
      }.start();
   }

   public void sendUpdate() {
      for (final ActionListener listener : this.listeners) {
         listener.actionPerformed(new ActionEvent(this, 2, "update"));
      }
   }

   public IntersectionInformation getClosestIntersection(final BoundingVolume mirrorObject, Vector3f origin, final Vector3f direction, final Vector3f normal,
         final int depth) {

      if (normal != null) {
         origin = RTStatics.offsetPosition(origin, normal);
      }

      final Ray ray = new Ray(origin, direction);
      IntersectionInformation closest = null;
      IntersectionInformation temp = null;

      for (final BoundingVolume object : this.objects) {
         if (((mirrorObject == null) || !mirrorObject.equals(object)) && object.intersects(ray)) {
            temp = object.getChildIntersection(ray, depth + 1);

            if ((temp != null) && (temp.w > RTStatics.EPSILON) && ((closest == null) || (temp.w < closest.w))) {
               closest = temp;
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
      float total = 0;
      // Y' = 0.299 r + 0.587 g + 0.114 b

      for (int i = 0; i < 2; i++) {
         for (int x = 0; x < this.normalizedImage.getWidth(); x++) {
            for (int y = 0; y < this.normalizedImage.getHeight(); y++) {
               if (i == 0) {
                  if ((this.pixels[x][y] != null) && !Float.valueOf(this.pixels[x][y][0]).equals(Float.NaN) && !Float.valueOf(this.pixels[x][y][1]).equals(Float.NaN)
                        && !Float.valueOf(this.pixels[x][y][2]).equals(Float.NaN)) {
                     // luma
                     // min = Math.min(min, 0.299f * pixels[x][y][0] + 0.587f * pixels[x][y][1] + 0.114f * pixels[x][y][2]);
                     // max = Math.max(max, 0.299f * pixels[x][y][0] + 0.587f * pixels[x][y][1] + 0.114f * pixels[x][y][2]);

                     // brightness
                     final float[] hsv = RTStatics.convertRGBtoHSV(this.pixels[x][y]);
                     total += hsv[2];
                     min = Math.min(min, hsv[2]);
                     max = Math.max(max, hsv[2]);
                  }
               } else {
                  if ((x == 0) && (y == 0)) {
                     total /= this.image.getWidth() * this.image.getHeight();
                  }
                  if ((this.pixels[x][y] == null) || (this.pixels[x][y][0] == Float.NaN) || (this.pixels[x][y][1] == Float.NaN) || (this.pixels[x][y][2] == Float.NaN)) {
                     this.normalizedImage.setRGB(x, y, 0);
                  } else {
                     final float[] hsv = RTStatics.convertRGBtoHSV(this.pixels[x][y]);
                     // final float oldV = hsv[2];
                     hsv[2] = (hsv[2] - min) / (max - min);
                     final float[] rgb = RTStatics.convertHSVtoRGB(hsv);

                     // System.err.println("(" + oldV + " - " + min + ") / " + "(" + max + " - " + min + ") == " + hsv[2]);

                     this.normalizedImage.setRGB(x, y, new Color(RTStatics.bound(0, 1, rgb[0]), RTStatics.bound(0, 1, rgb[1]), RTStatics.bound(0, 1, rgb[2]))
                     .getRGB());
                  }
               }
            }
         }
      }
   }

   public void writeOutputFile(final String outputFile, final boolean normalized) {
      try {
         final File output = new File(outputFile);

         if (normalized) {
            this.updateNormalizedImage();
         }

         if (output.createNewFile() || output.canWrite()) {
            final String[] split = outputFile.split("\\.");
            if (ImageIO.write(normalized ? this.normalizedImage : this.image, split[split.length - 1], output)) {
               System.out.println("Image saved to " + outputFile + " successfully");
            } else {
               System.err.println("Error writing image to " + outputFile);
            }
         }
      } catch (final Exception e) {
         e.printStackTrace();
      }
   }

   public void addActionListener(final ActionListener actionListener) {
      this.listeners.add(actionListener);
   }
}
