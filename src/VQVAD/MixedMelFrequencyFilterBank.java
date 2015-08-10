package VQVAD;

import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.frontend.frequencywarp.MelFrequencyFilterBank;

public class MixedMelFrequencyFilterBank extends MelFrequencyFilterBank {

	public MixedMelFrequencyFilterBank(double minFreq, double maxFreq, int numFilters) {
		super(minFreq, maxFreq, numFilters);
	}

	@Override
	public Data getData() {
		Data mfccData = super.getData();
		Data originalData = this.getPredecessor().getData();

		if (mfccData instanceof DoubleData && originalData instanceof DoubleData) {
			return new MFCCPacket((DoubleData) originalData, (DoubleData) mfccData);
		}

		return mfccData;
	}
}
