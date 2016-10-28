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

/**
 * Used by MixedMelFrequencyFilterBank to represent both the
 * original audio data and the computed MFCC data so the original
 * data is not lost when the result is passed down the pipeline.
 *
 */
public class MFCCPacket implements Data {

	private static final long serialVersionUID = -8383714866669035326L;

	protected final DoubleData audio;
	protected final DoubleData mfcc;
	protected final DoubleData denoisedAudio;

	public MFCCPacket(DoubleData audio, DoubleData mfcc, DoubleData denoisedAudio) {
		this.audio = audio;
		this.mfcc = mfcc;
		this.denoisedAudio = denoisedAudio;
	}

	public DoubleData getAudioFrame() {
		return audio;
	}

	public DoubleData getDenoisedAudioFrame() {
		return denoisedAudio;
	}

	public DoubleData getMFCC() {
		return mfcc;
	}

}
