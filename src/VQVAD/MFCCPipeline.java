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

import java.util.ArrayList;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.frequencywarp.MelFrequencyFilterBank;
import edu.cmu.sphinx.frontend.transform.DiscreteCosineTransform2;
import edu.cmu.sphinx.frontend.transform.DiscreteFourierTransform;

/**
 * Generates MFCCPacket objects containing the audio frame before processing,
 * a vector with MFCC and the de-noised power spectrum of the audio frame to
 * compute the cleaned energy from.
 *
 * As suggested by [1], spectral subtraction is used for de-noising as
 * implemented in {@link Denoise}.
 *
 * All parameters are taken from the reference implementation provided by [1].
 *
 * [1]: 'A Practical, Self-Adaptive Voice Activity Detector for Speaker Verification
 * 		 with Noisy Telephone and Microphone Data' by Tomi Kinnunen and Padmanabhan Rajan
 */
public class MFCCPipeline extends BaseDataProcessor {

	/** MFCC filter pipeline including denoising. */
	final protected FrontEnd frontend;

	/** Buffers incoming unprocessed audio frames to pass down with the processed frames. */
	final protected SingleDataBuffer singleDataBuffer;

	/** Buffers denoised power spectrum frames to calculate the energy from later. */
	final protected SingleDataBuffer denoisedFrameBuffer;


	public MFCCPipeline(double minFreq, double maxFreq, int numFilters) {
		final ArrayList<DataProcessor> pipeline = new ArrayList<DataProcessor>();

		singleDataBuffer = new SingleDataBuffer();
		denoisedFrameBuffer = new SingleDataBuffer();

		pipeline.add(singleDataBuffer);
		pipeline.add(new DiscreteFourierTransform());
		pipeline.add(new Denoise(0.7, 0.995, 0.5));
		pipeline.add(denoisedFrameBuffer);
		pipeline.add(new MelFrequencyFilterBank(minFreq, maxFreq, numFilters));
		pipeline.add(new DiscreteCosineTransform2(numFilters, 12));

		frontend = new FrontEnd(pipeline);
	}

	/**
	 * Creates MFCCPacket objects holding the unprocessed audio frame which served
	 * as basis for the analysis, the denoised MFCC vector and the denoised power
	 * spectrum of the audio frame.
	 *
	 *  The power spectrum is passed down to compute the energy of the frame but
	 *  taking advantage of the denoising. Since the energy is used for sorting
	 *  frames in the training process it is mandatory that the energy value is
	 *  computed on the denoised frame.
	 */
	@Override
	public Data getData() throws DataProcessingException {
		frontend.setPredecessor(getPredecessor());

		final Data buffered = singleDataBuffer.getBufferedData();
		final Data processed = frontend.getData();
		final Data denoised = denoisedFrameBuffer.getBufferedData();

		if (buffered instanceof DoubleData && processed instanceof DoubleData) {
			return new MFCCPacket((DoubleData) buffered, (DoubleData) processed, (DoubleData) denoised);
		}

		return processed;
	}
}
