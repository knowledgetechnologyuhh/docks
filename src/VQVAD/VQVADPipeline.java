package VQVAD;

import java.util.ArrayList;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.util.AudioFileDataSource;

import edu.cmu.sphinx.frontend.filter.Dither;


public class VQVADPipeline extends BaseDataProcessor {

	protected FrontEnd frontend;

	public VQVADPipeline(AudioFileDataSource src) {
		ArrayList<DataProcessor> pipeline = new ArrayList<DataProcessor>();

		pipeline.add(src);
		pipeline.add(new Dither());
		// TODO: SpectralSubtraction
		pipeline.add(new MixedMelFrequencyFilterBank());
		pipeline.add(new VQVADTrainer());
		pipeline.add(new VQVADClassifier());

		frontend = new FrontEnd(pipeline);
	}

	@Override
	public Data getData() throws DataProcessingException {
		return frontend.getData();
	}

}
