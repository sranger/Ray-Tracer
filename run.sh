#!/bin/bash

javac -cp ./lib/vecmath.jar -d bin src/stephen/ranger/ar/*.java src/stephen/ranger/ar/bounds/*.java src/stephen/ranger/ar/lighting/*.java src/stephen/ranger/ar/sceneObjects/*.java src/stephen/ranger/ar/materials/*.java src/stephen/ranger/ar/photons/*.java

cp resources/* bin
cd bin
jar cf ../dist/stephen.ranger.ar.raytracer.jar *
cd ..

java -Xmx128g -cp .:./lib/vecmath.jar:./dist/stephen.ranger.ar.raytracer.jar stephen.ranger.ar.RayTracer $1