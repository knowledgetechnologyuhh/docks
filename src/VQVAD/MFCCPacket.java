package VQVAD;

import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DoubleData;

public class MFCCPacket implements Data {

	private static final long serialVersionUID = -8383714866669035326L;

	protected DoubleData audio;
	protected DoubleData mfcc;

	public MFCCPacket(DoubleData audio, DoubleData mfcc) {
		this.audio = audio;
		this.mfcc = mfcc;
	}

	public DoubleData getAudioFrame() {
		return audio;
	}

	public DoubleData getMFCC() {
		return mfcc;
	}

}
