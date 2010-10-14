package stephen.ranger.ar;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import stephen.ranger.ar.lighting.Light;

public class BRDFMaterial extends ColorInformation {

   /**
    * http://www1.cs.columbia.edu/CAVE/software/curet/html/brdfm.html<br/>
    * <br/>
    * Contains:<br/>
    * ------------------------<br/>
    * - theta_v (polar angle of the viewing direction),<br/>
    * - phi_v (azimuthal angle of the viewing direction),<br/>
    * - theta_i (polar angle of the illumination direction),<br/>
    * - and phi_i (the azimuthal angle of the illumination direction).<br/>
    * <br/>
    * major array: entry for the 205 sample directions<br/>
    * minor array: theta/phi values
    */
   public static final float[][] brdfDirections = parseBRDFFile("resources/table.txt");

   /**
    * http://www1.cs.columbia.edu/CAVE/software/curet/html/brdfm.html<br/>
    * <br/>
    * Contains:<br/>
    * ------------------------<br/>
    * major array: entry for the 61 materials<br/>
    * minor array: weights for the 205 sample directions that correspond to the major array in 'brdfDirections'
    */
   public static final float[][] brdfWeights = parseBRDFFile("resources/abrdf.dat");

   public final int materialID;

   public BRDFMaterial(final int materialID, final Color color) {
      super(color);

      this.materialID = materialID;
   }

   public static float getBRDFLuminance(final IntersectionInformation info, final Light light) {

      return 1f;
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
}
