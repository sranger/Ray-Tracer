package stephen.ranger.ar;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.vecmath.Vector3f;

public class BRDFMaterial extends ColorInformation {

   /**
    * http://www1.cs.columbia.edu/CAVE/software/curet/html/brdfm.html<br/>
    * <br/>
    * Contains:<br/>
    * ------------------------<br/>
    * - theta_v (polar angle of the viewing direction) (pitch),<br/>
    * - phi_v (azimuthal angle of the viewing direction) (yaw),<br/>
    * - theta_i (polar angle of the illumination direction) (pitch),<br/>
    * - and phi_i (the azimuthal angle of the illumination direction) (yaw).<br/>
    * <br/>
    * major array: entry for the 205 sample directions<br/>
    * minor array: theta/phi values
    */
   public static final float[][] brdfDirections = remapDirections(parseBRDFFile("resources/table.txt"));

   /**
    * http://www1.cs.columbia.edu/CAVE/software/curet/html/brdfm.html<br/>
    * <br/>
    * Contains:<br/>
    * ------------------------<br/>
    * major array: entry for the 61 materials<br/>
    * minor array: weights for the 205 sample directions that correspond to the major array in 'brdfDirections'
    */
   public static final float[][] brdfWeights = parseBRDFFile("resources/abrdf.dat");

   private final float[] material;

   public BRDFMaterial(final int materialID, final Color color) {
      super(color);

      material = brdfWeights[materialID];
   }

   public float getBRDFLuminocity(final IntersectionInformation info, final Camera camera) {
      final Random random = new Random();
      final float[] in = new float[2];
      final float[] out = new float[2];
      float luminocity = 0f;
      float weight = 0f;
      int ctr = 0;

      final Vector3f tempDir = new Vector3f();

      for (int i = 0; i < camera.brdfSamples; i++) {
         tempDir.sub(info.intersection, camera.light.origin);
         tempDir.normalize();

         in[0] = (float) Math.acos(tempDir.z);
         in[1] = (float) Math.atan(tempDir.y / tempDir.x);

         // get random view direction in range of [0,2PI], [0,PI/2]
         out[0] = random.nextFloat() * PBRTMath.F_2_PI;
         out[1] = random.nextFloat() * PBRTMath.F_HALF_PI;

         final float[] remaped = PBRTMath.getRemapedDirection(in, out);
         float lastMaxDist2 = 0.001f;

         while (ctr < 2 && lastMaxDist2 < 1.5f) {
            for (int j = 0; j < brdfDirections.length; j++) {
               final float dist2 = PBRTMath.getDistance2(remaped, brdfDirections[j]);

               if (dist2 < lastMaxDist2) {
                  final float temp = (float) Math.exp(-100.0 * dist2);
                  luminocity += material[j] * temp;
                  weight += temp;
                  ctr++;
               }
            }

            lastMaxDist2 *= 2f;
         }

      }

      if (ctr == 0) {
         return 0f;
      }

      return luminocity / weight;
   }

   /**
    * parse BRDF file (either abrdf.dat or table.txt).
    * @param file
    * @return
    */
   private static float[][] parseBRDFFile(final String file) {
      final List<float[]> values = new ArrayList<float[]>();
      float[][] valuesAsArray = null;

      try {
         final BufferedReader fin = new BufferedReader(new FileReader(new File(file)));
         String temp = null;
         String[] split = null;
         final List<Float> tempValues = new ArrayList<Float>();

         while ((temp = fin.readLine()) != null) {
            split = temp.split(" ");

            for (int j = 1; j < split.length; j++) {
               if (!split[j].replaceAll("\\D", "").equals("")) {
                  tempValues.add(Float.parseFloat(split[j]));
               }
            }

            final float[] buffer = new float[tempValues.size()];

            for (int i = 0; i < buffer.length; i++) {
               buffer[i] = tempValues.get(i);
            }

            values.add(buffer);
            tempValues.clear();
         }

         valuesAsArray = new float[values.size()][];

         for (int i = 0; i < values.size(); i++) {
            valuesAsArray[i] = values.get(i);
         }
      } catch (final IOException e) {
         e.printStackTrace();
      }

      return valuesAsArray;
   }

   /**
    * PBRT page 465.
    * 
    * Remaps viewing/light directions to sin(theta-i) * sin(theta-o), delta-phi / PI, cos(theta-i) * cos(theta-o)
    * 
    * @param directions
    * @return
    */
   private static float[][] remapDirections(final float[][] directions) {
      final float[][] remapedDirections = new float[directions.length][];

      for (int i = 0; i < directions.length; i++) {
         remapedDirections[i] = PBRTMath.getRemapedDirection(new float[] { directions[i][0], directions[i][1] }, new float[] { directions[i][2], directions[i][3] });
      }

      return remapedDirections;
   }
}
