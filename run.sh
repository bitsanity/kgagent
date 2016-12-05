#!/bin/bash

source env.sh

java $JLIB -cp $JARS:./java:. a.kgagent.FXBrowser

