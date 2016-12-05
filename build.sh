#!/bin/bash

source env.sh

javac -g -classpath $JARS:./java:. java/a/kgagent/util/*.java
javac -g -classpath $JARS:./java:. java/a/kgagent/*.java

cd ./C
./compile.sh
cd ..

