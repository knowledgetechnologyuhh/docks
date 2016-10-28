package VQVAD;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.endpoint.SpeechClassifiedData;
import edu.cmu.sphinx.frontend.endpoint.SpeechEndSignal;
import edu.cmu.sphinx.frontend.endpoint.SpeechStartSignal;

public class SpeechEndMarker extends BaseDataProcessor {
	protected int speechFrameCount = 0;
	protected int nonspeechFrameCount = 0;
	protected double silenceInMs = 500;
	protected double speechInMs = 180;
	protected boolean started = false;

	@SuppressWarnings("serial")
	protected Data skip = new Data() {};

	public SpeechEndMarker() {
	}

	public SpeechEndMarker(double speechInMs, double silenceInMs) {
		this.speechInMs = speechInMs;
		this.silenceInMs = silenceInMs;
	}

	public Data process(Data d) {
		if (d instanceof DataEndSignal) {
			return new SpeechEndSignal();
		}

		if (!(d instanceof SpeechClassifiedData)) {
			return d;
		}

		SpeechClassifiedData sd = (SpeechClassifiedData) d;

		if (!sd.isSpeech()) {
			nonspeechFrameCount++;
			if (!started) {
				speechFrameCount = 0;
			}
		} else {
			speechFrameCount++;
			nonspeechFrameCount = 0;
		}

		double currentSilenceMs = (double) nonspeechFrameCount * sd.getDoubleData().dimension() / sd.getSampleRate() * 1000;
		double currentSpeechMs = (double) speechFrameCount * sd.getDoubleData().dimension() / sd.getSampleRate() * 1000;

		if (currentSilenceMs >= silenceInMs) {
			started = false;
			return new SpeechEndSignal();
		}

		if (started) {
			return sd.getDoubleData();
		}

		if (currentSpeechMs >= speechInMs) {
			started = true;
			return new SpeechStartSignal();
		}

		return skip;
	}

	@Override
	public Data getData() throws DataProcessingException {
		Data out;
		do {
			out = process(getPredecessor().getData());
		} while (out == skip);
		return out;
	}

}
