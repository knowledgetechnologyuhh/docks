package VQVAD;

import org.apache.commons.collections.buffer.CircularFifoBuffer;

import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DataStartSignal;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.frontend.endpoint.AbstractVoiceActivityDetector;
import edu.cmu.sphinx.frontend.endpoint.SpeechClassifiedData;



public class VoiceActivityDetector extends AbstractVoiceActivityDetector {

	/** 200 frames (assuming each 10ms long) are captured for training by default */
	final static int DEFAULT_FRAME_BUFFER_SIZE = 200;


	/** Indicator used after classification, only global for {@link #isSpeech} */
	protected boolean isSpeech = false;

	/** Buffer that caches frames to train the model on adaptively */
	protected CircularFifoBuffer trainingFrameBuffer;

	/** Buffer to collect enough samples for classification */
	protected CircularFifoBuffer classifyFrameBuffer;

	/** Count of frames newly received (after training) */
	protected int newFrameCount = 0;

	/** Count of samples gathered for classification so far */
	protected int classifySampleCount = 0;

	/** Model that actually classifies speech */
	protected VQVADModel model;


	/**
	 * Defaults to DEFAULT_FRAME_BUFFER_SIZE frames for training.
	 */
	public VoiceActivityDetector() {
		this(DEFAULT_FRAME_BUFFER_SIZE);
	}

	/**
	 * The training frame buffer size governs how much data the training has.
	 * The more data, the better the result (obviously).
	 *
	 * @param trainingFrameBufferSize
	 */
	public VoiceActivityDetector(int trainingFrameBufferSize) {
		trainingFrameBuffer = new CircularFifoBuffer(trainingFrameBufferSize);
		classifyFrameBuffer = new CircularFifoBuffer(trainingFrameBufferSize);
		model = new VQVADModel();
	}


    /**
     * Method that returns if current returned frame contains speech.
     * It could be used by noise filter for example to adjust noise
     * spectrum estimation.
     *
     * @return if current frame is speech
     */
	@Override
	public boolean isSpeech() {
		return isSpeech;
	}

	protected void reset() {
		trainingFrameBuffer.clear();
	}

	/**
	 * A new model should be trained if there is currently no model or
	 * if we captured enough data to train again. Enough data is defined
	 * as one frame buffer length of newly captured frames.
	 *
	 * @return
	 */
	protected boolean shouldTrain() {
		return trainingFrameBuffer.isFull() && (newFrameCount == trainingFrameBuffer.size() || !model.isTrained());
	}

	/**
	 * Speech is classified according to the trained model. If no model
	 * is trained yet, every incoming frame is classified as audio since
	 * we do not know better and we do not want to lose information.
	 *
	 * @param audio
	 * @return
	 */
	protected SpeechClassifiedData classify(DoubleData audio) {
		if (shouldTrain()) {
			// Boxing yay
			DoubleData[] frames = new DoubleData[trainingFrameBuffer.size()];
			Object[] rawFrames = trainingFrameBuffer.toArray();

			for (int i=0; i < trainingFrameBuffer.size(); i++) {
				frames[i] = (DoubleData) rawFrames[i];
			}

			model = VQVADModel.train(frames);
			newFrameCount = 0;
		}

		isSpeech = true;

		if (model.isTrained()) {
			classifyFrameBuffer.add(audio);
			classifySampleCount += audio.dimension();

			if (model.getNecessaryClassificationSampleCount() <= classifySampleCount) {
				DoubleData[] frames = new DoubleData[classifyFrameBuffer.size()];
				Object[] rawFrames = classifyFrameBuffer.toArray();

				for (int i=0; i < classifyFrameBuffer.size(); i++) {
					frames[i] = (DoubleData) rawFrames[i];
				}

				isSpeech = model.classify(frames);

				classifySampleCount = 0;
				classifyFrameBuffer.clear();
			}
		}

		return new SpeechClassifiedData(audio, isSpeech);
	}

    /**
     * Returns the next Data object.
     *
     * @return the next Data object, or null if none available
     * @throws DataProcessingException if a data processing error occurs
     */
    @Override
    public Data getData() throws DataProcessingException {
        Data audio = getPredecessor().getData();

        if (audio instanceof DataStartSignal)
            reset();

        if (audio instanceof DoubleData) {
            DoubleData data = (DoubleData) audio;

            trainingFrameBuffer.add(data);
            newFrameCount++;

            audio = classify(data);
        }

        return audio;
    }

}
