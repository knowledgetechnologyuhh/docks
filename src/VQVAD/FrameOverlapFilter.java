package VQVAD;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.endpoint.SpeechClassifiedData;

/**
 *
 */
public class FrameOverlapFilter extends BaseDataProcessor {

	protected int frameShift;
	protected int frameLen;
	protected int frameCounter = 0;

	protected long lastSampleNumber = 0;

	public FrameOverlapFilter(int frameLen, int frameShift) {
		this.frameShift = frameShift;
		this.frameLen = frameLen;
	}

	@Override
	public Data getData() throws DataProcessingException {
		Data d = getPredecessor().getData();

		if (d instanceof SpeechClassifiedData) {
			SpeechClassifiedData sd = (SpeechClassifiedData) d;

			int windowLength = (int) (((double)frameLen/1000) * sd.getSampleRate());

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
