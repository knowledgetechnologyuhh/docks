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

public class MFCCPipeline extends BaseDataProcessor {

	final protected FrontEnd frontend;
	protected SingleDataBuffer singleDataBuffer;

	public MFCCPipeline(double minFreq, double maxFreq, int numFilters) {
		final ArrayList<DataProcessor> pipeline = new ArrayList<DataProcessor>();

		singleDataBuffer = new SingleDataBuffer();

		pipeline.add(singleDataBuffer);
		pipeline.add(new DiscreteFourierTransform());
		pipeline.add(new Denoise(0.7, 0.995, 0.5));
		pipeline.add(new MelFrequencyFilterBank(minFreq, maxFreq, numFilters));
		pipeline.add(new DiscreteCosineTransform2(numFilters, 12));

		frontend = new FrontEnd(pipeline);
	}

	@Override
	public Data getData() throws DataProcessingException {
		frontend.setPredecessor(getPredecessor());

		Data buffered = singleDataBuffer.getBufferedData();
		Data processed = frontend.getData();

		if (buffered instanceof DoubleData && processed instanceof DoubleData) {
			return new MFCCPacket((DoubleData) buffered, (DoubleData) processed);
		}

		return processed;
	}
}
