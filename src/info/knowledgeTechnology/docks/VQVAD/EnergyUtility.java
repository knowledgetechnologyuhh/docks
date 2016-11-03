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
package info.knowledgeTechnology.docks.VQVAD;

import edu.cmu.sphinx.frontend.DoubleData;

public class EnergyUtility {

	// matlab epsilon
	public final static double eps = 2.2204e-16;

	// std (x) = sqrt ( 1/(N-1) SUM_i (x(i) - mean(x))^2 )
	public static double std(DoubleData frame) {
		double std = 0;
		double mean = 0;

		for (double v : frame.getValues()) {
			mean += v;
		}

		mean /= frame.dimension();

		for (double v : frame.getValues()) {
			std += (v - mean) * (v - mean);
		}

		std /= frame.dimension() - 1;

		return Math.sqrt(std);
	}

    // as seen in VQVAD
    public static double computeEnergy(DoubleData frame) {
    	return 20 * Math.log10(std(frame) + eps);
    }

    public static double[] computeEnergyPerFrame(DoubleData[] frames) {
    	final double[] energies = new double[frames.length];
    	for (int i = 0; i < frames.length; i++) {
    		energies[i] = computeEnergy(frames[i]);
    	}
    	return energies;
    }
}
