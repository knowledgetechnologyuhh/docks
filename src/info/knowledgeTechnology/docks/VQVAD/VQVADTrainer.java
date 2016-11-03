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
package info.knowledgeTechnology.docks.VQVAD;

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
 * A new model is trained when the training buffer contains at least
 * {@value #DEFAULT_MIN_FRAME_NUMBER} of newly received packets. The model
 * is trained on all values in the buffer and not only the newly received
 * packets.
 *
 * Parameters, ordered by importance, are:
 *
 * - minFrameCount: How many frames are needed for one training
 * - trainingBufferSize: How many frames can be buffered in total
 * - energyFraction: Percent of data used for training speech/non-speech models
 * - energyLevel: Minimum energy level for speech, passed down to model
 * - vqSize: Size of the cluster center vector used as model
 * - maxKMeansIter: Maximum number of k-means++ iterations
 *
 * Since training works better if there is more data the minimum frame count
 * and the training buffer size are the most important training parameters.
 * More data means more information which in turn means better classification
 * results. However, using high minimum frame count values reduces the
 * adaptiveness of the VAD since changes in signal are not learned immediately.
 * The training buffer size can be increased with less care since it only
 * affects the amount of data available when training. A risk when increasing
 * the frame buffer size is that high energy noise is longer present and may
 * gradually worsen the classifiers.
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

	/** The number of frames captured for training by default */
	final static int DEFAULT_FRAME_BUFFER_SIZE = 800;

	/** The default minimum number of frames that are needed for training */
	final static int DEFAULT_MIN_FRAME_NUMBER = 400;

	/** Default maximum number of k-means iterations */
	final static int DEFAULT_KMEANS_MAX_ITER = 20;


	/** Buffer that caches frames to train the model on adaptively */
	protected CircularFifoBuffer trainingFrameBuffer;

	/** Count of frames newly received (after training) */
	protected int newFrameCount = 0;

	/** Codebook size */
	protected int vqSize = 16;

	/** "Fraction of high/low energy frames picked for speech/nonspeech codebook training" */
	protected double energyFraction = 0.10;

	/** Training is done by generating cluster points from this clusterer */
	protected KMeansPlusPlusClusterer<DoublePoint> clusterer;

	/** Minimum required energy level for the signal to be speech in dB */
	// FIXME: energy does not appear to be negative. Investigate.
	protected double energyMinLevel = -75;

	/** State whether the default model should be sent via getData or not. Set in reset() on data begin */
	protected boolean shouldSendInitialModel = false;

	/** Minimum number of frames that are needed to start training */
	protected int minFrameCount = DEFAULT_MIN_FRAME_NUMBER;

	/** Model to use if no model is trained yet. If no model is set from the outside a default one will be loaded. */
	protected VQVADModel defaultModel;


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
	public VQVADTrainer(int trainingBufferSize, int minFrameCount, double energyMinLevel, double energyFraction, int vqSize, int maxKMeansIter) {
		this.minFrameCount = minFrameCount;
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
		return newFrameCount == minFrameCount;
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

				frames[i] = packet.getDenoisedAudioFrame();
				mfccs[i] = packet.getMFCC();
			}

			newFrameCount = 0;

			return trainNewModel(frames, mfccs);
		}

		final Data data = getPredecessor().getData();

		if (data instanceof DataStartSignal) {
			reset();
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
	protected VQVADModel trainNewModel(final DoubleData[] frames, final DoubleData[] mfccs) {
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

		// dumpCentroids(speech_centroids, nonspeech_centroids);

		return new VQVADModel(speech_centroids, nonspeech_centroids, energyMinLevel);
	}

	/**
	 * Return the cluster center points. Each point is n-dimensional where n is the
	 * number of cepstral coefficients used.
	 *
	 * @param cepstralCoefficients
	 * @return
	 */
	protected DoublePoint[] trainCodebook(final List<DoublePoint> cepstralCoefficients) {
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
	protected void dumpCentroids(final DoublePoint[] speech_centroids, final DoublePoint[] nonspeech_centroids) {
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

	public void setDefaultModel(VQVADModel model) {
		defaultModel = model;
	}

	public VQVADModel getDefaultModel() {
		if (defaultModel == null) {
			defaultModel = loadDefaultModel();
		}

		return defaultModel;
	}

	/**
	 *
	 * @return
	 */
	protected VQVADModel loadDefaultModel() {
		final DoublePoint[] speech_model = new DoublePoint[16];
		final DoublePoint[] nonspeech_model = new DoublePoint[16];

		speech_model[0] = new DoublePoint(new double[]{73.43083624965422,10.394350631608269,-10.922286605444967,6.041793574840437,-4.843765073283623,-5.855214363561233,2.3974732714743507,1.2644508519803743,1.4751282685680485,-1.1309810719727613,-4.857724048877473,-2.898994583337023,});
		speech_model[1] = new DoublePoint(new double[]{77.39919895132662,-2.141709286704863,-11.935665301014863,-2.863705119433309,-6.492530673062442,-1.838327633921583,-1.7233369410092092,-0.03394015173181311,-5.3588137468716415,-0.9534757837685232,-5.300987444793364,0.08175187277175609,});
		speech_model[2] = new DoublePoint(new double[]{66.95585531034753,2.916076432326996,-15.374974043698195,-0.5047107251369654,-7.185558590648824,-6.33048297323252,-3.8378539918538728,1.051370657014462,-5.222024522165167,2.5887499376735326,0.21651877149157842,2.6637044226166546,});
		speech_model[3] = new DoublePoint(new double[]{70.63274734779378,3.4242984114203723,-5.496537018410478,-5.329171788155094,-8.073360001143918,-5.080132067175988,-2.361651263201399,2.4147097231608186,5.981234578303595,-0.40236004983896034,-2.367050837327154,-1.3282448897397752,});
		speech_model[4] = new DoublePoint(new double[]{83.30941778609657,2.657197027831244,-9.254744549431223,-2.020259317380732,-0.16036894772951135,-2.3574760275847595,1.6289166639285044,1.3984516816343966,-3.3427716868999737,3.0379946775269517,-2.9874108200276708,-0.4190866436641494,});
		speech_model[5] = new DoublePoint(new double[]{62.660403615024194,2.652702402400599,-1.9789357230014375,-4.362137297484496,-11.354437051668988,-0.004084220427918043,1.2223819536202154,2.9433254946995233,2.44652698565597,2.132744892473416,-2.1518044863191204,-0.8799275119776353,});
		speech_model[6] = new DoublePoint(new double[]{69.9722461608389,3.5240379733710454,-7.186403311907938,-4.701511782637894,-6.965965648109705,-3.991844920399367,0.2069908929365126,1.9986008456033901,0.2979883420846205,2.3460326159874816,-2.440654102307375,0.07203619631219571,});
		speech_model[7] = new DoublePoint(new double[]{81.29616038365127,4.510322398983529,-10.046484027487335,2.723752514283473,-6.091290196558541,0.39206673508261114,1.3411123465355734,-0.46849895644123296,0.7500464993281608,-0.6745376279897308,-0.485514299313068,-3.613221296448563,});
		speech_model[8] = new DoublePoint(new double[]{58.57347255561829,6.6684476045544505,-11.57842866757116,-1.791025146575453,-2.443383821527581,-6.370755635219029,-4.146290575550923,6.3194223081134595,0.548033499906991,-3.3482777703743674,-1.975876533065628,1.138325726974016,});
		speech_model[9] = new DoublePoint(new double[]{78.70802713229017,9.683851869144991,-8.711463049561159,0.9786264079595415,0.4783367759397741,-4.5090383344611205,5.648818985330292,0.5460954607013858,-1.9080077514329905,0.07591863941972998,-3.406288516879642,0.5752585063898019,});
		speech_model[10] = new DoublePoint(new double[]{76.7339895760351,-3.885140913195335,-5.73225693604998,-5.472950060200319,-5.695284358207559,4.119245637366164,-1.26615601244316,1.8358068388955189,-0.17336438429057235,4.173078137404011,-3.6692889436898395,0.7586738655641303,});
		speech_model[11] = new DoublePoint(new double[]{69.12405112819687,4.597752077637453,-12.14203926297671,-1.8723132150379782,-9.73773537655708,-6.09876646727483,-1.1628728447667522,4.2766163529605175,-4.712920924817231,4.9708276967977,0.7171073728096076,2.5028040681836075,});
		speech_model[12] = new DoublePoint(new double[]{72.27778042416179,1.3660131805201488,-9.93641899838623,-1.3360295981383383,-7.186108503214507,-6.433648069943134,1.740317131071396,-0.7953339221150932,2.5371754921900065,-1.9226155460221774,-5.708045382066981,-5.714850137016267,});
		speech_model[13] = new DoublePoint(new double[]{82.73247396580693,3.8160634804857843,-9.678064046738156,1.1638310311317988,-5.363791051784244,2.113682890381652,0.7984569305171823,2.8840517383625275,-4.0939881176161,2.8451168967162466,-5.0315814195477255,1.3293421145991657,});
		speech_model[14] = new DoublePoint(new double[]{67.87875540146138,12.885823042522675,-10.56026885220689,-0.04252633430368746,1.0366219409549269,-5.6307380643309095,-0.6095888719375,5.160453645956256,-1.4485639868316942,-2.594479364597065,-1.0008958902023046,-1.2761456324106255,});
		speech_model[15] = new DoublePoint(new double[]{78.60267253690762,4.189832070867091,-6.839950173004546,-3.5094166712613672,-4.525005070436086,-2.4704185376299947,2.581812210848002,0.21252827112456443,-0.20009564986797446,2.2610857057042573,-2.713214501894697,1.2527104179845379,});
		nonspeech_model[0] = new DoublePoint(new double[]{40.05028751266598,0.7577281988658138,-4.984768105821941,-3.09966860836316,-6.088555609124367,0.26998971337362676,-2.43886576831134,0.987872149280142,-0.8875863795722814,0.4943932858771595,-0.39852445785511087,0.9646228138562436,});
		nonspeech_model[1] = new DoublePoint(new double[]{44.11704630276205,5.508220541214067,-1.6341707218924122,-0.9802828381938439,-5.638009660535506,-0.22158822879964513,-2.3120916933062077,1.376968660599894,-0.4104837478048738,1.0844832037530296,-0.6872684345377797,0.18413959094719273,});
		nonspeech_model[2] = new DoublePoint(new double[]{44.31985442683958,0.07389958084984413,-4.324830830490983,-2.192262619322844,-5.727152797614905,-0.40996717759698176,-2.1584821321990044,-0.7989026235737251,0.009210768230454478,0.3698031988098397,0.44417017356731814,0.6849230925665258,});
		nonspeech_model[3] = new DoublePoint(new double[]{49.01159430663755,2.9255393713074,0.10031490615135358,-5.0444613590038045,-7.0805794641994195,-1.982126268884138,-3.3935007014640237,1.9219569962723033,0.16765510715456272,3.606292062374173,-0.10467228076179601,1.9073099925053487,});
		nonspeech_model[4] = new DoublePoint(new double[]{45.378103093026986,4.413280270290279,-3.6690654202958566,-1.2285407607381709,-6.286624527414786,0.14511524216714508,-1.5478717329826666,1.8552970500466417,-0.16240789544024126,1.3656174080697732,-0.16933585962228384,1.2713254423033433,});
		nonspeech_model[5] = new DoublePoint(new double[]{44.89234862180845,1.6155188309544963,-4.503847625939277,-2.743592119634072,-4.471962526697005,0.45570439429034787,-1.7993111082646602,0.797567436812368,-0.5037600872950979,0.8250519643012266,-0.756718283522975,0.01610169447950021,});
		nonspeech_model[6] = new DoublePoint(new double[]{41.02878519412142,1.8547812084742556,-3.264954679366893,-1.8536329557017122,-4.942407812503128,1.1789857793566594,-1.328634844088294,1.6933115145228856,-0.49616552388645846,0.3113415148907597,-0.6681989407204186,0.8426246567450213,});
		nonspeech_model[7] = new DoublePoint(new double[]{44.84393298019154,3.1187146430748283,-4.495008797924867,-3.879128816198538,-5.326548761947693,1.69221392357861,-1.5215746613901207,-1.6127595723981683,-2.4091322545050313,0.8421685773947484,1.0840644891699194,0.7618870490744898,});
		nonspeech_model[8] = new DoublePoint(new double[]{44.712911200549456,1.1431050455303537,-5.987933407715146,1.1905251602494977,-7.746464482641689,-0.12896224777206486,-1.1137673653308477,-2.7642333473958365,-1.266941852465987,0.4868261391029499,-2.283372025543442,0.3824250888033033,});
		nonspeech_model[9] = new DoublePoint(new double[]{44.12534247631106,4.995098316505264,-2.4367448661488416,-3.7292434294023735,-9.751195224260062,-3.0726243073316715,-0.3142741261591395,0.21243567642782826,-0.26430807226623243,-0.19570660611931223,-1.7360651879011102,-1.1497572515331371,});
		nonspeech_model[10] = new DoublePoint(new double[]{49.5302837557189,-0.2514488693744196,-7.4215743150577955,-1.0731399506959396,-6.237510864842569,-4.289251708064733,-0.6430626741508831,1.7733113130269198,-3.1133911241275505,0.6352240139771922,0.3823346731929525,-0.798051238580764,});
		nonspeech_model[11] = new DoublePoint(new double[]{46.861761197808654,2.036717484169827,-3.4441256832058564,-1.4619304873851435,-4.710489580534686,0.23831190065977678,-2.244349269903903,-0.020313770828357593,-0.49124575476228294,1.4215028908374525,-0.5394156982530398,0.21259157522070665,});
		nonspeech_model[12] = new DoublePoint(new double[]{40.097360961818,0.27561598811211363,-3.9562124489807458,-3.2057243004886304,-5.674825861200982,1.2259060999941969,-0.8979964223119116,2.2586806552060534,0.05827175210511516,0.7086244425965842,-0.7676289663160469,0.5086614161511221,});
		nonspeech_model[13] = new DoublePoint(new double[]{44.64654305361995,3.1102389785735642,-3.5385472837357965,-1.9743383659414364,-5.651371065934722,-0.061531774597738384,-1.8909917546570303,0.31214907981405726,-0.29089006064989964,0.5595279446773291,-0.6298948361995648,0.5758909827301448,});
		nonspeech_model[14] = new DoublePoint(new double[]{48.34088343341446,0.7434668586108006,-2.831935565167714,-0.09586415169421628,-5.587998082783896,2.6727061550324676,-1.6231244903595052,1.2551262191065438,1.0849474973723072,0.11966134236183783,0.7287484443809323,0.3414633188531112,});
		nonspeech_model[15] = new DoublePoint(new double[]{40.365920023027286,1.78440962909333,-3.0696854377846448,-2.312937614327543,-5.435623789671233,0.43012496492996705,-2.095738943703519,1.2429952476585493,-0.20426305219889293,0.9664974358255904,-0.4092099766586026,0.5328530071032047,});

		return new VQVADModel(speech_model, nonspeech_model, energyMinLevel);
	}

}
