package VQVAD;

import java.util.Iterator;

import org.apache.commons.collections.buffer.CircularFifoBuffer;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.endpoint.SpeechClassifiedData;

/**
 * Since VQVAD is quite good at discerning noise and speech there
 * are bound to be gaps between utterances due to speech pauses.
 *
 * This filter attempts to smooth gaps of a configurable width.
 */
public class GapSmoothing extends BaseDataProcessor {

	public final static int DEFAULT_GAP_WIDTH_IN_FRAMES = 10;

	protected int gapWidthInFrames = DEFAULT_GAP_WIDTH_IN_FRAMES;
	protected CircularFifoBuffer frameBuffer;

	@SuppressWarnings("serial")
	protected Data skip = new Data() {};

	protected boolean dataEndSeen = false;
	protected DataEndSignal des = null;

	public GapSmoothing(int gapWidthInFrames) {
		this.gapWidthInFrames = gapWidthInFrames;
		frameBuffer = new CircularFifoBuffer(gapWidthInFrames+1);
	}

	public GapSmoothing() {
		this(DEFAULT_GAP_WIDTH_IN_FRAMES);
	}

	@Override
	public Data getData() throws DataProcessingException {
		Data d;
		do {
			d = process(getPredecessor().getData());
		} while (d == skip);
		return d;
	}

	protected Data process(Data d) {
		if (d instanceof DataEndSignal) {
			dataEndSeen = true;
			des = (DataEndSignal) d;
		}

		// Drain buffer and emit DataEndSignal.
		if (dataEndSeen) {
			if (frameBuffer.size() > 0) {
				SpeechClassifiedData v = (SpeechClassifiedData) frameBuffer.get();
				frameBuffer.remove();
				return v;
			} else {
				return des;
			}
		}

		if (!(d instanceof SpeechClassifiedData)) {
			return d;
		}

		SpeechClassifiedData sd = (SpeechClassifiedData) d;
		frameBuffer.add(sd);

		// Return early as we cannot do smoothing without a full buffer.
		if (!frameBuffer.isFull()) {
			return skip; //return (SpeechClassifiedData) frameBuffer.get();
		}

		@SuppressWarnings("unchecked")
		Iterator<SpeechClassifiedData> iterator = frameBuffer.iterator();

		// First and second frame forming a potential gap of size gapWidthInFrames.
		SpeechClassifiedData first, second;

		first = iterator.next();

		for (int i=gapWidthInFrames-1; i > 0; i--) {
			iterator.next();
		}

		second = iterator.next();

		// Both are speech. That means there is a small enough gap so mark all frames in between as speech.
		if (first.isSpeech() && second.isSpeech()) {
			fillGap();
		}

		return (SpeechClassifiedData) frameBuffer.get();
	}

	protected void fillGap() {
		@SuppressWarnings("unchecked")
		Iterator<SpeechClassifiedData> iterator = frameBuffer.iterator();

		for (int i=gapWidthInFrames; i > 0; i--) {
			SpeechClassifiedData sd = iterator.next();
			sd.setSpeech(true);
		}
	}

}
