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
      objects = scene.objects;
      lightingModel = scene.lightingModel;
      light = scene.light;
      this.multiSamples = multiSamples;
      this.brdfSamples = brdfSamples;
      orientation = scene.cameraOrientation;
      nearPlaneDistance = nearPlane;
      this.screenWidth = screenWidth;
      this.screenHeight = screenHeight;
      viewportWidth = (screenWidth >= screenHeight ? (float) screenWidth / (float) screenHeight : 1.0f) * nearPlaneDistance;
      viewportHeight = (screenWidth >= screenHeight ? 1.0f : (float) screenHeight / (float) screenWidth) * nearPlaneDistance;

      image = new BufferedImage(screenWidth, screenHeight, BufferedImage.TYPE_INT_RGB);
      pixels = new float[screenWidth][screenHeight][];

      rotation = new Matrix4f();
      rotation.set(RTStatics.initializeQuat4f(orientation));

      final float[][] minMax = new float[][] { { Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE }, { -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE } };

      for (final BoundingVolume object : objects) {
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
      origin = new Vector3f(centerX, centerY, centerZ + distance);

      rotation.transform(origin);
      final Vector3f viewportDirection = new Vector3f(0, 0, -nearPlaneDistance);
      rotation.transform(viewportDirection);
      viewportDirection.normalize();

      //      if (viewportDirection.z < 0) {
      //         origin.z = -origin.z;
      //      }

      System.out.println("camera location:  " + origin);
      System.out.println("camera direction: " + viewportDirection);

      lightingModel.setCamera(this);
   }

   public void setPixel(final int x, final int y, final float[] color) {
      if (color == null || Float.valueOf(color[0]).equals(Float.NaN) || Float.valueOf(color[1]).equals(Float.NaN) || Float.valueOf(color[2]).equals(Float.NaN)) {
         pixels[x][y] = new float[3];
      } else {
         pixels[x][y] = color;
      }

      final float r = RTStatics.bound(0f, 1f, pixels[x][y][0]);
      final float g = RTStatics.bound(0f, 1f, pixels[x][y][1]);
      final float b = RTStatics.bound(0f, 1f, pixels[x][y][2]);

      image.setRGB(x, y, new Color(r, g, b).getRGB());
   }

   public BufferedImage getNormalizedImage() {
      updateNormalizedImage();
      return normalizedImage;
   }

   public BufferedImage getImage() {
      return image;
   }

   public void createImage() {
      new Thread() {
         @Override
         public void run() {
            final float xStart = -(viewportWidth / 2.0f);
            final float yStart = viewportHeight / 2.0f;
            final float xInc = viewportWidth / screenWidth;
            final float yInc = viewportHeight / screenHeight;
            final long startTime = System.nanoTime();

            final List<int[]> nodes = new ArrayList<int[]>();

            for (int x = 0; x < image.getWidth(); x = x + nodeSize) {
               for (int y = 0; y < image.getHeight(); y = y + nodeSize) {
                  nodes.add(new int[] { x, y, x + nodeSize > image.getWidth() ? image.getWidth() - x : nodeSize, y + nodeSize > image.getHeight() ? image.getHeight() - y : nodeSize });
               }
            }

            int temp = Runtime.getRuntime().availableProcessors();
            temp = Math.max(1, temp);
            temp = Math.min(nodes.size(), temp);
            final int cpus = temp;

            RTStatics.setProgressBarMinMax(0, image.getWidth() * image.getHeight());
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
                  totalTime += seconds;
                  System.out.println("node #" + event.getID() + ": " + seconds + " seconds,  threads running: " + threads.size());

                  for (final ActionListener listener : listeners) {
                     listener.actionPerformed(new ActionEvent(this, 2, "update"));
                  }

                  if (nodePosition < nodes.size()) {
                     final int[] node = nodes.get(nodePosition);
                     System.out.println("creating node for: " + Arrays.toString(node));
                     nodePosition++;
                     final RenderThread thread = new RenderThread(Camera.this, this, nodePosition, node[0], node[1], node[2], node[3], xStart, yStart, xInc, yInc);
                     threads.add(thread);
                     thread.start();
                  } else if (nodePosition == nodes.size() && threads.size() == 0) {
                     final long endTime = System.nanoTime();

                     System.out.println("total elapsed time: " + (endTime - startTime) / 1000000000. + " seconds");
                     System.out.println("total cpu time:     " + totalTime + " seconds");

                     RTStatics.setProgressBarValue(image.getWidth() * image.getHeight());
                     RTStatics.setProgressBarString("Rendered image completed!");

                     for (final ActionListener listener : listeners) {
                        listener.actionPerformed(new ActionEvent(this, 1, "finished"));
                     }
                     listeners.clear();
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

   public IntersectionInformation getClosestIntersection(final BoundingVolume mirrorObject, final Vector3f origin, final Vector3f direction, final int depth) {
      final Ray ray = new Ray(origin, direction);
      IntersectionInformation closest = null;
      IntersectionInformation temp = null;

      for (final BoundingVolume object : objects) {
         if ((mirrorObject == null || !mirrorObject.equals(object)) && object.intersects(ray)) {
            temp = object.getChildIntersection(ray, depth + 1);

            if (temp != null && temp.w > RTStatics.EPSILON && (closest == null || temp.w < closest.w)) {
               closest = temp;
            }
         }
      }

      return closest;
   }

   private void updateNormalizedImage() {
      if (normalizedImage == null || normalizedImage.getWidth() != image.getWidth() || normalizedImage.getHeight() != normalizedImage.getHeight()) {
         normalizedImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
      }

      float min = Float.MAX_VALUE, max = -Float.MAX_VALUE;
      // Y' = 0.299 r + 0.587 g + 0.114 b

      for (int i = 0; i < 2; i++) {
         for (int x = 0; x < normalizedImage.getWidth(); x++) {
            for (int y = 0; y < normalizedImage.getHeight(); y++) {
               if (i == 0) {
                  if (pixels[x][y] != null && !Float.valueOf(pixels[x][y][0]).equals(Float.NaN) && !Float.valueOf(pixels[x][y][1]).equals(Float.NaN)
                        && !Float.valueOf(pixels[x][y][2]).equals(Float.NaN)) {
                     // luma
                     // min = Math.min(min, 0.299f * pixels[x][y][0] + 0.587f * pixels[x][y][1] + 0.114f * pixels[x][y][2]);
                     // max = Math.max(max, 0.299f * pixels[x][y][0] + 0.587f * pixels[x][y][1] + 0.114f * pixels[x][y][2]);

                     // brightness
                     final float[] hsv = RTStatics.convertRGBtoHSV(pixels[x][y]);

                     min = Math.min(min, hsv[2]);
                     max = Math.max(max, hsv[2]);
                  }
               } else {
                  if (pixels[x][y] == null || pixels[x][y][0] == Float.NaN || pixels[x][y][1] == Float.NaN || pixels[x][y][2] == Float.NaN) {
                     normalizedImage.setRGB(x, y, 0);
                  } else {
                     final float[] hsv = RTStatics.convertRGBtoHSV(pixels[x][y]);
                     // final float oldV = hsv[2];
                     hsv[2] = (hsv[2] - min) / (max - min);
                     final float[] rgb = RTStatics.convertHSVtoRGB(hsv);

                     // System.err.println("(" + oldV + " - " + min + ") / " + "(" + max + " - " + min + ") == " + hsv[2]);

                     normalizedImage.setRGB(x, y, new Color(RTStatics.bound(0, 1, rgb[0]), RTStatics.bound(0, 1, rgb[1]), RTStatics.bound(0, 1, rgb[2])).getRGB());
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
            updateNormalizedImage();
         }

         if (output.createNewFile() || output.canWrite()) {
            final String[] split = outputFile.split("\\.");
            ImageIO.write(normalized ? normalizedImage : image, split[split.length - 1], output);
            System.out.println("Image saved to " + outputFile + " successfully");
         }
      } catch (final Exception e) {
         e.printStackTrace();
      }
   }

   public void addActionListener(final ActionListener actionListener) {
      listeners.add(actionListener);
   }
}
