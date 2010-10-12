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
import javax.swing.SwingUtilities;
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

   public Camera(final BoundingVolume[] objects, final LightingModel lightingModel, final Light light, final int samples, final float nearPlane, final int screenWidth, final int screenHeight) {
      this.objects = objects;
      this.lightingModel = lightingModel;
      this.light = light;
      this.samples = samples;
      orientation = new float[] { 0, 0, 0 };
      nearPlaneDistance = nearPlane;
      this.screenWidth = screenWidth;
      this.screenHeight = screenHeight;
      viewportWidth = (screenWidth >= screenHeight ? (float) screenWidth / (float) screenHeight : 1.0f) * nearPlaneDistance;
      viewportHeight = (screenWidth >= screenHeight ? 1.0f : (float) screenHeight / (float) screenWidth) * nearPlaneDistance;

      image = new BufferedImage(screenWidth, screenHeight, BufferedImage.TYPE_INT_RGB);

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

      origin = new Vector3f((minMax[1][0] - minMax[0][0]) / 2f + minMax[0][0], (minMax[1][1] - minMax[0][1]) / 2f + minMax[0][1], minMax[1][2] * 4f);
      System.out.println("camera origin: " + origin);
   }

   public void setPixel(final int x, final int y, final Color color) {
      image.setRGB(x, y, color.getRGB());
   }

   public BufferedImage getImage() {
      //      final BufferedImage imageCopy = new BufferedImage(screenWidth, screenHeight, BufferedImage.TYPE_INT_RGB);
      //      final Graphics g = imageCopy.getGraphics();
      //      g.drawImage(image, 0, 0, null);
      //      g.dispose();
      //
      //      return imageCopy;

      return image;
   }

   public void createImage() {
      SwingUtilities.invokeLater(new Runnable() {
         @Override
         public void run() {
            final float xStart = -(viewportWidth / 2.0f);
            final float yStart = viewportHeight / 2.0f;
            final float xInc = viewportWidth / screenWidth;
            final float yInc = viewportHeight / screenHeight;
            final Random random = new Random();

            final Color[] colors = new Color[samples];

            final Vector3f viewportDirection = new Vector3f();
            IntersectionInformation closest = null;
            IntersectionInformation temp = null;
            float centerX = 0, centerY = 0, xmin = 0, ymin = 0;

            final long startTime = System.nanoTime();
            long innerStart = 0, innerEnd = 0;

            for (int x = 0; x < screenWidth; x++) {
               if (x % 100 == 0) {
                  innerStart = System.nanoTime();
                  System.out.print("lines " + x + " - " + (x + 99) + ": ");
               }

               for (int y = 0; y < screenHeight; y++) {
                  centerX = xStart + xInc * x;
                  centerY = yStart - yInc * y;
                  xmin = centerX - xInc / 2f;
                  ymin = centerY - yInc / 2f;

                  for (int i = 0; i < samples; i++) {
                     viewportDirection.x = i == 0 ? centerX : random.nextFloat() * xInc + xmin;
                     viewportDirection.y = i == 0 ? centerY : random.nextFloat() * yInc + ymin;
                     viewportDirection.z = -nearPlaneDistance;
                     rotation.transform(viewportDirection);
                     viewportDirection.normalize();
                     final Ray ray = new Ray(origin, viewportDirection);
                     closest = null;
                     temp = null;

                     for (final BoundingVolume object : objects) {
                        if (object.intersects(ray)) {
                           temp = object.getChildIntersection(ray);

                           if (temp != null) {
                              closest = closest == null ? temp : closest.w <= temp.w ? closest : temp;
                           }
                        }
                     }

                     if (closest != null) {
                        colors[i] = lightingModel.getPixelColor(closest);
                     } else {
                        colors[i] = light.ambient;
                     }
                  }

                  setPixel(x, y, computeAverage(colors));
               }

               if (x % 100 == 99) {
                  innerEnd = System.nanoTime();
                  System.out.println("duration: " + (innerEnd - innerStart) / 1000000000. + " seconds");

                  for (final ActionListener listener : listeners) {
                     listener.actionPerformed(new ActionEvent(this, 2, "update"));
                  }
               }
            }

            final long endTime = System.nanoTime();

            System.out.println("elapsed time: " + (endTime - startTime) / 1000000000. + " seconds");

            for (final ActionListener listener : listeners) {
               listener.actionPerformed(new ActionEvent(this, 1, "finished"));
            }

            listeners.clear();
         }
      });
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
            ImageIO.write(image, split[split.length - 1], output);
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
