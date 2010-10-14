javac -cp ./lib/vecmath.jar -d bin src/stephen/ranger/ar/*.java src/stephen/ranger/ar/bounds/*.java src/stephen/ranger/ar/lighting/*.java src/stephen/ranger/ar/sceneObjects/*.java
java -Xmx4g -cp ./lib/vecmath.jar;./bin;./resources stephen.ranger.ar.RayTracer
pause