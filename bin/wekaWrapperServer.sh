#!/bin/bash 

## Start a Weka server

version=3.8


## Find where the weka-wrapper directory is
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

## Check if the weka-wrapper software has been installed properly, which means
## it must have been built!
if [[ -f ${ROOTDIR}/target/weka-wrapper-${version}-jar-with-dependencies.jar ]]
then
  ## Run it
  java -cp ${ROOTDIR}/target/weka-wrapper-${version}-jar-with-dependencies.jar gate.lib.wekawrapper.WekaApplicationServer  "$@"
else
  echo 'ERROR: weka-wrapper not fully installed, please run the following commands first:'
  echo mvn dependency:copy-dependencies
  echo mvn install
fi
