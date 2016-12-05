#!/bin/bash

source env.sh

# unit tests for utils
java $JLIB -cp $JARS:./java:. a.kgagent.util.MessagePart
java $JLIB -cp $JARS:./java:. a.kgagent.util.Message

