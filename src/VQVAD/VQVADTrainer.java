package VQVAD;

import java.util.Arrays;
import java.util.Comparator;

import org.apache.commons.collections.buffer.CircularFifoBuffer;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DataStartSignal;
import edu.cmu.sphinx.frontend.DoubleData;

public class VQVADTrainer extends BaseDataProcessor {

	/** 200 frames (assuming each 10ms long) are captured for training by default */
	final static int DEFAULT_FRAME_BUFFER_SIZE = 200;


	/** Buffer that caches frames to train the model on adaptively */
	protected CircularFifoBuffer trainingFrameBuffer;

	/** Count of frames newly received (after training) */
	protected int newFrameCount = 0;

	protected boolean noModelTrainedYet = true;

	/** Codebook size */
	protected int vqSize = 16;

	/** "Fraction of high/low energy frames picked for speech/nonspeech codebook training" */
	protected double energyFraction = 0.1;


	public VQVADTrainer() {
		trainingFrameBuffer = new CircularFifoBuffer(DEFAULT_FRAME_BUFFER_SIZE);
	}


	void reset() {}

	/**
	 * A new model should be trained if there is currently no model or
	 * if we captured enough data to train again. Enough data is defined
	 * as one frame buffer length of newly captured frames.
	 *
	 * @return
	 */
	protected boolean shouldTrain() {
		return trainingFrameBuffer.isFull() && (newFrameCount == trainingFrameBuffer.size() || noModelTrainedYet);
	}

    @Override
    public Data getData() throws DataProcessingException {
    	if (shouldTrain()) {
			final DoubleData[] frames = new DoubleData[trainingFrameBuffer.size()];
			final DoubleData[] mfccs = new DoubleData[trainingFrameBuffer.size()];
			final Object[] packets = trainingFrameBuffer.toArray();

			for (int i=0; i < trainingFrameBuffer.size(); i++) {
				final MFCCPacket packet = (MFCCPacket) packets[i];

				frames[i] = packet.getAudioFrame();
				mfccs[i] = packet.getMFCC();
			}

    		return trainNewModel(frames, mfccs);
    	}

        final Data data = getPredecessor().getData();

        if (data instanceof DataStartSignal)
            reset();

        if (data instanceof MFCCPacket) {
            trainingFrameBuffer.add(data);
            newFrameCount++;
        }

        return data;
    }


    /**
     * Matlab equivalent of [~,idx] = sort(energies)
     *
     * @param energies
     * @return
     */
    protected Integer[] sortedEnergyIndices(final double[] energies) {
    	final Integer[] idx = new Integer[energies.length];

		for(int i=0; i < energies.length; i++)
			idx[i] = i;

		Arrays.sort(idx, new Comparator<Integer>() {
		    @Override public int compare(final Integer o1, final Integer o2) {
		        return Double.compare(energies[o1], energies[o2]);
		    }
		});

		return idx;
    }

    protected int roundInt(double x) {
    	return (int) Math.floor(x + 0.5);
    }

    /**
     *
     * @param frames Audio input frames
     * @param mfccs Cepstral coefficients corresponding to the input frames
     * @return
     */
	protected VQVADModel trainNewModel(DoubleData[] frames, DoubleData[] mfccs) {
		noModelTrainedYet = false;

		final double[] energies = EnergyUtility.computeEnergyPerFrame(frames);

		final Integer[] idx = sortedEnergyIndices(energies);

		final int nf = frames.length;
		final int trainingFragmentSize = roundInt(nf * energyFraction);
		final int mfccFeatureSize = mfccs[0].dimension();

		final double[][] nonspeech_mfcc = new double[trainingFragmentSize][mfccFeatureSize];
		final double[][] speech_mfcc = new double[trainingFragmentSize][mfccFeatureSize];

		// first trainingFragmentSize frames are nonspeech frames
		// nonspeech_frames  = frame_idx(1:round(nf * params.energy_fraction));
		for (int i=0; i < nonspeech_mfcc.length; i++)
			nonspeech_mfcc[i] = mfccs[idx[i].intValue()].getValues();

		// last trainingFragmentSize frames are speech frames
		for (int i=nf - trainingFragmentSize; i < nf; i++)
			speech_mfcc[i] = mfccs[idx[i].intValue()].getValues();

		// % Train the speech and nonspeech models from the MFCC vectors corresponding
		// % to the highest and lowest frame energies, respectively
		final double[] speech_model    = trainCodebook(speech_mfcc);
		final double[] nonspeech_model = trainCodebook(nonspeech_mfcc);

		// TODO
		return null;
	}

	protected double[] trainCodebook(double[][] cepstralCoefficients) {
		if (cepstralCoefficients.length < vqSize) {
			throw new RuntimeException("Not enough training data to train model.");
		}

		// TODO: [C,junk] = my_kmeans(X, params.vq_size, params.max_kmeans_iter);

		return null;
	}

}
