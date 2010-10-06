package stephen.ranger.ar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.vecmath.Vector3d;

import stephen.ranger.ar.bounds.BoundingVolume;
import stephen.ranger.ar.lighting.Light;
import stephen.ranger.ar.lighting.LightingModel;
import stephen.ranger.ar.lighting.PhongLightingModel;
import stephen.ranger.ar.sceneObjects.TriangleMesh;

public class RayTracer {
   public final Light light = new Light(new Vector3d(0, 100, -100), new Color(0.3f, 0.3f, 0.3f, 1.0f), new Color(0.5f,
         0.5f, 0.9f, 1.0f));
   // public final Camera camera = new Camera(new Vector3d(0, 0.08, -0.2), new Vector3d(0, 0, 1), 1600, 1000); // bunny
   public final Camera camera = new Camera(new Vector3d(0, 0.12, -0.2), new Vector3d(0, 0, 1), 400, 250); // dragon
   //    public final Camera camera = new Camera(new Vector3d(-4.5, 0, 3), new Vector3d(0, 0, 1), 1600, 1000); // tris

   public final LightingModel lightingModel;

   public final boolean isSupersamplingEnabled;
   public final boolean isWeighted;

   public final BoundingVolume[] objects;

   public RayTracer(final boolean isSupersamplingEnabled, final boolean isWeighted, final String outputFile) {
      this.isSupersamplingEnabled = isSupersamplingEnabled;
      this.isWeighted = isWeighted;

      //        final Plane plane = new Plane(new Vector3d[] { new Vector3d(-300, -500, -100), new Vector3d(-300, 500, -100), new Vector3d(300, 500, -100),
      //                new Vector3d(300, -500, -100) }, new ColorInformation(Color.yellow));
      //
      //        final Plane plane = new Plane(new Vector3d[] { new Vector3d(-100, -50, 100), new Vector3d(-100, -50, 1100), new Vector3d(600, -50, 1100),
      //                new Vector3d(600, -50, 100) }, new ColorInformation(Color.yellow));
      //
      //        final Sphere sphere1 = new Sphere(6, new Vector3d(0, 0, 45), new ColorInformation(Color.blue, new Color(0.2f, 0.2f, 0.2f, 1.0f), new Color(0.8f, 0.8f,
      //                0.8f, 1.0f), new Color(0.8f, 0.8f, 0.8f, 1.0f), 10, false), this);
      //
      //        final Sphere sphere2 = new Sphere(7, new Vector3d(10, -10, 60), new ColorInformation(new Color(0.5f, 0.5f, 0.5f, 1f),
      //                new Color(0.2f, 0.2f, 0.2f, 1.0f), new Color(0.8f, 0.8f, 0.8f, 1.0f), new Color(0.8f, 0.8f, 0.8f, 1.0f), 200, true), this);
      //
      //        objects = new BoundingVolume[] { plane.getBoundingVolume(), sphere1.getBoundingVolume(), sphere2.getBoundingVolume() };

      // this.objects = new BoundingVolume[] { new TriangleMesh(new File("D:/bun_zipper_normals_v3.ply"), new
      // ColorInformation(Color.white)).boundingVolume };
      //        objects = new BoundingVolume[] { new TriangleMesh(new File("C:/Users/sano/Documents/models/dragon_recon/dragon_vrip_res2.ply"), new ColorInformation(Color.white)).boundingVolume };
      //        objects = new BoundingVolume[] { new TriangleMesh(new File("C:/Users/sano/Documents/models/tris.ply"), new ColorInformation(Color.yellow)).boundingVolume };
      this.objects = new BoundingVolume[] { new TriangleMesh(new File(
      "C:/Users/rangers.ROME-RD/Downloads/dragon_recon.tar/dragon_recon/dragon_vrip.ply"), new ColorInformation(
            Color.white)).boundingVolume };

      this.lightingModel = new PhongLightingModel(this.camera, this.light, this.objects);
      // lightingModel = new LightingModel();

      this.camera.createImage(this.objects, this.lightingModel, this.light);

      if (outputFile != null) {
         this.camera.writeOutputFile(outputFile);
      }

      final ImageIcon image = new ImageIcon(this.camera.getImage());

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
