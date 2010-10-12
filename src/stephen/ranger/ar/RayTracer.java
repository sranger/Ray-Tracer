package stephen.ranger.ar;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.vecmath.Vector3f;

import stephen.ranger.ar.bounds.BoundingVolume;
import stephen.ranger.ar.lighting.Light;
import stephen.ranger.ar.lighting.PhongLightingModel;
import stephen.ranger.ar.sceneObjects.Plane;
import stephen.ranger.ar.sceneObjects.Sphere;
import stephen.ranger.ar.sceneObjects.TriangleMesh;

public class RayTracer {
   public static final String baseDir = "C:/Users/sano/Documents/models/";
   public static final String[] sceneLabels = new String[] { "Whitted Scene", "Stanford Bunny", "Stanford Dragon", "Stanford Buddha" };

   private Camera camera = null;

   public RayTracer() {
      initializeUI();
   }

   public static void main(final String[] args) {
      new RayTracer();
   }

   private final Scene getScene(final String label) {
      final Light light = new Light(new Vector3f(0, 100, 100), new Color(0.3f, 0.3f, 0.3f, 1.0f), new Color(0.5f, 0.5f, 0.9f, 1.0f));

      if (label.equals(sceneLabels[0])) {
         final BoundingVolume[] whittedObjects = getWhittedObjects();
         return new Scene(sceneLabels[0], whittedObjects, light, new float[] { 0, 0, 0 }, new float[] { 0, 0, 0 }, new PhongLightingModel(new Vector3f(), light, whittedObjects));
      } else if (label.equals(sceneLabels[1])) {
         final BoundingVolume[] bunnyModel = new BoundingVolume[] { new TriangleMesh(new File(baseDir + "bunny/reconstruction/bun_zipper.ply"), new ColorInformation(Color.white)).boundingVolume };
         return new Scene(sceneLabels[1], bunnyModel, light, new float[] { -0.02f, 0.1f, 0.2f }, new float[] { 0, 0, 0 }, new PhongLightingModel(new Vector3f(-0.02f, 0.1f, 0.2f), light, bunnyModel));
      } else if (label.equals(sceneLabels[2])) {
         final BoundingVolume[] dragonModel = new BoundingVolume[] { new TriangleMesh(new File(baseDir + "dragon_recon/dragon_vrip.ply"), new ColorInformation(Color.white)).boundingVolume };
         return new Scene(sceneLabels[2], dragonModel, light, new float[] { 0f, 0.12f, -0.2f }, new float[] { 180, 0, 0 }, new PhongLightingModel(new Vector3f(0f, 0.12f, -0.2f), light, dragonModel));
      } else if (label.equals(sceneLabels[3])) {
         final BoundingVolume[] buddhaModel = new BoundingVolume[] { new TriangleMesh(new File(baseDir + "happy_recon/happy_vrip.ply"), new ColorInformation(Color.white)).boundingVolume };
         return new Scene(sceneLabels[3], buddhaModel, light, new float[] { 0f, 0.12f, -0.2f }, new float[] { 180, 0, 0 }, new PhongLightingModel(new Vector3f(0f, 0.12f, -0.2f), light, buddhaModel));
      }

      return null;
   }

   private final BoundingVolume[] getWhittedObjects() {
      final Plane plane = new Plane(new Vector3f[] { new Vector3f(-100, -50, 100), new Vector3f(-100, -50, 1100), new Vector3f(600, -50, 1100), new Vector3f(600, -50, 100) }, new ColorInformation(
            Color.yellow));

      final Sphere sphere1 = new Sphere(6, new Vector3f(0, 0, 45), new ColorInformation(Color.blue));
      final Sphere sphere2 = new Sphere(7, new Vector3f(10, -10, 60), new ColorInformation(new Color(0.5f, 0.5f, 0.5f, 1f)));

      return new BoundingVolume[] { plane.getBoundingVolume(), sphere1.getBoundingVolume(), sphere2.getBoundingVolume() };

   }

   private void initializeUI() {
      final JFrame frame = new JFrame("Ray-Tracer");

      final JPanel sidePanel = new JPanel();
      final JPanel imagePanel = new JPanel();

      final JLabel samplesLabel = new JLabel("Samples");
      final JLabel sceneLabel = new JLabel("Scene");
      final JLabel imageWidthLabel = new JLabel("Image Width");
      final JLabel imageHeightLabel = new JLabel("Image Height");

      final JSpinner samplesField = new JSpinner(new SpinnerNumberModel(1, 1, 400, 1));
      final JSpinner imageXField = new JSpinner(new SpinnerNumberModel(512, 1, 3000, 1));
      final JSpinner imageYField = new JSpinner(new SpinnerNumberModel(512, 1, 3000, 1));

      final JComboBox sceneComboBox = new JComboBox(sceneLabels);
      sceneComboBox.setPreferredSize(new Dimension(150, 50));

      final ImageIcon icon = new ImageIcon();
      final JLabel iconLabel = new JLabel();
      iconLabel.setIcon(icon);
      final JScrollPane imagePane = new JScrollPane(iconLabel);
      imagePane.setPreferredSize(new Dimension(550, 550));

      final JButton renderButton = new JButton("Render Scene");
      final JButton closeButton = new JButton("Close");

      renderButton.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(final ActionEvent event) {
            sceneComboBox.setEnabled(false);
            imageXField.setEnabled(false);
            imageYField.setEnabled(false);
            samplesField.setEnabled(false);
            renderButton.setEnabled(false);
            closeButton.setEnabled(false);

            final Scene scene = getScene(sceneComboBox.getSelectedItem().toString());
            camera = new Camera(scene.objects, scene.lightingModel, scene.light, (Integer) samplesField.getValue(), RTStatics.nearPlane, (Integer) imageXField.getValue(), (Integer) imageYField
                  .getValue());
            icon.setImage(camera.getImage());
            camera.addActionListener(new ActionListener() {
               @Override
               public void actionPerformed(final ActionEvent event) {
                  if (event.getID() == 1) {
                     System.out.println("finished");
                     sceneComboBox.setEnabled(true);
                     imageXField.setEnabled(true);
                     imageYField.setEnabled(true);
                     samplesField.setEnabled(true);
                     renderButton.setEnabled(true);
                     closeButton.setEnabled(true);
                     imagePane.invalidate();
                     imagePane.repaint();
                  } else if (event.getID() == 2) {
                     imagePane.invalidate();
                     imagePane.repaint();
                  }
               }
            });

            camera.createImage();
         }
      });

      sidePanel.setLayout(new GridLayout(5, 2));
      sidePanel.add(sceneLabel);
      sidePanel.add(sceneComboBox);
      sidePanel.add(imageWidthLabel);
      sidePanel.add(imageXField);
      sidePanel.add(imageHeightLabel);
      sidePanel.add(imageYField);
      sidePanel.add(samplesLabel);
      sidePanel.add(samplesField);
      sidePanel.add(renderButton);
      sidePanel.add(closeButton);

      imagePanel.add(imagePane);

      frame.getContentPane().setLayout(new FlowLayout());
      frame.getContentPane().add(sidePanel);
      frame.getContentPane().add(imagePanel);

      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.pack();
      frame.setVisible(true);
   }
}
