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

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.frontend.endpoint.SpeechClassifiedData;

/**
 * Classification element in the VQVAD classifier pipeline.
 *
 * Awaits trained VQVADModel models from a VQVADTrainer element.
 * As soon as a model is received, incoming audio frames are
 * fed to the model and audio is classified according to the
 * model's prediction.
 *
 * If no model is trained yet, the classification defaults to
 * speech to avoid data loss.
 *
 */
public class VQVADClassifier extends BaseDataProcessor {

	protected VQVADModel currentModel;

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
			isSpeech = currentModel.classify(frame, mfcc);
		}

		return new SpeechClassifiedData(frame, isSpeech);
	}

    @Override
    public Data getData() throws DataProcessingException {
         Data data = getPredecessor().getData();

        if (data instanceof VQVADModel) {
        	currentModel = (VQVADModel) data;
        }

        if (data instanceof MFCCPacket) {
        	// Audio data
        	MFCCPacket packet = (MFCCPacket) data;
            return classify(packet.getAudioFrame(), packet.getMFCC());
        }

        return data;
    }

}
