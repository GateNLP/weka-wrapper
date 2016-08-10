/*
 *  Copyright (c) The University of Sheffield.
 *
 *  This file is free software, licensed under the 
 *  GNU Library General Public License, Version 2.1, June 1991.
 *  See the file LICENSE.txt that comes with this software.
 *
 */
package gate.lib.wekawrapper.utils;

import gate.lib.interaction.data.SparseDoubleVector;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.SparseInstance;
import weka.core.converters.ConverterUtils;

/**
 * Various static utility functions.
 *
 * @author Johann Petrak
 */
public class WekaWrapperUtils {

  /**
   * Load a Weke classifier or return null in case of an error.
   *
   * @param modelFileName
   * @return
   */
  public static Classifier loadClassifier(String modelFileName) {

    File modelFile = new File(modelFileName);

    if (!modelFile.exists()) {
      System.err.println("WekaApplication: Model file does not exist: " + modelFile.getAbsolutePath());
      return null;
    }

    // Load the model
    ObjectInputStream wois = null;
    try {
      wois = new ObjectInputStream(new FileInputStream(modelFile));
    } catch (IOException ex) {
      System.err.println("WekaApplication: IO error when trying to open model file: " + modelFile.getAbsolutePath());
      ex.printStackTrace(System.err);
      return null;
    }
    Classifier classifier;
    try {
      classifier = (Classifier) wois.readObject();
    } catch (Exception ex) {
      return null;
    }
    try {
      wois.close();
    } catch (IOException ex) {
      //
    }
    return classifier;
  }

  public static Instances loadDataset(String headerFileName) {
    File headerFile = new File(headerFileName);
    // Load the ARFF header
    ConverterUtils.DataSource source;
    try {
      source = new ConverterUtils.DataSource(headerFileName);
    } catch (Exception ex) {
      System.err.println("Could not get the data source: " + ex.getMessage());
      return null;
    }
    Instances dataset;
    try {
      dataset = source.getDataSet();
    } catch (Exception ex) {
      System.err.println("Could not get the Weka header dataset: " + ex.getMessage());
      return null;
    }

    dataset.setClassIndex(dataset.numAttributes() - 1);
    //System.err.println("nomnal target="+isNominal);
    return dataset;

  }

  public static synchronized double[] classifyInstance(SparseDoubleVector sdv, Classifier classifier,
          Instances dataset) {
    Attribute target = dataset.classAttribute();
    boolean isNominal = target.isNominal();

    SparseInstance instance = new SparseInstance(1.0, sdv.getValues(), sdv.getLocations(), dataset.numAttributes() - 1);
    double instanceWeight = sdv.getInstanceWeight();
    if (!Double.isNaN(instanceWeight)) {
      instance.setWeight(instanceWeight);
    }
    instance.setDataset(dataset);
    double[] ret;
    if (isNominal) {
      try {
        ret = classifier.distributionForInstance(instance);
      } catch (Exception ex) {
        System.err.println("Error trying to get probability distribution for instance: "+ex.getMessage());
        return null;
      }
    } else {
      ret = new double[1];
      try {
        ret[0] = classifier.classifyInstance(instance);
      } catch (Exception ex) {
        System.err.println("Error trying to classify instance: "+ex.getMessage());
        return null;
      }
    }
    return ret;
  }

}
