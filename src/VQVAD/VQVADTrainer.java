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

/**
 * Trains VQVADModel objects using audio and MFCC data from previous
 * processing steps in the VQVADPipeline.
 *
 * This processor buffers MFCCPacket objects until enough data is gathered
 * for training. This number defaults to {@value #DEFAULT_FRAME_BUFFER_SIZE}.
 *
 * A new model is trained when the training buffer is full and the number
 * of newly received packets is equal to one buffer length. So each model
 * represents one buffer length of data.
 *
 * Parameters, ordered by importance, are:
 *
 * - trainingBufferSize: How many frames are buffered until training
 * - energyLevel: Minimum energy level for speech
 * - energyFraction: Percent of data used for training speech/non-speech models
 * - vqSize: Size of the cluster center vector used as model
 * - maxKMeansIter: Maximum number of k-means++ iterations
 *
 * Obviously, the number of data in the training buffer is the parameter
 * with the most variance. More data means more information which in turn
 * means better classification results. However, using high buffer values
 * reduces the adaptiveness of the VAD since changes in signal are not
 * learned immediately.
 *
 * The energy level does not influence training, only the resulting model
 * since the model decides if the speech portion is high enough AND the
 * energy is sufficient.
 *
 * The energy fraction is the amount of data that is used from the gathered,
 * sorted by energy data for training the speech and non-speech model.
 * A higher percentage means that more data is used to train each model but
 * it also means that more data of the other kind of data is used, resulting
 * in mis-classification for high values of energyFraction.
 *
 * The vector quantization size dictates the size of the vector of center
 * points when doing K-means which is then used as the respective model.
 * Higher vector sizes result in better capturing outliers but less
 * generalization.
 *
 * Finally, the max. number of k-means iterations may improve clustering
 * results but also results in more computation time.
 *
 */
public class VQVADTrainer extends BaseDataProcessor {

	/** 200 frames (assuming each 10ms long) are captured for training by default */
	final static int DEFAULT_FRAME_BUFFER_SIZE = 200;

	/** Default maximum number of k-means iterations */
	final static int DEFAULT_KMEANS_MAX_ITER = 20;


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

	/** Training is done by generating cluster points from this clusterer */
	protected KMeansPlusPlusClusterer<DoublePoint> clusterer;

	/** Minimum required energy level for the signal to be speech in dB */
	protected double energyMinLevel = -75;


	/**
	 * Create a trainer with default values. Should work fine for most cases.
	 */
	public VQVADTrainer() {
		trainingFrameBuffer = new CircularFifoBuffer(DEFAULT_FRAME_BUFFER_SIZE);
		clusterer = new KMeansPlusPlusClusterer<DoublePoint>(vqSize, DEFAULT_KMEANS_MAX_ITER);
	}

	/**
	 * Create a trained with default values except for the minimum energy level
	 * that gets passed to each trained model. Speech signals must exceed this
	 * energy level to be classified as speech.
	 *
	 * @param energyMinLevel
	 */
	public VQVADTrainer(double energyMinLevel) {
		super();
		this.energyMinLevel = energyMinLevel;
	}

	/**
	 * See the class documentation for a full explanation of the parameters.
	 *
	 * @param trainingBufferSize
	 * @param energyMinLevel
	 * @param energyFraction
	 * @param vqSize
	 * @param maxKMeansIter
	 */
	public VQVADTrainer(int trainingBufferSize, double energyMinLevel, double energyFraction, int vqSize, int maxKMeansIter) {
		this.energyMinLevel = energyMinLevel;
		this.energyFraction = energyFraction;
		this.vqSize = vqSize;

		trainingFrameBuffer = new CircularFifoBuffer(trainingBufferSize);
		clusterer = new KMeansPlusPlusClusterer<DoublePoint>(vqSize, maxKMeansIter);
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

	/**
	 * Gather MFCCPacket objects and invoke {{@link #trainNewModel(DoubleData[], DoubleData[])}
	 * when {@link #shouldTrain()} returns true.
	 *
	 * When a new model is trained, the model is returned as data before getting the
	 * predecessor's data. Otherwise, every incoming packet is forwarded so no data is lost.
	 */
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


			newFrameCount = 0;

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
