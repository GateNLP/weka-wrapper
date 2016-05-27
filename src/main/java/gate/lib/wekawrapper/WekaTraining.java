/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gate.lib.wekawrapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.OptionHandler;
import weka.core.converters.ConverterUtils;

/**
 * Simple wrapper to train a model from the command line.
 * 
   * This needs exactly the following command line parameters in this order:
   * <ul>
   * <li>the path to the ARF file with the training data
   * <li>the path to the model file to save
   * <li>the class of the training algorithm
   * </ul>
   * 
   * All additional parameters will get passed on to the training algorithm.
   * 
 * 
 * @author Johann Petrak
 */
public class WekaTraining {
  public static void main(String[] args) {
    if(args.length < 3) {
      throw new RuntimeException("WekaTraining: Not at least three parameters: arffFile, modeFile, trainerClass");
    }
    String arffFileName = args[0];
    String modelFileName = args[1];
    String trainerClass = args[2];
        
    
    File modelFile = new File(modelFileName);
    File arffFile = new File(arffFileName);
    
    if(!arffFile.exists()) {
      System.err.println("WekaTraining: ARFF file does not exist: "+arffFile.getAbsolutePath());
      System.exit(1);
    }
    
    // Load the ARFF file
    ConverterUtils.DataSource source;
    try {
      source = new ConverterUtils.DataSource(arffFileName);
    } catch (Exception ex) {
      throw new RuntimeException("Could not read ARFF file "+arffFile.getAbsolutePath(),ex);
    }
    Instances dataset;
    try {
      dataset = source.getDataSet();
    } catch (Exception ex) {
      throw new RuntimeException("Could not read ARFF file "+arffFile.getAbsolutePath(),ex);
    }
    
    dataset.setClassIndex(dataset.numAttributes()-1);
    Attribute target = dataset.classAttribute();
    boolean isNominal = target.isNominal();
    
    // Now try to instantiate the training algorithm
    Classifier classifier = null;
    try {
      classifier = (Classifier)Class.forName(trainerClass).newInstance();
    } catch (Exception ex) {
      throw new RuntimeException("Could not create trainer class "+trainerClass,ex);
    }
    
    String[] options = null;
    // If we have remaining arguments, assume they are options
    if(args.length > 3) {
      options = new String[args.length-3];
      for(int i=3;i<args.length;i++) {
        options[i-3] = args[i];
      }
    }
    System.err.println("Have options: "+Arrays.toString(options));
    
    if(options!=null) {
      AbstractClassifier classifierAsAC = (AbstractClassifier)classifier;
      try {
        classifierAsAC.setOptions(options);
      } catch (Exception ex) {
        System.err.println("WekaTraining: could not set options, "+ex.getMessage());
        ex.printStackTrace(System.err);
      }
    }
    
    try {
      ((AbstractClassifier)classifier).setDebug(true);
      classifier.buildClassifier(dataset);
    } catch (Exception ex) {
      throw new RuntimeException("Could not train classifier ",ex);
    }
    System.err.println("Got model: "+classifier);
    // Now save the trained classifier
    ObjectOutputStream oos = null;
    try {
      oos = new ObjectOutputStream(new FileOutputStream(modelFile));
      oos.writeObject(classifier);
    } catch (Exception ex) {
      throw new RuntimeException("Could not save Weka model to "+modelFile,ex);
    } finally {
      if(oos!=null) try {
        oos.close();
      } catch (IOException ex) {
        // ignore
      }
    }
    
    
  }
}
