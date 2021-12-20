#!/bin/bash

versions=("1.18.1" "1.18" "1.17.1" "1.17" "1.16.5" "1.16.4" "1.16.3" "1.16.2" "1.16.1" "1.16" "1.15.2" "1.15.1" "1.15" "1.14.4" "1.14.3" "1.14.2" "1.14.1" "1.14" "1.13.2", "1.13.1")
for i in "${versions[@]}"; do
  echo "Generating data for version $i..."
  java -jar ./build/libs/datagenerator-"$DATAGEN_VERSION"-all.jar -v "$i"
done
