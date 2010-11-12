package stephen.ranger.ar;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;

import javax.vecmath.Vector3f;

public class RenderThread extends Thread {
   private final Camera camera;
   private final ActionListener listener;
   private final int nodePosition;
   private final int xPos;
   private final int yPos;
   private final int xSpan;
   private final int ySpan;
   private final float xStart;
   private final float yStart;
   private final float xInc;
   private final float yInc;

   public RenderThread(final Camera camera, final ActionListener listener, final int nodePosition, final int xPos, final int yPos, final int xSpan, final int ySpan, final float xStart,
         final float yStart, final float xInc, final float yInc) {
      this.camera = camera;
      this.listener = listener;
      this.nodePosition = nodePosition;
      this.xPos = xPos;
      this.yPos = yPos;
      this.xSpan = xSpan;
      this.ySpan = ySpan;
      this.xStart = xStart;
      this.yStart = yStart;
      this.xInc = xInc;
      this.yInc = yInc;
   }

   @Override
   public void run() {
      final Vector3f viewportDirection = new Vector3f();
      float centerX, centerY, xmin, ymin;
      final Random random = new Random();
      IntersectionInformation closest = null;
      final float[][] colors = new float[this.camera.multiSamples][];
      final long startTime = System.nanoTime();

      for (int x = this.xPos; x < this.xPos + this.xSpan; x++) {
         for (int y = this.yPos; y < this.yPos + this.ySpan; y++) {
            centerX = this.xStart + this.xInc * x;
            centerY = this.yStart - this.yInc * y;
            xmin = centerX - this.xInc / 2f;
            ymin = centerY - this.yInc / 2f;

            for (int i = 0; i < this.camera.multiSamples; i++) {
               viewportDirection.x = i == 0 ? centerX : random.nextFloat() * this.xInc + xmin;
               viewportDirection.y = i == 0 ? centerY : random.nextFloat() * this.yInc + ymin;
               viewportDirection.z = -this.camera.nearPlaneDistance;
               this.camera.rotation.transform(viewportDirection);
               viewportDirection.normalize();

               closest = this.camera.getClosestIntersection(null, this.camera.origin, viewportDirection, 0);

               if (closest != null) {
                  colors[i] = this.camera.lightingModel.getPixelColor(closest, 0);
               } else {
                  colors[i] = this.camera.light.ambient.getColorComponents(new float[3]);
               }
            }

            this.camera.setPixel(x, y, RTStatics.computeColorAverage(colors));
         }

         RTStatics.incrementProgressBarValue(this.ySpan);
      }

      final long endTime = System.nanoTime();
      this.listener.actionPerformed(new ActionEvent(this, this.nodePosition, Double.toString((endTime - startTime) / 1000000000.)));
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
