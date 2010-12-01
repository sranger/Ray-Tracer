package stephen.ranger.ar;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import stephen.ranger.ar.RayTracer.Scenes;

public class RayTracerInterface extends JFrame {
   public RayTracerInterface(final RayTracer rayTracer, final int width, final int height, final int x, final int y, final int imageWidth, final int imageHeight) {
      this.setTitle("Ray-Tracer");
      this.setLocation(x, y);

      final JPanel sidePanel = new JPanel();
      sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.PAGE_AXIS));
      sidePanel.setPreferredSize(new Dimension(300, 600));
      final JPanel imagePanel = new JPanel();
      imagePanel.setLayout(new GridLayout(1, 1));

      final JLabel multiSamplesLabel = new JLabel("Multi-Samples");
      final JLabel brdfSamplesLabel = new JLabel("BRDF-Samples");
      final JLabel sceneLabel = new JLabel("Scene");
      final JLabel imageWidthLabel = new JLabel("Image Width");
      final JLabel imageHeightLabel = new JLabel("Image Height");

      final JSpinner multiSamplesField = new JSpinner(new SpinnerNumberModel(1, 1, 400, 1));
      multiSamplesField.setMaximumSize(new Dimension(200, 20));
      final JSpinner brdfSamplesField = new JSpinner(new SpinnerNumberModel(1, 1, 205, 1));
      brdfSamplesField.setMaximumSize(new Dimension(200, 20));
      final JSpinner imageXField = new JSpinner(new SpinnerNumberModel(imageWidth, 1, 10240, 128));
      imageXField.setMaximumSize(new Dimension(200, 20));
      final JSpinner imageYField = new JSpinner(new SpinnerNumberModel(imageHeight, 1, 10240, 128));
      imageYField.setMaximumSize(new Dimension(200, 20));

      final JList sceneList = new JList(RayTracer.Scenes.values());
      sceneList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      sceneList.setSelectedIndex(0);
      // sceneList.setVisibleRowCount(4);
      sceneList.addListSelectionListener(new ListSelectionListener() {
         @Override
         public void valueChanged(final ListSelectionEvent event) {
            if (sceneList.getSelectedIndex() == 1) {
               brdfSamplesField.setValue(Integer.valueOf(25));
            }
         }
      });

      final JScrollPane sceneScrollPane = new JScrollPane(sceneList);
      sceneScrollPane.setMaximumSize(new Dimension(200, 150));

      final JCheckBox useNormalizedImageCheckBox = new JCheckBox("Display Normalized Image");
      useNormalizedImageCheckBox.setSelected(false);
      useNormalizedImageCheckBox.setPreferredSize(new Dimension(200, 20));

      final JLabel iconLabel = new JLabel() {
         @Override
         public void paint(final Graphics g) {
            if (rayTracer.camera != null) {
               final BufferedImage image = useNormalizedImageCheckBox.isSelected() ? rayTracer.camera.getNormalizedImage() : rayTracer.camera.getImage();
               g.drawImage(image, 0, 0, null);
               g.finalize();
            }
         }
      };

      useNormalizedImageCheckBox.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(final ActionEvent event) {
            iconLabel.repaint();
         }
      });

      final JScrollPane imagePane = new JScrollPane(iconLabel);
      imagePane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
      imagePane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
      imagePane.setPreferredSize(new Dimension(imageWidth, imageHeight));

      final JButton renderButton = new JButton("Render Scene");
      final JButton closeButton = new JButton("Close");

      final JCheckBox useKDTreeCheckBox = new JCheckBox("Enable KD-Tree");
      useKDTreeCheckBox.setSelected(true);
      useKDTreeCheckBox.setPreferredSize(new Dimension(200, 20));
      useKDTreeCheckBox.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(final ActionEvent event) {
            RayTracer.Scenes.resetKDTreeMeshes();
         }
      });

      renderButton.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(final ActionEvent event) {
            sceneScrollPane.setEnabled(false);
            imageXField.setEnabled(false);
            imageYField.setEnabled(false);
            multiSamplesField.setEnabled(false);
            brdfSamplesField.setEnabled(false);
            renderButton.setEnabled(false);
            closeButton.setEnabled(false);

            final ActionListener listener = new ActionListener() {
               @Override
               public void actionPerformed(final ActionEvent event) {
                  rayTracer.camera = new Camera(rayTracer.currentScene, (Integer) multiSamplesField.getValue(), (Integer) brdfSamplesField.getValue(),
                        RTStatics.NEAR_PLANE, (Integer) imageXField.getValue(), (Integer) imageYField.getValue());
                  rayTracer.camera.addActionListener(new ActionListener() {
                     @Override
                     public void actionPerformed(final ActionEvent event) {
                        if (event.getID() == 1) {
                           sceneScrollPane.setEnabled(true);
                           imageXField.setEnabled(true);
                           imageYField.setEnabled(true);
                           multiSamplesField.setEnabled(true);
                           brdfSamplesField.setEnabled(true);
                           renderButton.setEnabled(true);
                           closeButton.setEnabled(true);
                           iconLabel.revalidate();

                           imagePane.repaint();
                        } else if (event.getID() == 2) {
                           final BufferedImage image = rayTracer.camera.getImage();
                           iconLabel.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
                           iconLabel.revalidate();

                           imagePane.repaint();
                        }
                     }
                  });

                  rayTracer.camera.createImage();
               }
            };

            new Thread() {
               @Override
               public void run() {
                  rayTracer.currentScene = ((Scenes) sceneList.getSelectedValue()).getScene(useKDTreeCheckBox.isSelected());
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

      imagePanel.add(imagePane);

      final JButton saveButton = new JButton("Save Image to File...");
      saveButton.setAlignmentX(JDialog.CENTER_ALIGNMENT);
      saveButton.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(final ActionEvent event) {
            final JFileChooser fileChooser = new JFileChooser();
            final int result = fileChooser.showSaveDialog(RayTracerInterface.this);

            if (result == JFileChooser.APPROVE_OPTION) {
               final String path = fileChooser.getSelectedFile().getAbsolutePath();

               rayTracer.camera.writeOutputFile(path.endsWith(".png") || path.endsWith(".jpg") ? path : path + ".png", useNormalizedImageCheckBox.isSelected());
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

      final JProgressBar progressBar = new JProgressBar();
      progressBar.setStringPainted(true);
      progressBar.setPreferredSize(new Dimension(300, 30));

      RTStatics.setProgressBar(progressBar);

      final JPanel scenePanel = new JPanel(new FlowLayout());
      scenePanel.add(sceneLabel);
      scenePanel.add(sceneScrollPane);

      final JPanel widthPanel = new JPanel(new FlowLayout());
      widthPanel.add(imageWidthLabel);
      widthPanel.add(imageXField);

      final JPanel heightPanel = new JPanel(new FlowLayout());
      heightPanel.add(imageHeightLabel);
      heightPanel.add(imageYField);

      final JPanel multiSamplesPanel = new JPanel(new FlowLayout());
      multiSamplesPanel.add(multiSamplesLabel);
      multiSamplesPanel.add(multiSamplesField);

      final JPanel brdfPanel = new JPanel(new FlowLayout());
      brdfPanel.add(brdfSamplesLabel);
      brdfPanel.add(brdfSamplesField);

      final JPanel buttonPanel = new JPanel(new FlowLayout());
      buttonPanel.add(renderButton);
      buttonPanel.add(closeButton);

      final JPanel checkboxPanel = new JPanel(new FlowLayout());
      checkboxPanel.add(useKDTreeCheckBox);
      checkboxPanel.add(useNormalizedImageCheckBox);

      sidePanel.add(scenePanel);
      sidePanel.add(widthPanel);
      sidePanel.add(heightPanel);
      sidePanel.add(multiSamplesPanel);
      sidePanel.add(brdfPanel);
      sidePanel.add(buttonPanel);
      sidePanel.add(progressBar);
      sidePanel.add(saveButton);
      sidePanel.add(checkboxPanel);

      this.getContentPane().setLayout(new BorderLayout());
      this.getContentPane().add(sidePanel, BorderLayout.WEST);
      this.getContentPane().add(imagePanel, BorderLayout.CENTER);
      this.getContentPane().add(scrollPane, BorderLayout.SOUTH);

      this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      this.pack();
      this.setSize(width, height);
      this.setVisible(true);
   }
}
