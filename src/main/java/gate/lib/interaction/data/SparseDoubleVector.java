package gate.lib.interaction.data;

import java.io.Serializable;

/**
 * Minimal container for a sparse vector of n non-zero locations.
 * This is meant to be used only for passing on sparse vectors, so 
 * no attempt is made to have any of the specific methods normally
 * needed for sparse vectors, and especially there is no code to make
 * accessing vector elements fast.
 * 
 * NOTE: this has been copied from the gatelib-interaction project by the author
 * so that this project does not need to depend on the whole gatelib-interaction
 * project. The license of this within the weka-wrapper project will be
 * the same as for the whole weka-wrapper project, while the license of
 * the copy in the gatelib-interaction project is the same as for the 
 * whole gatelib-interaction project. If in doubt, the license for this
 * file is Apache 2.0.
 * 
 * @author Johann Petrak
 */
public class SparseDoubleVector implements Serializable {

  private static final long serialVersionUID = 2L;
 
  protected int[] indices;
  protected double[] values;
  protected double instanceWeight = Double.NaN;
  public SparseDoubleVector(int numberOfLocations) {
    indices = new int[numberOfLocations];
    values = new double[numberOfLocations];
  }
  
  public int[] getLocations() { return indices; }
  public double[] getValues() { return values; }
  public int nLocations() { return indices.length; }
  public double getInstanceWeight () { return instanceWeight; }
  public void setInstanceWeight(double weight) { instanceWeight = weight; }
  
}
