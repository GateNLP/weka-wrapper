package gate.lib.wekawrapper;

import gate.lib.interaction.data.SparseDoubleVector;
import gate.lib.wekawrapper.utils.WekaWrapperUtils;
import java.io.IOException;
import weka.classifiers.Classifier;
import weka.core.Instances;
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
      System.err.println("Not exactly two arguments: modelpath, arffheaderfile");
      System.exit(1);
    }
    String modelFileName = args[0];
    String headerFileName = args[1];

    if(modelFileName == null || modelFileName.isEmpty()) {
      System.err.println("No model file name specified, need modelpath and arffheaderfilepath");
      System.exit(1);
    }
    if(headerFileName == null || headerFileName.isEmpty()) {
      System.err.println("No header file name specified, need modelpath and arffheaderfilepath");
      System.exit(1);
    }
    
    System.err.println("Argument 1: "+modelFileName);
    System.err.println("Argument 2: "+headerFileName);
    
    Classifier classifier = WekaWrapperUtils.loadClassifier(modelFileName);
    if(classifier == null) System.exit(1);

    Instances dataset = WekaWrapperUtils.loadDataset(headerFileName);

    
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
        double[] ret = WekaWrapperUtils.classifyInstance(sdv, classifier, dataset);
        if(ret==null) System.exit(1);
        oos.writeObject(ret);
        oos.flush();
      } else {
        System.err.println("Received something which is not a SparseDoubleVector!");
        break;
      } // if SparseDoubleVector
    } // while
  } // main
  
}
