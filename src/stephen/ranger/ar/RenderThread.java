package stephen.ranger.ar;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Random;

import javax.vecmath.Vector3f;

public class RenderThread extends Thread {
   private final Camera camera;
   private final List<int[]> pixels;
   private final float xMin, yMin, xInc, yInc;
   private final ActionListener listener;
   private final int threadNumber;

   public RenderThread(final Camera camera, final List<int[]> pixels, final float xMin, final float yMin, final float xInc, final float yInc,
         final ActionListener listener, final int threadNumber) {
      this.camera = camera;
      this.pixels = pixels;
      this.xMin = xMin;
      this.yMin = yMin;
      this.xInc = xInc;
      this.yInc = yInc;
      this.listener = listener;
      this.threadNumber = threadNumber;
   }

   @Override
   public void run() {
      final Vector3f viewportDirection = new Vector3f();
      final Random random = new Random();
      IntersectionInformation closest = null;
      final float[][] colors = new float[camera.multiSamples][];
      final long startTime = System.nanoTime();

      for (int i = 0; i < pixels.size(); i++) {
         final int[] pixel = pixels.get(i);
         final float x = pixel[0] * xInc + xMin;
         final float y = pixel[1] * yInc + yMin;

         for (int j = 0; j < camera.multiSamples; j++) {
            viewportDirection.x = j == 0 ? x : x + (random.nextFloat() * 2f - 1f) * xInc / 2f;
            viewportDirection.y = j == 0 ? y : y + +(random.nextFloat() * 2f - 1f) * yInc / 2f;
            viewportDirection.z = -camera.nearPlaneDistance;
            camera.rotation.transform(viewportDirection);
            viewportDirection.normalize();

            closest = camera.getClosestIntersection(null, camera.origin, viewportDirection, 0);

            if (closest != null) {
               colors[j] = camera.lightingModel.getPixelColor(closest, 0);
            } else {
               colors[j] = camera.light.ambient.getColorComponents(new float[3]);
            }
         }

         camera.setPixel(pixel[0], pixel[1], RTStatics.computeColorAverage(colors));

         RTStatics.incrementProgressBarValue(1);

         if (i % 1000 == 0) {
            camera.sendUpdate();
         }
      }

      final long endTime = System.nanoTime();
      listener.actionPerformed(new ActionEvent(this, threadNumber, Double.toString((endTime - startTime) / 1000000000.)));
   }
   //   private IntersectionInformation getClosestIntersection(final BoundingVolume mirrorObject, final Vector3f origin, final Vector3f direction) {
   //      final Ray ray = new Ray(origin, direction);
   //      IntersectionInformation closest = null;
   //      IntersectionInformation temp = null;
   //
   //      for (final BoundingVolume object : camera.objects) {
   //         if ((mirrorObject == null || !mirrorObject.equals(object)) && object.intersects(ray)) {
   //            temp = object.getChildIntersection(ray);
   //
   //            if (temp != null && temp.w > RTStatics.EPSILON) {
   //               closest = closest == null ? temp : closest.w <= temp.w ? closest : temp;
   //            }
   //         }
   //      }
   //
   //      return closest;
   //   }
}
