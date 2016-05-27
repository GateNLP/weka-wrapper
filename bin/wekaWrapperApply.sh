#!/bin/bash

model="$1"
header="$2"

PRG="$0"
CURDIR="`pwd`"
# need this for relative symlinks
while [ -h "$PRG" ] ; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`"/$link"
  fi
done
SCRIPTDIR=`dirname "$PRG"`
SCRIPTDIR=`cd "$SCRIPTDIR"; pwd -P`
ROOTDIR=`cd "$SCRIPTDIR/.."; pwd -P`

pushd "$ROOTDIR" >/dev/null
## NOTE: it is not trivial to make absolutely sure that there is no output from maven to 
## standard output (which is required here!)
## The -q parameter is supposed to work, but there have been reports that sometimes it still lets through INFO messages.
## An alternate approach is to set the log level via MAVEN_OPTS, so we do both
export MAVEN_OPTS=-Dorg.slf4j.simpleLogger.defaultLogLevel=error
mvn -q exec:java -Dexec.mainClass="gate.lib.wekawrapper.WekaApplication" -Dexec.args="${model} ${header}"
## java -cp $ROOTDIR/lib/'*':target/weka-wrapper-1.0-SNAPSHOT.jar gate.lib.wekawrapper.WekaApplication $model $header
popd >/dev/null
