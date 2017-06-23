#!/bin/bash

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
MYROOTDIR=`cd "$SCRIPTDIR/.."; pwd -P`

ROOTDIR="$WEKA_WRAPPER_HOME"
arff="$1"
shift
model="$1"
shift
class="$1"
shift

if [ "x$ROOTDIR" == "x" ]
then
  export ROOTDIR="$MYROOTDIR"
else
  pushd "$ROOTDIR" >/dev/null
  pushed=true
fi

if [ "$class" == "" ]
then
  echo 1>&2 'wekaWrapperTrain.sh: not enough parameters!'
  echo 1>&2 ROOTDIR=$ROOTDIR arff=$arff model=$model class=$class
  exit -1
fi

echo 1>&2 RUNNING java -cp "$ROOTDIR/target/*":"$ROOTDIR/target/dependency/*" gate.lib.wekawrapper.WekaTraining "${arff}" "${model}" "${class}" $*
java -cp "$ROOTDIR/target/*":"$ROOTDIR/target/dependency/*" gate.lib.wekawrapper.WekaTraining "${arff}" "${model}" "${class}" $*
if [ "x$pushed" != "x" ]
then
  popd >/dev/null
fi
