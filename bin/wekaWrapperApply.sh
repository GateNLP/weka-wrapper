#!/bin/bash

ROOTDIR="$1"
model="$2"
header="$3"


pushd "$ROOTDIR" >/dev/null
## NOTE: it is not trivial to make absolutely sure that there is no output from maven to 
## standard output (which is required here!)
## The -q parameter is supposed to work, but there have been reports that sometimes it still lets through INFO messages.
## An alternate approach is to set the log level via MAVEN_OPTS, so we do both
## export MAVEN_OPTS=-Dorg.slf4j.simpleLogger.defaultLogLevel=error
## mvn -q exec:java -Dexec.mainClass="gate.lib.wekawrapper.WekaApplication" -Dexec.args="${model} ${header}"
java -cp $ROOTDIR/target/'*':$ROOTDIR/target/dependency/'*' gate.lib.wekawrapper.WekaApplication $model $header
popd >/dev/null
