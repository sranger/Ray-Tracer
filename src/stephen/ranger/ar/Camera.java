package stephen.ranger.ar;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;

import stephen.ranger.ar.bounds.BoundingVolume;
import stephen.ranger.ar.lighting.Light;
import stephen.ranger.ar.lighting.LightingModel;

public class Camera {
   private final Matrix4f rotation;

   public final BoundingVolume[] objects;
   public final LightingModel lightingModel;
   public final Light light;
   public final int samples;
   public final Vector3f origin;
   public final float[] orientation;
   public final float nearPlaneDistance;
   public final float viewportWidth, viewportHeight;
   public final int screenWidth, screenHeight;

   private final BufferedImage image;

   private final Set<ActionListener> listeners = new HashSet<ActionListener>();

   public Camera(final BoundingVolume[] objects, final LightingModel lightingModel, final Light light, final int samples, final float nearPlane, final int screenWidth, final int screenHeight,
         final float fov) {
      this.objects = objects;
      this.lightingModel = lightingModel;
      this.light = light;
      this.samples = samples;
      this.orientation = new float[] { 0, 0, 0 };
      this.nearPlaneDistance = nearPlane;
      this.screenWidth = screenWidth;
      this.screenHeight = screenHeight;
      this.viewportWidth = (screenWidth >= screenHeight ? (float) screenWidth / (float) screenHeight : 1.0f) * this.nearPlaneDistance;
      this.viewportHeight = (screenWidth >= screenHeight ? 1.0f : (float) screenHeight / (float) screenWidth) * this.nearPlaneDistance;

      this.image = new BufferedImage(screenWidth, screenHeight, BufferedImage.TYPE_INT_RGB);

      this.rotation = new Matrix4f();
      this.rotation.set(RTStatics.initializeQuat4f(this.orientation));

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
      final float distance = width / 2f / (float) Math.tan(Math.toRadians(fov));
      this.origin = new Vector3f(centerX, centerY, centerZ + distance);

      this.lightingModel.setCameraPosition(new float[] { centerX, centerY, centerZ + distance });
   }

   public void setPixel(final int x, final int y, final Color color) {
      this.image.setRGB(x, y, color.getRGB());
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
            final float yInc = Camera.this.viewportHeight / Camera.this.screenHeight;
            final Random random = new Random();

            final Color[] colors = new Color[Camera.this.samples];

            final Vector3f viewportDirection = new Vector3f();
            IntersectionInformation closest = null;
            final IntersectionInformation temp = null;
            float centerX = 0, centerY = 0, xmin = 0, ymin = 0;

            final long startTime = System.nanoTime();
            long innerStart = 0;
            long innerEnd = 0;

            for (int x = 0; x < Camera.this.screenWidth; x++) {
               if (x % 100 == 0) {
                  innerStart = System.nanoTime();
                  System.out.print("lines " + x + " - " + (x + 99) + ": ");
               }

               for (int y = 0; y < Camera.this.screenHeight; y++) {
                  centerX = xStart + xInc * x;
                  centerY = yStart - yInc * y;
                  xmin = centerX - xInc / 2f;
                  ymin = centerY - yInc / 2f;

                  for (int i = 0; i < Camera.this.samples; i++) {
                     viewportDirection.x = i == 0 ? centerX : random.nextFloat() * xInc + xmin;
                     viewportDirection.y = i == 0 ? centerY : random.nextFloat() * yInc + ymin;
                     viewportDirection.z = -Camera.this.nearPlaneDistance;
                     Camera.this.rotation.transform(viewportDirection);
                     viewportDirection.normalize();

                     closest = Camera.this.getClosestIntersection(null, Camera.this.origin, viewportDirection);

                     if (closest != null) {
                        colors[i] = Camera.this.lightingModel.getPixelColor(closest);

                        if ((closest != null) && closest.intersectionObject.getColorInformation(closest).isMirror) {
                           final Vector3f V = new Vector3f(closest.intersection);
                           V.sub(Camera.this.origin);
                           V.normalize();

                           final IntersectionInformation mirrorInfo = Camera.this
                           .getClosestIntersection(closest.intersectionObject, closest.intersection, RTStatics.getReflectionDirection(closest, V));
                           final float[] mirrorColor = mirrorInfo == null ? Camera.this.light.ambient.getColorComponents(new float[3]) : mirrorInfo.intersectionObject.getColor(mirrorInfo)
                                 .getColorComponents(new float[3]);
                           final float[] color = colors[i].getColorComponents(new float[3]);

                           colors[i] = new Color(color[0] * mirrorColor[0], color[1] * mirrorColor[1], color[2] * mirrorColor[2]);
                        }
                     } else {
                        colors[i] = Camera.this.light.ambient;
                     }
                  }

                  Camera.this.setPixel(x, y, Camera.this.computeAverage(colors));
               }

               if (x % 100 == 99) {
                  innerEnd = System.nanoTime();
                  System.out.println("duration: " + (innerEnd - innerStart) / 1000000000. + " seconds");

                  for (final ActionListener listener : Camera.this.listeners) {
                     listener.actionPerformed(new ActionEvent(this, 2, "update"));
                  }
               }
            }

            final long endTime = System.nanoTime();

            System.out.println("elapsed time: " + (endTime - startTime) / 1000000000. + " seconds");

            for (final ActionListener listener : Camera.this.listeners) {
               listener.actionPerformed(new ActionEvent(this, 1, "finished"));
            }

            Camera.this.listeners.clear();
         }
      }.start();
   }

   private IntersectionInformation getClosestIntersection(final BoundingVolume mirrorObject, final Vector3f origin, final Vector3f direction) {
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

   private Color computeAverage(final Color[] colors) {
      float r = 0, g = 0, b = 0;

      for (final Color color : colors) {
         final float[] c = color.getColorComponents(new float[3]);
         r += c[0];
         g += c[1];
         b += c[2];
      }

      return new Color(r / colors.length, g / colors.length, b / colors.length, 1f);
   }

   public void writeOutputFile(final String outputFile) {
      final File output = new File(outputFile);

      try {
         if (output.createNewFile() || output.canWrite()) {
            final String[] split = outputFile.split("\\.");
            ImageIO.write(this.image, split[split.length - 1], output);
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
