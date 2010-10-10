package stephen.ranger.ar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.vecmath.Vector3f;

import stephen.ranger.ar.bounds.BoundingVolume;
import stephen.ranger.ar.lighting.Light;
import stephen.ranger.ar.lighting.LightingModel;
import stephen.ranger.ar.lighting.PhongLightingModel;
import stephen.ranger.ar.sceneObjects.TriangleMesh;

public class RayTracer {
   public static String baseDir = "C:/Users/sano/Documents/models/";

   public final Light light = new Light(new Vector3f(0, 100, 100), new Color(0.3f, 0.3f, 0.3f, 1.0f), new Color(0.5f, 0.5f, 0.9f, 1.0f));
   public final Camera camera = new Camera(new Vector3f(-0.02f, 0.1f, 0.2f), new Vector3f(0, 0, 0), 1600, 1000); // bunny
   // public final Camera camera = new Camera(new Vector3f(0f, 0.12f, -0.2f), new Vector3f(180, 0, 0), 1600, 1000); // dragon

   public final LightingModel lightingModel;

   public final boolean isSupersamplingEnabled;
   public final boolean isWeighted;

   public BoundingVolume[] objects;

   public RayTracer(final boolean isSupersamplingEnabled, final boolean isWeighted, final String outputFile) {
      this.isSupersamplingEnabled = isSupersamplingEnabled;
      this.isWeighted = isWeighted;

      objects = new BoundingVolume[] { new TriangleMesh(new File(baseDir + "bunny/reconstruction/bun_zipper.ply"), new ColorInformation(Color.white)).boundingVolume };
      // this.objects = new BoundingVolume[] { new TriangleMesh(new File(baseDir + "dragon_recon/dragon_vrip.ply"), new ColorInformation(Color.white)).boundingVolume };
      // this.objects = new BoundingVolume[] { new TriangleMesh(new File(baseDir + "xyzrgb_dragon.ply"), new ColorInformation(Color.white)).boundingVolume };
      // this.objects = new BoundingVolume[] { new TriangleMesh(new File(baseDir + "happy_recon/happy_vrip.ply"), new ColorInformation(Color.white)).boundingVolume };

      lightingModel = new PhongLightingModel(camera, light, objects);
      // lightingModel = new LightingModel();

      camera.createImage(objects, lightingModel, light);

      if (outputFile != null) {
         camera.writeOutputFile(outputFile);
      }

      final ImageIcon image = new ImageIcon(camera.getImage());

      final JLabel label = new JLabel();
      label.setSize(image.getIconWidth(), image.getIconHeight());
      label.setIcon(image);

      final JPanel panel = new JPanel();
      panel.setSize(label.getSize());
      panel.add(label, BorderLayout.CENTER);

      final JFrame frame = new JFrame();
      frame.getContentPane().add(panel, BorderLayout.CENTER);
      frame.pack();
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.setVisible(true);
   }

   public static void main(final String[] args) {
      new RayTracer(false, false, "output.png");
   }
}
