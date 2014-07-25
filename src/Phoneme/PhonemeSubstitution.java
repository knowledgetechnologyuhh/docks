/**
 * DOCKS is a framework for post-processing results of Cloud-based speech 
 * recognition systems.
 * Copyright (C) 2014 Johannes Twiefel
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
 * 7twiefel@informatik.uni-hamburg.de
 */
package Phoneme;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;

import Data.Result;
import Phoneme.Categories.Excitation;
import Phoneme.Categories.MannerOfArticulation;
import Phoneme.Categories.PlaceOfArticfulation;
import Phoneme.Categories.VowelBackness;
import Phoneme.Categories.VowelHeight;
import Utils.FileLoop;
import Utils.FileProcessor;
import edu.cmu.sphinx.util.NISTAlign;
/**
 * containes the 0.1/0.9, Derived IPA and Google Revers Heuristic
 * used to calculate substitution scores
 * @author 7twiefel
 *
 */
public class PhonemeSubstitution {

	private String[] signs = new String[] { "SIL", "AA", "AE", "AH", "AO", "AW", "AY",
			"B", "CH", "D", "DH", "EH", "ER", "EY", "F", "G", "HH", "IH", "IY",
			"JH", "K", "L", "M", "N", "NG", "OW", "OY", "P", "R", "S", "SH",
			"T", "TH", "UH", "UW", "V", "W", "Y", "Z", "ZH" };

	public static int STANDARD = 0;
	public static int IPA_TABLE = 1;
	public static int GOOGLE_REVERSE = 2;

	private PhonemeSubstitution(int method) {

		if (method == IPA_TABLE) {
			init();
			initMapsIPA();
		} else if (method == GOOGLE_REVERSE)
			initMapsGoogleReverse();
		else if (method == STANDARD)
			initMapsStandard();


	}

	/**
	 * returns an instance of a substitutor based on the heuristic chosen
	 * @param method 
	 * @return
	 */
	public static PhonemeSubstitution getInstance(int method) {
		switch (method) {
		case 0:
			if (stStandard == null)
				stStandard = new PhonemeSubstitution(method);
			return stStandard;

		case 1:
			if (stIPA == null)
				stIPA = new PhonemeSubstitution(method);
			return stIPA;

		case 2:
			if (stGoogleReverse == null)
				stGoogleReverse = new PhonemeSubstitution(method);
			return stGoogleReverse;

		default:
			return null;
		}

	}

	private static PhonemeSubstitution stStandard = null;
	private static PhonemeSubstitution stIPA = null;
	private static PhonemeSubstitution stGoogleReverse = null;

	/**
	 * prints out the substitution table
	 */
	public void printTable() {
		int max = 0;
		System.out.print("     ");
		for (String s : signs) {
			s = String.format("%4s", s);
			System.out.print(s + " ");
		}
		System.out.println();
		for (String s1 : signs) {
			String s = s1;
			s = String.format("%4s", s);
			System.out.print(s + " ");
			for (String s2 : signs) {
				int dist = getDistance(s1, s2);
				if (dist > max)
					max = dist;
				String result = dist + "";
				result = String.format("%4s", result);
				System.out.print(result + " ");
			}
			System.out.println();
		}
		System.out.println();
		System.out.println("max: " + max);
	}

	/**
	 * normalizes a distance using a special function.
	 * the values are normalizes to values between start and end
	 * @param min minimal possible input value
	 * @param max maximal possible input value
	 * @param value value to be normalized
	 * @param start lowest value to be normalized to
	 * @param end highest value to be normalized to
	 * @return
	 */
	private static double normalize(double min, double max, double value,
			double start, double end) {
		if (value == 0)
			return end;

		if (max <= min)
			System.err
					.println("Min cannot be higher than max. Values entered are not valid.");

		if (end <= start)
			System.err
					.println("End cannot be higher than start. Values entered are not valid.");
		if (value >= max)
			return end;
		if (value <= min)
			return start;

		int a = 250;
		return 1 / ((value * max + a) * .01) + .1;


	}

	
	/**
	 * normalizes a distance linearly function.
	 * the values are normalizes to values between start and end
	 * @param min minimal possible input value
	 * @param max maximal possible input value
	 * @param value value to be normalized
	 * @param start lowest value to be normalized to
	 * @param end highest value to be normalized to
	 * @return
	 */
	private static double normalizeLinear(double min, double max, double value,
			double start, double end) {
		if (max <= min)
			System.err
					.println("Min cannot be higher than max. Values entered are not valid.");

		if (end <= start)
			System.err
					.println("End cannot be higher than start. Values entered are not valid.");
		if (value >= max)
			return end;
		if (value <= min)
			return start;

		double i1 = max - min;
		double i2 = end - start;
		double y = (value - min) * i2 / i1;
		return y + start;
	}



	private Hashtable<String, Phoneme> phonemes = new Hashtable<String, Phoneme>();
	private HashMap<String, Integer> fastScoreMap = new HashMap<String, Integer>();
	private HashMap<String, Double> fastScoreMapNormalized = new HashMap<String, Double>();

	/**
	 * initializes the score map for 0.1/0.9
	 */
	private void initMapsStandard() {
		for (String s1 : signs) {
			for (String s2 : signs) {
				double score = 0.1;
				if (s1.equals(s2))
					score = 0.9;
				fastScoreMapNormalized.put(s1 + ";" + s2, score);
			}
		}
	}

	/**
	 * initializes the score map for Derived IPA
	 */
	private void initMapsIPA() {
		int max = 0;
		//gets the max distance
		for (String s1 : signs) {
			for (String s2 : signs) {
				int dist = getDistance(s1, s2);
				if (dist > max)
					max = dist;
			}
		}
		//fills the non-normalized score table
		for (String s1 : signs) {
			for (String s2 : signs) {
				//get the IPA derived distance
				int dist = getDistance(s1, s2);
				fastScoreMap.put(s1 + ";" + s2, max - dist);
			}
		}
		System.out.print("     ");
		for (String s : signs) {
			s = String.format("%4s", s);
			System.out.print(s + " ");
		}
		System.out.println();
		DecimalFormat f = new DecimalFormat("#0.00");
		
		//normalizes the scores
		for (String s1 : signs) {
			String s = s1;
			s = String.format("%4s", s);
			System.out.print(s + " ");
			for (String s2 : signs) {
				//get the ipa derived distance
				int dist = max - getDistance(s1, s2);
				//normalize with special function
				double normalizedDist = normalize(0, max, dist, 0.1, 0.9);
				fastScoreMapNormalized.put(s1 + ";" + s2, normalizedDist);
				String result = normalizedDist + "";
				result = String.format("%4s", result);
				System.out.print(f.format(normalizedDist) + " ");
			}
			System.out.println();
		}
		printTable();

	}

	/**
	 * initializes the score map for Derived IPA
	 */
	
	private void initMapsGoogleReverse() {
		//process a cached result file of half of the scripted data set containing reference and hypothesis
		new FileProcessor(
				"heinrichLab.google.refhyp",
				new FileLoop() {
					
					PhonemeCreator pc = PhonemeCreator.getInstance();

					final NISTAlign alignerPhonemesReverse = new NISTAlign(
							true, true);
					ArrayList<PhonemeContainer> phonemesSpeech = null;
					Result r;
					HashMap<String, Integer> hm = new HashMap<String, Integer>();

					@Override
					public void process(String line) {
						r = new Result();

						String[] strLineSplit = line.split(";");
						String ref = strLineSplit[1];
						String hyp = strLineSplit[0];

						//get the phonemes for reference and hypothesis
						r.addResult(ref);
						r.addResult(hyp);
						phonemesSpeech = pc.getPhonemes(r);

						String refPhonemes = "";
						for (String s : phonemesSpeech.get(0).getPhonemes())
							refPhonemes = refPhonemes + s + " ";

						String hypPhonemes = "";
						for (String s : phonemesSpeech.get(1).getPhonemes())
							hypPhonemes = hypPhonemes + s + " ";


						//align ref and hyp
						alignerPhonemesReverse.align(refPhonemes, hypPhonemes);



						String alignedRef = alignerPhonemesReverse
								.getAlignedReference();
						String alignedHyp = alignerPhonemesReverse
								.getAlignedHypothesis();


						alignedRef = alignedRef.replaceAll(" +", " ");
						alignedHyp = alignedHyp.replaceAll(" +", " ");
						String[] refWords = alignedRef.split(" ");
						String[] hypWords = alignedHyp.split(" ");

						//calculate the number of substitutions for each phoneme by each other phoneme
						for (int i = 0; i < refWords.length; i++) {
							String key = refWords[i].toUpperCase() + ";"
									+ hypWords[i].toUpperCase();
							if (!hm.containsKey(key))
								hm.put(key, 1);
							else
								hm.put(key, hm.get(key) + 1);
						}
					}

					@Override
					public void start() {
						// TODO Auto-generated method stub

					}

					@Override
					public void end() {



						// TODO Auto-generated method stub
						System.out.print("     ");
						for (String s : signs) {
							s = String.format("%4s", s);
							System.out.print(s + " ");
						}
						System.out.println();
						DecimalFormat f = new DecimalFormat("#0.00");

						//normalize the distance table to a score table
						for (String s1 : signs) {
							String s = s1;
							s = String.format("%4s", s);
							System.out.print(s + " ");
							for (String s2 : signs) {
								double normalizedScore = 0.1;

								if (hm.containsKey(s1 + ";" + s2))
									normalizedScore = normalizeLinear(0,
											hm.get(s1 + ";" + s1),
											hm.get(s1 + ";" + s2), 0.1, 0.9);
								fastScoreMapNormalized.put(s1 + ";" + s2,
										normalizedScore);
								String result = normalizedScore + "";
								result = String.format("%4s", result);
																		
								System.out.print(f.format(normalizedScore)
										+ " ");
							}
							System.out.println();
						}
						
						fastScoreMapNormalized.put("SIL;SIL", 0.9);

					}

				});
	}
/**
 * calculates positive distance
 * @param i
 * @param j
 * @return
 */
	private int dist(int i, int j) {
		if (i < j)
			return j - i;
		else
			return i - j;

	}

/**
 * get the score comparing to phonemes
 * @param phoneme1 reference
 * @param phoneme2 input
 * @return
 */
	public double getScore(String phoneme1, String phoneme2) {
		return fastScoreMapNormalized.get(phoneme1 + ";" + phoneme2);
	}

	/**
	 * calculates the distance between to phonemes based on IPA categories
	 * @param phoneme1 reference
	 * @param phoneme2 input
	 * @return
	 */
	private int getDistance(String phoneme1, String phoneme2) {
		int distance = 0;

		//hardcode silence
		if (phoneme1.equals("SIL") && phoneme2.equals("SIL"))
			return 0;
		if (phoneme1.equals("SIL") || phoneme2.equals("SIL"))
			return 35;
		Phoneme p1 = phonemes.get(phoneme1);
		Phoneme p2 = phonemes.get(phoneme2);

		//weights for categories
		int classWeight = 10;
		int pronWeight = 2;
		int articWeight = 2;
		int mouthWeight = 2;
		int vowelArticWeight = 2;

		//calculate distance for place of articulation
		int classDist = dist(p1.getPhonemeClass().ordinal(), p2
				.getPhonemeClass().ordinal());
		//calculate distance for excitation
		int pronDist = dist(p1.getPronounciation().ordinal(), p2
				.getPronounciation().ordinal());
		
		int articDist = 0;
		int mouthDist = 0;
		int vowelArticDist = 0;
		int mouthDist2 = 0;
		int vowelArticDist2 = 0;
		
		//calculate distance for two monophtongs
		if (p1.getPhonemeClass() == PlaceOfArticfulation.MONOPHTONG
				&& p2.getPhonemeClass() == PlaceOfArticfulation.MONOPHTONG) {
			mouthDist = dist(p1.getMouthStatus().ordinal(), p2.getMouthStatus()
					.ordinal());
			vowelArticDist = dist(p1.getVowelArticulation().ordinal(), p2
					.getVowelArticulation().ordinal());
		}
		//calculate distance for two diphtongs
		else if (p1.getPhonemeClass() == PlaceOfArticfulation.DIPHTONG
				&& p2.getPhonemeClass() == PlaceOfArticfulation.DIPHTONG) {
			mouthDist = dist(p1.getMouthStatus().ordinal(), p2.getMouthStatus()
					.ordinal());
			vowelArticDist = dist(p1.getVowelArticulation().ordinal(), p2
					.getVowelArticulation().ordinal());
			mouthDist2 = dist(p1.getMouthStatus2().ordinal(), p2
					.getMouthStatus2().ordinal());
			vowelArticDist2 = dist(p1.getVowelArticulation2().ordinal(), p2
					.getVowelArticulation2().ordinal());
		}
		//calculate distance for a monophtong and a diphtong
		else if (p1.getPhonemeClass() == PlaceOfArticfulation.MONOPHTONG
				&& p2.getPhonemeClass() == PlaceOfArticfulation.DIPHTONG) {
			mouthWeight = 1;
			vowelArticWeight = 1;
			mouthDist = dist(p1.getMouthStatus().ordinal(), p2.getMouthStatus()
					.ordinal());
			vowelArticDist = dist(p1.getVowelArticulation().ordinal(), p2
					.getVowelArticulation().ordinal());
			mouthDist2 = dist(p1.getMouthStatus().ordinal(), p2
					.getMouthStatus2().ordinal());
			vowelArticDist2 = dist(p1.getVowelArticulation().ordinal(), p2
					.getVowelArticulation2().ordinal());
		}
		//calculate distance for a monophtong and a diphtong
		else if (p1.getPhonemeClass() == PlaceOfArticfulation.DIPHTONG
				&& p2.getPhonemeClass() == PlaceOfArticfulation.MONOPHTONG) {
			mouthWeight = 1;
			vowelArticWeight = 1;
			mouthDist = dist(p1.getMouthStatus().ordinal(), p2.getMouthStatus()
					.ordinal());
			vowelArticDist = dist(p1.getVowelArticulation().ordinal(), p2
					.getVowelArticulation().ordinal());
			mouthDist2 = dist(p1.getMouthStatus2().ordinal(), p2
					.getMouthStatus().ordinal());
			vowelArticDist2 = dist(p1.getVowelArticulation2().ordinal(), p2
					.getVowelArticulation().ordinal());
		} else //calculate distance for all other combinations
			articDist = dist(p1.getArticulation().ordinal(), p2
					.getArticulation().ordinal());

		//sum up the distance of the categories
		distance = classDist * classWeight + pronDist * pronWeight + articDist
				* articWeight + mouthDist * mouthWeight + mouthDist2
				* mouthWeight + vowelArticDist * vowelArticWeight
				+ vowelArticDist2 * vowelArticWeight;

		return distance;
	}

	/**
	 * initialize the phoneme categories
	 */
	private void init() {

		// plosives
		phonemes.put("P", new Phoneme("P", Excitation.VOICELESS,
				PlaceOfArticfulation.PLOSIVE, MannerOfArticulation.BILABIAL)); // P=p
		phonemes.put("B", new Phoneme("B", Excitation.VOICED,
				PlaceOfArticfulation.PLOSIVE, MannerOfArticulation.BILABIAL)); // B=b
		phonemes.put("T", new Phoneme("T", Excitation.VOICELESS,
				PlaceOfArticfulation.PLOSIVE, MannerOfArticulation.ALVEOLAR)); // T=t
		phonemes.put("D", new Phoneme("D", Excitation.VOICED,
				PlaceOfArticfulation.PLOSIVE, MannerOfArticulation.ALVEOLAR)); // D=d
		phonemes.put("K", new Phoneme("K", Excitation.VOICELESS,
				PlaceOfArticfulation.PLOSIVE, MannerOfArticulation.VELAR)); // K=k
		phonemes.put("G", new Phoneme("G", Excitation.VOICED,
				PlaceOfArticfulation.PLOSIVE, MannerOfArticulation.VELAR)); // G=g

		// fricatives
		phonemes.put("F", new Phoneme("F", Excitation.VOICELESS,
				PlaceOfArticfulation.FRICATIVE, MannerOfArticulation.LABIODENTAL)); // F=f
		phonemes.put("V", new Phoneme("V", Excitation.VOICED,
				PlaceOfArticfulation.FRICATIVE, MannerOfArticulation.LABIODENTAL)); // V=v
		phonemes.put("TH", new Phoneme("TH", Excitation.VOICELESS,
				PlaceOfArticfulation.FRICATIVE, MannerOfArticulation.DENTAL)); // TH=θ
		phonemes.put("DH", new Phoneme("DH", Excitation.VOICED,
				PlaceOfArticfulation.FRICATIVE, MannerOfArticulation.DENTAL)); // DH=ð
		phonemes.put("S", new Phoneme("S", Excitation.VOICELESS,
				PlaceOfArticfulation.FRICATIVE, MannerOfArticulation.ALVEOLAR)); // S=s
		phonemes.put("Z", new Phoneme("Z", Excitation.VOICED,
				PlaceOfArticfulation.FRICATIVE, MannerOfArticulation.ALVEOLAR)); // Z=z
		phonemes.put("SH", new Phoneme("SH", Excitation.VOICELESS,
				PlaceOfArticfulation.FRICATIVE, MannerOfArticulation.PALATOALVEOLAR)); // SH=ʃ
		phonemes.put("ZH", new Phoneme("ZH", Excitation.VOICED,
				PlaceOfArticfulation.FRICATIVE, MannerOfArticulation.PALATOALVEOLAR)); // ZH=ʒ
		phonemes.put("HH", new Phoneme("HH", Excitation.VOICELESS,
				PlaceOfArticfulation.FRICATIVE, MannerOfArticulation.GLOTTAL)); // HH=h
		// affricates (zu fricatives)
		phonemes.put("CH", new Phoneme("CH", Excitation.VOICELESS,
				PlaceOfArticfulation.FRICATIVE, MannerOfArticulation.ALVEOLOPALATAL)); // CH=tʃ
		phonemes.put("JH", new Phoneme("JH", Excitation.VOICED,
				PlaceOfArticfulation.FRICATIVE, MannerOfArticulation.ALVEOLOPALATAL)); // JH=dʒ

		// nasals
		phonemes.put("M", new Phoneme("M", Excitation.VOICED,
				PlaceOfArticfulation.NASAL, MannerOfArticulation.BILABIAL)); // M=m
		phonemes.put("N", new Phoneme("N", Excitation.VOICED,
				PlaceOfArticfulation.NASAL, MannerOfArticulation.ALVEOLAR)); // N=n
		phonemes.put("NG", new Phoneme("NG", Excitation.VOICED,
				PlaceOfArticfulation.NASAL, MannerOfArticulation.VELAR)); // NG=ŋ

		// approximants
		// liquids
		phonemes.put("L", new Phoneme("L", Excitation.VOICELESS,
				PlaceOfArticfulation.APPROXIMANT, MannerOfArticulation.ALVEOLAR)); // L=ɫ
		phonemes.put("R", new Phoneme("R", Excitation.VOICED,
				PlaceOfArticfulation.APPROXIMANT, MannerOfArticulation.ALVEOLAR)); // R=r or ɹ
		// semivowels
		phonemes.put("Y", new Phoneme("Y", Excitation.VOICED,
				PlaceOfArticfulation.APPROXIMANT, MannerOfArticulation.PALATAL)); // Y=j
		phonemes.put("W", new Phoneme("W", Excitation.VOICED,
				PlaceOfArticfulation.APPROXIMANT, MannerOfArticulation.VELAR)); // W=w
		// r-colored vowels
		phonemes.put("ER", new Phoneme("ER", Excitation.VOICED,
				PlaceOfArticfulation.APPROXIMANT)); // ER=ɝ

		// monophtongs
		phonemes.put("AO", new Phoneme("AO", PlaceOfArticfulation.MONOPHTONG,
				VowelBackness.BACK, VowelHeight.OPENMID)); // AO=ɔ
		phonemes.put("AA", new Phoneme("AA", PlaceOfArticfulation.MONOPHTONG,
				VowelBackness.BACK, VowelHeight.OPEN)); // AA=ɑ
		phonemes.put("IY", new Phoneme("IY", PlaceOfArticfulation.MONOPHTONG,
				VowelBackness.FRONT, VowelHeight.CLOSE)); // IY=i
		phonemes.put("UW", new Phoneme("UW", PlaceOfArticfulation.MONOPHTONG,
				VowelBackness.BACK, VowelHeight.CLOSE)); // UW=u
		phonemes.put("EH", new Phoneme("EH", PlaceOfArticfulation.MONOPHTONG,
				VowelBackness.NEARFRONT, VowelHeight.OPENMID)); // EH=ɛ
		phonemes.put("IH", new Phoneme("IH", PlaceOfArticfulation.MONOPHTONG,
				VowelBackness.NEARFRONT, VowelHeight.NEARCLOSE)); // IH=ɪ
		phonemes.put("UH", new Phoneme("UH", PlaceOfArticfulation.MONOPHTONG,
				VowelBackness.NEARBACK, VowelHeight.NEARCLOSE)); // UH=ʊ
		phonemes.put("AH", new Phoneme("AH", PlaceOfArticfulation.MONOPHTONG,
				VowelBackness.CENTRAL, VowelHeight.CLOSEMID)); // AH=ʌ/ə
		phonemes.put("AE", new Phoneme("AE", PlaceOfArticfulation.MONOPHTONG,
				VowelBackness.CENTRAL, VowelHeight.NEAROPEN)); // AE=æ

		// diphtongs
		phonemes.put("EY", new Phoneme("EY", PlaceOfArticfulation.DIPHTONG,
				VowelBackness.NEARFRONT, VowelHeight.CLOSEMID,
				VowelBackness.NEARFRONT, VowelHeight.NEARCLOSE)); // EY=eɪ
		phonemes.put("AY", new Phoneme("AY", PlaceOfArticfulation.DIPHTONG,
				VowelBackness.CENTRAL, VowelHeight.OPEN,
				VowelBackness.NEARFRONT, VowelHeight.NEARCLOSE)); // AY=aɪ
		phonemes.put("OW", new Phoneme("OW", PlaceOfArticfulation.DIPHTONG,
				VowelBackness.BACK, VowelHeight.CLOSEMID,
				VowelBackness.NEARBACK, VowelHeight.NEARCLOSE)); // OW=oʊ
		phonemes.put("AW", new Phoneme("AW", PlaceOfArticfulation.DIPHTONG,
				VowelBackness.CENTRAL, VowelHeight.OPEN,
				VowelBackness.NEARBACK, VowelHeight.NEARCLOSE)); // AW=aʊ
		phonemes.put("OY", new Phoneme("OY", PlaceOfArticfulation.DIPHTONG,
				VowelBackness.BACK, VowelHeight.OPENMID,
				VowelBackness.NEARFRONT, VowelHeight.NEARCLOSE)); // OY=ɔɪ

	}
}
