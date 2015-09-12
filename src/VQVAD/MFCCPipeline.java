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
	final protected SingleDataBuffer singleDataBuffer, denoisedFrameBuffer;

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

	@Override
	public Data getData() throws DataProcessingException {
		frontend.setPredecessor(getPredecessor());

		final Data buffered = singleDataBuffer.getBufferedData();
		final Data processed = frontend.getData();
		final Data denoised = denoisedFrameBuffer.getBufferedData();

		if (buffered instanceof DoubleData && processed instanceof DoubleData) {
			DiscreteFourierTransform idft = new DiscreteFourierTransform(-1, true);
			BaseDataProcessor supplier = (new BaseDataProcessor() {
				@Override
				public Data getData() throws DataProcessingException {
					return denoised;
				}
			});
			idft.setPredecessor(supplier);

			Data d = idft.getData();
			DoubleData dd = (DoubleData) denoised;
			double[] v = dd.getValues();
			for (int i=0; i < v.length; i++) v[i] = Math.sqrt(v[i]);
			return new MFCCPacket((DoubleData) buffered, (DoubleData) processed, dd);
		}

		return processed;
	}
}
