package stephen.ranger.ar;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.vecmath.AxisAngle4d;
import javax.vecmath.Matrix4d;
import javax.vecmath.Vector3d;

import stephen.ranger.ar.bounds.BoundingVolume;
import stephen.ranger.ar.lighting.Light;
import stephen.ranger.ar.lighting.LightingModel;

public class Camera {
    private static final Vector3d FORWARD = new Vector3d(0, 0, 1);

    private final Matrix4d rotation;

    public final Vector3d origin;
    public final Vector3d lookAt;
    public final double nearPlaneDistance;
    public final double viewportWidth, viewportHeight;
    public final int screenWidth, screenHeight;

    private final BufferedImage image;

    public Camera(final Vector3d origin, final Vector3d lookAt, final int screenWidth, final int screenHeight) {
        this.origin = origin;
        this.lookAt = lookAt;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        nearPlaneDistance = RTStatics.nearPlane;
        viewportWidth = (screenWidth >= screenHeight ? (double) screenWidth / (double) screenHeight : 1.0) * nearPlaneDistance;
        viewportHeight = (screenWidth >= screenHeight ? 1.0 : (double) screenHeight / (double) screenWidth) * nearPlaneDistance;

        image = new BufferedImage(screenWidth, screenHeight, BufferedImage.TYPE_INT_RGB);

        lookAt.normalize();

        final Vector3d rotationAxis = new Vector3d();
        rotationAxis.cross(Camera.FORWARD, lookAt);

        final double rotationAngle = Math.acos(Camera.FORWARD.dot(lookAt));

        if (rotationAxis.length() == 0.0 || rotationAngle == 0.0) {
            rotationAxis.set(0, 1, 0);
        }

        rotationAxis.normalize();

        System.out.println("rotation axis: " + rotationAxis);
        System.out.println("rotation angle: " + rotationAngle);

        rotation = new Matrix4d();
        rotation.set(new AxisAngle4d(rotationAxis.x, rotationAxis.y, rotationAxis.z, rotationAngle));
    }

    public Camera() {
        this(new Vector3d(), new Vector3d(0, 0, -1), 1600, 1000);
    }

    public void setPixel(final int x, final int y, final Color color) {
        image.setRGB(x, y, color.getRGB());
    }

    public BufferedImage getImage() {
        final BufferedImage imageCopy = new BufferedImage(screenWidth, screenHeight, BufferedImage.TYPE_INT_RGB);
        final Graphics g = imageCopy.getGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();

        return imageCopy;
    }

    public void createImage(final BoundingVolume[] objects, final LightingModel lightingModel, final Light light) {
        final double xStart = -(viewportWidth / 2.0);
        final double yStart = viewportHeight / 2.0;
        final double xInc = viewportWidth / screenWidth;
        final double yInc = viewportHeight / screenHeight;

        final long startTime = System.nanoTime();
        long innerStart = 0, innerEnd = 0;

        for (int x = 0; x < screenWidth; x++) {
            if (x % 100 == 0) {
                innerStart = System.nanoTime();
                System.out.print("lines " + x + " - " + (x + 99) + ": ");
            }

            for (int y = 0; y < screenHeight; y++) {
                final Vector3d viewportDirection = new Vector3d(xStart + xInc * x, yStart - yInc * y, nearPlaneDistance);
                rotation.transform(viewportDirection);
                viewportDirection.normalize();
                final Ray ray = new Ray(origin, viewportDirection);
                IntersectionInformation closest = null;
                IntersectionInformation temp = null;

                for (final BoundingVolume object : objects) {
                    temp = object.getChildIntersection(ray);

                    if (temp != null) {
                        closest = closest == null ? temp : closest.w <= temp.w ? closest : temp;
                    }
                }

                if (closest != null) {
                    setPixel(x, y, lightingModel.getPixelColor(closest));
                } else {
                    setPixel(x, y, light.ambient);
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
                ImageIO.write(image, split[split.length - 1], output);
                System.out.println("Image saved to " + outputFile + " successfully");
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
}
