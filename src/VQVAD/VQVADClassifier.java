package VQVAD;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DataStartSignal;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.frontend.endpoint.SpeechClassifiedData;

public class VQVADClassifier extends BaseDataProcessor {

	protected VQVADModel currentModel;

	void reset() {}

	/**
	 * Defaults to a speech classification to avoid data loss if no model
	 * is trained yet.
	 *
	 * @param frame
	 * @return
	 */
	protected SpeechClassifiedData classify(DoubleData frame, DoubleData mfcc) {
		boolean isSpeech = true;

		if (currentModel != null) {
			isSpeech = currentModel.classify(new DoubleData[]{frame});
		}

		return new SpeechClassifiedData(frame, isSpeech);
	}

    @Override
    public Data getData() throws DataProcessingException {
         Data data = getPredecessor().getData();

        if (data instanceof DataStartSignal)
            reset();

        if (data instanceof VQVADModel)
        	currentModel = (VQVADModel) data;

        if (data instanceof MFCCPacket) {
        	// Audio data
        	MFCCPacket packet = (MFCCPacket) data;
            return classify(packet.getAudioFrame(), packet.getMFCC());
        }

        return data;
    }

}
