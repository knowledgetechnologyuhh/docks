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
	public double minDistance(DoublePoint[] centers, double[] input) {
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

}
