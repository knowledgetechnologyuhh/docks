package VQVAD;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.DoublePoint;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;

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

	/** Indicator whether there was already a model trained or not. */
	protected boolean noModelTrainedYet = true;

	/** Codebook size */
	protected int vqSize = 16;

	/** "Fraction of high/low energy frames picked for speech/nonspeech codebook training" */
	protected double energyFraction = 0.1;

	/** Maximum number of k-means iterations */
	protected int maxKmeansIter = 20;

	/** Training is done by generating cluster points from this clusterer */
	protected KMeansPlusPlusClusterer<DoublePoint> clusterer;

	/** Minimum required energy level for the signal to be speech in dB */
	protected double energyMinLevel = -75;


	public VQVADTrainer() {
		trainingFrameBuffer = new CircularFifoBuffer(DEFAULT_FRAME_BUFFER_SIZE);
		clusterer = new KMeansPlusPlusClusterer<DoublePoint>(vqSize, maxKmeansIter);
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

		final List<DoublePoint> nonspeech_mfcc = new ArrayList<DoublePoint>(trainingFragmentSize);
		final List<DoublePoint> speech_mfcc = new ArrayList<DoublePoint>(trainingFragmentSize);

		// first trainingFragmentSize frames are assumed to be nonspeech frames (lowest energy)
		for (int i=0; i < trainingFragmentSize; i++) {
			nonspeech_mfcc.add(new DoublePoint(mfccs[idx[i].intValue()].getValues()));
		}

		// last trainingFragmentSize frames are assumed to be speech frames (highest energy)
		for (int i=nf - trainingFragmentSize; i < nf; i++) {
			speech_mfcc.add(new DoublePoint(mfccs[idx[i].intValue()].getValues()));
		}

		// % Train the speech and nonspeech models from the MFCC vectors corresponding
		// % to the highest and lowest frame energies, respectively
		final DoublePoint[] speech_centroids    = trainCodebook(speech_mfcc);
		final DoublePoint[] nonspeech_centroids = trainCodebook(nonspeech_mfcc);

		return new VQVADModel(speech_centroids, nonspeech_centroids, energyMinLevel);
	}

	/**
	 * Return the cluster center points. Each point is n-dimensional where n is the
	 * number of cepstral coefficients used.
	 *
	 * @param cepstralCoefficients
	 * @return
	 */
	protected DoublePoint[] trainCodebook(List<DoublePoint> cepstralCoefficients) {
		if (cepstralCoefficients.size() < vqSize) {
			throw new IllegalArgumentException("Not enough training data to train model: " +
					"coefficient count " + cepstralCoefficients.size() + " < " + vqSize);
		}

		final List<CentroidCluster<DoublePoint>> centroids = clusterer.cluster(cepstralCoefficients);
		final DoublePoint[] centers = new DoublePoint[centroids.size()];

		int i = 0;
		for (CentroidCluster<DoublePoint> c : centroids) {
			centers[i] = new DoublePoint(c.getCenter().getPoint());
			i++;
		}

		return centers;
	}

}
