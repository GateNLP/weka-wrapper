/*
 *  Copyright (c) The University of Sheffield.
 *
 *  This file is free software, licensed under the 
 *  GNU Library General Public License, Version 2.1, June 1991.
 *  See the file LICENSE.txt that comes with this software.
 *
 */
package gate.lib.wekawrapper.utils;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonValue;
import gate.lib.interaction.data.SparseDoubleVector;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
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

  
  public static byte[] pred2binary(double[] pred) {
    try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
       ObjectOutput out = new ObjectOutputStream(bos)) {
       out.writeObject(pred);
       return bos.toByteArray();
    }  catch (Exception ex)    {
      throw new RuntimeException("Could not convert predictions to binary buffer",ex);
    }
  }

  /**
   * Convert an ObjectStream buffer to a SDV, or return null if we got the String stop
   * signal. 
   * All other cases will generate a runtime exception.
   * @param buffer
   * @return 
   */
  public static SparseDoubleVector binary2sdv(byte[] buffer) {
    try (ByteArrayInputStream bis = new ByteArrayInputStream(buffer);
      ObjectInput in = new ObjectInputStream(bis)) {
      Object obj = in.readObject();
      if(obj != null && obj.equals("STOP")) {
        return null;
      }
      if(obj == null) { 
        throw new RuntimeException("Got null buffer");
      } 
      if(!(obj instanceof SparseDoubleVector)) {                
        throw new RuntimeException("Got something that is not a SparseDoubleVector or the stop signal but of type "+obj.getClass()+" value="+obj);        
      }
      return (SparseDoubleVector)obj;
    } catch(Exception ex) {
      throw new RuntimeException("Problem unpacking object buffer",ex);
    }
  }
  
  public static String pred2json(double[] pred) {
    return Json.array(pred).toString();
  }

  public static SparseDoubleVector json2sdv(String json) {
    JsonValue jv = Json.parse(json);
    // we expect a "JsonObject"  which corresponds to a map. That map must have
    // the entries "indices", "values" and optionally "instanceWeight";
    if(jv.isObject()) {
      JsonArray jindices = jv.asObject().get("indices").asArray();
      JsonArray jvalues = jv.asObject().get("values").asArray();
      JsonValue wobj = jv.asObject().get("instanceWeight");
      double instanceWeight = Double.NaN;
      if(wobj!=null) {
        instanceWeight = wobj.asDouble();
      }
      SparseDoubleVector sdv = new SparseDoubleVector(jindices.size());
      sdv.setInstanceWeight(instanceWeight);
      int[] indices = sdv.getLocations();      
      double[] values = sdv.getValues();
      for(int i = 0; i<jindices.size(); i++) {
        indices[i] = jindices.get(i).asInt();
        values[i] = jvalues.get(i).asDouble();
      }
      return sdv;
    } else {
      throw new RuntimeException("JSON not parsable, got "+json);
    }
    
  }
}
