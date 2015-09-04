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

	/** 200 frames are captured for training by default */
	final static int DEFAULT_FRAME_BUFFER_SIZE = 200;

	/** Default maximum number of k-means iterations */
	final static int DEFAULT_KMEANS_MAX_ITER = 20;


	/** Buffer that caches frames to train the model on adaptively */
	protected CircularFifoBuffer trainingFrameBuffer;

	/** Count of frames newly received (after training) */
	protected int newFrameCount = 0;

	/** Codebook size */
	protected int vqSize = 16;

	/** "Fraction of high/low energy frames picked for speech/nonspeech codebook training" */
	protected double energyFraction = 0.1;

	/** Training is done by generating cluster points from this clusterer */
	protected KMeansPlusPlusClusterer<DoublePoint> clusterer;

	/** Minimum required energy level for the signal to be speech in dB */
	protected double energyMinLevel = -75;

	/** State whether the default model should be sent via getData or not. Set in reset() on data begin */
	protected boolean shouldSendInitialModel = false;


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


	void reset() {
		newFrameCount = 0;
		shouldSendInitialModel = true;
	}

	/**
	 * A new model should be trained if there is currently no model or
	 * if we captured enough data to train again. Enough data is defined
	 * as one frame buffer length of newly captured frames.
	 *
	 * @return
	 */
	protected boolean shouldTrain() {
		return trainingFrameBuffer.isFull() && newFrameCount == trainingFrameBuffer.size();
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
		if (shouldSendInitialModel) {
			shouldSendInitialModel = false;
			return getDefaultModel();
		}

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

		if (data instanceof DataStartSignal) {
			reset();
			return data;
		}

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
	public Integer[] sortedEnergyIndices(final double[] energies) {
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
	 * Trains a new model based on the gathered MFCCs.
	 *
	 * The MFCC vectors are sorted according to the energy per frame.
	 * The highest energyFraction number of MFCC vectors are used to train
	 * the speech model while the non-speech model is trained on the
	 * lowest-energy energyFraction number of MFCC vectors.
	 *
	 * @param frames Audio input frames
	 * @param mfccs Cepstral coefficients corresponding to the input frames
	 * @return
	 */
	protected VQVADModel trainNewModel(DoubleData[] frames, DoubleData[] mfccs) {
		final double[] energies = EnergyUtility.computeEnergyPerFrame(frames);

		final Integer[] idx = sortedEnergyIndices(energies);

		final int nf = frames.length;
		final int trainingFragmentSize = roundInt(nf * energyFraction);

		final List<DoublePoint> nonspeech_mfcc = new ArrayList<DoublePoint>(trainingFragmentSize);
		final List<DoublePoint> speech_mfcc = new ArrayList<DoublePoint>(trainingFragmentSize);

		// first trainingFragmentSize frames are assumed to be non-speech frames (lowest energy)
		for (int i=0; i < trainingFragmentSize; i++) {
			nonspeech_mfcc.add(new DoublePoint(mfccs[idx[i].intValue()].getValues()));
		}

		// last trainingFragmentSize frames are assumed to be speech frames (highest energy)
		for (int i=nf - trainingFragmentSize; i < nf; i++) {
			speech_mfcc.add(new DoublePoint(mfccs[idx[i].intValue()].getValues()));
		}

		// % Train the speech and non-speech models from the MFCC vectors corresponding
		// % to the highest and lowest frame energies, respectively
		final DoublePoint[] speech_centroids	= trainCodebook(speech_mfcc);
		final DoublePoint[] nonspeech_centroids = trainCodebook(nonspeech_mfcc);

		//dumpCentroids(speech_centroids, nonspeech_centroids);

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

	/**
	 * Just an utility function to generate the default models seen in getDefaultModel().
	 *
	 * @param speech_centroids
	 * @param nonspeech_centroids
	 */
	protected void dumpCentroids(DoublePoint[] speech_centroids, DoublePoint[] nonspeech_centroids) {
		for (int i=0; i < speech_centroids.length; i++) {
			System.out.print("speech_model[" + i + "] = new DoublePoint(new double[]{");
			for (double v : speech_centroids[i].getPoint()) {
				System.out.print(v + ",");
			}
			System.out.println("});");
		}

		for (int i=0; i < nonspeech_centroids.length; i++) {
			System.out.print("nonspeech_model[" + i + "] = new DoublePoint(new double[]{");
			for (double v : nonspeech_centroids[i].getPoint()) {
				System.out.print(v + ",");
			}
			System.out.println("});");
		}
	}

	/**
	 *
	 * @return
	 */
	protected VQVADModel getDefaultModel() {
		final DoublePoint[] speech_model = new DoublePoint[16];
		final DoublePoint[] nonspeech_model = new DoublePoint[16];

		speech_model[0] = new DoublePoint(new double[]{-66.257087, 23.922137, -29.240720, 11.558031, -15.711847, 2.673890, -7.300918, -16.690770, 0.358239, -3.125369, -6.071106, -3.947746, });
		speech_model[1] = new DoublePoint(new double[]{-86.686111, 38.975888, -19.149106, 3.109304, 0.230376, 1.579103, -10.084873, -10.809500, -3.048159, -6.238269, -7.834217, -8.529160, });
		speech_model[2] = new DoublePoint(new double[]{-59.417812, 22.967271, -26.110079, 5.703296, -11.471308, 2.733179, -10.977457, -14.894308, -1.838123, -2.907765, -5.417400, -4.488642, });
		speech_model[3] = new DoublePoint(new double[]{-78.637902, 29.835458, -30.197570, -5.908746, -3.681953, 6.083711, -7.112498, -16.450927, -10.262909, -7.386568, -4.557021, -5.493957, });
		speech_model[4] = new DoublePoint(new double[]{-55.453186, 19.033169, -28.948440, 11.234282, -22.021636, 3.234136, -15.606631, -10.705779, 0.594128, -5.615076, -4.026538, -4.056525, });
		speech_model[5] = new DoublePoint(new double[]{-67.377238, 27.356853, -31.998195, 12.750788, -17.388750, 5.086354, -10.879501, -15.321807, -0.740174, -3.000090, -5.738178, -6.776679, });
		speech_model[6] = new DoublePoint(new double[]{-76.019579, 34.501934, -29.200866, 3.371861, -6.807366, 7.521572, -10.632998, -13.161105, -6.145513, -2.720957, -2.519553, -3.977040, });
		speech_model[7] = new DoublePoint(new double[]{-103.784192, 36.134965, -2.280360, 11.409800, -14.138300, 8.090554, -8.703832, -5.259920, -6.712584, -9.932951, -5.928623, -8.861757, });
		speech_model[8] = new DoublePoint(new double[]{-78.815081, 28.004038, -19.787477, -1.988922, -21.196926, 9.436384, -11.222247, -14.838502, 0.728396, -1.562416, 0.151549, -8.389121, });
		speech_model[9] = new DoublePoint(new double[]{-76.086269, 30.941594, -28.130899, 7.989741, -3.409723, -0.257404, -10.200134, -9.863711, -4.562282, -3.122898, -6.324242, -6.333844, });
		speech_model[10] = new DoublePoint(new double[]{-84.658750, 30.820808, -23.418405, 0.978571, -0.185124, 1.064641, -14.597459, -8.654061, -6.462755, -6.170563, -6.150640, -7.347359, });
		speech_model[11] = new DoublePoint(new double[]{-73.577996, 26.568358, -24.235554, -1.629211, -24.183957, 12.852134, -13.770452, -14.879839, -0.906410, -3.237822, -0.058147, -8.349141, });
		speech_model[12] = new DoublePoint(new double[]{-78.987859, 27.922701, -20.142723, -7.011477, -24.571778, 8.821903, -10.825134, -14.328051, -7.955682, -8.639239, 2.355408, -4.620296, });
		speech_model[13] = new DoublePoint(new double[]{-90.010961, 29.784884, -6.225518, 9.945682, -10.535341, 6.289012, -14.026019, -5.425150, -3.429088, -8.965420, -6.253052, -9.117891, });
		speech_model[14] = new DoublePoint(new double[]{-80.967109, 30.205279, -25.648406, -1.714289, -26.614381, 11.559728, -12.884748, -12.891537, -5.374172, -3.419228, 1.257717, -7.647485, });
		speech_model[15] = new DoublePoint(new double[]{-80.862946, 32.221055, -18.896838, -11.922687, -2.448317, 5.257888, -0.772585, -6.895289, -12.576547, -9.432923, -2.604836, -3.880847, });

		nonspeech_model[0] = new DoublePoint(new double[]{-357.047997, 24.389627, 3.591398, 8.761987, 0.583841, 4.605026, -1.223113, -2.023430, -2.595956, -2.169173, -1.137753, -1.607378, });
		nonspeech_model[1] = new DoublePoint(new double[]{-371.460610, 14.847418, -1.973209, 4.439398, -3.739810, 1.437902, -2.691266, -4.668350, -3.884956, -3.990607, -1.484965, -3.266963, });
		nonspeech_model[2] = new DoublePoint(new double[]{-242.187695, 4.824776, -1.118666, 3.017516, -17.272111, 21.810141, -8.213012, -4.891097, -2.161724, -10.464717, -4.133560, -11.593537, });
		nonspeech_model[3] = new DoublePoint(new double[]{-241.290157, 22.505199, -5.825505, 8.788106, -9.688581, 7.649946, -6.711140, -5.355648, -6.075780, -3.112433, -6.709030, -8.068759, });
		nonspeech_model[4] = new DoublePoint(new double[]{-255.864759, 14.217998, -4.661912, -2.031366, -12.010995, 4.870726, -14.841522, -3.079454, 10.050390, -4.438053, -1.828090, -18.026530, });
		nonspeech_model[5] = new DoublePoint(new double[]{-344.585263, 25.205471, 3.870895, 7.472562, 4.826768, 5.805197, 1.306946, -2.568679, -4.282840, -5.455670, -1.832752, -1.684311, });
		nonspeech_model[6] = new DoublePoint(new double[]{-339.665263, 20.329182, 2.044520, 9.317042, 1.805414, 10.535514, 0.639413, -0.619985, -6.460992, -12.056664, -3.195968, -3.952323, });
		nonspeech_model[7] = new DoublePoint(new double[]{-337.347746, 21.489167, 0.049338, 13.537192, 0.034200, 7.944541, -0.418534, -3.794419, -9.678574, -11.873982, -1.726618, -7.507554, });
		nonspeech_model[8] = new DoublePoint(new double[]{-288.602965, 24.840310, -4.321896, 7.522614, -7.918882, 6.532189, -10.351529, -1.505274, -1.817706, -0.000158, -3.690760, -9.772216, });
		nonspeech_model[9] = new DoublePoint(new double[]{-262.094315, 27.875453, -6.310709, 16.897743, -12.738212, 8.778231, -2.183458, -14.014870, -7.162143, 2.946465, 1.358982, -9.642782, });
		nonspeech_model[10] = new DoublePoint(new double[]{-331.293384, 24.563094, 5.696384, 15.564212, -1.527766, 5.834701, -3.927110, -8.885752, -7.754307, -7.129621, -2.107295, -8.319539, });
		nonspeech_model[11] = new DoublePoint(new double[]{-367.938565, 14.043137, -3.132238, 6.479272, -3.831076, 0.314709, -2.659163, -1.914666, -2.157909, -4.850466, -2.848356, 0.072384, });
		nonspeech_model[12] = new DoublePoint(new double[]{-274.380155, 15.108867, -2.041976, 3.203189, -8.920141, 9.663704, -11.744228, -4.642995, 3.957800, -3.057226, 1.337889, -11.283411, });
		nonspeech_model[13] = new DoublePoint(new double[]{-294.200933, 24.692577, -4.679451, 6.080939, -6.522726, 0.441747, -4.110078, -4.700634, -6.485600, 0.249612, 0.463274, -4.138112, });
		nonspeech_model[14] = new DoublePoint(new double[]{-285.689413, 19.110715, -1.730406, 1.067525, -5.890680, 5.802432, -5.456561, -4.294478, -1.928728, -5.947941, -2.617723, -8.760291, });
		nonspeech_model[15] = new DoublePoint(new double[]{-298.201228, 30.780776, -4.868077, 8.970700, -5.091068, 2.782379, -0.811419, -0.468765, -4.426155, 2.124989, 0.033874, -6.896625, });

		return new VQVADModel(speech_model, nonspeech_model, energyMinLevel);
	}

}
