#!/usr/bin/env bash

SHARKFW=/home/j4rvis/dev/shark/projects/SharkFW/sharkfw
DEST=/home/j4rvis/dev/shark/projects/SharkNetNG/AndroidSharkFW

cd $SHARKFW && mvn clean install
cp ./core/target/sharkfw-core-1.0-SNAPSHOT.jar $DEST/libs/
