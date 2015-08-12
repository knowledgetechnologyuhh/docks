/**
 * Copyright (C) 2015 Marian Tietz
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contact:
 * vqvad@nemo.ikkoku.de
 */
package VQVAD;

import edu.cmu.sphinx.frontend.DoubleData;

public class EnergyUtility {

    // 20*log10(std(frame)+eps);
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

    	// 20*log10(std(frame) + eps)
    	return 20 * Math.log10(Math.sqrt(energy) + Double.MIN_NORMAL);
    }

    public static double[] computeEnergyPerFrame(DoubleData[] frames) {
    	double[] energies = new double[frames.length];
    	for (int i = 0; i < frames.length; i++) {
    		energies[i] = computeEnergy(frames[i]);
    	}
    	return energies;
    }
}
