package stephen.ranger.ar;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;

import stephen.ranger.ar.bounds.BoundingVolume;
import stephen.ranger.ar.lighting.Light;
import stephen.ranger.ar.lighting.LightingModel;

public class Camera {
   private static final Vector3f FORWARD = new Vector3f(0, 0, 1);

   private final Matrix4f rotation;

   public final Vector3f origin, orientation;
   public final float nearPlaneDistance;
   public final float viewportWidth, viewportHeight;
   public final int screenWidth, screenHeight;

   private final BufferedImage image;

   public Camera(final Vector3f origin, final Vector3f orientation, final int screenWidth, final int screenHeight) {
      this.origin = origin;
      this.orientation = orientation;
      this.screenWidth = screenWidth;
      this.screenHeight = screenHeight;
      this.nearPlaneDistance = RTStatics.nearPlane;
      this.viewportWidth = (screenWidth >= screenHeight ? (float) screenWidth / (float) screenHeight : 1.0f) * this.nearPlaneDistance;
      this.viewportHeight = (screenWidth >= screenHeight ? 1.0f : (float) screenHeight / (float) screenWidth) * this.nearPlaneDistance;

      this.image = new BufferedImage(screenWidth, screenHeight, BufferedImage.TYPE_INT_RGB);

      this.rotation = new Matrix4f();
      this.rotation.set(RTStatics.initializeQuat4f(orientation));
   }

   public Camera() {
      this(new Vector3f(), new Vector3f(0, 0, -1), 1600, 1000);
   }

   public void setPixel(final int x, final int y, final Color color) {
      this.image.setRGB(x, y, color.getRGB());
   }

   public BufferedImage getImage() {
      final BufferedImage imageCopy = new BufferedImage(this.screenWidth, this.screenHeight, BufferedImage.TYPE_INT_RGB);
      final Graphics g = imageCopy.getGraphics();
      g.drawImage(this.image, 0, 0, null);
      g.dispose();

      return imageCopy;
   }

   public void createImage(final BoundingVolume[] objects, final LightingModel lightingModel, final Light light) {
      final float xStart = -(this.viewportWidth / 2.0f);
      final float yStart = this.viewportHeight / 2.0f;
      final float xInc = this.viewportWidth / this.screenWidth;
      final float yInc = this.viewportHeight / this.screenHeight;

      final long startTime = System.nanoTime();
      long innerStart = 0, innerEnd = 0;

      for (int x = 0; x < this.screenWidth; x++) {
         if (x % 100 == 0) {
            innerStart = System.nanoTime();
            System.out.print("lines " + x + " - " + (x + 99) + ": ");
         }

         for (int y = 0; y < this.screenHeight; y++) {
            final Vector3f viewportDirection = new Vector3f(xStart + xInc * x, yStart - yInc * y, -this.nearPlaneDistance);
            this.rotation.transform(viewportDirection);
            viewportDirection.normalize();
            final Ray ray = new Ray(this.origin, viewportDirection);
            IntersectionInformation closest = null;
            IntersectionInformation temp = null;

            for (final BoundingVolume object : objects) {
               if (object.intersects(ray)) {
                  temp = object.getChildIntersection(ray);

                  if (temp != null) {
                     closest = closest == null ? temp : closest.w <= temp.w ? closest : temp;
                  }
               }
            }

            if (closest != null) {
               this.setPixel(x, y, lightingModel.getPixelColor(closest));
            } else {
               this.setPixel(x, y, light.ambient);
            }
         }

         if (x % 100 == 99) {
            innerEnd = System.nanoTime();
            System.out.println("duration: " + (innerEnd - innerStart) / 1000000000. + " seconds");
         }
      }

      final long endTime = System.nanoTime();

      System.out.println("elapsed time: " + (endTime - startTime) / 1000000000. + " seconds");
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
}
