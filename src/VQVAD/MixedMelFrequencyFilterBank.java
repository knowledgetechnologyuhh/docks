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

import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.frontend.frequencywarp.MelFrequencyFilterBank;

/**
 * Simple extension of the Sphinx MelFrequencyFilterBank to wrap the result
 * in a MFCCPacket to avoid losing the original audio data and be able to
 * pass it down further down the chain.
 *
 * This is important since we cannot compute properties like signal energy
 * without the original frames.
 *
 */
public class MixedMelFrequencyFilterBank extends MelFrequencyFilterBank {

	public MixedMelFrequencyFilterBank(double minFreq, double maxFreq, int numFilters) {
		super(minFreq, maxFreq, numFilters);
	}

	@Override
	public Data getData() {
		Data mfccData = super.getData();
		Data originalData = this.getPredecessor().getData();

		if (mfccData instanceof DoubleData && originalData instanceof DoubleData) {
			return new MFCCPacket((DoubleData) originalData, (DoubleData) mfccData);
		}

		return mfccData;
	}
}
