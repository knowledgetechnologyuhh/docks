package VQVAD;

import edu.cmu.sphinx.frontend.DoubleData;

public class EnergyUtility {

    // 20*log10(std(frame)+eps);
	// TODO: eps
    public static double computeEnergy(DoubleData frame) {
    	double energy = 0;

    	// std(frame) = sqrt(mean(abs(x - x.mean())**2))
    	double mean = 0;
    	for (double v : frame.getValues()) {
    		mean += v;
    	}
    	mean /= frame.dimension();
    	for(double v : frame.getValues()) {
    		double abs = Math.abs(v - mean);
    		energy += abs * abs;
    	}
    	energy /= frame.dimension();

    	// log10()
    	return 20 * Math.log10(Math.sqrt(energy));
    }

    public static double[] computeEnergyPerFrame(DoubleData[] frames) {
    	double[] energies = new double[frames.length];
    	for (int i = 0; i < frames.length; i++) {
    		energies[i] = computeEnergy(frames[i]);
    	}
    	return energies;
    }
}
