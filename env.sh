# some reusable definitions for inclusion in build run and test
# scripts

JLIB=-Djava.library.path=./lib:/home/skull/secp256k1/.libs

JARS=\
./lib/hsqldb.jar:\
./lib/core-3.2.1.jar:\
./lib/javase-3.2.1.jar:\
./lib/json-simple-1.1.1.jar

