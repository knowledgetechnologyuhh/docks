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
package Utils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Console;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import Data.Result;
import Frontend.LocalMicrophone;
import Frontend.VoiceActivityDetector;
import Phoneme.PhonemeContainer;
import Phoneme.PhonemeCreator;
import PostProcessor.SentencelistPostProcessor;
import PostProcessor.SphinxBasedPostProcessor;
import PostProcessor.WordlistPostProcessor;
import PostProcessor.LevenshteinBased.Levenshtein;
import Recognizer.RawGoogleRecognizer;
import Recognizer.SphinxRecognizer;
import edu.cmu.sphinx.util.NISTAlign;
/**
 * experimental class for different tests (undocumented)
 * @author 7twiefel
 *
 */
public class TestSupervisor {
	public static String TAG = "TestSupervisor";
	public static String key = "yourkeyhere";
	
	
	public static void testLive2() {


		RawGoogleRecognizer br = new RawGoogleRecognizer(key);
		SentencelistPostProcessor lr = new SentencelistPostProcessor(
				"speech_wtm_5words", 1,key);
		Result r;
		ExampleChooser ec = new ExampleChooser("speech_wtm_5words");

		String sentence = "";
		VoiceActivityDetector vac = new VoiceActivityDetector(
				new LocalMicrophone(), "LocalMicrophone");

		while (true) {

			sentence = ec.printRandomExample();

			r = br.recognize(vac);
			Printer.printTime();


			String speechResult = "no speech result";
			String finalResult = "no speech result";

			if (r != null) {

				speechResult = r.getBestResult();

				r = lr.recognizeFromResult(r);

				finalResult = r.getBestResult();

			}

			System.out.print("G: ");
			if (speechResult.equals(sentence)) {

				Printer.printGreen(speechResult);

			} else if (speechResult.equals("no speech result")) {
				Printer.printRed(speechResult);

			} else {
				Printer.printRed(speechResult);

			}


			Printer.printTime();

			System.out.print("L: ");
			if (finalResult.equals(sentence)) {

				Printer.printGreen(finalResult);

			} else if (finalResult.equals("no speech result")) {
				Printer.printRed(finalResult);

			} else {
				Printer.printRed(finalResult);

			}
		}

	}

	public static void getBestParameters() {
		new FileProcessor("results/performance.csv", new FileLoop() {

			int i;
			double minStandardWER;
			int minStandardLine;
			double minStandardLanguageWeight;
			double minStandardWIP;
			double minStandardRatio;

			double minIPAWER;
			int minIPALine;
			double minIPALanguageWeight;
			double minIPAWIP;
			double minIPARatio;

			double minRevWER;
			int minRevLine;
			double minRevLanguageWeight;
			double minRevWIP;
			double minRevRatio;

			@Override
			public void start() {
				// TODO Auto-generated method stub
				i = 0;
				minStandardWER = 10;
				minIPAWER = 10;
				minRevWER = 10;

			}

			@Override
			public void process(String line) {
				// TODO Auto-generated method stub
				String[] fields = line.split(";");
				if (fields[0].equals("Standard")) {
					double lineWER = Double.parseDouble(fields[5]);

					if (Double.compare(lineWER, minStandardWER) == -1) {

						minStandardWER = Double.parseDouble(fields[5]);

						minStandardLine = i;
						minStandardLanguageWeight = Double
								.parseDouble(fields[1]);
						minStandardWIP = Double.parseDouble(fields[2]);
						minStandardRatio = Double.parseDouble(fields[3]);
					}
				} else if (fields[0].equals("IPA Table")) {
					double lineWER = Double.parseDouble(fields[5]);
					if (Double.compare(lineWER, minIPAWER) == -1) {
						minIPAWER = Double.parseDouble(fields[5]);
						minIPALine = i;
						minIPALanguageWeight = Double.parseDouble(fields[1]);
						minIPAWIP = Double.parseDouble(fields[2]);
						minIPARatio = Double.parseDouble(fields[3]);
					}
				} else if (fields[0].equals("Google Reverse")) {
					double lineWER = Double.parseDouble(fields[5]);
					if (Double.compare(lineWER, minRevWER) == -1) {
						minRevWER = Double.parseDouble(fields[5]);
						minRevLine = i;
						minRevLanguageWeight = Double.parseDouble(fields[1]);
						minRevWIP = Double.parseDouble(fields[2]);
						minRevRatio = Double.parseDouble(fields[3]);
					}
				}
				i++;

			}

			@Override
			public void end() {
				// TODO Auto-generated method stub
				System.out.print("minStandardWER: ");
				System.out.println(minStandardWER);
				System.out.print("minStandardLine: ");
				System.out.println(minStandardLine);
				System.out.print("minStandardLanguageWeight: ");
				System.out.println(minStandardLanguageWeight);
				System.out.print("minStandardWIP: ");
				System.out.println(minStandardWIP);
				System.out.print("minStandardRatio: ");
				System.out.println(minStandardRatio);

				System.out.print("minIPAWER: ");
				System.out.println(minIPAWER);
				System.out.print("minIPALine: ");
				System.out.println(minIPALine);
				System.out.print("minIPALanguageWeight: ");
				System.out.println(minIPALanguageWeight);
				System.out.print("minIPAWIP: ");
				System.out.println(minIPAWIP);
				System.out.print("minIPARatio: ");
				System.out.println(minIPARatio);

				System.out.print("minRevWER: ");
				System.out.println(minRevWER);
				System.out.print("minRevLine: ");
				System.out.println(minRevLine);
				System.out.print("minRevLanguageWeight: ");
				System.out.println(minRevLanguageWeight);
				System.out.print("minRevWIP: ");
				System.out.println(minRevWIP);
				System.out.print("minRevRatio: ");
				System.out.println(minRevRatio);

			}

		});

	}

	public static void modifyCSV(String file) {
		FileInputStream fstream = null;
		try {
			fstream = new FileInputStream(file);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine;

		file = file.substring(0, file.lastIndexOf("."));
		file = file.substring(0, file.lastIndexOf("."));

		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter(file + ".csv"));
			out.write("Filename;Reference Text;Hypothesis;0-Best;1-Best;2-Best;3-Best;4-Best;5-Best;6-Best;7-Best;8-Best;9-Best;"
					+ "Matching Google Sentence;Matching Google Sentence Index;Levenshtein Distance to Matching Google Sentence;Correct Recognition;"
					+ "Levenshtein Distance to 0-Best Result;Index of best matching n-best result;Levenshtein Distance from best match to reference\n");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}


		PhonemeCreator pc = PhonemeCreator.getInstance("speech_wtm_5words");
		Levenshtein ls = new Levenshtein();
		try {
			boolean correct;
			br.readLine();
			while ((strLine = br.readLine()) != null) {
				if (file.contains("leven")) {

					correct = false;

					String fileEntry = strLine.split(";")[0];

					strLine = strLine.substring(strLine.indexOf(";"));
					strLine = strLine.replaceAll("[^a-zA-Z 0-9;]", "");
					strLine = strLine.replaceAll(" +", " ");


					if (!strLine.equals(""))
						if (strLine.charAt(0) == ' ')
							strLine = strLine.substring(1);

					strLine = strLine.substring(strLine.indexOf(";",
							strLine.indexOf(";")));

					String[] column = strLine.split(";");


					if (column[1].equals(column[2])) {
						strLine = strLine + ";yes";
						correct = true;
					} else
						strLine = strLine + ";no";


					if (!column[14].equals("0")) {
						Result r = new Result();
						r.addResult(column[2]); // hyp
						r.addResult(column[3]); // 0-best


						if (!correct) {
							for (int i = 0; i < 9; i++) {
								if (!column[i + 4].equals("")) {
									r.addResult(column[i + 4]);
								} else {
									// System.out.println("breaking");
									break;
								}
							}
						}
						ArrayList<PhonemeContainer> phonemes = pc.getPhonemes(r);

						int dist = ls.diff(phonemes.get(0).getPhonemes(),
								phonemes.get(1).getPhonemes());
						strLine = strLine + ";" + dist;

					} else
						strLine = strLine + ";" + column[15];

					if (!correct) {
						Result r = new Result();
						r.addResult(column[1]); // reference


						for (int i = 0; i < 10; i++) {
							if (!column[i + 3].equals("")) {
								// System.out.print("|"+column[i+3]+"|");
								r.addResult(column[i + 3]);
							} else {
								// System.out.println("breaking");
								break;
							}
						}

						ArrayList<PhonemeContainer> phonemes = pc.getPhonemes(r);

						int minDist = 1000;
						int result = -1;
						for (int i = 1; i < phonemes.size(); i++) {

							int dist = ls.diff(phonemes.get(0).getPhonemes(),
									phonemes.get(i).getPhonemes());

							if (dist < minDist) {
								minDist = dist;
								result = i;
							}

						}

						strLine = strLine + ";" + (result - 1) + ";" + minDist;

					}

					else
						strLine = strLine + ";" + column[14] + ";" + column[15];



					strLine = fileEntry + strLine;


					out.write(strLine + "\n");

				} else {
					strLine = strLine + ";;;;";
					out.write(strLine + "\n");
				}

			}


			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void modifyAllCSV() {

		String path_L = "/informatik/isr/wtm/TEMP/7twiefel/recs_extMic_iCubOff/amplified_16bit_16000Hz_mono_L/";
		String path_R = "/informatik/isr/wtm/TEMP/7twiefel/recs_extMic_iCubOff/amplified_16bit_16000Hz_mono_R/";
		String path_mean = "/informatik/isr/wtm/TEMP/7twiefel/recs_extMic_iCubOff/amplified_16bit_16000Hz_mono_mean/";

		for (int i = 0; i <= 180; i = i + 15) {
			String formNumber = String.format("%03d", i);

			System.out.println("L google " + formNumber);
			modifyCSV(path_L + formNumber + ".batch_google.txt.csv");
			System.out.println("L leven " + formNumber);
			modifyCSV(path_L + formNumber + ".batch_leven.txt.csv");
			System.out.println("L sphinx " + formNumber);
			modifyCSV(path_L + formNumber + ".batch_sphinx.txt.csv");
			System.out.println("R google " + formNumber);
			modifyCSV(path_R + formNumber + ".batch_google.txt.csv");
			System.out.println("R leven " + formNumber);
			modifyCSV(path_R + formNumber + ".batch_leven.txt.csv");
			System.out.println("R sphinx " + formNumber);
			modifyCSV(path_R + formNumber + ".batch_sphinx.txt.csv");
			System.out.println("M google " + formNumber);
			modifyCSV(path_mean + formNumber + ".batch_google.txt.csv");
			System.out.println("M leven " + formNumber);
			modifyCSV(path_mean + formNumber + ".batch_leven.txt.csv");
			System.out.println("M sphinx " + formNumber);
			modifyCSV(path_mean + formNumber + ".batch_sphinx.txt.csv");
		}

	}

	public static void convertAllToCSV() {

		String path_L = "/informatik/isr/wtm/TEMP/7twiefel/recs_extMic_iCubOff/amplified_16bit_16000Hz_mono_L/";
		String path_R = "/informatik/isr/wtm/TEMP/7twiefel/recs_extMic_iCubOff/amplified_16bit_16000Hz_mono_R/";
		String path_mean = "/informatik/isr/wtm/TEMP/7twiefel/recs_extMic_iCubOff/amplified_16bit_16000Hz_mono_mean/";

		for (int i = 0; i <= 180; i = i + 15) {
			String formNumber = String.format("%03d", i);
			convertToCSV(path_L + formNumber + ".batch_google.txt.csv");
			convertToCSV(path_L + formNumber + ".batch_leven.txt.csv");
			convertToCSV(path_L + formNumber + ".batch_sphinx.txt.csv");
			convertToCSV(path_R + formNumber + ".batch_google.txt.csv");
			convertToCSV(path_R + formNumber + ".batch_leven.txt.csv");
			convertToCSV(path_R + formNumber + ".batch_sphinx.txt.csv");
			convertToCSV(path_mean + formNumber + ".batch_google.txt.csv");
			convertToCSV(path_mean + formNumber + ".batch_leven.txt.csv");
			convertToCSV(path_mean + formNumber + ".batch_sphinx.txt.csv");
		}

	}

	public static void convertToCSV(String file) {
		FileInputStream fstream = null;
		try {
			fstream = new FileInputStream(file);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine;

		String csvLine = "";

		String filename = "";
		String ref = "";
		String hyp = "";
		String nbestlist = "";

		String matchingSentence = "";
		String matchingId = "";
		String distance = "";
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter(file + ".csv"));
			out.write("Filename;Reference Text;Hypothesis;0-Best;1-Best;2-Best;3-Best;4-Best;5-Best;6-Best;7-Best;8-Best;9-Best;Matching Google Sentence; Matching Google Sentence Index; Levenshtein Distance\n");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try {
			while ((strLine = br.readLine()) != null) {
				if (strLine.equals("<file>"))
					filename = br.readLine();
				else if (strLine.equals("<ref>"))
					ref = br.readLine();
				else if (strLine.equals("<hyp>"))
					hyp = br.readLine();
				else if (strLine.equals("<n-bestlist>")) {
					boolean hasNextRes = true;
					for (int i = 0; i < 10; i++) {
						if (hasNextRes)
							strLine = br.readLine();
						try {
							if (strLine.equals("</n-bestlist>")) {
								hasNextRes = false;
								strLine = "";
							}
						} catch (Exception e) {
							e.printStackTrace();
							System.out.println(filename + " in " + file);
						}
						nbestlist = nbestlist + strLine;
						if (i < 9)
							nbestlist = nbestlist + ";";

					}
				} else if (strLine.equals("<matching_google_sentence>"))
					matchingSentence = br.readLine();
				else if (strLine.equals("<matching_google_sentence_id>"))
					matchingId = br.readLine();
				else if (strLine.equals("<distance>"))
					distance = br.readLine();
				else if (strLine.equals("</result>")) {
					csvLine = filename + ";" + ref + ";" + hyp + ";"
							+ nbestlist + ";" + matchingSentence + ";"
							+ matchingId + ";" + distance;

					out.write(csvLine + "\n");
					csvLine = "";

					filename = "";
					ref = "";
					hyp = "";
					nbestlist = "";

					matchingSentence = "";
					matchingId = "";
					distance = "";
				}

			}

			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void createBatchFilesIcub() {
		System.out.println("i start");
		String examplePath = "/informatik/isr/wtm/TEMP/7twiefel/recs_extMic_iCubOff/";
		String exampleBatchfile = "example.batch";

		String path_L = "/informatik2/wtm/KT-Datasets/20140117_recs-extMic-woodHead_speech/amplified_16bit_16000Hz_mono_L/";
		String path_R = "/informatik2/wtm/KT-Datasets/20140117_recs-extMic-woodHead_speech/amplified_16bit_16000Hz_mono_R/";
		String path_mean = "/informatik2/wtm/KT-Datasets/20140117_recs-extMic-woodHead_speech/amplified_16bit_16000Hz_mono_mean/";

		FileInputStream fstream;

		try {

			ArrayList<BufferedWriter> br_L = new ArrayList<BufferedWriter>();
			ArrayList<BufferedWriter> br_R = new ArrayList<BufferedWriter>();
			ArrayList<BufferedWriter> br_mean = new ArrayList<BufferedWriter>();
			for (int j = 1; j <= 10; j++) {
				for (int i = 0; i <= 180; i = i + 15) {
					String formNumber = String.format("%03d", i);
					String sessionNumber = String.format("%02d", j);
					br_L.add(new BufferedWriter(new FileWriter(path_L
							+ "session_" + sessionNumber + "/" + formNumber
							+ "_batch")));
					br_R.add(new BufferedWriter(new FileWriter(path_R
							+ "session_" + sessionNumber + "/" + formNumber
							+ "_batch")));
					br_mean.add(new BufferedWriter(new FileWriter(path_mean
							+ "session_" + sessionNumber + "/" + formNumber
							+ "_batch")));
				}
			}

			fstream = new FileInputStream(examplePath + exampleBatchfile);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			int i = 0;
			while ((strLine = br.readLine()) != null) {
				int endName = strLine.indexOf(" ");

				String sentence = strLine.substring(endName + 1);
				String formNumber = String.format("%03d", i);

				int k = 1;
				int j = 0;
				for (BufferedWriter bw : br_L) {
					String formDegree = String.format("%03d", j);
					String sessionNumber = String.format("%02d", k);
					bw.write(path_L + "session_" + sessionNumber + "/" + "rec_"
							+ formDegree + "/" + formDegree + "deg_"
							+ formNumber + "rec_session" + sessionNumber
							+ ".wav " + sentence);
					if (i != 191)
						bw.write("\n");
					else
						bw.close();
					j = j + 15;
					if (j > 180) {
						k++;
						j = 0;
					}
					if (k > 10)
						k = 1;
				}
				k = 1;
				j = 0;
				for (BufferedWriter bw : br_R) {
					String formDegree = String.format("%03d", j);
					String sessionNumber = String.format("%02d", k);
					bw.write(path_R + "session_" + sessionNumber + "/" + "rec_"
							+ formDegree + "/" + formDegree + "deg_"
							+ formNumber + "rec_session" + sessionNumber
							+ ".wav " + sentence);
					if (i != 191)
						bw.write("\n");
					else
						bw.close();
					j = j + 15;
					if (j > 180) {
						k++;
						j = 0;
					}
					if (k > 10)
						k = 1;
				}
				k = 1;
				j = 0;
				for (BufferedWriter bw : br_mean) {
					String sessionNumber = String.format("%02d", k);
					String formDegree = String.format("%03d", j);
					bw.write(path_mean + "session_" + sessionNumber + "/"
							+ "rec_" + formDegree + "/" + formDegree + "deg_"
							+ formNumber + "rec_session" + sessionNumber
							+ ".wav " + sentence);
					if (i != 191)
						bw.write("\n");
					else
						bw.close();
					j = j + 15;
					if (j > 180) {
						k++;
						j = 0;
					}
					if (k > 10)
						k = 1;
				}



				i++;
			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("i finished");
	}

	public static void createBatchFiles() {
		String examplePath = "/informatik/isr/wtm/TEMP/7twiefel/recs_extMic_iCubOff/";
		String exampleBatchfile = "example.batch";
		String path_L = "/informatik/isr/wtm/TEMP/7twiefel/recs_extMic_iCubOff/amplified_16bit_16000Hz_mono_L/";
		String path_R = "/informatik/isr/wtm/TEMP/7twiefel/recs_extMic_iCubOff/amplified_16bit_16000Hz_mono_R/";
		String path_mean = "/informatik/isr/wtm/TEMP/7twiefel/recs_extMic_iCubOff/amplified_16bit_16000Hz_mono_mean/";

		FileInputStream fstream;

		try {

			ArrayList<BufferedWriter> br_L = new ArrayList<BufferedWriter>();
			ArrayList<BufferedWriter> br_R = new ArrayList<BufferedWriter>();
			ArrayList<BufferedWriter> br_mean = new ArrayList<BufferedWriter>();
			for (int i = 0; i <= 180; i = i + 15) {
				String formNumber = String.format("%03d", i);
				br_L.add(new BufferedWriter(new FileWriter(path_L + formNumber
						+ "_batch")));
				br_R.add(new BufferedWriter(new FileWriter(path_R + formNumber
						+ "_batch")));
				br_mean.add(new BufferedWriter(new FileWriter(path_mean
						+ formNumber + "_batch")));
			}

			fstream = new FileInputStream(examplePath + exampleBatchfile);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			int i = 0;
			while ((strLine = br.readLine()) != null) {
				int endName = strLine.indexOf(" ");

				String sentence = strLine.substring(endName + 1);
				String formNumber = String.format("%03d", i);

				int j = 0;
				for (BufferedWriter bw : br_L) {
					String formDegree = String.format("%03d", j);
					bw.write(path_L + "rec_" + formDegree + "/" + formDegree
							+ "deg_" + formNumber + "rec.wav " + sentence);
					if (i != 191)
						bw.write("\n");
					else
						bw.close();
					j = j + 15;
				}
				j = 0;
				for (BufferedWriter bw : br_R) {
					String formDegree = String.format("%03d", j);
					bw.write(path_R + "rec_" + formDegree + "/" + formDegree
							+ "deg_" + formNumber + "rec.wav " + sentence);
					if (i != 191)
						bw.write("\n");
					else
						bw.close();
					j = j + 15;
				}
				j = 0;
				for (BufferedWriter bw : br_mean) {
					String formDegree = String.format("%03d", j);
					bw.write(path_mean + "rec_" + formDegree + "/" + formDegree
							+ "deg_" + formNumber + "rec.wav " + sentence);
					if (i != 191)
						bw.write("\n");
					else
						bw.close();
					j = j + 15;
				}



				i++;
			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void icubTestParallel(String[] args) {
		class OneShotTask implements Runnable {
			String path;
			String batch;

			OneShotTask(String p, String b) {
				path = p;
				batch = b;
			}

			public void run() {
				System.out.println(path + batch);
				test(path, batch, null);
			}
		}

		String soundFolderEnding = args[0];
		int threads = Integer.parseInt(args[1]);
		int begin = Integer.parseInt(args[2]);

		String path = "/informatik/isr/wtm/TEMP/7twiefel/recs_extMic_iCubOff/amplified_16bit_16000Hz_mono_";

		System.out.println("Starting channel " + soundFolderEnding + " with "
				+ threads + " threads from " + begin);

		for (int i = begin; i < (threads * 15 + begin); i = i + 15) {
			System.out.println("starting thread for degree " + i);
			String formNumber = String.format("%03d", i);
			Thread t = new Thread(new OneShotTask(path + soundFolderEnding
					+ "/", formNumber + ".batch"));
			t.start();
		}

	}

	public static void icubTest(String sentenceFile) {
		String path_L = "/informatik/isr/wtm/TEMP/7twiefel/recs_extMic_iCubOff/amplified_16bit_16000Hz_mono_L/";
		String path_R = "/informatik/isr/wtm/TEMP/7twiefel/recs_extMic_iCubOff/amplified_16bit_16000Hz_mono_R/";
		String path_mean = "/informatik/isr/wtm/TEMP/7twiefel/recs_extMic_iCubOff/amplified_16bit_16000Hz_mono_mean/";
		for (int i = 0; i <= 180; i = i + 15) {
			Printer.reset();
			String formNumber = String.format("%03d", i);
			test(path_L, formNumber + "_batch", sentenceFile);
			test(path_R, formNumber + "_batch", sentenceFile);
			test(path_mean, formNumber + "_batch", sentenceFile);
			Printer.printWithTimeImportant(TAG, i + "");
		}


	}

	public static void iCubTestSession(String senFile, int sesNumber) {
		System.out.println("starting session");
		final String sentenceFile = senFile;
		final String sessionNumber = String.format("%02d", sesNumber);
		new Thread(new Runnable() {
			@Override
			public void run() {
				System.out.println("starting thread 1");

				String path_L = "/informatik2/wtm/KT-Datasets/20140117_recs-extMic-woodHead_speech/amplified_16bit_16000Hz_mono_L/";

				for (int i = 0; i <= 180; i = i + 15) {
					Printer.reset();
					String formNumber = String.format("%03d", i);
					test(path_L + "session_" + sessionNumber + "/", formNumber
							+ "_batch", sentenceFile);
					Printer.printWithTimeImportant(TAG, i + "");
				}
				System.out.println("exiting thread 1");

			}
		}).start();
		new Thread(new Runnable() {
			@Override
			public void run() {
				System.out.println("starting thread 2");
				String path_R = "/informatik2/wtm/KT-Datasets/20140117_recs-extMic-woodHead_speech/amplified_16bit_16000Hz_mono_R/";


				for (int i = 0; i <= 180; i = i + 15) {
					Printer.reset();
					String formNumber = String.format("%03d", i);
					test(path_R + "session_" + sessionNumber + "/", formNumber
							+ "_batch", sentenceFile);
					Printer.printWithTimeImportant(TAG, i + "");
				}
				System.out.println("exiting thread 2");

			}
		}).start();

		System.out.println("starting mainthread");
		String path_mean = "/informatik2/wtm/KT-Datasets/20140117_recs-extMic-woodHead_speech/amplified_16bit_16000Hz_mono_mean/";


		for (int i = 0; i <= 180; i = i + 15) {
			Printer.reset();
			String formNumber = String.format("%03d", i);
			test(path_mean + "session_" + sessionNumber + "/", formNumber
					+ "_batch", sentenceFile);
			Printer.printWithTimeImportant(TAG, i + "");
		}
		System.out.println("exiting mainthread");
	}

	public static void bigIcubTest(String sentenceFile) {
		String path_L = "/informatik2/wtm/KT-Datasets/20140114_recs-extMic-iCubOff_speech/amplified_16bit_16000Hz_mono_L/";
		String path_R = "/informatik2/wtm/KT-Datasets/20140114_recs-extMic-iCubOff_speech/amplified_16bit_16000Hz_mono_R/";
		String path_mean = "/informatik2/wtm/KT-Datasets/20140114_recs-extMic-iCubOff_speech/amplified_16bit_16000Hz_mono_mean/";

		for (int j = 1; j <= 10; j++)
			for (int i = 0; i <= 180; i = i + 15) {
				Printer.reset();
				String sessionNumber = String.format("%02d", j);
				String formNumber = String.format("%03d", i);
				test(path_L + "session_" + sessionNumber + "/", formNumber
						+ "_batch", sentenceFile);
				test(path_R + "session_" + sessionNumber + "/", formNumber
						+ "_batch", sentenceFile);
				test(path_mean + "session_" + sessionNumber + "/", formNumber
						+ "_batch", sentenceFile);
				Printer.printWithTimeImportant(TAG, i + "");
			}
	}

	public static void convertTo16kMono(String folder) {
		File dir = new File(folder);
		File[] listOfFiles = dir.listFiles();
		BufferedWriter Lout = null;
		BufferedWriter Rout = null;
		BufferedWriter Mout = null;
		try {
			Lout = new BufferedWriter(new FileWriter(dir + "_L.batch"));
			Rout = new BufferedWriter(new FileWriter(dir + "_R.batch"));
			Mout = new BufferedWriter(new FileWriter(dir + "_M.batch"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println(dir.getName());

		(new File(dir.getAbsolutePath() + "/16kHzMonoLeft/")).mkdirs();
		(new File(dir.getAbsolutePath() + "/16kHzMonoRight/")).mkdirs();
		(new File(dir.getAbsolutePath() + "/16kHzMonoMixed/")).mkdirs();
		int i = 1;
		int total = listOfFiles.length;
		for (File file : listOfFiles) {
			if (file.isFile()) {

				String filename = file.getName().substring(0,
						file.getName().lastIndexOf("."));
				String sentence = filename.replace("_", " ");


				String path_L = file.getParent() + "/16kHzMonoLeft/"
						+ file.getName();
				String path_R = file.getParent() + "/16kHzMonoRight/"
						+ file.getName();
				String path_M = file.getParent() + "/16kHzMonoMixed/"
						+ file.getName();

				try {
					Lout.write(path_L + " " + sentence + "\n");
					Rout.write(path_R + " " + sentence + "\n");
					Mout.write(path_M + " " + sentence + "\n");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				String soxCommandL = "sox " + file.getAbsolutePath()
						+ " -b 16 -r 16k " + path_L + " remix 1";
				String soxCommandR = "sox " + file.getAbsolutePath()
						+ " -b 16 -r 16k " + path_R + " remix 2";
				String soxCommandM = "sox " + file.getAbsolutePath()
						+ " -b 16 -r 16k " + path_M + " remix 1,2";
				//
				System.out.println("processing " + i + " of " + total);
				System.out.println(soxCommandL);
				i++;
				Process p1;
				Process p2;
				Process p3;
				try {

					p1 = Runtime.getRuntime().exec(soxCommandL);
					p1.waitFor();
					p2 = Runtime.getRuntime().exec(soxCommandR);
					p2.waitFor();
					p3 = Runtime.getRuntime().exec(soxCommandM);
					p3.waitFor();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}


			}
		}
		try {
			Lout.close();
			Rout.close();
			Mout.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static int count(String filename) throws IOException {
		InputStream is = new BufferedInputStream(new FileInputStream(filename));
		try {
			byte[] c = new byte[1024];
			int count = 0;
			int readChars = 0;
			boolean empty = true;
			while ((readChars = is.read(c)) != -1) {
				empty = false;
				for (int i = 0; i < readChars; ++i) {
					if (c[i] == '\n') {
						++count;
					}
				}
			}
			return (count == 0 && !empty) ? 1 : count;
		} finally {
			is.close();
		}
	}

	public static void testGooglePhonemes(String path, String batchfile) {

		new FileProcessor(
				"/informatik/isr/wtm/TEMP/7twiefel/110302_MuliDecoderAsr/Raw/heinrichLab.google.refhyp",
				new FileLoop() {
					PhonemeCreator pc = PhonemeCreator
							.getInstance("heinrichLab.google.refhyp");
					final NISTAlign alignerPhonemes = new NISTAlign(true, true);
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

						r.addResult(ref);
						r.addResult(hyp);
						phonemesSpeech = pc.getPhonemes(r);

						String refPhonemes = "";
						for (String s : phonemesSpeech.get(0).getPhonemes())
							refPhonemes = refPhonemes + s + " ";

						String hypPhonemes = "";
						for (String s : phonemesSpeech.get(1).getPhonemes())
							hypPhonemes = hypPhonemes + s + " ";

						System.out.println(ref);
						System.out.println(hyp);

						alignerPhonemes.align(refPhonemes, hypPhonemes);
						alignerPhonemesReverse.align(hypPhonemes, refPhonemes);
						Printer.printColor(
								Printer.ANSI_BLUE,
								"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Phonemes  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
						alignerPhonemes.printSentenceSummary();
						alignerPhonemes.printTotalSummary();

						Printer.printColor(
								Printer.ANSI_BLUE,
								"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Phonemes Reverse  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
						alignerPhonemesReverse.printSentenceSummary();
						alignerPhonemesReverse.printTotalSummary();

						String alignedRef = alignerPhonemesReverse
								.getAlignedReference();
						String alignedHyp = alignerPhonemesReverse
								.getAlignedHypothesis();

						System.out.println(alignedRef);
						System.out.println(alignedHyp);
						alignedRef = alignedRef.replaceAll(" +", " ");
						alignedHyp = alignedHyp.replaceAll(" +", " ");
						String[] refWords = alignedRef.split(" ");
						String[] hypWords = alignedHyp.split(" ");

						for (int i = 0; i < refWords.length; i++) {
							String key = refWords[i] + ";" + hypWords[i];
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
						@SuppressWarnings("rawtypes")
						Iterator it = hm.entrySet().iterator();
						while (it.hasNext()) {
							@SuppressWarnings("rawtypes")
							Map.Entry pairs = (Map.Entry) it.next();
							System.out.println(pairs.getKey() + " = "
									+ pairs.getValue());
							it.remove(); // avoids a
											// ConcurrentModificationException
						}
					}

				});

	}

	public static void testGoogle(String path, String batchfile) {

		System.out.println("Starting BaseRecognizer");
		RawGoogleRecognizer bare = new RawGoogleRecognizer(key);
		FileInputStream fstream = null;
		try {
			fstream = new FileInputStream(path + batchfile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));

		String strLine;
		try {
			int resultCounter = 0;
			int noResultCounter = 0;
			while ((strLine = br.readLine()) != null) {
				// Print the content on the console
				// System.out.println(strLine);
				int endName = strLine.indexOf(" ");
				String filename = strLine.substring(0, endName);

				String file = filename.substring(filename.lastIndexOf("/") + 1,
						filename.length());

				Result r = bare.recognizeFromFile(filename);
				System.out.println();
				if (r != null) {
					resultCounter++;
					for (String s : r.getResultList()) {
						Printer.printGreen(file + " : " + s);
					}
				} else {
					noResultCounter++;
					Printer.printRed(file + " : no result");
				}
			}
			System.out.println("results: " + resultCounter);
			System.out.println("noResults: " + noResultCounter);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void testSLR(String path, String batchfile,
			String sentenceFile) {
		final NISTAlign alignerGoogle = new NISTAlign(true, true);
		final NISTAlign alignerLeven = new NISTAlign(true, true);
		System.out.println("Starting BaseRecognizer");
		RawGoogleRecognizer bare = new RawGoogleRecognizer(key);

		System.out.println("Starting LevenshteinRecognizer");
		SentencelistPostProcessor lr = new SentencelistPostProcessor(sentenceFile, 1,key);
		FileInputStream fstream = null;
		try {
			fstream = new FileInputStream(path + batchfile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine = null;
		String filename;
		String sentence;
		String file;

		int numberExamples;
		System.out.println("Starting loop");
		int count = 1;
		try {
			numberExamples = count(path + batchfile);
			while ((strLine = br.readLine()) != null) {
				int endName = strLine.indexOf(" ");
				filename = strLine.substring(0, endName);

				if (!filename.contains("/"))
					filename = path + filename;
				sentence = strLine.substring(endName + 1);
				file = filename.substring(filename.lastIndexOf("/") + 1,
						filename.length());

				sentence = sentence.replaceAll("[^a-zA-Z 0-9;]", "");
				sentence = sentence.replaceAll(" +", " ");
				if (!sentence.equals(""))
					if (sentence.charAt(0) == ' ')
						sentence = sentence.substring(1);

				Printer.printColor(Printer.ANSI_CYAN, count + "/"
						+ numberExamples + " " + file + " : " + sentence);

				Result r = bare.recognizeFromFile(filename);
				if (r != null) {

					String googleResult = r.getBestResult();
					Result r2 = lr.recognizeFromResult(r);

					String levenResult = r2.getBestResult();
					alignerGoogle.align(sentence, googleResult);
					Printer.printColor(
							Printer.ANSI_BLUE,
							"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
					alignerGoogle.printSentenceSummary();
					alignerGoogle.printTotalSummary();
					alignerLeven.align(sentence, levenResult);
					Printer.printColor(
							Printer.ANSI_BLUE,
							"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google Sentence List  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
					alignerLeven.printSentenceSummary();
					alignerLeven.printTotalSummary();
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Processed File: " + batchfile);
		System.out.println("Processed Examples: " + count);
		Printer.printColor(
				Printer.ANSI_BLUE,
				"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
		alignerGoogle.printTotalSummary();
		Printer.printColor(
				Printer.ANSI_BLUE,
				"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google Sentence List  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
		alignerLeven.printTotalSummary();
	}

	public static void testWER(String path, String batchfile, String configname) {
		Printer.printColor(Printer.ANSI_RED,
				"DONT FORGET TO ADJUST THE SENTENCES,WORDS,XML");

		final NISTAlign alignerGoogle = new NISTAlign(true, true);
		final NISTAlign alignerLeven = new NISTAlign(true, true);
		final NISTAlign alignerSphinxNgram = new NISTAlign(true, true);
		final NISTAlign alignerSphinxFsg = new NISTAlign(true, true);
		final NISTAlign alignerSphinxFsgSentences = new NISTAlign(true, true);
		;
		final NISTAlign alignerSphinxLeven = new NISTAlign(true, true);
		final NISTAlign alignerLLR = new NISTAlign(true, true);
		final NISTAlign alignerPNR = new NISTAlign(true, true);
		final NISTAlign alignerPGR = new NISTAlign(true, true);
		final NISTAlign alignerPGRsentences = new NISTAlign(true, true);
		final NISTAlign alignerPhonemes = new NISTAlign(true, true);

		System.out.println("Starting BaseRecognizer");
		RawGoogleRecognizer bare = new RawGoogleRecognizer(key);

		System.out.println("Starting LevenshteinRecognizer");
		SentencelistPostProcessor lr = new SentencelistPostProcessor(configname
				+ ".sentences", 1,key);

		System.out.println("Starting SimpleSphinxRecognizerNgram");
		SphinxRecognizer sr_ngram = new SphinxRecognizer(configname
				+ ".ngram.xml");

		System.out.println("Starting SimpleSphinxRecognizerFSG");
		SphinxRecognizer sr_fsg = new SphinxRecognizer(configname
				+ ".fsg.xml");

		System.out.println("Starting SimpleSphinxRecognizerFSGsentenceList");
		SphinxRecognizer sr_fsg_sentences = new SphinxRecognizer(
				configname + ".fsgsentences.xml");

		System.out.println("Starting PhonemeNgramRecognizer");
		final SphinxBasedPostProcessor pnr = new SphinxBasedPostProcessor(
				configname + ".pngram.xml", configname + ".words", 0, 0, 0);

		System.out.println("Starting PhonemeGrammarRecognizer");
		final SphinxBasedPostProcessor pgr = new SphinxBasedPostProcessor(
				configname + ".pgrammar.xml", configname + ".words", 0, 0, 0);

		System.out.println("Starting PhonemeGrammarRecognizer");
		final SphinxBasedPostProcessor pgr_sentences = new SphinxBasedPostProcessor(
				configname + ".pgrammarsentences.xml", configname + ".words",
				0, 0, 0);

		System.out.println("Starting LexiconLookupRecognizer");
		WordlistPostProcessor llr = new WordlistPostProcessor(configname
				+ ".words",key);

		FileInputStream fstream = null;
		try {
			fstream = new FileInputStream(path + batchfile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine = null;
		String filename;
		String sentence;
		String file;

		try {
			int numberExamples = count(path + batchfile);
			System.out.println("Starting loop");
			int count = 1;

			while ((strLine = br.readLine()) != null) {

				int endName = strLine.indexOf(" ");
				filename = strLine.substring(0, endName);
				sentence = strLine.substring(endName + 1);
				file = filename.substring(filename.lastIndexOf("/") + 1,
						filename.length());

				sentence = sentence.replaceAll("[^a-zA-Z 0-9;]", "");
				sentence = sentence.replaceAll(" +", " ");
				if (!sentence.equals(""))
					if (sentence.charAt(0) == ' ')
						sentence = sentence.substring(1);

				Printer.printColor(Printer.ANSI_CYAN, count + "/"
						+ numberExamples + " " + file + " : " + sentence);

				Result r = bare.recognizeFromFile(filename);
				if (r != null) {

					String googleResult = r.getBestResult();
					Result r2 = lr.recognizeFromResult(r);

					String levenResult = r2.getBestResult();
					alignerGoogle.align(sentence, googleResult);
					Printer.printColor(
							Printer.ANSI_BLUE,
							"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
					alignerGoogle.printSentenceSummary();
					alignerGoogle.printTotalSummary();
					alignerLeven.align(sentence, levenResult);
					Printer.printColor(
							Printer.ANSI_BLUE,
							"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google Sentence List  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
					alignerLeven.printSentenceSummary();
					alignerLeven.printTotalSummary();
					r = new Result();
					r.addResult(googleResult);
					r = llr.recognizeFromResult(r);
					String hypLLR = r.getBestResult();
					alignerLLR.align(sentence, hypLLR);

					Printer.printColor(
							Printer.ANSI_BLUE,
							"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google Word List  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
					alignerLLR.printSentenceSummary();
					alignerLLR.printTotalSummary();

					r = new Result();
					r.addResult(googleResult);
					r = pnr.recognizeFromResult(r);
					if (r != null) {
						String hypPNR = r.getBestResult();
						alignerPNR.align(sentence, hypPNR);
					}

					Printer.printColor(
							Printer.ANSI_BLUE,
							"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google Phoneme N-Gram  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
					alignerPNR.printSentenceSummary();
					alignerPNR.printTotalSummary();

					r = new Result();
					r.addResult(googleResult);
					r = pgr.recognizeFromResult(r);
					if (r != null) {
						String hypPGR = r.getBestResult();
						alignerPGR.align(sentence, hypPGR);
					}

					Printer.printColor(
							Printer.ANSI_BLUE,
							"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google Phoneme Grammar  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
					alignerPGR.printSentenceSummary();
					alignerPGR.printTotalSummary();

					r = new Result();
					r.addResult(googleResult);
					r = pgr_sentences.recognizeFromResult(r);
					if (r != null) {
						String hypPGRsentences = r.getBestResult();
						alignerPGRsentences.align(sentence, hypPGRsentences);
					}

					Printer.printColor(
							Printer.ANSI_BLUE,
							"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google Phoneme Grammar Sentence List \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
					alignerPGRsentences.printSentenceSummary();
					alignerPGRsentences.printTotalSummary();

					if (r != null) {
						alignerPhonemes.align(r.getRefPhoneme(),
								r.getHypPhoneme());
					} else
						Printer.printRed("NO RESULT");

					Printer.printColor(
							Printer.ANSI_BLUE,
							"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google Phonemes \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
					alignerPhonemes.printSentenceSummary();
					alignerPhonemes.printTotalSummary();

				}

				r = sr_fsg.recognizeFromFile(filename);
				if (r != null) {
					alignerSphinxFsg.align(sentence, r.getBestResult());
					Printer.printColor(
							Printer.ANSI_BLUE,
							"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Sphinx Grammar  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
					alignerSphinxFsg.printSentenceSummary();
					alignerSphinxFsg.printTotalSummary();
				}
				r = sr_fsg_sentences.recognizeFromFile(filename);
				if (r != null) {
					alignerSphinxFsgSentences.align(sentence,
							r.getBestResult());
					Printer.printColor(
							Printer.ANSI_BLUE,
							"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Sphinx Grammar Sentencelist  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
					alignerSphinxFsgSentences.printSentenceSummary();
					alignerSphinxFsgSentences.printTotalSummary();
				}
				r = sr_ngram.recognizeFromFile(filename);
				if (r != null) {
					alignerSphinxNgram.align(sentence, r.getBestResult());
					Printer.printColor(
							Printer.ANSI_BLUE,
							"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Sphinx N-Gram  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
					alignerSphinxNgram.printSentenceSummary();
					alignerSphinxNgram.printTotalSummary();
				}
				System.out.println(r);
				if (r != null) {
					r = lr.recognizeFromResult(r);
					alignerSphinxLeven
							.align(sentence, r.getBestResult());
					Printer.printColor(
							Printer.ANSI_BLUE,
							"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Sphinx Sentence List \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
					alignerSphinxLeven.printSentenceSummary();
					alignerSphinxLeven.printTotalSummary();
				}
				count++;

			}
			// print endresults
			System.out.println("Processed File: " + batchfile);
			System.out.println("Processed Examples: " + count);
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerGoogle.printTotalSummary();
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google Sentence List  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerLeven.printTotalSummary();
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google Word List  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerLLR.printTotalSummary();
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google Phoneme N-Gram  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerPNR.printTotalSummary();
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google Phoneme Grammar  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerPGR.printTotalSummary();
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google Phoneme Grammar Sentence List \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerPGRsentences.printTotalSummary();
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google Phonemes \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerPhonemes.printTotalSummary();
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Sphinx Grammar  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerSphinxFsg.printTotalSummary();
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Sphinx Grammar Sentence List \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerSphinxFsgSentences.printTotalSummary();
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Sphinx N-Gram  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerSphinxNgram.printTotalSummary();
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Sphinx Sentence List  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerSphinxLeven.printTotalSummary();

			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerGoogle.printNISTTotalSummary();
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google Sentence List  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerLeven.printNISTTotalSummary();
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google Word List  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerLLR.printNISTTotalSummary();
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google Phoneme N-Gram  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerPNR.printNISTTotalSummary();
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google Phoneme Grammar  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerPGR.printNISTTotalSummary();
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google Phoneme Grammar Sentence List \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerPGRsentences.printNISTTotalSummary();
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google Phonemes \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerPhonemes.printNISTTotalSummary();
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Sphinx Grammar  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerSphinxFsg.printNISTTotalSummary();
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Sphinx Grammar Sentence List \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerSphinxFsgSentences.printNISTTotalSummary();
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Sphinx N-Gram  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerSphinxNgram.printNISTTotalSummary();
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Sphinx Sentence List  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerSphinxLeven.printNISTTotalSummary();

			PerformanceFileWriter tableRows = new PerformanceFileWriter();

			tableRows.add("Google", 0, 0, alignerGoogle);
			tableRows.add("Google Sentence List", 0, 0, alignerLeven);
			tableRows.add("Google Word List", 0, 0, alignerLLR);
			tableRows.add("Google Phoneme N-Gram", pnr.getLanguageWeight(),
					pnr.getWIP(), alignerPNR);
			tableRows.add("Google Phoneme Grammar", pgr.getLanguageWeight(),
					pgr.getWIP(), alignerPGR);
			tableRows.add("Google Phoneme Grammar Sentence List",
					pgr_sentences.getLanguageWeight(), pgr_sentences.getWIP(),
					alignerPGRsentences);
			tableRows.add("Google Phonemes", 0, 0, alignerPhonemes);
			tableRows.add("Sphinx Grammar", sr_fsg.getLanguageWeight(),
					sr_fsg.getWIP(), alignerSphinxFsg);
			tableRows.add("Sphinx Grammar Sentence List",
					sr_fsg_sentences.getLanguageWeight(),
					sr_fsg_sentences.getWIP(), alignerSphinxFsgSentences);
			tableRows.add("Sphinx N-Gram", sr_ngram.getLanguageWeight(),
					sr_ngram.getWIP(), alignerSphinxNgram);
			tableRows.add("Sphinx Sentence List", 0, 0, alignerSphinxLeven);

			tableRows.createPerformanceTable(batchfile
					+ " without empty results", false, false, true);
			tableRows.createPerformanceTable(batchfile
					+ " without empty results" + " rounded", false, true, true);
			tableRows.createPerformanceTable(batchfile
					+ " without empty results" + " in percent", true, false,
					true);
			tableRows.createPerformanceTable(batchfile
					+ " without empty results" + " rounded in percent", true,
					true, true);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void testWERall(String path, String batchfile,
			String configname, String title) {
		Printer.printColor(Printer.ANSI_RED,
				"DONT FORGET TO ADJUST THE SENTENCES,WORDS,XML");
		System.out.println("processing batch file: " + batchfile);
		boolean caching = true;// true;
		boolean loadCache = false;

		final NISTAlign alignerGoogle = new NISTAlign(true, true);
		final NISTAlign alignerLeven = new NISTAlign(true, true);
		final NISTAlign alignerSphinxNgram = new NISTAlign(true, true);
		final NISTAlign alignerSphinxFsg = new NISTAlign(true, true);
		final NISTAlign alignerSphinxFsgSentences = new NISTAlign(true, true);
		;
		final NISTAlign alignerSphinxLeven = new NISTAlign(true, true);
		final NISTAlign alignerLLR = new NISTAlign(true, true);
		final NISTAlign alignerPNR = new NISTAlign(true, true);
		final NISTAlign alignerPUR = new NISTAlign(true, true);
		final NISTAlign alignerPGR = new NISTAlign(true, true);
		final NISTAlign alignerPGRsentences = new NISTAlign(true, true);
		final NISTAlign alignerPhonemes = new NISTAlign(true, true);

		System.out.println("Starting BaseRecognizer");
		RawGoogleRecognizer bare = new RawGoogleRecognizer(key);

		System.out.println("Starting LevenshteinRecognizer");
		SentencelistPostProcessor lr = new SentencelistPostProcessor(configname
				+ ".sentences", 1,key);

		System.out.println("Starting SimpleSphinxRecognizerNgram");
		SphinxRecognizer sr_ngram = new SphinxRecognizer(configname
				+ ".ngram.xml");

		System.out.println("Starting SimpleSphinxRecognizerFSG");
		SphinxRecognizer sr_fsg = new SphinxRecognizer(configname
				+ ".fsg.xml");

		System.out.println("Starting SimpleSphinxRecognizerFSGsentenceList");
		SphinxRecognizer sr_fsg_sentences = new SphinxRecognizer(
				configname + ".fsgsentences.xml");

		System.out.println("Starting PhonemeNgramRecognizer");
		final SphinxBasedPostProcessor pnr = new SphinxBasedPostProcessor(
				configname + ".pngram.xml", configname + ".words", 0, 0, 0);

		System.out.println("Starting PhonemeUnigramRecognizer");
		final SphinxBasedPostProcessor pur = new SphinxBasedPostProcessor(
				configname + ".punigram.xml", configname + ".words", 0, 0, 0);

		System.out.println("Starting PhonemeGrammarRecognizer");
		final SphinxBasedPostProcessor pgr = new SphinxBasedPostProcessor(
				configname + ".pgrammar.xml", configname + ".words", 0, 0, 0);

		System.out.println("Starting PhonemeGrammarSentenceRecognizer");
		final SphinxBasedPostProcessor pgr_sentences = new SphinxBasedPostProcessor(
				configname + ".pgrammarsentences.xml", configname + ".words",
				0, 0, 0);

		System.out.println("Starting LexiconLookupRecognizer");
		WordlistPostProcessor llr = new WordlistPostProcessor(configname
				+ ".words",key);

		FileInputStream fstream = null;
		try {
			fstream = new FileInputStream(path + batchfile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine = null;
		String filename;
		String sentence;
		String file;

		try {
			int numberExamples = count(path + batchfile);
			System.out.println("Starting loop");
			int count = 1;
			int emptyGoogle = 0;
			int emptyLeven = 0;
			int emptyLLR = 0;
			int emptyPGR = 0;
			int emptyPNR = 0;
			int emptyPUR = 0;
			int emptyPGRsentences = 0;
			int emptySphinxFsg = 0;
			int emptySphinxFsgSentences = 0;
			int emptySphinxNgram = 0;
			int emptySphinxLeven = 0;

			long totalTimeGoogle = 0;
			long totalTimeLeven = 0;
			long totalTimeLLR = 0;
			long totalTimePGR = 0;
			long totalTimePNR = 0;
			long totalTimePUR = 0;
			long totalTimePGRsentences = 0;
			long totalTimeSphinxFsg = 0;
			long totalTimeSphinxFsgSentences = 0;
			long totalTimeSphinxNgram = 0;
			long totalTimeSphinxLeven = 0;

			while ((strLine = br.readLine()) != null) {

				int endName = strLine.indexOf(" ");
				filename = strLine.substring(0, endName);
				sentence = strLine.substring(endName + 1);
				file = filename.substring(filename.lastIndexOf("/") + 1,
						filename.length());

				sentence = sentence.replaceAll("[^a-zA-Z 0-9;]", "");
				sentence = sentence.replaceAll(" +", " ");
				if (!sentence.equals(""))
					if (sentence.charAt(0) == ' ')
						sentence = sentence.substring(1);

				Printer.printColor(Printer.ANSI_CYAN, count + "/"
						+ numberExamples + " " + file + " : " + sentence);

				String googleResult = "";
				String levenResult = "";
				String hypLLR = "";
				String hypPNR = "";
				String hypPUR = "";
				String hypPGR = "";
				String hypPGRsentences = "";

				long timeGoogle = Printer.reset();
				long timeLeven = 0;
				long timeLLR = 0;
				long timePGR = 0;
				long timePNR = 0;
				long timePUR = 0;
				long timePGRsentences = 0;
				long timeSphinxFsg = 0;
				long timeSphinxFsgSentences = 0;
				long timeSphinxNgram = 0;
				long timeSphinxLeven = 0;

				Result r = null;
				if (!loadCache)
					r = bare.recognizeFromFile(filename);
				else
					r = Result.load("cache/" + batchfile + "/" + bare.getName()
							+ "/" + file + ".res");
				timeGoogle = Printer.reset() - timeGoogle;
				totalTimeGoogle += timeGoogle;
				if (r != null) {
					googleResult = r.getBestResult();

					if (caching) {
						File theDir = new File("cache/" + batchfile);
						if (!theDir.exists()) {
							theDir.mkdir();
						}

						String cachepath = "cache/" + batchfile + "/"
								+ bare.getName();
						theDir = new File(cachepath);
						if (!theDir.exists()) {
							theDir.mkdir();
						}
						r.save(cachepath + "/" + file + ".res");
						System.out.println(cachepath);

					}
					timeLeven = Printer.reset();
					r = lr.recognizeFromResult(r);

					timeLeven = Printer.reset() - timeLeven;
					totalTimeLeven += timeLeven;
					if (caching && r != null) {

						String cachepath = "cache/" + batchfile + "/"
								+ lr.getName();
						File theDir = new File(cachepath);
						if (!theDir.exists()) {
							theDir.mkdir();
						}
						r.save(cachepath + "/" + file + ".res");
						System.out.println(cachepath);

					}
					if (r != null)
						levenResult = r.getBestResult();
					r = new Result();
					r.addResult(googleResult);

					timeLLR = Printer.reset();
					r = llr.recognizeFromResult(r);
					timeLLR = Printer.reset() - timeLLR;
					totalTimeLLR += timeLLR;

					if (caching && r != null) {
						String cachepath = "cache/" + batchfile + "/"
								+ llr.getName();
						File theDir = new File(cachepath);
						if (!theDir.exists()) {
							theDir.mkdir();
						}
						r.save(cachepath + "/" + file + ".res");
						System.out.println(cachepath);

					}
					if (r != null)
						hypLLR = r.getBestResult();
					r = new Result();
					r.addResult(googleResult);

					timePNR = Printer.reset();
					r = pnr.recognizeFromResult(r);
					timePNR = Printer.reset() - timePNR;
					totalTimePNR += timePNR;

					if (caching && r != null) {
						String cachepath = "cache/" + batchfile + "/"
								+ pnr.getName() + "Ngram";
						File theDir = new File(cachepath);
						if (!theDir.exists()) {
							theDir.mkdir();
						}
						r.save(cachepath + "/" + file + ".res");
						System.out.println(cachepath);

					}
					if (r != null)
						hypPNR = r.getBestResult();

					r = new Result();
					r.addResult(googleResult);
					timePUR = Printer.reset();
					r = pur.recognizeFromResult(r);
					timePUR = Printer.reset() - timePUR;
					totalTimePUR += timePUR;

					if (caching && r != null) {
						String cachepath = "cache/" + batchfile + "/"
								+ pur.getName() + "Unigram";
						File theDir = new File(cachepath);
						if (!theDir.exists()) {
							theDir.mkdir();
						}
						r.save(cachepath + "/" + file + ".res");
						System.out.println(cachepath);

					}
					if (r != null)
						hypPUR = r.getBestResult();

					r = new Result();
					r.addResult(googleResult);

					timePGR = Printer.reset();
					r = pgr.recognizeFromResult(r);
					timePGR = Printer.reset() - timePGR;
					totalTimePGR += timePGR;

					if (caching && r != null) {
						String cachepath = "cache/" + batchfile + "/"
								+ pgr.getName() + "Grammar";
						File theDir = new File(cachepath);
						if (!theDir.exists()) {
							theDir.mkdir();
						}
						r.save(cachepath + "/" + file + ".res");
						System.out.println(cachepath);

					}
					if (r != null)
						hypPGR = r.getBestResult();
					r = new Result();
					r.addResult(googleResult);

					timePGRsentences = Printer.reset();
					r = pgr_sentences.recognizeFromResult(r);
					timePGRsentences = Printer.reset() - timePGRsentences;
					totalTimePGRsentences += timePGRsentences;

					if (caching && r != null) {
						String cachepath = "cache/" + batchfile + "/"
								+ pgr_sentences.getName() + "GrammarSentences";
						File theDir = new File(cachepath);
						if (!theDir.exists()) {
							theDir.mkdir();
						}
						r.save(cachepath + "/" + file + ".res");
						System.out.println(cachepath);

					}
					if (r != null)
						hypPGRsentences = r.getBestResult();

				} else {
					emptyGoogle++;
					emptyLeven++;
					emptyLLR++;
					emptyPGR++;
					emptyPNR++;
					emptyPGRsentences++;
				}

				alignerGoogle.align(sentence, googleResult);
				Printer.printColor(
						Printer.ANSI_BLUE,
						"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
				alignerGoogle.printSentenceSummary();
				alignerGoogle.printTotalSummary();
				alignerLeven.align(sentence, levenResult);
				Printer.printColor(
						Printer.ANSI_BLUE,
						"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google Sentence List  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
				alignerLeven.printSentenceSummary();
				alignerLeven.printTotalSummary();

				alignerLLR.align(sentence, hypLLR);
				Printer.printColor(
						Printer.ANSI_BLUE,
						"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google Word List  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
				alignerLLR.printSentenceSummary();
				alignerLLR.printTotalSummary();

				alignerPNR.align(sentence, hypPNR);
				Printer.printColor(
						Printer.ANSI_BLUE,
						"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google Phoneme N-Gram  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
				alignerPNR.printSentenceSummary();
				alignerPNR.printTotalSummary();

				alignerPUR.align(sentence, hypPUR);
				Printer.printColor(
						Printer.ANSI_BLUE,
						"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google Phoneme UniGram  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
				alignerPUR.printSentenceSummary();
				alignerPUR.printTotalSummary();

				alignerPGR.align(sentence, hypPGR);
				Printer.printColor(
						Printer.ANSI_BLUE,
						"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google Phoneme Grammar  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
				alignerPGR.printSentenceSummary();
				alignerPGR.printTotalSummary();

				alignerPGRsentences.align(sentence, hypPGRsentences);
				Printer.printColor(
						Printer.ANSI_BLUE,
						"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google Phoneme Grammar Sentence List \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
				alignerPGRsentences.printSentenceSummary();
				alignerPGRsentences.printTotalSummary();

				timeSphinxFsg = Printer.reset();
				r = sr_fsg.recognizeFromFile(filename);
				timeSphinxFsg = Printer.reset() - timeSphinxFsg;
				totalTimeSphinxFsg += timeSphinxFsg;

				String result = "";
				if (r != null) {
					result = r.getBestResult();
					if (caching) {
						String cachepath = "cache/" + batchfile + "/"
								+ sr_fsg.getName() + "Grammar";
						File theDir = new File(cachepath);
						if (!theDir.exists()) {
							theDir.mkdir();
						}
						r.save(cachepath + "/" + file + ".res");
						System.out.println(cachepath);

					}
				} else
					emptySphinxFsg++;
				alignerSphinxFsg.align(sentence, result);
				Printer.printColor(
						Printer.ANSI_BLUE,
						"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Sphinx Grammar  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
				alignerSphinxFsg.printSentenceSummary();
				alignerSphinxFsg.printTotalSummary();

				timeSphinxFsgSentences = Printer.reset();
				r = sr_fsg_sentences.recognizeFromFile(filename);
				timeSphinxFsgSentences = Printer.reset()
						- timeSphinxFsgSentences;
				totalTimeSphinxFsgSentences += timeSphinxFsgSentences;

				result = "";
				if (r != null) {
					result = r.getBestResult();
					if (caching) {
						String cachepath = "cache/" + batchfile + "/"
								+ sr_fsg_sentences.getName()
								+ "GrammarSentences";
						File theDir = new File(cachepath);
						if (!theDir.exists()) {
							theDir.mkdir();
						}
						r.save(cachepath + "/" + file + ".res");
						System.out.println(cachepath);

					}
				} else
					emptySphinxFsgSentences++;

				alignerSphinxFsgSentences.align(sentence, result);
				Printer.printColor(
						Printer.ANSI_BLUE,
						"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Sphinx Grammar Sentencelist  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
				alignerSphinxFsgSentences.printSentenceSummary();
				alignerSphinxFsgSentences.printTotalSummary();

				timeSphinxNgram = Printer.reset();
				r = sr_ngram.recognizeFromFile(filename);
				timeSphinxNgram = Printer.reset() - timeSphinxNgram;
				totalTimeSphinxNgram += timeSphinxNgram;

				result = "";
				if (r != null) {
					result = r.getBestResult();
					if (caching) {
						String cachepath = "cache/" + batchfile + "/"
								+ sr_ngram.getName() + "Ngram";
						File theDir = new File(cachepath);
						if (!theDir.exists()) {
							theDir.mkdir();
						}
						r.save(cachepath + "/" + file + ".res");
						System.out.println(cachepath);

					}
				} else
					emptySphinxNgram++;

				alignerSphinxNgram.align(sentence, result);
				Printer.printColor(
						Printer.ANSI_BLUE,
						"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Sphinx N-Gram  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
				alignerSphinxNgram.printSentenceSummary();
				alignerSphinxNgram.printTotalSummary();



				timeSphinxLeven = Printer.reset();
				r = lr.recognizeFromResult(r);
				timeSphinxLeven = Printer.reset() - timeSphinxLeven;
				totalTimeSphinxLeven += timeSphinxLeven;

				result = "";
				if (r != null) {
					result = r.getBestResult();
					if (caching) {
						String cachepath = "cache/" + batchfile + "/"
								+ lr.getName() + "Sphinx";
						File theDir = new File(cachepath);
						if (!theDir.exists()) {
							theDir.mkdir();
						}
						r.save(cachepath + "/" + file + ".res");
						System.out.println(cachepath);

					}
				} else
					emptySphinxLeven++;

				alignerSphinxLeven.align(sentence, result);
				Printer.printColor(
						Printer.ANSI_BLUE,
						"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Sphinx Sentence List \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
				alignerSphinxLeven.printSentenceSummary();
				alignerSphinxLeven.printTotalSummary();

				count++;

			}
			// print endresults
			System.out.println("Processed File: " + batchfile);
			System.out.println("Processed Examples: " + count);
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerGoogle.printTotalSummary();
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google Sentence List  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerLeven.printTotalSummary();
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google Word List  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerLLR.printTotalSummary();
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google Phoneme N-Gram  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerPNR.printTotalSummary();
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google Phoneme UniGram  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerPUR.printTotalSummary();
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google Phoneme Grammar  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerPGR.printTotalSummary();
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google Phoneme Grammar Sentence List \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerPGRsentences.printTotalSummary();
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google Phonemes \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerPhonemes.printTotalSummary();
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Sphinx Grammar  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerSphinxFsg.printTotalSummary();
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Sphinx Grammar Sentence List \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerSphinxFsgSentences.printTotalSummary();
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Sphinx N-Gram  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerSphinxNgram.printTotalSummary();
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Sphinx Sentence List  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerSphinxLeven.printTotalSummary();

			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerGoogle.printNISTTotalSummary();
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google Sentence List  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerLeven.printNISTTotalSummary();
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google Word List  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerLLR.printNISTTotalSummary();
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google Phoneme N-Gram  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerPNR.printNISTTotalSummary();
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google Phoneme UniGram  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerPUR.printNISTTotalSummary();
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google Phoneme Grammar  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerPGR.printNISTTotalSummary();
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google Phoneme Grammar Sentence List \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerPGRsentences.printNISTTotalSummary();
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google Phonemes \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerPhonemes.printNISTTotalSummary();
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Sphinx Grammar  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerSphinxFsg.printNISTTotalSummary();
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Sphinx Grammar Sentence List \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerSphinxFsgSentences.printNISTTotalSummary();
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Sphinx N-Gram  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerSphinxNgram.printNISTTotalSummary();
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Sphinx Sentence List  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerSphinxLeven.printNISTTotalSummary();

			PerformanceFileWriter tableRows = new PerformanceFileWriter();

			tableRows.add("Google", 0, 0, alignerGoogle, emptyGoogle,
					totalTimeGoogle / numberExamples);
			tableRows.add("Google Sentence List", 0, 0, alignerLeven,
					emptyLeven, totalTimeLeven / numberExamples);
			tableRows.add("Google Word List", 0, 0, alignerLLR, emptyLLR,
					totalTimeLLR / numberExamples);
			tableRows.add("Google Phoneme N-Gram", pnr.getLanguageWeight(),
					pnr.getWIP(), alignerPNR, emptyPNR, totalTimePNR
							/ numberExamples);
			tableRows.add("Google Phoneme Unigram", pur.getLanguageWeight(),
					pur.getWIP(), alignerPUR, emptyPUR, totalTimePUR
							/ numberExamples);
			tableRows.add("Google Phoneme Grammar", pgr.getLanguageWeight(),
					pgr.getWIP(), alignerPGR, emptyPGR, totalTimePGR
							/ numberExamples);
			tableRows.add("Google Phoneme Grammar Sentence List",
					pgr_sentences.getLanguageWeight(), pgr_sentences.getWIP(),
					alignerPGRsentences, emptyPGRsentences,
					totalTimePGRsentences / numberExamples);

			tableRows.add("Sphinx Grammar", sr_fsg.getLanguageWeight(),
					sr_fsg.getWIP(), alignerSphinxFsg, emptySphinxFsg,
					totalTimeSphinxFsg / numberExamples);
			tableRows.add("Sphinx Grammar Sentence List",
					sr_fsg_sentences.getLanguageWeight(),
					sr_fsg_sentences.getWIP(), alignerSphinxFsgSentences,
					emptySphinxFsgSentences, totalTimeSphinxFsgSentences
							/ numberExamples);
			tableRows.add("Sphinx N-Gram", sr_ngram.getLanguageWeight(),
					sr_ngram.getWIP(), alignerSphinxNgram, emptySphinxNgram,
					totalTimeSphinxNgram / numberExamples);
			tableRows.add("Sphinx Sentence List", 0, 0, alignerSphinxLeven,
					emptySphinxLeven, totalTimeSphinxLeven / numberExamples);

			tableRows.createPerformanceTable(title
					+ ".rounded_in_percent_with_empty_results", true, true,
					true);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void testWERallUnigram(String path, String batchfile,
			String configname, String title) {
		Printer.printColor(Printer.ANSI_RED,
				"DONT FORGET TO ADJUST THE SENTENCES,WORDS,XML");
		System.out.println("processing batch file: " + batchfile);
		boolean caching = true;

		final NISTAlign alignerGoogle = new NISTAlign(true, true);
		final NISTAlign alignerLLR = new NISTAlign(true, true);
		final NISTAlign alignerPNR = new NISTAlign(true, true);

		System.out.println("Starting BaseRecognizer");
		RawGoogleRecognizer bare = new RawGoogleRecognizer(key);

		System.out.println("Starting PhonemeNgramRecognizer");
		final SphinxBasedPostProcessor pnr = new SphinxBasedPostProcessor(
				configname + ".punigram.xml", configname + ".words", 0, 0, 0);

		System.out.println("Starting LexiconLookupRecognizer");
		WordlistPostProcessor llr = new WordlistPostProcessor(configname
				+ ".words",key);

		FileInputStream fstream = null;
		try {
			fstream = new FileInputStream(path + batchfile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine = null;
		String filename;
		String sentence;
		String file;

		try {
			int numberExamples = count(path + batchfile);
			System.out.println("Starting loop");
			int count = 1;
			int emptyGoogle = 0;
			int emptyPNR = 0;
			int emptyLLR = 0;

			long totalTimeGoogle = 0;
			long totalTimeLLR = 0;
			long totalTimePNR = 0;

			while ((strLine = br.readLine()) != null) {

				int endName = strLine.indexOf(" ");
				filename = strLine.substring(0, endName);
				sentence = strLine.substring(endName + 1);
				file = filename.substring(filename.lastIndexOf("/") + 1,
						filename.length());

				sentence = sentence.replaceAll("[^a-zA-Z 0-9;]", "");
				sentence = sentence.replaceAll(" +", " ");
				if (!sentence.equals(""))
					if (sentence.charAt(0) == ' ')
						sentence = sentence.substring(1);

				Printer.printColor(Printer.ANSI_CYAN, count + "/"
						+ numberExamples + " " + file + " : " + sentence);


				String googleResult = "";
				String hypLLR = "";
				String hypPNR = "";

				long timeGoogle = Printer.reset();
				long timeLLR = 0;
				long timePNR = 0;

				Result r = bare.recognizeFromFile(filename);
				timeGoogle = Printer.reset() - timeGoogle;
				totalTimeGoogle += timeGoogle;
				if (r != null) {
					googleResult = r.getBestResult();

					if (caching) {
						File theDir = new File("cache/" + batchfile);
						if (!theDir.exists()) {
							theDir.mkdir();
						}

						String cachepath = "cache/" + batchfile + "/"
								+ bare.getName();
						theDir = new File(cachepath);
						if (!theDir.exists()) {
							theDir.mkdir();
						}
						r.save(cachepath + "/" + file + ".res");
						System.out.println(cachepath);

					}

					timeLLR = Printer.reset();
					r = llr.recognizeFromResult(r);
					timeLLR = Printer.reset() - timeLLR;
					totalTimeLLR += timeLLR;

					if (caching && r != null) {
						String cachepath = "cache/" + batchfile + "/"
								+ llr.getName();
						File theDir = new File(cachepath);
						if (!theDir.exists()) {
							theDir.mkdir();
						}
						r.save(cachepath + "/" + file + ".res");
						System.out.println(cachepath);

					}
					if (r != null)
						hypLLR = r.getBestResult();

					r = new Result();
					r.addResult(googleResult);

					timePNR = Printer.reset();
					r = pnr.recognizeFromResult(r);
					timePNR = Printer.reset() - timePNR;
					totalTimePNR += timePNR;

					if (caching && r != null) {
						String cachepath = "cache/" + batchfile + "/"
								+ pnr.getName() + "Ngram";
						File theDir = new File(cachepath);
						if (!theDir.exists()) {
							theDir.mkdir();
						}
						r.save(cachepath + "/" + file + ".res");
						System.out.println(cachepath);

					}
					if (r != null)
						hypPNR = r.getBestResult();

				} else {
					emptyGoogle++;
					emptyLLR++;
					emptyPNR++;

				}

				alignerGoogle.align(sentence, googleResult);
				Printer.printColor(
						Printer.ANSI_BLUE,
						"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
				alignerGoogle.printSentenceSummary();
				alignerGoogle.printTotalSummary();

				alignerLLR.align(sentence, hypLLR);
				Printer.printColor(
						Printer.ANSI_BLUE,
						"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google LLR  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
				alignerLLR.printSentenceSummary();
				alignerLLR.printTotalSummary();

				alignerPNR.align(sentence, hypPNR);
				Printer.printColor(
						Printer.ANSI_BLUE,
						"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google Phoneme N-Gram  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
				alignerPNR.printSentenceSummary();
				alignerPNR.printTotalSummary();

				count++;

			}
			// print endresults
			System.out.println("Processed File: " + batchfile);
			System.out.println("Processed Examples: " + count);
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerGoogle.printTotalSummary();

			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google LLR  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerLLR.printTotalSummary();

			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google Phoneme N-Gram  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerPNR.printTotalSummary();

			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerGoogle.printNISTTotalSummary();

			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google LLR  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerLLR.printNISTTotalSummary();

			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google Phoneme N-Gram  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerPNR.printNISTTotalSummary();

			PerformanceFileWriter tableRows = new PerformanceFileWriter();

			tableRows.add("Google", 0, 0, alignerGoogle, emptyGoogle,
					totalTimeGoogle / numberExamples);

			tableRows.add("Google Word List", 0, 0, alignerLLR, emptyLLR,
					totalTimeLLR / numberExamples);

			tableRows.add("Google Phoneme N-Gram", pnr.getLanguageWeight(),
					pnr.getWIP(), alignerPNR, emptyPNR, totalTimePNR
							/ numberExamples);


			tableRows.createPerformanceTable(title
					+ ".rounded_in_percent_with_empty_results", true, true,
					false);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void testWERallPhonemeConfusion(String path,
			String batchfile, String configname, String title) {
		Printer.printColor(Printer.ANSI_RED,
				"DONT FORGET TO ADJUST THE SENTENCES,WORDS,XML");

		final NISTAlign alignerGoogle = new NISTAlign(true, true);
		final NISTAlign alignerStandard = new NISTAlign(true, true);
		final NISTAlign alignerIPA = new NISTAlign(true, true);
		final NISTAlign alignerGoogleReverse = new NISTAlign(true, true);

		System.out.println("Starting BaseRecognizer");
		RawGoogleRecognizer bare = new RawGoogleRecognizer(key);

		System.out.println("Starting standard");
		final SphinxBasedPostProcessor standard = new SphinxBasedPostProcessor(
				configname + ".pngram.xml", configname + ".words", 0, 0, 0);

		System.out.println("Starting ipa");
		final SphinxBasedPostProcessor ipa = new SphinxBasedPostProcessor(
				configname + ".pngram.xml", configname + ".words", 1.6f, 2.85f,
				1);

		System.out.println("Starting google reverse");
		final SphinxBasedPostProcessor googleReverse = new SphinxBasedPostProcessor(
				configname + ".pngram.xml", configname + ".words", 2.1f, 4.1f,
				2);

		FileInputStream fstream = null;
		try {
			fstream = new FileInputStream(path + batchfile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine = null;
		String filename;
		String sentence;
		String file;

		try {
			int numberExamples = count(path + batchfile);
			System.out.println("Starting loop");
			int count = 1;

			int emptyResults = 0;
			while ((strLine = br.readLine()) != null) {

				int endName = strLine.indexOf(" ");
				filename = strLine.substring(0, endName);
				sentence = strLine.substring(endName + 1);
				file = filename.substring(filename.lastIndexOf("/") + 1,
						filename.length());

				sentence = sentence.replaceAll("[^a-zA-Z 0-9;]", "");
				sentence = sentence.replaceAll(" +", " ");
				if (!sentence.equals(""))
					if (sentence.charAt(0) == ' ')
						sentence = sentence.substring(1);

				Printer.printColor(Printer.ANSI_CYAN, count + "/"
						+ numberExamples + " " + file + " : " + sentence);

				Result r = bare.recognizeFromFile(filename);

				String googleResult = "";
				String standardResult = "";
				String ipaResult = "";
				String googleReverseResult = "";

				if (r != null) {
					googleResult = r.getBestResult();
					standardResult = standard.recognizeFromResult(r)
							.getBestResult();
					r = new Result();
					r.addResult(googleResult);
					r = ipa.recognizeFromResult(r);
					ipaResult = r.getBestResult();
					r = new Result();
					r.addResult(googleResult);
					r = googleReverse.recognizeFromResult(r);
					googleReverseResult = r.getBestResult();

				} else
					emptyResults++;
				alignerGoogle.align(sentence, googleResult);
				Printer.printColor(
						Printer.ANSI_BLUE,
						"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
				alignerGoogle.printSentenceSummary();
				alignerGoogle.printTotalSummary();
				alignerStandard.align(sentence, standardResult);
				Printer.printColor(
						Printer.ANSI_BLUE,
						"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Standard  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
				alignerStandard.printSentenceSummary();
				alignerStandard.printTotalSummary();

				alignerIPA.align(sentence, ipaResult);
				Printer.printColor(
						Printer.ANSI_BLUE,
						"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  IPA  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
				alignerIPA.printSentenceSummary();
				alignerIPA.printTotalSummary();

				alignerGoogleReverse.align(sentence, googleReverseResult);
				Printer.printColor(
						Printer.ANSI_BLUE,
						"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google Reverse  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
				alignerGoogleReverse.printSentenceSummary();
				alignerGoogleReverse.printTotalSummary();

				count++;

			}
			// print endresults
			System.out.println("Processed File: " + batchfile);
			System.out.println("Processed Examples: " + count);
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerGoogle.printTotalSummary();
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Standard  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerStandard.printTotalSummary();
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  IPA  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerIPA.printTotalSummary();
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google Reverse  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerGoogleReverse.printTotalSummary();

			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerGoogle.printNISTTotalSummary();
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Standard  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerStandard.printNISTTotalSummary();
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  IPA  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerIPA.printNISTTotalSummary();
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google Reverse  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerGoogleReverse.printNISTTotalSummary();

			PerformanceFileWriter tableRows = new PerformanceFileWriter();

			tableRows.add("Google", 0, 0, alignerGoogle, emptyResults);
			tableRows.add("Standard", standard.getLanguageWeight(),
					standard.getWIP(), alignerStandard, emptyResults);
			tableRows.add("IPA", ipa.getLanguageWeight(), ipa.getWIP(),
					alignerIPA, emptyResults);
			tableRows.add("Google Reverse", googleReverse.getLanguageWeight(),
					googleReverse.getWIP(), alignerGoogleReverse, emptyResults);

			tableRows.createPerformanceTable(title + ".with_empty_results",
					false, false, true);
			tableRows.createPerformanceTable(title
					+ ".rounded_with_empty_results", false, true, true);
			tableRows.createPerformanceTable(title
					+ ".in_percent_with_empty_results", true, false, true);
			tableRows.createPerformanceTable(title
					+ ".rounded_in_percent_with_empty_results", true, true,
					true);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}


	public static void testWERallOptimized(String path, String batchfile,
			String configname, String title) {
		Printer.printColor(Printer.ANSI_RED,
				"DONT FORGET TO ADJUST THE SENTENCES,WORDS,XML");

		final NISTAlign alignerGoogle = new NISTAlign(true, true);
		final NISTAlign alignerStandard = new NISTAlign(true, true);
		final NISTAlign alignerIPA = new NISTAlign(true, true);
		final NISTAlign alignerGoogleReverse = new NISTAlign(true, true);

		System.out.println("Starting BaseRecognizer");
		RawGoogleRecognizer bare = new RawGoogleRecognizer(key);

		System.out.println("Starting standard");
		final SphinxBasedPostProcessor standard = new SphinxBasedPostProcessor(
				configname + ".pngram.xml", configname + ".words", 0, 0, 0);

		System.out.println("Starting ipa");
		final SphinxBasedPostProcessor ipa = new SphinxBasedPostProcessor(
				configname + ".pngram.xml", configname + ".words", 1.6f, 2.85f,
				0);

		System.out.println("Starting google reverse");
		final SphinxBasedPostProcessor googleReverse = new SphinxBasedPostProcessor(
				configname + ".pngram.xml", configname + ".words", 2.1f, 4.1f,
				0);

		FileInputStream fstream = null;
		try {
			fstream = new FileInputStream(path + batchfile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine = null;
		String filename;
		String sentence;
		String file;

		try {
			int numberExamples = count(path + batchfile);
			System.out.println("Starting loop");
			int count = 1;

			int emptyResults = 0;
			while ((strLine = br.readLine()) != null) {

				int endName = strLine.indexOf(" ");
				filename = strLine.substring(0, endName);
				sentence = strLine.substring(endName + 1);
				file = filename.substring(filename.lastIndexOf("/") + 1,
						filename.length());

				sentence = sentence.replaceAll("[^a-zA-Z 0-9;]", "");
				sentence = sentence.replaceAll(" +", " ");
				if (!sentence.equals(""))
					if (sentence.charAt(0) == ' ')
						sentence = sentence.substring(1);

				Printer.printColor(Printer.ANSI_CYAN, count + "/"
						+ numberExamples + " " + file + " : " + sentence);

				Result r = bare.recognizeFromFile(filename);

				String googleResult = "";
				String standardResult = "";
				String ipaResult = "";
				String googleReverseResult = "";

				if (r != null) {
					googleResult = r.getBestResult();
					standardResult = standard.recognizeFromResult(r)
							.getBestResult();
					r = new Result();
					r.addResult(googleResult);
					r = ipa.recognizeFromResult(r);
					ipaResult = r.getBestResult();
					r = new Result();
					r.addResult(googleResult);
					r = googleReverse.recognizeFromResult(r);
					googleReverseResult = r.getBestResult();

				} else
					emptyResults++;
				alignerGoogle.align(sentence, googleResult);
				Printer.printColor(
						Printer.ANSI_BLUE,
						"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
				alignerGoogle.printSentenceSummary();
				alignerGoogle.printTotalSummary();
				alignerStandard.align(sentence, standardResult);
				Printer.printColor(
						Printer.ANSI_BLUE,
						"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Standard  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
				alignerStandard.printSentenceSummary();
				alignerStandard.printTotalSummary();

				alignerIPA.align(sentence, ipaResult);
				Printer.printColor(
						Printer.ANSI_BLUE,
						"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  IPA  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
				alignerIPA.printSentenceSummary();
				alignerIPA.printTotalSummary();

				alignerGoogleReverse.align(sentence, googleReverseResult);
				Printer.printColor(
						Printer.ANSI_BLUE,
						"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google Reverse  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
				alignerGoogleReverse.printSentenceSummary();
				alignerGoogleReverse.printTotalSummary();

				count++;

			}
			// print endresults
			System.out.println("Processed File: " + batchfile);
			System.out.println("Processed Examples: " + count);
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerGoogle.printTotalSummary();
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Standard  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerStandard.printTotalSummary();
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  IPA  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerIPA.printTotalSummary();
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google Reverse  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerGoogleReverse.printTotalSummary();

			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerGoogle.printNISTTotalSummary();
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Standard  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerStandard.printNISTTotalSummary();
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  IPA  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerIPA.printNISTTotalSummary();
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google Reverse  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerGoogleReverse.printNISTTotalSummary();

			PerformanceFileWriter tableRows = new PerformanceFileWriter();

			tableRows.add("Google", 0, 0, alignerGoogle, emptyResults);
			tableRows.add("Standard", standard.getLanguageWeight(),
					standard.getWIP(), alignerStandard, emptyResults);
			tableRows.add("IPA", ipa.getLanguageWeight(), ipa.getWIP(),
					alignerIPA, emptyResults);
			tableRows.add("Google Reverse", googleReverse.getLanguageWeight(),
					googleReverse.getWIP(), alignerGoogleReverse, emptyResults);

			tableRows.createPerformanceTable(title + ".with_empty_results",
					false, false, true);
			tableRows.createPerformanceTable(title
					+ ".rounded_with_empty_results", false, true, true);
			tableRows.createPerformanceTable(title
					+ ".in_percent_with_empty_results", true, false, true);
			tableRows.createPerformanceTable(title
					+ ".rounded_in_percent_with_empty_results", true, true,
					true);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void testPNRbig(String path, String batchfile,
			String configname, String title) {
		int i = 0;
		for (float languageWeight = 0.1f; languageWeight <= 5; languageWeight = languageWeight + 0.25f)
			for (float wordInsertionProbability = 0.1f; wordInsertionProbability <= 15; wordInsertionProbability = wordInsertionProbability + 0.25f) {

				i++;
				Printer.printRed(i + "/1200");
				if (i >= 242) {
					try {
						testPNR(path, batchfile, languageWeight,
								wordInsertionProbability, configname, title);
					} catch (Exception e) {
						e.printStackTrace();
						testPNR(path, batchfile, languageWeight,
								wordInsertionProbability, configname, title);
					}
				}
			}

	}

	public static void testPNR(String path, String batchfile,
			float languageWeight, float wordInsertionProbability,
			String configname, String title) {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(new DataInputStream(
					new FileInputStream(path + batchfile))));
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		final SphinxBasedPostProcessor standard = new SphinxBasedPostProcessor(
				configname + ".pgrammar.xml", configname + ".words",
				languageWeight, wordInsertionProbability, 0);

		final SphinxBasedPostProcessor ipa = new SphinxBasedPostProcessor(
				configname + ".pgrammar.xml", configname + ".words",
				languageWeight, wordInsertionProbability, 1);

		final SphinxBasedPostProcessor googleReverse = new SphinxBasedPostProcessor(
				configname + ".pgrammar.xml", configname + ".words",
				languageWeight, wordInsertionProbability, 2);

		final NISTAlign alignerStandard = new NISTAlign(true, true);
		final NISTAlign alignerIpa = new NISTAlign(true, true);
		final NISTAlign alignerGoogleReverse = new NISTAlign(true, true);

		String strLine;
		String filename;
		String sentence;
		String file;
		try {
			while ((strLine = br.readLine()) != null) {
				int endName = strLine.indexOf(" ");
				filename = strLine.substring(0, endName);
				sentence = strLine.substring(endName + 1);
				file = filename.substring(filename.lastIndexOf("/") + 1,
						filename.length());

				String standardResult = "";
				String ipaResult = "";
				String googleReverseResult = "";
				Result googleResult = getCachedResult(batchfile, "Google", file);
				if (googleResult != null) {
					Result r = standard.recognizeFromResult(googleResult);
					if (r != null)
						standardResult = r.getBestResult();
					r = ipa.recognizeFromResult(googleResult);
					if (r != null)
						ipaResult = r.getBestResult();
					r = googleReverse.recognizeFromResult(googleResult);
					if (r != null)
						googleReverseResult = r.getBestResult();
				}
				alignerStandard.align(sentence, standardResult);

				alignerIpa.align(sentence, ipaResult);

				alignerGoogleReverse.align(sentence, googleReverseResult);

				Printer.printColor(
						Printer.ANSI_BLUE,
						"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Standard  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
				alignerStandard.printSentenceSummary();
				alignerStandard.printTotalSummary();

				Printer.printColor(
						Printer.ANSI_BLUE,
						"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  IPA  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
				alignerIpa.printSentenceSummary();
				alignerIpa.printTotalSummary();

				Printer.printColor(
						Printer.ANSI_BLUE,
						"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  GoogleReverse  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
				alignerGoogleReverse.printSentenceSummary();
				alignerGoogleReverse.printTotalSummary();

			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Printer.printColor(
				Printer.ANSI_BLUE,
				"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Standard  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
		alignerStandard.printTotalSummary();
		Printer.printColor(
				Printer.ANSI_BLUE,
				"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  IPA  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
		alignerIpa.printTotalSummary();
		Printer.printColor(
				Printer.ANSI_BLUE,
				"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  GoogleReverse  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
		alignerGoogleReverse.printTotalSummary();
		Printer.printColor(
				Printer.ANSI_BLUE,
				"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Standard  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
		alignerStandard.printNISTTotalSummary();
		Printer.printColor(
				Printer.ANSI_BLUE,
				"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  IPA  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
		alignerIpa.printNISTTotalSummary();
		Printer.printColor(
				Printer.ANSI_BLUE,
				"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  GoogleReverse  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
		alignerGoogleReverse.printNISTTotalSummary();

		PerformanceFileWriter tableRows = new PerformanceFileWriter();

		tableRows.add("Standard", standard.getLanguageWeight(),
				standard.getWIP(), alignerStandard, 0);
		tableRows.add("IPA", ipa.getLanguageWeight(), ipa.getWIP(), alignerIpa,
				0);
		tableRows.add("Google Reverse", googleReverse.getLanguageWeight(),
				googleReverse.getWIP(), alignerGoogleReverse, 0);

		tableRows.createPerformanceTable(title
				+ ".rounded_in_percent_with_empty_results", true, true, false);
	
	}

	

	public static Result getCachedResult(String batchfile,
			String recognizerName, String wavFile) {
		InputStream fis = null;
		Result r = null;

		try {

			fis = new FileInputStream("cache/" + batchfile + "/"
					+ recognizerName + "/" + wavFile + ".res");
			ObjectInputStream o = new ObjectInputStream(fis);
			r = (Result) o.readObject();
		} catch (IOException e) {
			System.err.println(e);
		} catch (ClassNotFoundException e) {
			System.err.println(e);
		} finally {
			try {
				fis.close();
			} catch (Exception e) {
			}
		}
		return r;

	}

	public static void testLLR(String path, String resultfile) {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(new DataInputStream(
					new FileInputStream(path + resultfile))));
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		WordlistPostProcessor llr = new WordlistPostProcessor(
				"heinrichLab.words",key);

		final NISTAlign alignerGoogle = new NISTAlign(true, true);
		final NISTAlign alignerLLR = new NISTAlign(true, true);

		String strLine;
		try {
			while ((strLine = br.readLine()) != null) {
				System.out.println(strLine);
				String[] strLineSplit = strLine.split(";");
				String ref = strLineSplit[1];
				String hypGoogle = strLineSplit[0];
				alignerGoogle.align(ref, hypGoogle);
				Result r = new Result();
				r.addResult(hypGoogle);
				r = llr.recognizeFromResult(r);
				String hypLLR = r.getBestResult();
				alignerLLR.align(ref, hypLLR);

				Printer.printColor(
						Printer.ANSI_BLUE,
						"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
				alignerGoogle.printSentenceSummary();
				alignerGoogle.printTotalSummary();

				Printer.printColor(
						Printer.ANSI_BLUE,
						"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  LLR  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
				alignerLLR.printSentenceSummary();
				alignerLLR.printTotalSummary();

			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Printer.printColor(
				Printer.ANSI_BLUE,
				"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
		alignerGoogle.printTotalSummary();
		Printer.printColor(
				Printer.ANSI_BLUE,
				"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  LLR  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
		alignerLLR.printTotalSummary();
		Printer.printColor(
				Printer.ANSI_BLUE,
				"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
		alignerGoogle.printNISTTotalSummary();
		Printer.printColor(
				Printer.ANSI_BLUE,
				"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  LLR  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
		alignerLLR.printNISTTotalSummary();

	}

	public static void testGoogleOOV(String path, String batchfile) {
		final NISTAlign alignerGoogle = new NISTAlign(true, true);
		RawGoogleRecognizer brn = new RawGoogleRecognizer(key);
		HashSet<String> existingWords = ConfigCreator.getWordList(path,
				batchfile);

	
		FileInputStream fstream = null;
		try {
			fstream = new FileInputStream(path + batchfile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		BufferedReader resultFile = null;
		try {
			resultFile = new BufferedReader(new InputStreamReader(
					new DataInputStream(new FileInputStream(path
							+ "heinrichLab.google.refhyp"))));
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		String strLine = null;
		String filename;
		String sentence;
		String file;

		try {
			int numberExamples = count(path + batchfile);
			System.out.println("Starting loop");
			int count = 1;
			int substitutions = 0;
			int oovSubstitutions = 0;
			int doubleSubstitutions = 0;
			int doubleOOVSubstitutions = 0;

			boolean doRecognition = true;
			File f = new File(path + "heinrichLab.google.refhyp");
			BufferedWriter out = null;
			if (f.exists())
				doRecognition = false;
			else
				out = new BufferedWriter(new FileWriter(path
						+ "heinrichLab.google.refhyp"));

			while ((strLine = br.readLine()) != null) {

				int endName = strLine.indexOf(" ");
				filename = strLine.substring(0, endName);
				sentence = strLine.substring(endName + 1);
				file = filename.substring(filename.lastIndexOf("/") + 1,
						filename.length());

				sentence = sentence.replaceAll("[^a-zA-Z 0-9;]", "");
				sentence = sentence.replaceAll(" +", " ");
				if (!sentence.equals(""))
					if (sentence.charAt(0) == ' ')
						sentence = sentence.substring(1);

				Printer.printColor(Printer.ANSI_CYAN, count + "/"
						+ numberExamples + " " + file + " : " + sentence);

				String googleResult = null;
				if (doRecognition) {
					Result r = brn.recognizeFromFile(filename);
					googleResult = r.getBestResult();
					out.write(googleResult + ";" + sentence + "\n");
				} else {
					strLine = resultFile.readLine();
					String[] refhyp = strLine.split(";");
					googleResult = refhyp[0];
					sentence = refhyp[1];
				}

				if (googleResult != null) {

					alignerGoogle.align(sentence, googleResult);
					String alignedRef = alignerGoogle.getAlignedReference();
					String alignedHyp = alignerGoogle.getAlignedHypothesis();

					System.out.println(alignedRef);
					System.out.println(alignedHyp);
					alignedRef = alignedRef.replaceAll(" +", " ");
					alignedHyp = alignedHyp.replaceAll(" +", " ");
					String[] refWords = alignedRef.split(" ");
					String[] hypWords = alignedHyp.split(" ");
					boolean[] hasSubstitution = new boolean[refWords.length];
					boolean[] hasOOVSubstitution = new boolean[refWords.length];

					for (int i = 0; i < refWords.length; i++) {


						hasOOVSubstitution[i] = false;
						if (Character.isUpperCase(refWords[i].charAt(0))) {
							if (Character.isUpperCase(hypWords[i].charAt(0))) {
								hasSubstitution[i] = true;
								hasOOVSubstitution[i] = true;
								substitutions++;
								if (i != 0)
									if (hasSubstitution[i - 1])
										doubleSubstitutions++;
								Iterator<String> wordIterator = existingWords
										.iterator();
								while (wordIterator.hasNext()) {

									if (wordIterator.next().equals(hypWords[i])) {

										hasOOVSubstitution[i] = false;
										break;

									}
								}
								if (hasOOVSubstitution[i]) {
									oovSubstitutions++;
									if (i != 0)
										if (hasOOVSubstitution[i - 1]) {
											doubleOOVSubstitutions++;
											System.out
													.println("Double OOV Substitution word "
															+ (i - 1)
															+ " and "
															+ i);
										}
									System.out
											.println("OOV Substitution in word "
													+ i);
								} else
									System.out.println("Substitution in word "
											+ i);

							} else
								hasSubstitution[i] = false;
						} else
							hasSubstitution[i] = false;

					}



				}

				count++;

			}
			try {
				out.close();
				resultFile.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			// print endresults
			System.out.println("Processed File: " + batchfile);
			System.out.println("Processed Examples: " + count);
			System.out.println("Substitutions: " + substitutions);
			System.out.println("Double Substitutions: " + doubleSubstitutions);
			System.out.println("OOV Substitutions: " + oovSubstitutions);
			System.out.println("Double OOV Substitutions: "
					+ doubleOOVSubstitutions);

			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerGoogle.printTotalSummary();
			Printer.printColor(
					Printer.ANSI_BLUE,
					"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0  Google  \u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
			alignerGoogle.printNISTTotalSummary();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	static class ourResult implements Comparable<ourResult> {
		int id;
		double dist;

		ourResult(int id, double dist) {
			this.id = id;
			this.dist = dist;
		}

		@Override
		public int compareTo(ourResult our) {
			if (dist == our.dist)
				return 0;
			if (dist > our.dist)
				return 1;
			else
				return -1;

		}
	}

	static ArrayList<ourResult> softmax(double[] distances) {
		ArrayList<ourResult> res = new ArrayList<ourResult>();
		double sum = 0;
		for (double i : distances) {
			sum += Math.exp(i);
		}
		for (int i = 0; i < distances.length; i++) {
			res.add(i, new ourResult(i, Math.exp(distances[i]) / sum));

		}
		return res;
	}

	public static void test(String path, String batchfile, String sentenceFile) {

//		System.out.println("This is my library path:");
//		System.out.println(System.getProperty("java.library.path"));
//
//		Printer.reset();
//		Printer.printRedWithTime(TAG, "START Google ASR");
//
//		SentencelistPostProcessor lr = new SentencelistPostProcessor(sentenceFile, 10);
//
//		RawGoogleRecognizer bare = new RawGoogleRecognizer();
//
//		FileInputStream fstream;
//		try {
//
//			fstream = new FileInputStream(path + batchfile);
//			DataInputStream in = new DataInputStream(fstream);
//			BufferedReader br = new BufferedReader(new InputStreamReader(in));
//
//
//			FileInputStream fstream2 = new FileInputStream(sentenceFile
//					+ ".txt");
//			DataInputStream in2 = new DataInputStream(fstream2);
//			BufferedReader br2 = new BufferedReader(new InputStreamReader(in2));
//			ArrayList<String> references = new ArrayList<String>();
//			String strLine2;
//			while ((strLine2 = br2.readLine()) != null) {
//
//				// Sentence manipulation
//				String sentence = strLine2;
//				sentence = sentence.toLowerCase();
//				sentence = sentence.replaceAll("[^a-zA-Z 0-9]", "");
//				sentence = sentence.replaceAll(" +", " ");
//				if (sentence.charAt(0) == ' ')
//					sentence = sentence.substring(1);
//
//				references.add(sentence);
//			}
//			br2.close();
//
//			String strLine;
//			String filename;
//			String sentence;
//			String file;
//
//
//
//			int endName;
//
//
//			FileWriter fstream1 = new FileWriter(path + batchfile
//					+ "_google.txt");
//
//
//			BufferedWriter googleOut = new BufferedWriter(fstream1);
//
//
//
//
//			// ############################################
//			// ########## MODIFICATIONS: START 1 ##########
//			// ############################################
//
//			googleOut.write("ASR Engine;");
//			googleOut.write("Filename;");
//			googleOut.write("Reference Text;");
//			googleOut.write("True/False Recognition;");
//			googleOut.write("0-Hypothesis;");
//			googleOut.write("0-Candidate;");
//			googleOut.write("Winning-Candidate;");
//			googleOut.write("Winning-Candidate Index;");
//			googleOut.write("Distance Reference To 0-Hypothesis;");
//			googleOut.write("Softmax Reference To 0-Hypothesis;");
//			googleOut.write("Distance Reference To 0-Candidate;");
//			googleOut.write("Softmax Reference To 0-Candidate;");
//			googleOut.write("Distance Reference To Winning-Candidate;");
//			googleOut.write("Softmax Reference To Winning-Candidate;");
//			googleOut.write("Distance Winning-Candidate To 0-Candidate;");
//			googleOut.write("Softmax Winning-Candidate To 0-Candidate;");
//			googleOut.write("Distance Winning-Candidate To 0-Hypothesis;");
//			googleOut.write("Softmax Winning-Candidate To 0-Hypothesis;");
//			googleOut.write("Distance 0-Hypothesis To Winning-Candidate;");
//			googleOut.write("Softmax 0-Hypothesis To Winning-Candidate;");
//			googleOut.write("0-Candidate;");
//			googleOut.write("1-Candidate;");
//			googleOut.write("2-Candidate;");
//			googleOut.write("3-Candidate;");
//			googleOut.write("4-Candidate;");
//			googleOut.write("5-Candidate;");
//			googleOut.write("6-Candidate;");
//			googleOut.write("7-Candidate;");
//			googleOut.write("8-Candidate;");
//			googleOut.write("9-Candidate;");
//			googleOut.write("0-Hypothesis;");
//			googleOut.write("1-Hypothesis;");
//			googleOut.write("2-Hypothesis;");
//			googleOut.write("3-Hypothesis;");
//			googleOut.write("4-Hypothesis;");
//			googleOut.write("5-Hypothesis;");
//			googleOut.write("6-Hypothesis;");
//			googleOut.write("7-Hypothesis;");
//			googleOut.write("8-Hypothesis;");
//			googleOut.write("9-Hypothesis;");
//			googleOut.write(" \n");
//
//			// ##########################################
//			// ########## MODIFICATIONS: END 1 ##########
//			// ##########################################
//
//			// this is for reading from csv
//
//
//			while ((strLine = br.readLine()) != null) {
//
//
//				endName = strLine.indexOf(" ");
//				filename = strLine.substring(0, endName);
//				sentence = strLine.substring(endName + 1);
//				file = filename.substring(filename.lastIndexOf("/") + 1,
//						filename.length());
//
//				String newfile = filename;
//
//
//				Result googleResult = bare.recognizeFromFile(newfile);
//
//				System.out.println(file);
//
//
//				Result levenResult = lr.recognizeFromResult(googleResult);
//
//
//				// ############################################
//				// ########## MODIFICATIONS: START 2 ##########
//				// ############################################
//
//				if (googleResult != null) {
//
//					// Sentence manipulation
//					sentence = sentence.toLowerCase();
//					sentence = sentence.replaceAll("[^a-zA-Z 0-9]", "");
//					sentence = sentence.replaceAll(" +", " ");
//					if (sentence.charAt(0) == ' ')
//						sentence = sentence.substring(1);
//
//					// ASR Engine
//					googleOut.write("google;");
//
//					// Filename
//					googleOut.write(file + ";");
//
//					// Reference Text
//					googleOut.write(sentence + ";");
//
//					// True/False Recognition
//					if (sentence.equals(levenResult.getBestResult()))
//						googleOut.write("True" + ";");
//					else
//						googleOut.write("False" + ";");
//
//					// 0-Hypothesis
//					googleOut.write(levenResult.getBestResult() + ";");
//
//					// Winning-Sentence Index
//					int index_WinSen = -1;
//					for (int i = 0; i < references.size(); i++) {
//						if (references.get(i).equals(
//								levenResult.getBestResult()))
//							index_WinSen = i;
//					}
//
//					// 0-Candidate
//					googleOut.write(googleResult.getResultList().get(0) + ";");
//
//					// Winning-Candidate
//					//int index_WinCan = levenResult.getMatchingSentenceId();
//					googleOut.write(googleResult.getResultList().get(
//							index_WinCan)
//							+ ";");
//
//					// Winning-Candidate Index
//					googleOut.write(index_WinCan + ";");
//
//					// Distance Reference To 0-Hypothesis
//					String[] input = new String[references.size()];
//
//					references.toArray(input);
//
//					double[] dist_Ref_Sen = lr.calculateAgainstArray(sentence,
//							input);
//					double dist_Ref_0Hyp = dist_Ref_Sen[index_WinSen];
//					googleOut.write(dist_Ref_0Hyp + ";");
//
//
//
//					ArrayList<ourResult> softmaxRefSen = softmax(dist_Ref_Sen);
//
//					ourResult softmaxRef0Hyp = softmaxRefSen.get(index_WinSen);
//					googleOut.write(softmaxRef0Hyp.dist + ";");
//
//					// Distance Reference To 0-Candidate
//					String[] inputStrings = new String[googleResult
//							.getResultList().size()];
//					googleResult.getResultList().toArray(inputStrings);
//					double[] dist_Ref_Can = lr.calculateAgainstArray(sentence,
//							inputStrings);
//					double dist_Ref_0Can = dist_Ref_Can[0];
//					googleOut.write(dist_Ref_0Can + ";");
//
//					// Softmax Reference To 0-Candidate
//					ArrayList<ourResult> softmax_Ref_Can = softmax(dist_Ref_Can);
//					ourResult softmax_Ref_0Can = softmax_Ref_Can.get(0);
//					googleOut.write(softmax_Ref_0Can.dist + ";");
//
//					// Distance Reference To Winning-Candidate
//					double dist_Ref_WinCan = dist_Ref_Can[index_WinCan];
//					googleOut.write(dist_Ref_WinCan + ";");
//
//					// Softmax Reference To Winning-Candidate
//					ourResult softmaxRefWinCan = softmax_Ref_Can
//							.get(index_WinCan);
//					googleOut.write(softmaxRefWinCan.dist + ";");
//
//					// Distance Winning-Candidate To 0-Candidate
//					inputStrings = new String[googleResult.getResultList()
//							.size()];
//					googleResult.getResultList().toArray(inputStrings);
//					double[] dist_WinCan_Can = lr.calculateAgainstArray(
//							googleResult.getResultList().get(index_WinCan),
//							inputStrings);
//					double dist_WinCan_0Can = dist_WinCan_Can[0];
//					googleOut.write(dist_WinCan_0Can + ";");
//
//					// Softmax Winning-Candidate To 0-Candidate
//					ArrayList<ourResult> softmax_WinCan_Can = softmax(dist_WinCan_Can);
//					ourResult softmax_WinCan_0Can = softmax_WinCan_Can.get(0);
//					googleOut.write(softmax_WinCan_0Can.dist + ";");
//
//					// Distance Winning-Candidate To 0-Hypothesis
//					inputStrings = new String[references.size()];
//					references.toArray(inputStrings);
//					double[] dist_WinCan_Sen = lr.calculateAgainstArray(
//							googleResult.getResultList().get(index_WinCan),
//							inputStrings);
//					double dist_WinCan_0Hyp = dist_WinCan_Sen[index_WinSen];
//					googleOut.write(dist_WinCan_0Hyp + ";");
//
//					// Softmax Winning-Candidate To 0-Hypothesis
//					ArrayList<ourResult> softmax_WinCan_Sen = softmax(dist_WinCan_Sen);
//					ourResult softmax_WinCan_0Hyp = softmax_WinCan_Sen
//							.get(index_WinSen);
//					googleOut.write(softmax_WinCan_0Hyp.dist + ";");
//
//					// Distance 0-Hypothesis To Winning-Candidate
//					inputStrings = new String[googleResult.getResultList()
//							.size()];
//					googleResult.getResultList().toArray(inputStrings);
//					double[] dist_0Hyp_Can = lr.calculateAgainstArray(
//							levenResult.getResultList().get(0), inputStrings);
//					double dist_0Hyp_WinCan = dist_0Hyp_Can[index_WinCan];
//					googleOut.write(dist_0Hyp_WinCan + ";");
//
//					// Softmax 0-Hypothesis To Winning-Candidate
//					ArrayList<ourResult> softmax_0Hyp_Can = softmax(dist_0Hyp_Can);
//					ourResult softmax_0Hyp_WinCan = softmax_0Hyp_Can
//							.get(index_WinCan);
//					googleOut.write(softmax_0Hyp_WinCan.dist + ";");
//
//					// 0:10-Candidate
//					for (int i = 0; i < 10; i++) {
//						try {
//							googleOut.write(googleResult.getResultList().get(i)
//									+ ";");
//						} catch (Exception e) {
//							googleOut.write(" ;");
//						}
//					}
//
//					// 0:10-Best-Hypothesis
//					for (int i = 0; i < 10; i++) {
//						try {
//
//							googleOut.write(levenResult.getResultList().get(i)
//									+ ";");
//						} catch (Exception e) {
//							googleOut.write(" ;");
//						}
//					}
//
//					// Terminate line
//					googleOut.write(" \n");
//
//				} else {
//
//					// ASR Engine
//					googleOut.write("google;");
//
//					// Filename
//					googleOut.write(file + ";");
//
//					// Reference Text
//					googleOut.write(sentence + ";");
//
//					// True/False Recognition
//					googleOut.write("False" + ";");
//
//					// Empty values
//					for (int i = 0; i < 36; i++)
//						googleOut.write(" ;");
//
//					// Terminate line
//					googleOut.write(" \n");
//				}
//				// break;
//			}
//
//			googleOut.close();
//
//			// ##########################################
//			// ########## MODIFICATIONS: END 2 ##########
//			// ##########################################
//
//
//
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}

	}



	public static void thesisExampleLevenshtein() {
		String ref = "this was a box";
		String hyp = "this is a red ball";


		PhonemeCreator pc = new PhonemeCreator();
		Result r = new Result();
		r.addResult(ref);
		r.addResult(hyp);
		String[] orig = pc.getPhonemes(r).get(0).getPhonemes();
		String[] eing = pc.getPhonemes(r).get(1).getPhonemes();

		for (String s : orig)
			System.out.print(s);
		System.out.println();
		for (String s : eing)
			System.out.print(s);
		System.out.println();

		int matrix[][] = new int[orig.length + 1][eing.length + 1];
		for (int i = 0; i < orig.length + 1; i++) {
			matrix[i][0] = i;
		}
		for (int i = 0; i < eing.length + 1; i++) {
			matrix[0][i] = i;
		}
		for (int a = 1; a < orig.length + 1; a++) {
			for (int b = 1; b < eing.length + 1; b++) {
				int right = 0;
				if (!orig[a - 1].equals(eing[b - 1])) {
					right = 1;
				}
				int mini = matrix[a - 1][b] + 1;
				if (matrix[a][b - 1] + 1 < mini) {
					mini = matrix[a][b - 1] + 1;
				}
				if (matrix[a - 1][b - 1] + right < mini) {
					mini = matrix[a - 1][b - 1] + right;
				}
				matrix[a][b] = mini;
			}
		}

		System.out.print("  &    ");
		for (String s : orig)
			System.out.print("& ~" + s + " ");
		System.out.println("\\\\");
		System.out.println("\\midrule");
		for (int j = 0; j < eing.length + 1; j++) {
			if (j == 0)
				System.out.print(" " + " ");
			else
				System.out.print(eing[j - 1] + " ");
			for (int i = 0; i < orig.length + 1; i++) {

				int distance = matrix[i][j];
				String formNumber = distance + "";
				if (distance < 10)
					formNumber = "~" + distance;

				System.out.print("& " + formNumber + " ");

			}
			System.out.println("\\\\");

		}


		NISTAlign aligner = new NISTAlign(true, true);
		ref = "";
		hyp = "";
		for (String s : orig)
			if (s.equals(" "))
				ref = ref + " " + "~";
			else
				ref = ref + " " + s;
		for (String s : eing)
			if (s.equals(" "))
				hyp = hyp + " " + "~";
			else
				hyp = hyp + " " + s;
		aligner.align(ref, hyp);
		System.out.println(ref);
		System.out.println(hyp);
		ref = aligner.getAlignedReference();
		hyp = aligner.getAlignedHypothesis();
		System.out.println(ref);
		System.out.println(hyp);
		ref = ref.replaceAll(" +", " ");
		hyp = hyp.replaceAll(" +", " ");
		System.out.println(ref);
		System.out.println(hyp);
		orig = ref.split(" ");
		eing = hyp.split(" ");
		ref = ref.replaceAll(" ", " & ");
		hyp = hyp.replaceAll(" ", " & ");
		System.out.println(orig.length);
		System.out.println(orig[12]);
		for (String s : orig)
			System.out.print(" " + s);
		System.out.println(orig[0].charAt(0));
		String replacements = "";
		for (int i = 0; i < orig.length; i++) {
			System.out.println(orig[i]);
			if (Character.isLetter(orig[i].charAt(0))
					&& Character.isUpperCase(orig[i].charAt(0))) {
				if (Character.isLetter(eing[i].charAt(0))
						&& Character.isUpperCase(eing[i].charAt(0)))
					replacements = replacements + "& S ";

				else if (eing[i].contains("*"))
					replacements = replacements + "& D ";
			} else if (orig[i].contains("*"))
				replacements = replacements + "& I ";
			else
				replacements = replacements + "&   ";
		}

		ref = ref + "\\\\";
		hyp = hyp + "\\\\";
		replacements = replacements.substring(1);
		replacements = replacements + "\\\\";
		System.out.println(ref);
		System.out.println(hyp);
		System.out.println("\\midrule");
		System.out.println("\\midrule");
		System.out.println(replacements);
		aligner.printSentenceSummary();

	}

	public static void matlabFormatPerformance() {
		String metric = "WER";
		String file = "results/performance.csv";

		String searchTag = "Google Reverse";
		String approachName = "Google_Reverse";
		String outFile = "results/performance_" + approachName + metric
				+ ".csv";

		boolean debug = true;
		try {

			FileWriter out = new FileWriter(outFile, false);

			BufferedReader br = new BufferedReader(new InputStreamReader(
					new DataInputStream(new FileInputStream(file))));
			String strLine;
			String lastLw = "0.1";
			System.out.print(",");
			for (float wordInsertionProbability = 0.1f; wordInsertionProbability <= 15; wordInsertionProbability = wordInsertionProbability + 0.25f) {
				System.out.print("," + wordInsertionProbability);
				out.write("," + wordInsertionProbability);
			}
			System.out.println();
			out.write("\n");
			System.out.print(lastLw + "# ");
			out.write(lastLw + ",");
			boolean first = true;
			while ((strLine = br.readLine()) != null) {

				String[] columns = strLine.split(";");

				if (columns[0] != null)
					if (columns[0].equals(searchTag)) {
						String lw = columns[1];

						String wer = columns[5];
						if (!wer.startsWith("0"))
							wer = "1";
						if (!first) {
							if (!lw.equals(lastLw)) {
								out.write("\n");
								out.write(lw + ",");
								if (debug) {
									System.out.println();
									System.out.print(lw + "# ");
								}
							} else {
								out.write(",");
								if (debug)
									System.out.print(",");
							}
						} else
							first = false;
						if (debug)
							System.out.print(wer);
						out.write(wer);
						lastLw = lw;
					}

			}
			br.close();
			out.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void matlabFormatResults(String file, String outFile,
			String countTag, String metric) {
		String[] approaches = { "Google", "Google Sentence List",
				"Google Word List", "Google Phoneme N-Gram", "Google Phoneme Unigram",
				"Google Phoneme Grammar",
				"Google Phoneme Grammar Sentence List", "Sphinx Grammar",
				"Sphinx Grammar Sentence List", "Sphinx N-Gram" };
		// select WER
		int selectedCol = 0;
		if (metric.equals("WER"))
			selectedCol = 5;
		else if (metric.equals("SER"))
			selectedCol = 11;
		int numberRuns = 10;

		String[][] werMatrix = new String[approaches.length][numberRuns];
		FileInputStream fstream = null;
		try {
			fstream = new FileInputStream(file);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			int count = -1;
			FileWriter out = new FileWriter(outFile, false);
			while ((strLine = br.readLine()) != null) {
				if (strLine.contains(countTag))
					count++;
				if (count == numberRuns)
					break;
				System.out.println("Col: " + strLine);
				String[] cols = strLine.split(";");

				for (int i = 0; i < approaches.length; i++)
					if (cols.length > 1)
						if (cols[0].equals(approaches[i])) {
							werMatrix[i][count] = cols[selectedCol];

						}

			}
			for (int approach = 0; approach < approaches.length; approach++) {
				System.out.print(approaches[approach] + ";");
				for (int run = 0; run < numberRuns; run++) {
					out.write(werMatrix[approach][run]);
					System.out.print(werMatrix[approach][run]);
					if (run < numberRuns - 1) {
						out.write(";");
						System.out.print(";");
					} else {
						out.write("\n");
						System.out.print("\n");
					}

				}
			}
			out.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	public static void matlabSigTest(String path, String batchfile,
			String configname, String title) {
		Printer.printColor(Printer.ANSI_RED,
				"DONT FORGET TO ADJUST THE SENTENCES,WORDS,XML");
		System.out.println("processing batch file: " + batchfile);



		FileInputStream fstream = null;
		try {
			fstream = new FileInputStream(path + batchfile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine = null;
		String filename;
		String sentence;
		String file;


		int numberExamples = 0;
		try {
			numberExamples = count(path + batchfile);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
			try {
				numberExamples = count(path + batchfile);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("Starting loop");
			int count = 1;
			
			String googleName = "Google";
			String levenName = "LevenshteinRecognizer";
			String nameLLR = "LexiconLookupRecognizer";
			String namePNR = "PhonemeNgramRecognizerNgram";
			String namePUR = "PhonemeNgramRecognizerUnigram";
			String namePGR = "PhonemeNgramRecognizerGrammar";
			String namePGRsentences = "PhonemeNgramRecognizerGrammarSentences";
			String nameSNR = "SimpleSphinxRecognizerNgram";
			String nameSGR = "SimpleSphinxRecognizerGrammar";
			String nameSGRsentences = "SimpleSphinxRecognizerGrammarSentences";
			
			

			try {
				FileWriter outReference = new FileWriter("results/transcripts/"+title+"/"+"reference"+".transcript", false);
				FileWriter outGoogle = new FileWriter("results/transcripts/"+title+"/"+googleName+".transcript", false);
				FileWriter outLeven = new FileWriter("results/transcripts/"+title+"/"+levenName+".transcript", false);
				FileWriter outLLR = new FileWriter("results/transcripts/"+title+"/"+nameLLR+".transcript", false);
				FileWriter outPNR = new FileWriter("results/transcripts/"+title+"/"+namePNR+".transcript", false);
				FileWriter outPUR = new FileWriter("results/transcripts/"+title+"/"+namePUR+".transcript", false);
				FileWriter outPGR = new FileWriter("results/transcripts/"+title+"/"+namePGR+".transcript", false);
				FileWriter outPGRsentences = new FileWriter("results/transcripts/"+title+"/"+namePGRsentences+".transcript", false);
				FileWriter outSNR = new FileWriter("results/transcripts/"+title+"/"+nameSNR+".transcript", false);
				FileWriter outSGR = new FileWriter("results/transcripts/"+title+"/"+nameSGR+".transcript", false);
				FileWriter outSGRsentences = new FileWriter("results/transcripts/"+title+"/"+nameSGRsentences+".transcript", false);
				
				while ((strLine = br.readLine()) != null) {

					
					int endName = strLine.indexOf(" ");
					filename = strLine.substring(0, endName);
					sentence = strLine.substring(endName + 1);
					file = filename.substring(filename.lastIndexOf("/") + 1,
							filename.length());

					sentence = sentence.replaceAll("[^a-zA-Z 0-9;]", "");
					sentence = sentence.replaceAll(" +", " ");
					if (!sentence.equals(""))
						if (sentence.charAt(0) == ' ')
							sentence = sentence.substring(1);

					Printer.printColor(Printer.ANSI_CYAN, count + "/"
							+ numberExamples + " " + file + " : " + sentence);
			
					
					
					String googleResult = "";
					String levenResult = "";
					String hypLLR = "";
					String hypPNR = "";
					String hypPUR = "";
					String hypPGR = "";
					String hypPGRsentences = "";
					String hypSNR = "";
					String hypSGR = "";
					String hypSGRsentences = "";

							

							
					Result r = null;

					r = Result.load("cache/" + batchfile + "/" + "Google"
								+ "/" + file + ".res");
					if (r != null) {
						googleResult = r.getBestResult();
						
					}
					r = Result.load("cache/" + batchfile + "/" + "LevenshteinRecognizer" + "/" + file + ".res");
					if (r != null) {
						levenResult = r.getBestResult();
						
					}
					r = Result.load("cache/" + batchfile + "/" + "LexiconLookupRecognizer" + "/" + file + ".res");
					if (r != null) {
						hypLLR = r.getBestResult();
						
					}
					r = Result.load("cache/" + batchfile + "/" + "PhonemeNgramRecognizerNgram" + "/" + file + ".res");
					if (r != null) {
						hypPNR = r.getBestResult();
						
					}
					r = Result.load("cache/" + batchfile + "/" + "PhonemeNgramRecognizerUnigram" + "/" + file + ".res");
					if (r != null) {
						hypPUR = r.getBestResult();
						
					}
					r = Result.load("cache/" + batchfile + "/" + "PhonemeNgramRecognizerGrammar" + "/" + file + ".res");
					if (r != null) {
						hypPGR = r.getBestResult();
						
					}
					r = Result.load("cache/" + batchfile + "/" + "PhonemeNgramRecognizerGrammarSentences" + "/" + file + ".res");
					if (r != null) {
						hypPGRsentences = r.getBestResult();
						
					}
					r = Result.load("cache/" + batchfile + "/" + "SimpleSphinxRecognizerNgram" + "/" + file + ".res");
					if (r != null) {
						hypSNR = r.getBestResult();
						
					}

					r = Result.load("cache/" + batchfile + "/" + "SimpleSphinxRecognizerGrammar" + "/" + file + ".res");
					if (r != null) {
						hypSGR = r.getBestResult();
						
					}
					r = Result.load("cache/" + batchfile + "/" + "SimpleSphinxRecognizerGrammarSentences" + "/" + file + ".res");
					if (r != null) {
						hypSGRsentences = r.getBestResult();
						
					}
					
					if(title.contains("scripted"))
					{
						double limit = numberExamples/2;
						System.out.println(count+"/"+limit);
						if(count > limit)
							file = file+"2";
							
						
					}
					
					System.out.println("Raw Google: "+ googleResult );
					System.out.println("Google+Sentencelist: "+ levenResult );
					System.out.println("Google+Wordlist: "+  hypLLR );
					System.out.println("Google+Sphinx N-Gram: "+  hypPNR );
					System.out.println("Google+Sphinx Unigram: "+  hypPUR );
					System.out.println("Google+Sphinx Grammar: "+  hypPGR );
					System.out.println("Google+Sphinx Sentences: "+  hypPGRsentences );
					System.out.println("Sphinx N-Gram: "+  hypSNR );
					System.out.println("Sphinx Grammar: "+  hypSGR );
					System.out.println("Sphinx Sentences: "+  hypSGRsentences );
					
					outReference.write(sentence+" "+"("+file+")\n");

					 outGoogle.write(googleResult+" "+"("+file+")\n");

					 outLeven.write(levenResult+" "+"("+file+")\n");

					 outLLR.write(hypLLR+" "+"("+file+")\n");

					 outPNR.write(hypPNR+" "+"("+file+")\n");
	
					 outPUR.write(hypPUR+" "+"("+file+")\n");
	
					 outPGR.write(hypPGR+" "+"("+file+")\n");
			
					 outPGRsentences.write(hypPGRsentences+" "+"("+file+")\n");
				
					 outSNR.write(hypSNR+" "+"("+file+")\n");

					 outSGR.write(hypSGR+" "+"("+file+")\n");
	
					 outSGRsentences.write(hypSGRsentences+" "+"("+file+")\n");
					count++;
					
					
				}
				outReference.close();
				 outGoogle.close();
				 outLeven.close();
				 outLLR.close();
				 outPNR.close();
				 outPUR.close();
				 outPGR.close();
				 outPGRsentences.close();
				 outSNR.close();
				 outSGR.close();
				 outSGRsentences.close();
				

				 
				 
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	


	}
	
	public static void matlabFormatResults100(String file, String outFile,
			String countTag, String metric) {
		String[] approaches = { "Google", "Google Sentence List",
				"Google Word List", "Google Phoneme N-Gram","Google Phoneme Unigram",
				"Google Phoneme Grammar",
				"Google Phoneme Grammar Sentence List", "Sphinx Grammar",
				"Sphinx Grammar Sentence List", "Sphinx N-Gram",
				 };
		// select WER
		int selectedCol = 0;
		if (metric.equals("WER"))
			selectedCol = 5;
		else if (metric.equals("SER"))
			selectedCol = 11;

		int numberRuns = 1;

		String[][] werMatrix = new String[approaches.length][10];
		FileInputStream fstream = null;
		try {
			fstream = new FileInputStream(file);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			int count = -1;
			FileWriter out = new FileWriter(outFile, false);
			while ((strLine = br.readLine()) != null) {
				if (strLine.contains(countTag))
					count++;
				if (count == numberRuns)
					break;
				System.out.println("Col: " + strLine);
				String[] cols = strLine.split(";");

				for (int i = 0; i < approaches.length; i++)
					if (cols.length > 1)
						if (cols[0].equals(approaches[i])) {
							for (int j = 0; j < 10; j++)
								werMatrix[i][j] = cols[selectedCol];

						}

			}
			for (int approach = 0; approach < approaches.length; approach++) {
				System.out.print(approaches[approach] + ";");
				for (int run = 0; run < 10; run++) {
					out.write(werMatrix[approach][run]);
					System.out.print(werMatrix[approach][run]);
					if (run < 10 - 1) {
						out.write(";");
						System.out.print(";");
					} else {
						out.write("\n");
						System.out.print("\n");
					}

				}
			}
			out.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void matlabFormatResults100newOrder(String file,
			String outFile, String countTag, String metric) {
		String[] approaches = { "Google", "Google Sentence List",
				"Google Word List", "Google Phoneme N-Gram",
				"Google Phoneme Unigram",
				"Google Phoneme Grammar",
				"Google Phoneme Grammar Sentence List", "Sphinx Grammar",
				"Sphinx Grammar Sentence List", "Sphinx N-Gram",
				"Sphinx Sentence List" };

		String[] approachesNewOrder = { "Google", "Sphinx N-Gram",
				"Sphinx Grammar Sentence List", "Google Word List",
				"Google Phoneme Unigram",
				"Google Phoneme N-Gram",
				"Google Phoneme Grammar Sentence List", "Google Sentence List" };

		if (countTag.equals("scripted")) {
			String[] approachesNewOrder2 = { "Google", "Sphinx N-Gram",
					"Sphinx Grammar",
					"Sphinx Grammar Sentence List", "Google Word List",
					"Google Phoneme Unigram",
					"Google Phoneme N-Gram", "Google Phoneme Grammar",
					"Google Phoneme Grammar Sentence List",
					"Google Sentence List" };
			approachesNewOrder = approachesNewOrder2.clone();
		}

		// select WER
		int selectedCol = 0;
		if (metric.equals("WER"))
			selectedCol = 5;
		else if (metric.equals("SER"))
			selectedCol = 11;

		int numberRuns = 1;

		String[][] werMatrix = new String[approaches.length][10];
		FileInputStream fstream = null;
		try {
			fstream = new FileInputStream(file);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			int count = -1;
			FileWriter out = new FileWriter(outFile, false);
			while ((strLine = br.readLine()) != null) {
				if (strLine.contains(countTag))
					count++;
				if (count == numberRuns)
					break;
				System.out.println("Col: " + strLine);
				String[] cols = strLine.split(";");

				for (int i = 0; i < approaches.length; i++)
					if (cols.length > 1)
						if (cols[0].equals(approaches[i])) {
							for (int j = 0; j < 10; j++)
								werMatrix[i][j] = cols[selectedCol];

						}

			}

			for (int i = 0; i < approachesNewOrder.length; i++) {
				int approach = 0;
				for (int j = 0; j < approaches.length; j++)
					if (approaches[j].equals(approachesNewOrder[i]))
						approach = j;
				System.out.print(approaches[approach] + ";");
				for (int run = 0; run < 10; run++) {
					out.write(werMatrix[approach][run]);
					System.out.print(werMatrix[approach][run]);
					if (run < 10 - 1) {
						out.write(";");
						System.out.print(";");
					} else {
						out.write("\n");
						System.out.print("\n");
					}

				}
			}
			out.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void matlabMerge() {
		String path = "results/final/";
		File folder = new File("results/final/");
		File[] listOfFiles = folder.listFiles();

		String endfile = ".rounded_in_percent_with_empty_results.csv";
		String scripted = "scripted_robot_commands";
		String timit = "timit";
		String spont = "spontaneous_robot_commands";
		
		String merge_5_scripted = "cat";
		String merge_10_scripted = "cat";
		String merge_20_scripted = "cat";
		String merge_50_scripted = "cat";
		String merge_5_timit = "cat";
		String merge_10_timit = "cat";
		String merge_20_timit = "cat";
		String merge_50_timit = "cat";
		String merge_5_spont = "cat";
		String merge_10_spont = "cat";
		String merge_20_spont = "cat";
		String merge_50_spont = "cat";
		
		for (File file : listOfFiles) {
		    if (file.isFile()) {
		        System.out.println(file.getName());
		        String name = file.getName();
		        if(name.contains(scripted+"_5_"))
		        	merge_5_scripted = merge_5_scripted+" "+path+name;
		        if(name.contains(scripted+"_10_"))
		        	merge_10_scripted = merge_10_scripted+" "+path+name;
		        if(name.contains(scripted+"_20_"))
		        	merge_20_scripted = merge_20_scripted+" "+path+name;
		        if(name.contains(scripted+"_50_"))
		        	merge_50_scripted = merge_50_scripted+" "+path+name;
		        if(name.contains(timit+"_5_"))
		        	merge_5_timit = merge_5_timit+" "+path+name;
		        if(name.contains(timit+"_10_"))
		        	merge_10_timit = merge_10_timit+" "+path+name;
		        if(name.contains(timit+"_20_"))
		        	merge_20_timit = merge_20_timit+" "+path+name;
		        if(name.contains(timit+"_50_"))
		        	merge_50_timit = merge_50_timit+" "+path+name;
		        if(name.contains(spont+"_5_"))
		        	merge_5_spont = merge_5_spont+" "+path+name;
		        if(name.contains(spont+"_10_"))
		        	merge_10_spont = merge_10_spont+" "+path+name;
		        if(name.contains(spont+"_20_"))
		        	merge_20_spont = merge_20_spont+" "+path+name;
		        if(name.contains(spont+"_50_"))
		        	merge_50_spont = merge_50_spont+" "+path+name;
		        
		    }
		}
		merge_5_scripted += " > "+path+scripted+"_5_percent"+endfile;
		 merge_10_scripted += " > "+path+scripted+"_10_percent"+endfile;
		 merge_20_scripted += " > "+path+scripted+"_20_percent"+endfile;
		 merge_50_scripted += " > "+path+scripted+"_50_percent"+endfile;
		 merge_5_timit += " > "+path+timit+"_5_percent"+endfile;
		 merge_10_timit +=  " > "+path+timit+"_10_percent"+endfile;
		 merge_20_timit += " > "+path+timit+"_20_percent"+endfile;
		 merge_50_timit +=  " > "+path+timit+"_50_percent"+endfile;
		 merge_5_spont +=  " > "+path+spont+"_5_percent"+endfile;
		 merge_10_spont += " > "+path+spont+"_10_percent"+endfile;
		 merge_20_spont += " > "+path+spont+"_20_percent"+endfile;
		 merge_50_spont += " > "+path+spont+"_50_percent"+endfile;
			System.out.println(merge_5_scripted);
			System.out.println(merge_10_scripted);
			System.out.println(merge_20_scripted);
			System.out.println( merge_50_scripted);
			System.out.println( merge_5_timit);
			System.out.println( merge_10_timit);
			System.out.println(merge_20_timit);
			System.out.println( merge_50_timit);
			System.out.println( merge_5_spont);
			System.out.println(merge_10_spont);
			System.out.println(merge_20_spont);
			System.out.println(merge_50_spont);
			try {
				FileWriter out = new FileWriter(path+scripted+"_5_percent"+endfile, false);
				Runtime r = Runtime.getRuntime();
				Process p = r.exec(merge_5_scripted);
				p.waitFor();
				BufferedReader b = new BufferedReader(new InputStreamReader(p.getInputStream()));
				String line = "";

				while ((line = b.readLine()) != null) {
				  System.out.println(line);
				  out.write(line+"\n");
				}
				out.close();
				
				out = new FileWriter(path+scripted+"10_percent"+endfile, false);
				r = Runtime.getRuntime();
				p = r.exec(merge_10_scripted);
				p.waitFor();
				b = new BufferedReader(new InputStreamReader(p.getInputStream()));
				line = "";

				while ((line = b.readLine()) != null) {
				  System.out.println(line);
				  out.write(line+"\n");
				}
				out.close();
				
				out = new FileWriter(path+scripted+"20_percent"+endfile, false);
				r = Runtime.getRuntime();
				p = r.exec(merge_20_scripted);
				p.waitFor();
				b = new BufferedReader(new InputStreamReader(p.getInputStream()));
				line = "";

				while ((line = b.readLine()) != null) {
				  System.out.println(line);
				  out.write(line+"\n");
				}
				out.close();
				
				out = new FileWriter(path+scripted+"50_percent"+endfile, false);
				r = Runtime.getRuntime();
				p = r.exec(merge_50_scripted);
				p.waitFor();
				b = new BufferedReader(new InputStreamReader(p.getInputStream()));
				line = "";

				while ((line = b.readLine()) != null) {
				  System.out.println(line);
				  out.write(line+"\n");
				}
				out.close();
				
				out = new FileWriter(path+timit+"5_percent"+endfile, false);
				r = Runtime.getRuntime();
				p = r.exec(merge_5_timit);
				p.waitFor();
				b = new BufferedReader(new InputStreamReader(p.getInputStream()));
				line = "";

				while ((line = b.readLine()) != null) {
				  System.out.println(line);
				  out.write(line+"\n");
				}
				out.close();

				out = new FileWriter(path+timit+"10_percent"+endfile, false);
				r = Runtime.getRuntime();
				p = r.exec(merge_10_timit);
				p.waitFor();
				b = new BufferedReader(new InputStreamReader(p.getInputStream()));
				line = "";

				while ((line = b.readLine()) != null) {
				  System.out.println(line);
				  out.write(line+"\n");
				}
				out.close();				out = new FileWriter(path+timit+"20_percent"+endfile, false);
				r = Runtime.getRuntime();
				p = r.exec(merge_20_timit);
				p.waitFor();
				b = new BufferedReader(new InputStreamReader(p.getInputStream()));
				line = "";

				while ((line = b.readLine()) != null) {
				  System.out.println(line);
				  out.write(line+"\n");
				}
				out.close();				out = new FileWriter(path+timit+"50_percent"+endfile, false);
				r = Runtime.getRuntime();
				p = r.exec(merge_50_timit);
				p.waitFor();
				b = new BufferedReader(new InputStreamReader(p.getInputStream()));
				line = "";

				while ((line = b.readLine()) != null) {
				  System.out.println(line);
				  out.write(line+"\n");
				}
				out.close();
				
				
				out = new FileWriter(path+spont+"5_percent"+endfile, false);
				r = Runtime.getRuntime();
				p = r.exec(merge_5_spont);
				p.waitFor();
				b = new BufferedReader(new InputStreamReader(p.getInputStream()));
				line = "";

				while ((line = b.readLine()) != null) {
				  System.out.println(line);
				  out.write(line+"\n");
				}
				out.close();
				out = new FileWriter(path+spont+"10_percent"+endfile, false);
				r = Runtime.getRuntime();
				p = r.exec(merge_10_spont);
				p.waitFor();
				b = new BufferedReader(new InputStreamReader(p.getInputStream()));
				line = "";

				while ((line = b.readLine()) != null) {
				  System.out.println(line);
				  out.write(line+"\n");
				}
				out.close();
				out = new FileWriter(path+spont+"20_percent"+endfile, false);
				r = Runtime.getRuntime();
				p = r.exec(merge_20_spont);
				p.waitFor();
				b = new BufferedReader(new InputStreamReader(p.getInputStream()));
				line = "";

				while ((line = b.readLine()) != null) {
				  System.out.println(line);
				  out.write(line+"\n");
				}
				out.close();
				out = new FileWriter(path+spont+"50_percent"+endfile, false);
				r = Runtime.getRuntime();
				p = r.exec(merge_50_spont);
				p.waitFor();
				b = new BufferedReader(new InputStreamReader(p.getInputStream()));
				line = "";

				while ((line = b.readLine()) != null) {
				  System.out.println(line);
				  out.write(line+"\n");
				}
				out.close();
				
				
				
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
	
	public static void semilive()
	{
		String configname = "wtm_experiment";
		


		System.out.println("Starting Raw Google");
		RawGoogleRecognizer bare = new RawGoogleRecognizer(key);

		System.out.println("Starting Google+Sentencelist");
		SentencelistPostProcessor lr = new SentencelistPostProcessor(configname
				+ ".sentences", 1,key);

		System.out.println("Starting Sphinx N-Gram");
		SphinxRecognizer sr_ngram = new SphinxRecognizer(configname
				+ ".ngram.xml");

		System.out.println("Starting Sphinx Sentences");
		SphinxRecognizer sr_fsg_sentences = new SphinxRecognizer(
				configname + ".fsgsentences.xml");

		System.out.println("Starting Google+Sphinx N-Gram");
		final SphinxBasedPostProcessor pnr = new SphinxBasedPostProcessor(
				configname + ".pngram.xml", configname + ".words", 0, 0, 0);

		System.out.println("Starting Google+Sphinx Unigram");
		final SphinxBasedPostProcessor pur = new SphinxBasedPostProcessor(
				configname + ".punigram.xml", configname + ".words", 0, 0, 0);


		System.out.println("Starting Google+Sphinx Sentences");
		final SphinxBasedPostProcessor pgr_sentences = new SphinxBasedPostProcessor(
				configname + ".pgrammarsentences.xml", configname + ".words",
				0, 0, 0);

		System.out.println("Starting Google+Wordlist");
		WordlistPostProcessor llr = new WordlistPostProcessor(configname
				+ ".words",key);
		
		

		String filename = "back_fs_1387386033021_m1.wav";
		String sentence = "there is a door in the back";
		playSound(filename);
		testFile(filename,
				 sentence,
					configname,  bare,  lr,  sr_ngram, sr_fsg_sentences
						, pnr, pur, pgr_sentences, llr);
		
		waitForEnter();

		filename = "front_fs_1387379085134_m1.wav";
		sentence = "the door is in front of you";
		playSound(filename);
		testFile(filename,
				 sentence,
					configname,  bare,  lr,  sr_ngram, sr_fsg_sentences
						, pnr, pur, pgr_sentences, llr);
		waitForEnter();
		filename = "home_fs_1387379071054_m1.wav";

		sentence = "the kitchen is at home";
		playSound(filename);
		testFile(filename,
				 sentence,
					configname,  bare,  lr,  sr_ngram, sr_fsg_sentences
						, pnr, pur, pgr_sentences, llr);
		waitForEnter();
	
		filename = "show_fs_1387385878857_m1.wav";
		sentence = "robot show me the pen";
		playSound(filename);
		testFile(filename,
				 sentence,
					configname,  bare,  lr,  sr_ngram, sr_fsg_sentences
						, pnr, pur, pgr_sentences, llr);
		
		
		
	}
	
	public static void playSound(String filename)
	{

		File file = new File(filename);

		try {

	        Clip clip = AudioSystem.getClip();
	        // Open audio clip and load samples from the audio input stream.
	        AudioInputStream inputStream = AudioSystem.getAudioInputStream(file);
	        
	        AudioFormat format = inputStream.getFormat();
            DataLine.Info info = new DataLine.Info(Clip.class, format);
	        
            clip = (Clip)AudioSystem.getLine(info);
            
	        clip.open(inputStream);
	        clip.start();
	        
	        

	        
		} catch (UnsupportedAudioFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        // Get a sound clip resource.
 catch (LineUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public static void waitForEnter() {
	    Console c = System.console();
	    if (c != null) {

	        c.format("\nPress ENTER to proceed.\n");
	        c.readLine();
	    }
	}
	
	public static void testFile(String filename,
	String sentence,
			String configname, RawGoogleRecognizer bare, SentencelistPostProcessor lr, SphinxRecognizer sr_ngram,SphinxRecognizer sr_fsg_sentences
			,SphinxBasedPostProcessor pnr,SphinxBasedPostProcessor pur,SphinxBasedPostProcessor pgr_sentences,WordlistPostProcessor llr) {

		
				sentence = sentence.replaceAll("[^a-zA-Z 0-9;]", "");
				sentence = sentence.replaceAll(" +", " ");
				if (!sentence.equals(""))
					if (sentence.charAt(0) == ' ')
						sentence = sentence.substring(1);

				Printer.printColor(Printer.ANSI_CYAN, sentence);


				String googleResult = "";
				String levenResult = "";
				String hypLLR = "";
				String hypPNR = "";
				String hypPUR = "";

				String hypPGRsentences = "";



				Result r = null;
				r = bare.recognizeFromFile(filename);

				if (r != null) {
					googleResult = r.getBestResult();


					r = lr.recognizeFromResult(r);


					if (r != null)
						levenResult = r.getBestResult();
					r = new Result();
					
					r.addResult(googleResult);


					r = llr.recognizeFromResult(r);
				
					if (r != null)
						hypLLR = r.getBestResult();
					r = new Result();
					r.addResult(googleResult);

					
					r = pnr.recognizeFromResult(r);
					
					
					if (r != null)
						hypPNR = r.getBestResult();

					r = new Result();
					r.addResult(googleResult);
					
					r = pur.recognizeFromResult(r);


					if (r != null)
						hypPUR = r.getBestResult();

					r = new Result();
					r.addResult(googleResult);



					r = new Result();
					r.addResult(googleResult);

					
					r = pgr_sentences.recognizeFromResult(r);
					
					if (r != null)
						hypPGRsentences = r.getBestResult();

				} 
				

				System.out.println("Raw Google: "+googleResult);
				

				System.out.println("Google+Sentencelist: "+levenResult);


				System.out.println("Google+Wordlist: "+hypLLR);

				System.out.println("Google+Sphinx N-Gram: "+hypPNR);

				System.out.println("Google+Sphinx Unigram: "+hypPUR);

				System.out.println("Google+Sphinx Sentences: "+hypPGRsentences);



				r = sr_fsg_sentences.recognizeFromFile(filename);


				String result = "";
				if (r != null) {
					result = r.getBestResult();

				} 

				System.out.println("Sphinx Sentences: "+result);

				r = sr_ngram.recognizeFromFile(filename);

				result = "";
				if (r != null) {
					result = r.getBestResult();

				} 

				System.out.println("Sphinx N-Gram: "+result);
	
	}
	
	
	public static void testLive() {


		String configname = "CORE_TEST_SET";
		


		System.out.println("Starting Raw Google");
		RawGoogleRecognizer bare = new RawGoogleRecognizer(key);

		System.out.println("Starting Google+Sentencelist");
		SentencelistPostProcessor lr = new SentencelistPostProcessor(configname
				+ ".sentences", 1,key);


		System.out.println("Starting Google+Sphinx N-Gram");
		final SphinxBasedPostProcessor pnr = new SphinxBasedPostProcessor(
				configname + ".pngram.xml", configname + ".words", 0, 0, 0);

		System.out.println("Starting Google+Sphinx Unigram");
		final SphinxBasedPostProcessor pur = new SphinxBasedPostProcessor(
				configname + ".punigram.xml", configname + ".words", 0, 0, 0);


		System.out.println("Starting Google+Sphinx Sentences");
		final SphinxBasedPostProcessor pgr_sentences = new SphinxBasedPostProcessor(
				configname + ".pgrammarsentences.xml", configname + ".words",
				0, 0, 0);

		System.out.println("Starting Google+Wordlist");
		WordlistPostProcessor llr = new WordlistPostProcessor(configname
				+ ".words",key);
		Result r;
		ExampleChooser ec = new ExampleChooser("speech_wtm_5words");


		VoiceActivityDetector vac = new VoiceActivityDetector(
				new LocalMicrophone(), "LocalMicrophone");

		while (true) {

			ec.printRandomExample();

			r = bare.recognize(vac);
			Printer.printTime();
			System.out.println();


			String googleResult = "";
			String levenResult = "";
			String hypLLR = "";
			String hypPNR = "";
			String hypPUR = "";
			String hypPGRsentences = "";






			if (r != null) {
				googleResult = r.getBestResult();


				r = lr.recognizeFromResult(r);


				if (r != null)
					levenResult = r.getBestResult();
				r = new Result();
				
				r.addResult(googleResult);


				r = llr.recognizeFromResult(r);
			
				if (r != null)
					hypLLR = r.getBestResult();
				r = new Result();
				r.addResult(googleResult);

				
				r = pnr.recognizeFromResult(r);
				
				
				if (r != null)
					hypPNR = r.getBestResult();

				r = new Result();
				r.addResult(googleResult);
				
				r = pur.recognizeFromResult(r);


				if (r != null)
					hypPUR = r.getBestResult();

				r = new Result();
				r.addResult(googleResult);



				r = new Result();
				r.addResult(googleResult);

				
				r = pgr_sentences.recognizeFromResult(r);
				
				if (r != null)
					hypPGRsentences = r.getBestResult();

			} 
			
		

			System.out.println("Raw Google: "+googleResult);
			

			System.out.println("Google+Sentencelist: "+levenResult);


			System.out.println("Google+Wordlist: "+hypLLR);

			System.out.println("Google+Sphinx N-Gram: "+hypPNR);

			System.out.println("Google+Sphinx Unigram: "+hypPUR);

			System.out.println("Google+Sphinx Sentences: "+hypPGRsentences);


		}

	}
	
	
}
