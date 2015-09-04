package VQVAD;

import java.util.ArrayList;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.denoise.Denoise;
import edu.cmu.sphinx.frontend.frequencywarp.MelFrequencyFilterBank;
import edu.cmu.sphinx.frontend.transform.DiscreteCosineTransform;
import edu.cmu.sphinx.frontend.transform.DiscreteCosineTransform2;
import edu.cmu.sphinx.frontend.transform.DiscreteFourierTransform;
import edu.cmu.sphinx.util.props.S4Double;
import edu.cmu.sphinx.util.props.S4Integer;

public class MFCCPipeline extends BaseDataProcessor {

	final protected FrontEnd frontend;
	protected SingleDataBuffer singleDataBuffer;

	public MFCCPipeline(double minFreq, double maxFreq, int numFilters) {
		final ArrayList<DataProcessor> pipeline = new ArrayList<DataProcessor>();

		singleDataBuffer = new SingleDataBuffer();

		Denoise denoise;
		// Load default configuration for Denoise component.
		try {
			denoise = new Denoise(
				Denoise.class.getField("LAMBDA_POWER")
				        .getAnnotation(S4Double.class)
				        .defaultValue(),
				Denoise.class.getField("LAMBDA_A")
				        .getAnnotation(S4Double.class)
				        .defaultValue(),
				Denoise.class.getField("LAMBDA_B")
				        .getAnnotation(S4Double.class)
				        .defaultValue(),
				Denoise.class.getField("LAMBDA_T")
				        .getAnnotation(S4Double.class)
				        .defaultValue(),
				Denoise.class.getField("MU_T")
				        .getAnnotation(S4Double.class)
				        .defaultValue(),
				Denoise.class.getField("EXCITATION_THRESHOLD")
				        .getAnnotation(S4Double.class)
				        .defaultValue(),
				Denoise.class.getField("MAX_GAIN")
				        .getAnnotation(S4Double.class)
				        .defaultValue(),
				Denoise.class.getField("SMOOTH_WINDOW")
				        .getAnnotation(S4Integer.class)
				        .defaultValue());
		} catch (NoSuchFieldException e) {
			throw new RuntimeException("Programming error: field does not exist. Original exception:" + e.getMessage());
		} catch (SecurityException e) {
			throw new RuntimeException("VM does not allow access to our own annotations. Original exception:" + e.getMessage());
		}

		pipeline.add(singleDataBuffer);
		pipeline.add(new DiscreteFourierTransform());
		pipeline.add(new MelFrequencyFilterBank(minFreq, maxFreq, numFilters));
		pipeline.add(denoise);
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
