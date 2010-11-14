Ray Tracer for Advanced Rendering
Author: Stephen Ranger

vecmath supplied is from java3d 1.5.2

To run, first, go into run.bat and set your path for the Stanford models. Lucy and the Thai Statue don't render on my machine (not enough memory). 

Setup for models dir
-----------------------------------------------------
<base dir>/models/bunny/reconstruction/bun_zipper.ply
<base dir>/models/dragon_recon/dragon_vrip.ply
<base dir>/models/happy_recon/happy_vrip.ply
<base dir>/models/lucy.ply
<base dir>/models/xyzrgb_dragon.ply
<base dir>/models/xyzrgb_statuette.ply
-----------------------------------------------------

Some of the settings haven't been added to the GUI yet (as I'm still finishing the photon mapping) but can be found in the RTStatics.java class. They are at the top of the file if you'd like to edit them.


To run (on Windows), double-click the run.bat file. On Max/Linux, open run.bat and use the javac/java commands contained within. Eclipse classpath/project files are also included. Just add your base model dir to the program arguments in your run configuration before running.