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

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.endpoint.SpeechClassifiedData;

/**
 *
 */
public class FrameOverlapFilter extends BaseDataProcessor {

	protected float frameShift;
	protected float frameLen;
	protected int frameCounter = 0;

	protected long lastSampleNumber = 0;

	public FrameOverlapFilter(float frame_length_ms, float frame_shift_ms) {
		this.frameShift = frame_shift_ms;
		this.frameLen = frame_length_ms;
	}

	@Override
	public Data getData() throws DataProcessingException {
		Data d = getPredecessor().getData();

		if (d instanceof SpeechClassifiedData) {
			SpeechClassifiedData sd = (SpeechClassifiedData) d;

			int windowLength = (int) ((frameLen/1000) * sd.getSampleRate());

			if (lastSampleNumber == 0 || sd.getFirstSampleNumber() - lastSampleNumber == windowLength) {
				lastSampleNumber = sd.getFirstSampleNumber();
				return d;
			}

			// Skip frames that are overlaps
			while (sd.getFirstSampleNumber() - lastSampleNumber != windowLength) {
				d = getPredecessor().getData();
				if (d instanceof SpeechClassifiedData) {
					sd = (SpeechClassifiedData) d;
				} else {
					return d;
				}
			}

			lastSampleNumber = sd.getFirstSampleNumber();

			// d is a guaranteed to be non-overlap frame here
			return d;
		} else {
			return d;
		}
	}
}
