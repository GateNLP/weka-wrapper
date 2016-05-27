package gate.lib.wekawrapper;

import gate.lib.interaction.data.SparseDoubleVector;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.SparseInstance;
import weka.core.converters.ConverterUtils.DataSource;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;

/**
 * Very simple wrapper for applying a model to feature vectors.
 * 
 * @author Johann Petrak
 */
public class WekaApplication {
  
  
  /**
   * Running the classifier application.
   * 
   * This needs exactly the following command line parameters in this order:
   * <ul>
   * <li>the path to the model file
   * <li>the path to an ARFF file with just the header and an empty data section
   * </ul>
   * 
   * @param args 
   */
  public static void main(String args[]) throws IOException, ClassNotFoundException, Exception {
    if(args.length != 2) {
      throw new RuntimeException("Not exactly two arguments: modelpath, arffheaderfile");
    }
    String modelFileName = args[0];
    String headerFileName = args[1];
    
    System.err.println("Argument 1: "+modelFileName);
    System.err.println("Argument 2: "+headerFileName);
    
    
    File modelFile = new File(modelFileName);
    File headerFile = new File(headerFileName);
    
    if(!modelFile.exists()) {
      System.err.println("WekaApplication: Model file does not exist: "+modelFile.getAbsolutePath());
      System.exit(1);
    }
    
    // Load the model
    ObjectInputStream wois = null;
    try {
      wois = new ObjectInputStream(new FileInputStream(modelFile));
    } catch (IOException ex) {
      System.err.println("WekaApplication: IO error when trying to open model file: "+modelFile.getAbsolutePath());
      ex.printStackTrace(System.err);
      System.exit(1);
    }
    Classifier classifier = (Classifier) wois.readObject();
    wois.close();
    
    // Load the ARFF header
    DataSource source = new DataSource(headerFileName);
    Instances dataset = source.getDataSet();
    
    dataset.setClassIndex(dataset.numAttributes()-1);
    Attribute target = dataset.classAttribute();
    boolean isNominal = target.isNominal();
    //System.err.println("nomnal target="+isNominal);
    
    
    // connect to the invoking process
    ObjectOutputStream oos = new ObjectOutputStream(System.out);
    // Send our hello 
    oos.writeObject("Hello from WekaApplication v1.0");
    ObjectInputStream ois = new ObjectInputStream(System.in);
    // read the hello from the other side
    Object obj = ois.readObject();
    System.err.println("WekaApplication: got hello: "+obj);
    
    // read from the object input stream and expect our sparsedoublefeature vector
    // until we get a string STOP
    while(true) {
      obj = ois.readObject();
      if(obj.equals("STOP")) {
        System.err.println("Terminating WekaApplication");
        break;
      }
      if(obj instanceof SparseDoubleVector) {
        SparseDoubleVector sdv = (SparseDoubleVector)obj;
        SparseInstance instance = new SparseInstance(1.0, sdv.getValues() , sdv.getLocations(), dataset.numAttributes()-1);        
        instance.setDataset(dataset);
        double[] ret;
        if(isNominal) {
          ret = classifier.distributionForInstance(instance);
        } else {
          ret = new double[1];
          ret[0] = classifier.classifyInstance(instance);
        }
        oos.writeObject(ret);
        oos.flush();
      } else {
        System.err.println("Received something which is not a SparseDoubleVector!");
        break;
      } // if SparseDoubleVector
    } // while
  } // main
  
}
