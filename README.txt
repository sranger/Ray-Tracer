Ray Tracer for Advanced Rendering
Author: Stephen Ranger

vecmath supplied is from java3d 1.5.2

To run, first, go into run.bat and set your path for the Stanford models. Lucy and the Thai Statue use ~50GB or memory. 

Setup for models dir
-----------------------------------------------------
<base dir>/bunny/reconstruction/bun_zipper.ply
<base dir>/dragon_recon/dragon_vrip.ply
<base dir>/happy_recon/happy_vrip.ply
<base dir>/lucy.ply
<base dir>/xyzrgb_dragon.ply/data
<base dir>/xyzrgb_statuette.ply/data
-----------------------------------------------------

Some of the settings haven't been added to the GUI yet (as I'm still finishing the photon mapping) but can be found in the RTStatics.java class. They are at the top of the file if you'd like to edit them.


To run (on Windows), double-click the run.bat file. On Max/Linux, execute run.sh. Eclipse classpath/project files are also included. Just add your base model dir to the program arguments in your run configuration before running.

USAGE ./run.sh <base dir>