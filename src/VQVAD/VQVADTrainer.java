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

	/**
	 *
	 * @return
	 */
	protected VQVADModel getDefaultModel() {
		final DoublePoint[] speech_model = new DoublePoint[16];
		final DoublePoint[] nonspeech_model = new DoublePoint[16];

		if (true) {
			speech_model[0] = new DoublePoint(new double[]{65.41652252583785,3.553472044624467,-2.8772089257636777,-3.7689075206326015,-14.025824492338732,-1.3651457115708172,2.42049202346575,3.186195770288153,2.0560785259510044,1.4281806753718547,-2.877795784875151,-0.8896418986589563,});
			speech_model[1] = new DoublePoint(new double[]{81.55705281782397,2.265571904039331,-8.219565128163437,-2.627998870543715,-1.69362250651429,-0.9921126512540087,1.2359304114296146,1.204608295969833,-2.345706379023985,2.8628580757003648,-3.1411850884911576,0.13139981890169689,});
			speech_model[2] = new DoublePoint(new double[]{77.2133347610015,-1.7173375409142526,-12.970915828255897,-2.236135438756269,-7.609356354189036,-1.0046166783145805,-2.258540020176239,0.9220544760166135,-5.737905298887777,0.26872196736701853,-5.501015090894695,1.066650111157536,});
			speech_model[3] = new DoublePoint(new double[]{62.84286117169526,10.013866791453193,-11.94982838287201,-0.5400876585829872,-0.9362801791818866,-5.369926202946569,-2.4598145742660367,6.559667152104867,-0.003415106776570309,-2.1399248925834966,-1.271555087303172,0.4230101524539124,});
			speech_model[4] = new DoublePoint(new double[]{57.77289703985154,19.23278582115165,7.6715481866295905,4.097088428231059,2.133539673462442,1.7055424144248483,0.14095379994034246,-0.7955093607500532,-1.8230523565982566,-1.462095217081572,1.393402398177368,2.958340261872693,});
			speech_model[5] = new DoublePoint(new double[]{74.08023389782512,8.571507195874812,-10.330169207331652,5.1301480227558915,-6.9294389383428285,-3.698022820974512,2.7006292733328574,1.865220322784675,1.7242433242387147,-0.3330782659385744,-4.242614956543686,-2.4851832392405133,});
			speech_model[6] = new DoublePoint(new double[]{73.76776556442294,10.705517517208595,-7.34895063846536,4.150485860432657,-3.1337585905098804,-2.6835575935766394,-0.6288967017190195,2.1471730879074364,-5.310633868149367,2.8060356777508164,-0.375160856507061,0.9055424925276726,});
			speech_model[7] = new DoublePoint(new double[]{80.95006930263625,5.017049602590639,-11.260964554688808,2.8279877553408985,-7.244883916430173,1.7767897194159872,0.09458588853102472,1.4943594321684013,-1.8702781622588134,0.8392005888557641,-2.6829286599364486,-1.6761567765701522,});
			speech_model[8] = new DoublePoint(new double[]{67.85834503442273,4.572577306875538,-13.706020564199918,-2.1305891065493,-9.142308818545933,-4.925181293127589,-1.0580983643705146,3.4578736189150976,-4.712759559751421,3.108081354552164,-0.8611811585138477,1.8934919339966252,});
			speech_model[9] = new DoublePoint(new double[]{70.8520611251364,3.2175547432748366,-6.2593756604027115,-4.167251771104312,-8.737651243994414,-4.4784723702452816,-2.607782882500514,3.106864192367127,3.7732567173090543,1.1913688134891143,-2.9746954040695637,-1.2162604335861362,});
			speech_model[10] = new DoublePoint(new double[]{71.39099829062198,-0.9842131720989591,-13.720551887338118,-7.722620460493536,-3.5915918525669737,-3.5674366655903427,2.8876385445405557,0.41733479917536687,-3.2752839440727595,1.247742582022036,-6.4575748323008835,1.1435687293025616,});
			speech_model[11] = new DoublePoint(new double[]{68.45696066828249,1.057408953386214,-8.768296585766597,-3.3356957127673925,-6.404401909130121,-6.106258564586389,1.6115617289366835,2.4780835067542726,1.7258266498899975,0.14612550197604565,-6.117058428332745,-5.587408259310963,});
			speech_model[12] = new DoublePoint(new double[]{78.10803893742745,3.557735075244983,-10.991538417857258,2.0881555452482066,-7.314023219707419,-3.944890112749198,1.3290145995649558,-3.2675119305365357,2.6542740480194063,-2.6417281855509898,-2.7492045684858226,-5.541056286669218,});
			speech_model[13] = new DoublePoint(new double[]{68.77442039579722,3.565704191232712,-5.096218701243291,-6.961094234437688,-7.406456380898575,-3.874081948380394,-2.2978800680933458,1.9110386276122038,6.255171328504373,-1.457706881951331,-2.0898947127849854,-1.1731843012445669,});
			speech_model[14] = new DoublePoint(new double[]{70.1123308533204,9.840952064683231,-11.33442284977122,5.740202434675034,-2.6489091230440067,-7.5996374955815345,0.6375870070270675,3.9450548218990305,-0.8063091305857256,-2.5897889084191466,-5.884277187474443,-2.2005146642441544,});
			speech_model[15] = new DoublePoint(new double[]{59.219104047136085,-0.7160789814842682,4.225863381255057,-3.4120402713686007,-6.959349010987534,1.2835220210035776,-3.5821781408940216,4.996765296805349,-0.29959124543231974,2.1557196594268655,-1.3707857384665785,-0.6833684505082724,});
			nonspeech_model[0] = new DoublePoint(new double[]{43.35196504279517,3.171395193988718,-3.0985090391990084,-1.6786724880619772,-5.544388638016195,0.4391610922241419,-1.4514509833237397,0.9893644616386067,-0.2010730486210991,0.6143995785479421,-0.605137205227657,0.5744905014256265,});
			nonspeech_model[1] = new DoublePoint(new double[]{49.74203411541244,0.7127137419436933,-7.106653847529211,-0.27236641178031495,-5.930328418009542,-3.499456957255601,-0.3754399115362247,2.306314930166905,-3.0519688646606324,1.0914918498697177,0.445391247017887,-0.523115825188925,});
			nonspeech_model[2] = new DoublePoint(new double[]{40.062055874953984,0.6372001461773887,-4.727629191611642,-3.1261825313945275,-5.985123172143521,0.5089688100287693,-2.053648431811483,1.3055742757616198,-0.6511218466529323,0.5479510750570157,-0.4908005849703449,0.8506324644299632,});
			nonspeech_model[3] = new DoublePoint(new double[]{45.35985070818988,3.4840948985040656,-3.173794211733374,-2.0753314076079734,-5.858789366162705,-0.05742547950314189,-2.0147069415497008,0.7415605906962344,-0.1556419742045748,0.8250053089548104,-0.9555400486390541,0.3572009705916436,});
			nonspeech_model[4] = new DoublePoint(new double[]{42.345231367471385,1.1914080226827914,-4.94920808968824,-3.9588838103888695,-6.971943342554394,-1.2743661124382728,-3.1805553711373156,-0.5151321519892127,-1.745426653836793,-1.043198673557963,-1.608241481711991,-0.6222552489217765,});
			nonspeech_model[5] = new DoublePoint(new double[]{49.01159430663755,2.9255393713074,0.10031490615135358,-5.0444613590038045,-7.0805794641994195,-1.982126268884138,-3.3935007014640237,1.9219569962723033,0.16765510715456272,3.606292062374173,-0.10467228076179601,1.9073099925053487,});
			nonspeech_model[6] = new DoublePoint(new double[]{44.70027305485806,1.8129259705988363,-4.6495476585061395,-2.778335308390819,-5.004568803597675,0.6275964659825454,-2.1563768185138215,0.08511226416804213,-1.0821022553469122,0.957167715948288,-0.3342042832558773,0.35644549164670775,});
			nonspeech_model[7] = new DoublePoint(new double[]{41.24810960219052,1.5337822088085886,-3.2851633161318836,-2.2051536992889953,-5.080462410640785,0.9078825105502568,-1.3542899134863877,1.4644551518786937,-0.4041580301737786,-0.5264519279319405,-0.8205208663317222,0.4371962793011062,});
			nonspeech_model[8] = new DoublePoint(new double[]{46.75270750350115,4.257023059594334,-2.1522278042616385,-0.7248346157090332,-4.8578611926937745,-0.5529894743965601,-2.8286990157294714,-0.5315050975875667,-0.7259389363096006,1.0123075383000562,-0.6046137988917112,0.45474610460804427,});
			nonspeech_model[9] = new DoublePoint(new double[]{44.772702549634545,2.208218074144815,-5.899438384309519,-3.5105927326192594,-7.936109558437642,-1.2777652613958146,-3.3277933527571766,0.25262136774436894,-0.7089298410239538,0.3531883925263254,-1.1577622827184708,1.0892722687483987,});
			nonspeech_model[10] = new DoublePoint(new double[]{43.596478080447355,1.3900578674515183,-3.9422288859152417,-2.519499202827732,-6.097760225278515,-0.4664035902560049,-2.355132101535749,0.512411862229747,-0.49401402413803963,0.2722243384987278,-0.9730940558720238,0.2718968451762773,});
			nonspeech_model[11] = new DoublePoint(new double[]{41.73055118936053,2.7493619255353656,-2.821666750954812,-1.314095167639891,-4.475601718278074,1.0420627621629224,-1.6599902072531016,0.9968777262931678,-0.6369634677308907,0.49541881293186635,-0.46246774198637625,0.9293539896859122,});
			nonspeech_model[12] = new DoublePoint(new double[]{40.36233823145279,1.6991625563583945,-3.1854985360006336,-2.2314886466971724,-5.368365163619701,0.4786313169790959,-1.9857147724132374,1.3295654039988523,-0.231587371233439,0.9021689688196338,-0.4551952294185783,0.575459974811766,});
			nonspeech_model[13] = new DoublePoint(new double[]{46.56723211701155,0.7221963208308253,-3.084630263104097,-1.1450700888941294,-5.755845121643234,1.147311913253316,-2.2181528094244345,0.5466348738715046,0.20271701744414589,0.24813469945985625,-0.22561901136748716,0.1473913747453979,});
			nonspeech_model[14] = new DoublePoint(new double[]{44.712911200549456,1.1431050455303537,-5.987933407715146,1.1905251602494977,-7.746464482641689,-0.12896224777206486,-1.1137673653308477,-2.7642333473958365,-1.266941852465987,0.4868261391029499,-2.283372025543442,0.3824250888033033,});
			nonspeech_model[15] = new DoublePoint(new double[]{46.47092283958057,2.3548346637860087,-3.738530459822428,-1.1183279763038807,-5.069093773429808,0.13897216390384598,-2.6519990257538835,0.17109985566358726,-0.633784021257493,1.3683834264774075,-1.0509611352038437,0.15795694245486624,});
		} else {

			// via VQVAD.m
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
		}

		return new VQVADModel(speech_model, nonspeech_model, energyMinLevel);
	}

}
