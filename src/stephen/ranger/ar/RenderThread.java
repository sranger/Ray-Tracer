package stephen.ranger.ar;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;

import javax.vecmath.Vector3f;

import stephen.ranger.ar.bounds.BoundingVolume;

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
      final float[][] colors = new float[camera.multiSamples][];
      final long startTime = System.nanoTime();

      for (int x = xPos; x < xPos + xSpan; x++) {
         for (int y = yPos; y < yPos + ySpan; y++) {
            centerX = xStart + xInc * x;
            centerY = yStart - yInc * y;
            xmin = centerX - xInc / 2f;
            ymin = centerY - yInc / 2f;

            for (int i = 0; i < camera.multiSamples; i++) {
               viewportDirection.x = i == 0 ? centerX : random.nextFloat() * xInc + xmin;
               viewportDirection.y = i == 0 ? centerY : random.nextFloat() * yInc + ymin;
               viewportDirection.z = -camera.nearPlaneDistance;
               camera.rotation.transform(viewportDirection);
               viewportDirection.normalize();

               closest = getClosestIntersection(null, camera.origin, viewportDirection);

               if (closest != null) {
                  final ColorInformation colorInfo = closest.intersectionObject.getColorInformation(closest);

                  if (colorInfo instanceof BRDFMaterial) {
                     colors[i] = closest.intersectionObject.getColor(closest).getColorComponents(new float[3]);
                     final float luminance = ((BRDFMaterial) colorInfo).getBRDFLuminocity(closest, camera);
                     colors[i][0] = luminance * colors[i][0];
                     colors[i][1] = luminance * colors[i][1];
                     colors[i][2] = luminance * colors[i][2];
                  } else {
                     colors[i] = camera.lightingModel.getPixelColor(closest).getColorComponents(new float[3]);

                     if (colorInfo.isMirror) {
                        final Vector3f V = new Vector3f(closest.intersection);
                        V.sub(camera.origin);
                        V.normalize();

                        final IntersectionInformation mirrorInfo = getClosestIntersection(closest.intersectionObject, closest.intersection, RTStatics.getReflectionDirection(closest, V));
                        final float[] mirrorColor = mirrorInfo == null ? camera.light.ambient.getColorComponents(new float[3]) : mirrorInfo.intersectionObject.getColor(mirrorInfo).getColorComponents(
                              new float[3]);

                        colors[i][0] = colors[i][0] * mirrorColor[0];
                        colors[i][1] = colors[i][1] * mirrorColor[1];
                        colors[i][2] = colors[i][2] * mirrorColor[2];
                     }
                  }
               } else {
                  colors[i] = camera.light.ambient.getColorComponents(new float[3]);
               }
            }

            camera.setPixel(x, y, RTStatics.computeColorAverage(colors));
         }
      }

      final long endTime = System.nanoTime();
      listener.actionPerformed(new ActionEvent(this, nodePosition, Double.toString((endTime - startTime) / 1000000000.)));
   }

   private IntersectionInformation getClosestIntersection(final BoundingVolume mirrorObject, final Vector3f origin, final Vector3f direction) {
      final Ray ray = new Ray(origin, direction);
      IntersectionInformation closest = null;
      IntersectionInformation temp = null;

      for (final BoundingVolume object : camera.objects) {
         if ((mirrorObject == null || !mirrorObject.equals(object)) && object.intersects(ray)) {
            temp = object.getChildIntersection(ray);

            if (temp != null && temp.w > RTStatics.EPSILON) {
               closest = closest == null ? temp : closest.w <= temp.w ? closest : temp;
            }
         }
      }

      return closest;
   }
}
