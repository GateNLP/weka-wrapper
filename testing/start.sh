#!/bin/bash 

## Run from root directory to start server after 
## mvn package

java -cp target/weka-wrapper-3.5.1-jar-with-dependencies.jar gate.lib.wekawrapper.WekaApplicationServer  testing/lf.model testing/header.arff 8080 10
