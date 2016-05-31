# weka-wrapper

A minimal wrapper around Weka for easy invocation of Weka by other Java processes.

The primary purpose of this is to be able to run a process from the command line which will accept
unclassified instances from a different process, apply a classifier, and send back the
prediction. For efficiency, ObjectInput/Output streams are used for the communcation.

This makes it possible to invoke Weka and use a separately trained Weka model in the
[GATE LearningFramework plugin](https://github.com/GateNLP/gateplugin-LearningFramework) (Weka cannot be directly used with this plugin because
there is no way to combine the licenses of all dependencies together with Weka.)

For instructions about how to use weka-wrapper from the GATE LearningFramework plugin, see [Using Weka](https://github.com/GateNLP/gateplugin-LearningFramework/wiki/UsingWeka)

## Installation

There are two possible ways of how to install weka-wrapper:

* Download one of the release archives and expand, optionally put the bin subdirectory of the unpacked archive on the bin path.

or

* To install by cloning the GitHub repository, first clone the repository, then run the
following commands:
````
mvn dependency:copy-dependencies
mvn install
````

## Applying a model to individual instances

This is done by starting the weka-wrapper with the command:
````
./bin/wekaWrapperApply.sh <modelFile> <arffHeaderFile>
````
Where `modelFile` is the full path of the model saved by Weka and `arffHeaderFile` is an ARFF file that contains the attribute definitions but no data rows.

After the program is started that way it expects to communicate with the invoking process using the protocol defined in the [gatelib-interaction](https://github.com/GateNLP/gatelib-interaction) library as defined in the `Process4ObjectStream` class:
1. Send a "hello string"
2. Read the hello string from the weka wrapper
3. Send an `SparseDoubleVector` object
4. Retrieve an `double[]` object (the probabilities for each lable for classifiers, or a single element, the target value for regression models)
5. Repeate steps 3 and 4 as often as needed
6. Send the string "STOP" to terminate the weka wrapper process

## Training a model from an ARFF file

This is done by running the folowing command:
````
./bin/wekaWrapperTrain.sh <arffFile> <modelFile> <javaClass> [<parm1> <parm2> ...]
````
Where `arffFile` is the full path to an ARFF file containing the data to train from, the `modelFile` is the full path to the file where the model should be saved, and `javaClass` is the full class of the Weka classifier to use for training. All remaining arguments are interpreted as parameters for the weka algorithm and passed on to it. 
