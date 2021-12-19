#!/bin/bash

versions=("1.18.1" "1.18" "1.17.1" "1.17" "1.16.5")
for i in "${versions[@]}"; do
  java -jar ./build/libs/datagenerator-"$DATAGEN_VERSION"-all.jar -v "$i"
done
