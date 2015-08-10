package VQVAD;

import org.apache.commons.math3.ml.clustering.DoublePoint;
import org.apache.commons.math3.ml.distance.EuclideanDistance;

import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DoubleData;

public class VQVADModel implements Data {

	private static final long serialVersionUID = 5646714890153166531L;

	final protected DoublePoint[] speechCenters;
	final protected DoublePoint[] nonspeechCenters;
	final protected double energyMinLevel;

	public VQVADModel(DoublePoint[] speechCenters, DoublePoint[] nonspeechCenters, double energyMinLevel) {
		this.speechCenters = speechCenters;
		this.nonspeechCenters = nonspeechCenters;
		this.energyMinLevel = energyMinLevel;
	}

	// pdist2(input, centers, "euclidean").^2
	protected double minDistance(DoublePoint[] centers, double[] input) {
		EuclideanDistance d = new EuclideanDistance();
		double minDistance = Double.POSITIVE_INFINITY;

		for (DoublePoint c : centers) {
			double v = d.compute(c.getPoint(), input);

			v = v * v;

			if (v < minDistance) {
				minDistance = v;
			}
		}

		return minDistance;
	}

	/**
	 *
	 * @param frames MFCC for the frame
	 * @return
	 */
	public boolean classify(DoubleData frame, DoubleData mfcc) {
		double speechMinDistance = minDistance(speechCenters, mfcc.getValues());
		double nonspeechMinDistance = minDistance(nonspeechCenters, mfcc.getValues());

		double LLR = nonspeechMinDistance - speechMinDistance;

		double energy = EnergyUtility.computeEnergy(frame);

		return LLR >= 0 && energy >= energyMinLevel;
	}

	public static void main(String[] args) {
		System.out.println("LOL");
//		OctaveEngine octave = new OctaveEngineFactory().getScriptEngine();
//		octave.eval("cd ..");
//		octave.eval("pwd");


		DoubleData[] frames = new DoubleData[3];
		frames[0] = new DoubleData(new double[]{1,2,3,4,5,6,1,2,3,4,5,6,1,2,3,4,5,6,1,2,3,4,5,6}, 8000, 0);
		frames[1] = new DoubleData(new double[]{-1,-2,-3,-4,-5,-6,-1,-2,-3,-4,-5,-6,-1,-2,-3,-4,-5,-6,-1,-2,-3,-4,-5,-6}, 8000, 6);
		frames[2] = new DoubleData(new double[]{6,5,4,3,2,1,6,5,4,3,2,1,6,5,4,3,2,1,6,5,4,3,2,1}, 8000, 12);


	}

}
