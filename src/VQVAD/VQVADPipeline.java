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
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.endpoint.SpeechClassifiedData;
import edu.cmu.sphinx.frontend.filter.Dither;
import edu.cmu.sphinx.frontend.util.AudioFileDataSource;
import edu.cmu.sphinx.frontend.window.RaisedCosineWindower;


/**
 * This class assembles the components of the VQ voice activity detector.
 *
 * It receives an audio input source and outputs audio segments that
 * are marked speech or non-speech represented as {@link SpeechClassifiedData}.
 *
 * Since it is a {@link BaseDataProcessor} it can be used in a Sphinx
 * FrontEnd pipeline. Having elements before this one in the pipeline
 * does not work since every output of previous elements is ignored.
 * This was done to ensure that there's only raw audio received by this
 * element.
 *
 */
public class VQVADPipeline extends BaseDataProcessor {

	final static public double DEFAULT_LEARNING_RATE = 0.05;

	protected FrontEnd frontend;

	public VQVADPipeline(AudioFileDataSource src) {
		this(src, DEFAULT_LEARNING_RATE);
	}

	/**
	 * The learning rate determines how a newly trained model is merged
	 * with the existing one according to:
	 *
	 * 	 model_values = lambda * new_model_values + (1-lambda) * old_model_values
	 *
	 * That means if the learning rate is 0, no new values are applied and
	 * the model is unchanged. If the learning rate is 1, the new model replaces
	 * the old model. Intermediate values merge the values of both models.
	 *
	 * To disable training, set the learning rate to 0.
	 *
	 * @param src
	 * @param learningRate
	 */
	public VQVADPipeline(AudioFileDataSource src, double learning_rate) {
		ArrayList<DataProcessor> pipeline = new ArrayList<DataProcessor>();

		float frame_length_ms = 30;
		float frame_shift_ms = 10;
		double lower_freq = 0;

		pipeline.add(src);
		pipeline.add(new Dither());
		pipeline.add(new RaisedCosineWindower(0, frame_length_ms, frame_shift_ms));
		pipeline.add(new MFCCPipeline(lower_freq, src.getSampleRate()/2, 27));
		pipeline.add(new VQVADTrainer());
		pipeline.add(new VQVADClassifier(learning_rate));
		pipeline.add(new FrameOverlapFilter(frame_length_ms, frame_shift_ms));
		pipeline.add(new GapSmoothing());
//		pipeline.add(new ClassificationResultDumper("/tmp/vqvad_classification_result"));

		frontend = new FrontEnd(pipeline);
	}


	/**
	 * Outputs the VQVAD result for the supplied audio source.
	 *
	 * Ignores data from previous elements in the pipeline.
	 */
	@Override
	public Data getData() throws DataProcessingException {
		Data d = frontend.getData();

		// This is a crappy workaround for bogus behaviour of
		// Sphinx' AudioFileDataSource which does not send a
		// DataEndSignal all the time. So if null data is reached
		// we send the DataEndSignal ourselves and hope for the best.
		//
		// FIXME: This is fixed in Sphinx4 0.5-prealpha but porting DOCKS involves some more work.
		if (d == null) {
			return new DataEndSignal(42);
		}

		return d;
	}

}
