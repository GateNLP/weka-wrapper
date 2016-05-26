#!/bin/bash

mvn exec:java -Dexec.mainClass="gate.lib.wekawrapper.WekaApplication" -Dexec.args="test/test1.model test/test1.arff"
