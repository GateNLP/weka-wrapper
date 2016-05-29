# weka-wrapper

A minimal wrapper around Weka for easy invocation of Weka by other Java processes.

The primary purpose of this is to be able to run a process from the command line which will accept
unclassified instances from a different process, apply a classifier, and send back the 
prediction. For efficiency, ObjectInput/Output streams are used for the communcation.

This makes it possible to invoke Weka and use a separately trained Weka model in the 
GATE LearningFramework plugin (Weka cannot be directly used with this plugin because 
there is no way to combine the licenses of all dependencies together with Weka.)

## Installation

Download one of the release archives and expand, optionall put the bin subdirectory of
the unpacked archive on the bin path.

To install by cloning the GitHub repository, first clone the repository, then run the
following commands:
````
mvn dependency:copy-dependencies
mvn install
````

## Applying a model to individual instances

## Training a model from an ARFF file
