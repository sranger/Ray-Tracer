package stephen.ranger.ar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.vecmath.Vector3f;

import stephen.ranger.ar.bounds.BoundingVolume;
import stephen.ranger.ar.lighting.Light;
import stephen.ranger.ar.lighting.PhongLightingModel;
import stephen.ranger.ar.sceneObjects.Plane;
import stephen.ranger.ar.sceneObjects.Sphere;
import stephen.ranger.ar.sceneObjects.TriangleMesh;

public class RayTracer {
   public static final String baseDir = "C:/Users/sano/Documents/";
   //   public static final String baseDir = "D:/";
   public static final String[] sceneLabels = new String[] { "Whitted Scene", "Whitted Scene (BRDF)", "Stanford Bunny", "Stanford Dragon", "Stanford Buddha", "Stanford Lucy", "XYZ RGB Dragon",
         "XYZ RGB Thai Statue", "Cornell Box", "Cornell Box (spheres)" };
   public static BoundingVolume[][] sceneVolumes = new BoundingVolume[sceneLabels.length][];

   private Scene currentScene = null;

   private Camera camera = null;

   public RayTracer() {
      initializeUI();
   }

   public static void main(final String[] args) {
      new RayTracer();
   }

   private final Scene getScene(final String label, final boolean useKDTree) {
      System.out.println("\n\n----------------------------------------\nRendering " + label + "\n----------------------------------------\n\n");

      final Light light = new Light(new Vector3f(0, 100, 100), new Color(0.3f, 0.3f, 0.3f, 1.0f), new Color(0.5f, 0.5f, 0.9f, 1.0f));
      final Light light2 = new Light(new Vector3f(0, 100, -100), new Color(0.3f, 0.3f, 0.3f, 1.0f), new Color(0.5f, 0.5f, 0.9f, 1.0f));
      // final Light cornellLight = new Light(new Vector3f(278, 540, 0.5f), new Color(1f, 0.85f, 0.43f), new Color(1f, 0.85f, 0.43f));
      final Light cornellLight = new Light(new Vector3f(0, 0, -300), new Color(0.75f, 0.75f, 0.75f), new Color(0.75f, 0.75f, 0.75f));

      if (label.equals(sceneLabels[0])) {
         if (sceneVolumes[0] == null) {
            sceneVolumes[0] = getWhittedObjects(false);
         }

         return new Scene(sceneLabels[0], sceneVolumes[0], light, new float[] { 0, 0, 0 }, new PhongLightingModel(light, sceneVolumes[0]), 35f);
      } else if (label.equals(sceneLabels[1])) {
         if (sceneVolumes[1] == null) {
            sceneVolumes[1] = getWhittedObjects(true);
         }

         return new Scene(sceneLabels[1], sceneVolumes[1], light, new float[] { 0, 0, 0 }, new PhongLightingModel(light, sceneVolumes[1]), 35f);
      } else if (label.equals(sceneLabels[2])) {
         if (sceneVolumes[2] == null) {
            sceneVolumes[2] = new BoundingVolume[] { new TriangleMesh(new File(baseDir + "models/bunny/reconstruction/bun_zipper.ply"), new ColorInformation(Color.white), useKDTree).boundingVolume };
         }

         return new Scene(sceneLabels[2], sceneVolumes[2], light, new float[] { 0, 0, 0 }, new PhongLightingModel(light, sceneVolumes[2]), 15f);
      } else if (label.equals(sceneLabels[3])) {
         if (sceneVolumes[3] == null) {
            sceneVolumes[3] = new BoundingVolume[] { new TriangleMesh(new File(baseDir + "models/dragon_recon/dragon_vrip.ply"), new ColorInformation(new Color(0.9f, 0.9f, 0.9f, 1f)), useKDTree).boundingVolume };
         }

         return new Scene(sceneLabels[3], sceneVolumes[3], light, new float[] { 0, 0, 0 }, new PhongLightingModel(light, sceneVolumes[3]), 23f);
      } else if (label.equals(sceneLabels[4])) {
         if (sceneVolumes[4] == null) {
            sceneVolumes[4] = new BoundingVolume[] { new TriangleMesh(new File(baseDir + "models/happy_recon/happy_vrip.ply"), new ColorInformation(Color.white), useKDTree).boundingVolume };
         }

         return new Scene(sceneLabels[4], sceneVolumes[4], light, new float[] { 180, 0, 0 }, new PhongLightingModel(light, sceneVolumes[4]), 10f);
      } else if (label.equals(sceneLabels[5])) {
         if (sceneVolumes[5] == null) {
            sceneVolumes[5] = new BoundingVolume[] { new TriangleMesh(new File(baseDir + "models/lucy.ply"), new ColorInformation(Color.white), useKDTree).boundingVolume };
         }

         return new Scene(sceneLabels[5], sceneVolumes[5], light, new float[] { 180, 0, 0 }, new PhongLightingModel(light, sceneVolumes[5]), 10f);
      } else if (label.equals(sceneLabels[6])) {
         if (sceneVolumes[6] == null) {
            sceneVolumes[6] = new BoundingVolume[] { new TriangleMesh(new File(baseDir + "models/xyzrgb_dragon.ply"), new ColorInformation(Color.white), useKDTree).boundingVolume };
         }

         return new Scene(sceneLabels[6], sceneVolumes[6], light2, new float[] { 220, 0, 0 }, new PhongLightingModel(light2, sceneVolumes[6]), 20f);
      } else if (label.equals(sceneLabels[7])) {
         if (sceneVolumes[7] == null) {
            sceneVolumes[7] = new BoundingVolume[] { new TriangleMesh(new File(baseDir + "models/xyzrgb_statuette.ply"), new ColorInformation(Color.white), useKDTree).boundingVolume };
         }

         return new Scene(sceneLabels[7], sceneVolumes[7], light, new float[] { 0, 0, 0 }, new PhongLightingModel(light, sceneVolumes[7]), 10f);
      } else if (label.equals(sceneLabels[8])) {
         if (sceneVolumes[8] == null) {
            sceneVolumes[8] = getCornellBox(false);
         }

         return new Scene(sceneLabels[8], sceneVolumes[8], cornellLight, new float[] { 180, 0, 0 }, new PhongLightingModel(cornellLight, sceneVolumes[8]), 20f);
      } else if (label.equals(sceneLabels[9])) {
         if (sceneVolumes[9] == null) {
            sceneVolumes[9] = getCornellBox(true);
         }

         return new Scene(sceneLabels[9], sceneVolumes[9], cornellLight, new float[] { 180, 0, 0 }, new PhongLightingModel(cornellLight, sceneVolumes[9]), 20f);
      }

      return null;
   }

   private final BoundingVolume[] getWhittedObjects(final boolean useBRDFs) {
      final Plane plane = new Plane(new Vector3f[] { new Vector3f(-50, 0, -100), new Vector3f(-50, -40, 25), new Vector3f(50, -40, 25), new Vector3f(50, 0, -100) },
            new ColorInformation(Color.yellow), true);

      final Sphere sphere1 = new Sphere(5, new Vector3f(0, -12, 0), useBRDFs ? new BRDFMaterial(1, Color.white) : new ColorInformation(Color.blue));
      final Sphere sphere2 = new Sphere(3, new Vector3f(5, -15, -10), useBRDFs ? new BRDFMaterial(16, Color.cyan) : new ColorInformation(Color.white, true));

      return new BoundingVolume[] { plane.getBoundingVolume(), sphere1.getBoundingVolume(), sphere2.getBoundingVolume() };

   }

   /**
    * http://www.cs.uiowa.edu/~cwyman/classes/spring07-22C251/code/cornellBoxScene.txt
    * 
    * http://www.graphics.cornell.edu/online/box/data.html
    * 
    * @return
    */
   private final BoundingVolume[] getCornellBox(final boolean useSpheres) {
      // final ColorInformation white = new ColorInformation(new Color(0.76f, 0.75f, 0.5f), 0);
      final ColorInformation white = new ColorInformation(new Color(0.75f, 0.75f, 0.75f), 0);
      final ColorInformation white2 = new ColorInformation(new Color(0.65f, 0.65f, 0.65f), 0);
      final ColorInformation red = new ColorInformation(new Color(0.63f, 0.06f, 0.04f), 0);
      final ColorInformation green = new ColorInformation(new Color(0.15f, 0.48f, 0.09f), 0);
      final ColorInformation blue = new ColorInformation(new Color(0.392f, 0.584f, 0.93f), 0);

      final float[][] box = new float[][] { { -278, -275, -280 }, { 278, 275, 280 } };

      final Plane floor = new Plane(new Vector3f[] { new Vector3f(box[1][0], box[0][1], box[0][2]), new Vector3f(box[0][0], box[0][1], box[0][2]), new Vector3f(box[0][0], box[0][1], box[1][2]),
            new Vector3f(box[1][0], box[0][1], box[1][2]) }, white, false);
      final Plane ceiling = new Plane(new Vector3f[] { new Vector3f(box[1][0], box[1][1], box[0][2]), new Vector3f(box[1][0], box[1][1], box[1][2]), new Vector3f(box[0][0], box[1][1], box[1][2]),
            new Vector3f(box[0][0], box[1][1], box[0][2]) }, white, false);
      final Plane back = new Plane(new Vector3f[] { new Vector3f(box[1][0], box[0][1], box[1][2]), new Vector3f(box[0][0], box[0][1], box[1][2]), new Vector3f(box[0][0], box[1][1], box[1][2]),
            new Vector3f(box[1][0], box[1][1], box[1][2]) }, white, false);
      final Plane left = new Plane(new Vector3f[] { new Vector3f(box[1][0], box[0][1], box[0][2]), new Vector3f(box[1][0], box[0][1], box[1][2]), new Vector3f(box[1][0], box[1][1], box[1][2]),
            new Vector3f(box[1][0], box[1][1], box[0][2]) }, red, false);
      final Plane right = new Plane(new Vector3f[] { new Vector3f(box[0][0], box[0][1], box[1][2]), new Vector3f(box[0][0], box[0][1], box[0][2]), new Vector3f(box[0][0], box[1][1], box[0][2]),
            new Vector3f(box[0][0], box[1][1], box[1][2]) }, green, false);

      if (useSpheres) {
         final Sphere s1 = new Sphere(82.5f, new Vector3f(-92f, -192.5f, -111.5f), blue);
         final Sphere s2 = new Sphere(82.5f, new Vector3f(116.5f, -192.5f, 71.5f), blue);

         return new BoundingVolume[] { floor.boundingVolume, ceiling.boundingVolume, back.boundingVolume, left.boundingVolume, right.boundingVolume, s1.boundingVolume, s2.boundingVolume };
      } else {
         final Plane small1 = new Plane(new Vector3f[] { new Vector3f(-148, -110, -215), new Vector3f(-196, -110, -55), new Vector3f(-32, -110, -8), new Vector3f(12, -110, -166) }, white2, false);
         final Plane small2 = new Plane(new Vector3f[] { new Vector3f(12, -275, -166), new Vector3f(12, -110, -166), new Vector3f(-32, -110, -8), new Vector3f(-32, -275, -8) }, white2, false);
         final Plane small3 = new Plane(new Vector3f[] { new Vector3f(-148, -275, -215), new Vector3f(-148, -110, -215), new Vector3f(12, -110, -166), new Vector3f(12, -275, -166) }, white2, false);
         final Plane small4 = new Plane(new Vector3f[] { new Vector3f(-196, -275, -55), new Vector3f(-196, -110, -55), new Vector3f(-148, -110, -215), new Vector3f(-148, -275, -215) }, white2, false);
         final Plane small5 = new Plane(new Vector3f[] { new Vector3f(-32, -275, -8), new Vector3f(-32, -110, -8), new Vector3f(-196, -110, -55), new Vector3f(-196, -275, -55) }, white2, false);

         final Plane large1 = new Plane(new Vector3f[] { new Vector3f(145, 55, -33), new Vector3f(-13, 55, 16), new Vector3f(36, 55, 176), new Vector3f(194, 55, 126) }, white2, false);
         final Plane large2 = new Plane(new Vector3f[] { new Vector3f(145, -275, -33), new Vector3f(145, 55, -33), new Vector3f(194, 55, 126), new Vector3f(194, -275, 126) }, white2, false);
         final Plane large3 = new Plane(new Vector3f[] { new Vector3f(194, -275, 126), new Vector3f(194, 55, 126), new Vector3f(36, 55, 176), new Vector3f(36, -275, 176) }, white2, false);
         final Plane large4 = new Plane(new Vector3f[] { new Vector3f(36, -275, 176), new Vector3f(36, 55, 176), new Vector3f(-13, 55, 16), new Vector3f(-13, -275, 16) }, white2, false);
         final Plane large5 = new Plane(new Vector3f[] { new Vector3f(-13, -275, 16), new Vector3f(-13, 55, 16), new Vector3f(145, 55, -33), new Vector3f(145, -275, -33) }, white2, false);

         return new BoundingVolume[] { floor.boundingVolume, ceiling.boundingVolume, back.boundingVolume, left.boundingVolume, right.boundingVolume, small1.boundingVolume, small2.boundingVolume,
               small3.boundingVolume, small4.boundingVolume, small5.boundingVolume, large1.boundingVolume, large2.boundingVolume, large3.boundingVolume, large4.boundingVolume, large5.boundingVolume };
      }
   }

   private void initializeUI() {
      final JFrame frame = new JFrame("Ray-Tracer");

      final JPanel sidePanel = new JPanel();
      final JPanel imagePanel = new JPanel();
      imagePanel.setLayout(new GridLayout(1, 1));

      final JLabel multiSamplesLabel = new JLabel("Multi-Samples");
      final JLabel brdfSamplesLabel = new JLabel("BRDF-Samples");
      final JLabel sceneLabel = new JLabel("Scene");
      final JLabel imageWidthLabel = new JLabel("Image Width");
      final JLabel imageHeightLabel = new JLabel("Image Height");

      final JSpinner multiSamplesField = new JSpinner(new SpinnerNumberModel(1, 1, 400, 1));
      final JSpinner brdfSamplesField = new JSpinner(new SpinnerNumberModel(1, 1, 205, 1));
      final JSpinner imageXField = new JSpinner(new SpinnerNumberModel(512, 1, 10240, 1));
      final JSpinner imageYField = new JSpinner(new SpinnerNumberModel(512, 1, 10240, 1));

      final JComboBox sceneComboBox = new JComboBox(sceneLabels);
      sceneComboBox.setPreferredSize(new Dimension(150, 50));
      sceneComboBox.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(final ActionEvent event) {
            if (sceneComboBox.getSelectedIndex() == 1) {
               brdfSamplesField.setValue(Integer.valueOf(25));
            }
         }
      });

      final JLabel iconLabel = new JLabel() {
         @Override
         public void paint(final Graphics g) {
            if (camera != null) {
               final BufferedImage image = camera.getImage();
               g.drawImage(image, 0, 0, null);
               g.finalize();
            }
         }
      };

      final JScrollPane imagePane = new JScrollPane(iconLabel);
      imagePane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
      imagePane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
      imagePane.setPreferredSize(new Dimension(530, 530));

      final JButton renderButton = new JButton("Render Scene");
      final JButton closeButton = new JButton("Close");

      final JCheckBox useKDTreeCheckBox = new JCheckBox("Enable KD-Tree");
      useKDTreeCheckBox.setSelected(true);
      useKDTreeCheckBox.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(final ActionEvent event) {
            sceneVolumes = new BoundingVolume[8][];
         }
      });

      renderButton.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(final ActionEvent event) {
            sceneComboBox.setEnabled(false);
            imageXField.setEnabled(false);
            imageYField.setEnabled(false);
            multiSamplesField.setEnabled(false);
            brdfSamplesField.setEnabled(false);
            renderButton.setEnabled(false);
            closeButton.setEnabled(false);

            final ActionListener listener = new ActionListener() {
               @Override
               public void actionPerformed(final ActionEvent event) {
                  camera = new Camera(currentScene, (Integer) multiSamplesField.getValue(), (Integer) brdfSamplesField.getValue(), RTStatics.nearPlane, (Integer) imageXField.getValue(),
                        (Integer) imageYField.getValue());
                  camera.addActionListener(new ActionListener() {
                     @Override
                     public void actionPerformed(final ActionEvent event) {
                        if (event.getID() == 1) {
                           sceneComboBox.setEnabled(true);
                           imageXField.setEnabled(true);
                           imageYField.setEnabled(true);
                           multiSamplesField.setEnabled(true);
                           brdfSamplesField.setEnabled(true);
                           renderButton.setEnabled(true);
                           closeButton.setEnabled(true);
                           iconLabel.revalidate();

                           imagePane.repaint();
                        } else if (event.getID() == 2) {
                           final BufferedImage image = camera.getImage();
                           iconLabel.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
                           iconLabel.revalidate();

                           imagePane.repaint();
                        }
                     }
                  });

                  camera.createImage();
               }
            };

            new Thread() {
               @Override
               public void run() {
                  currentScene = RayTracer.this.getScene(sceneComboBox.getSelectedItem().toString(), useKDTreeCheckBox.isSelected());
                  listener.actionPerformed(new ActionEvent(this, 0, "finished"));
               }
            }.start();
         }
      });

      closeButton.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(final ActionEvent event) {
            System.exit(0);
         }
      });

      sidePanel.setLayout(new GridLayout(6, 2));
      sidePanel.add(sceneLabel);
      sidePanel.add(sceneComboBox);
      sidePanel.add(imageWidthLabel);
      sidePanel.add(imageXField);
      sidePanel.add(imageHeightLabel);
      sidePanel.add(imageYField);
      sidePanel.add(multiSamplesLabel);
      sidePanel.add(multiSamplesField);
      sidePanel.add(brdfSamplesLabel);
      sidePanel.add(brdfSamplesField);
      sidePanel.add(renderButton);
      sidePanel.add(closeButton);

      sidePanel.setMaximumSize(new Dimension(250, 250));
      sidePanel.setAlignmentX(JDialog.CENTER_ALIGNMENT);

      imagePanel.add(imagePane);

      final JButton saveButton = new JButton("Save Image to File...");
      saveButton.setAlignmentX(JDialog.CENTER_ALIGNMENT);
      saveButton.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(final ActionEvent event) {
            final JFileChooser fileChooser = new JFileChooser();
            final int result = fileChooser.showSaveDialog(frame);

            if (result == JFileChooser.APPROVE_OPTION) {
               final String path = fileChooser.getSelectedFile().getAbsolutePath();

               camera.writeOutputFile(path.endsWith(".png") || path.endsWith(".jpg") ? path : path + ".png");
            }
         }
      });

      final JTextArea textArea = new JTextArea();
      textArea.setWrapStyleWord(true);
      textArea.setLineWrap(true);

      final JScrollPane scrollPane = new JScrollPane(textArea);
      scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
      scrollPane.setPreferredSize(new Dimension(700, 250));

      final OutputStream textAreaPrintStream = new OutputStream() {
         @Override
         public void write(final int b) throws IOException {
            textArea.append(Character.valueOf((char) b).toString());
            textArea.setCaretPosition(textArea.getText().length());
            scrollPane.repaint();
         }
      };

      System.setOut(new PrintStream(textAreaPrintStream));

      final JPanel sideParentPanel = new JPanel();
      sideParentPanel.setLayout(new BoxLayout(sideParentPanel, BoxLayout.PAGE_AXIS));
      sideParentPanel.add(sidePanel);
      sideParentPanel.add(Box.createRigidArea(new Dimension(0, 25)));
      sideParentPanel.add(saveButton);
      sideParentPanel.add(Box.createRigidArea(new Dimension(0, 25)));
      sideParentPanel.add(useKDTreeCheckBox);

      frame.getContentPane().setLayout(new BorderLayout());
      frame.getContentPane().add(sideParentPanel, BorderLayout.WEST);
      frame.getContentPane().add(imagePanel, BorderLayout.CENTER);
      frame.getContentPane().add(scrollPane, BorderLayout.SOUTH);

      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.pack();
      frame.setVisible(true);
   }
}
