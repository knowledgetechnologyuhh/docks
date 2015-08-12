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
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.util.AudioFileDataSource;
import edu.cmu.sphinx.frontend.window.RaisedCosineWindower;

import edu.cmu.sphinx.frontend.filter.Dither;


/**
 * This class assembles the components of the VQ voice activity detector.
 *
 * It receives an audio input source and outputs audio segments that
 * are marked speech or non-speech.
 *
 * Since it is a BaseDataProcessor it can be used in a Sphinx
 * FrontEnd pipeline. Having elements before this one in the pipeline
 * does not work since every output of previous elements is ignored.
 * This was done to ensure that there's only raw audio received by this
 * element.
 *
 */
public class VQVADPipeline extends BaseDataProcessor {

	protected FrontEnd frontend;

	public VQVADPipeline(AudioFileDataSource src) {
		ArrayList<DataProcessor> pipeline = new ArrayList<DataProcessor>();

		pipeline.add(src);
		pipeline.add(new Dither());
		// TODO: SpectralSubtraction
		pipeline.add(new RaisedCosineWindower(0, 30, 15));
		pipeline.add(new MixedMelFrequencyFilterBank(0, src.getSampleRate()/2, 27));
		pipeline.add(new VQVADTrainer());
		pipeline.add(new VQVADClassifier());

		frontend = new FrontEnd(pipeline);
	}


	/**
	 * Outputs the VQVAD result for the supplied audio source.
	 *
	 * Ignores data from previous elements in the pipeline.
	 */
	@Override
	public Data getData() throws DataProcessingException {
		return frontend.getData();
	}

}
