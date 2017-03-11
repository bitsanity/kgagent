#!/bin/bash

source env.sh

# note: if the AES test throws an exception remember to download Oracle's
#       JCE Unlimited Strength Jurisdiction Policy Files 8 Download and
#       replace default files in $JAVA_HOME/jre/lib/security/

java $JLIB -cp $JARS:./java:. a.kgagent.util.AES256
java $JLIB -cp $JARS:./java:. a.kgagent.util.ECIES

java $JLIB -cp $JARS:./java:. a.kgagent.util.MessagePart
java $JLIB -cp $JARS:./java:. a.kgagent.util.Message

