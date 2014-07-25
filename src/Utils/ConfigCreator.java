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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import Data.Result;
import Phoneme.PhonemeCreator;
/**
 * class used to create a full configuration from a batchfile
 * those are list of sentences, word list, XMLs for sphinx and sphinx based, language models etc.
 * @author 7twiefel
 *
 */
public class ConfigCreator {
	private static String TAG = "ConfigCreator";
	//creates a new xml file for the config
	private static void createXML(String basepath, String configname, String xmlname) {
		String prefix = "config/example/example";

		FileInputStream fstream = null;
		try {
			fstream = new FileInputStream(prefix + xmlname);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine;


		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter(basepath+configname + xmlname));
			
			//read in an example XML
			while ((strLine = br.readLine()) != null) {
				if(strLine.contains("<property name=\"grammarfile\""))
				{
					out.write("<property name=\"grammarfile\" value=\""+configname+"\"/>\n");
				}else if(strLine.contains("<property name=\"grammarpath\""))
				{
					out.write("<property name=\"grammarpath\" value=\"file:"+basepath+"/model/\"/>\n");
				} else if(strLine.contains("<property name=\"dictionaryfile\""))
				{
					out.write("<property name=\"dictionaryfile\" value=\"file:"+basepath+"model/"+configname+".dic\"/>\n");
				} else if(strLine.contains("<property name=\"languagemodelfile\""))
				{
					out.write("<property name=\"languagemodelfile\" value=\"file:"+basepath+"model/"+configname+".lm\"/>\n");
				} else
					out.write(strLine+"\n");
			}
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	//remove all serialized phonemes
	private static void removeSerializedFiles(String configname)
	{
		String sentenceFile = "config/"+configname+"/"+configname+".sentences.ser";
				String wordFile = "config/"+configname+"/"+configname+".words.ser";
				System.out.println("deleting: "+sentenceFile);
				System.out.println("deleting: "+wordFile);
		boolean sentenceFileDeleted = (new File(sentenceFile)).delete();
		boolean wordFileDeleted = (new File(wordFile)).delete();
		
		System.out.println("Sentence file deleted: "+Boolean.toString(sentenceFileDeleted)); 
				System.out.println("Word file deleted: "+Boolean.toString(wordFileDeleted));
	}

	
	/**
	 * creates a new config from a batchfile in /config/<nameofconfig>/
	 * @param configname name of the config
	 * @param batchfile path to batchfile
	 */
	public static void createConfig(String configname, String batchfile) {
		System.out.println("creating config:");
		System.out.println(configname);
		System.out.println("batchfile: "+batchfile);
		String basepath = "config/"+configname+"/";
		(new File(basepath)).mkdirs();
		(new File(basepath+"/model")).mkdirs();
		//remove serialized phonemes
		removeSerializedFiles(configname);
		//create sentence list
		createSentenceList(batchfile, basepath+configname + ".sentences.txt");
		//remove dublicates
		sentenceFileDeleteDublicates(basepath+configname+".sentences.txt");
		//create word list
		sentenceListToWordList(basepath+configname + ".sentences.txt", basepath+configname
				+ ".words.txt");
		//create dictionary
		wordListToDictionary(basepath,basepath+configname + ".words", basepath+"model/" + configname
				+ ".dic");
		//create a sentencelist grammar
		sentenceListToGrammar(basepath+configname + ".sentences.txt",basepath+"model/"+configname+".gram");
		//create a grammar of sentences config for sphinx
		createXML(basepath,configname,".fsgsentences.xml");
		//create an n-gram config for sphinx
		createXML(basepath,configname,".ngram.xml");
		//create a grammar of sentences config for sphinx based postprocessors
		createXML(basepath,configname,".pgrammarsentences.xml");
		//create an n-gram config for sphinx based postprocessors
		createXML(basepath,configname,".pngram.xml");
		createXML(basepath,configname,".punigram.xml");
		//create a sent file from a sentencelist
		sentenceListToSentFile(basepath,basepath+configname+".sentences.txt",configname);
		//create a language model
		sentFileToLanguageModel(basepath,basepath+"model/"+configname+".sent",configname);
	}

	
	//create a grammar of sentence out of a list of sentences
	private static void sentenceListToGrammar(String sentenceFile,
			String grammarFile) {

		FileInputStream fstream = null;
		try {
			fstream = new FileInputStream(sentenceFile);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine;


		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter(grammarFile));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try {
			out.write("#JSGF V1.0;" + "\n" + "\n" + "/**" + "\n"
					+ " * JSGF Grammar for WTM" + "\n" + " */" + "\n" + "\n"
					+ "grammar scenario;" + "\n" + "\n"
					+ "public <utterance> = " + "(" + br.readLine() + ")");

			while ((strLine = br.readLine()) != null) {
				System.out.println(strLine);
				out.write(" |" + "\n" + "(" + strLine + ")");

			}
			out.write(";");
			System.out.println("AAA");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	//create a sentencelist from a batch file
	private static void createSentenceList(String inputBatch,
			String nameSentenceFile) {
		FileInputStream fstream = null;
		try {
			fstream = new FileInputStream(inputBatch);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine;

		BufferedWriter out = null;

		try {
			out = new BufferedWriter(new FileWriter(nameSentenceFile));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try {
			while ((strLine = br.readLine()) != null) {
				int endName = strLine.indexOf(" ");
				String sentence = strLine.substring(endName + 1);
				sentence = sentence.replaceAll(",", "");
				sentence = sentence.replaceAll("\\.", "");
				out.write(sentence + "\n");

			}
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * change the path of audiofile in a batchfile
	 * @param batchfile
	 * @param newPath
	 */
	public static void changeBatchFilePath(String batchfile, String newPath) {

		FileInputStream fstream = null;
		try {
			fstream = new FileInputStream(batchfile);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine;


		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter(batchfile + ".new"));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try {
			while ((strLine = br.readLine()) != null) {

				String rest = strLine.substring(strLine.lastIndexOf("/") + 1);
				String line = newPath + rest + "\n";
				System.out.print(line);
				out.write(line);
			}
			System.out.println("AAA");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		(new File(batchfile)).delete();
		(new File(batchfile + ".new")).renameTo(new File(batchfile));
	}
	
	/**
	 * creates a sub data set randomly from a batch file
	 * @param path to batch file
	 * @param batchfile name of batchfile
	 * @param contentInPercent partition of whole data set in percent
	 * @param session provide a session number if there are more sessions
	 */
	public static void createSubBatchfile(String path, String batchfile, int contentInPercent, int session) {
		int numberExamples= -1;
		try {
			numberExamples = TestSupervisor.count(path + batchfile);
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		ArrayList<Integer> numbers = new ArrayList<Integer>();
		for (int i = 0; i < numberExamples; i++) {
			numbers.add(i);
		}

	     Collections.shuffle(numbers);

		
		ArrayList<Integer> chosenNumbers = new ArrayList<Integer>();
		
		int numberChosenExamples = (int) Math.round((numberExamples*contentInPercent)/100.0);
		System.out.println(numberChosenExamples+" "+numberExamples);
		for (int i = 0; i < numberChosenExamples; i++) {
			chosenNumbers.add(numbers.get(i));
		}

	     Collections.sort(chosenNumbers);

		

		FileInputStream fstream = null;
		try {
			fstream = new FileInputStream(path+batchfile);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine;


		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter(path+batchfile +"_"+contentInPercent+"_percent_session_"+session));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try {
			int count = 0;
			while ((strLine = br.readLine()) != null) {
				if(chosenNumbers.contains(count))
					out.write(strLine+"\n");
				count++;
			}
			System.out.println("AAA");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	
	//delete dublicates from a sentencelist
	private static void sentenceFileDeleteDublicates(String sentenceFile) {

		FileInputStream fstream = null;
		try {
			fstream = new FileInputStream(sentenceFile);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine;


		ArrayList<String> sentences = new ArrayList<String>();
		
		try {
			while ((strLine = br.readLine()) != null) {

				if(!sentences.contains(strLine))
				{
					sentences.add(strLine);
					System.out.println(strLine+" "+sentences.size()+" "+"added");
				}else 
				System.out.println(strLine+" "+sentences.size()+" "+"already exists");
			}
			System.out.println("AAA");
			br.close();
			BufferedWriter out = null;
			try {
				out = new BufferedWriter(new FileWriter(sentenceFile));
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			for(String s: sentences)
			{
				out.write(s+"\n");
			}
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	@SuppressWarnings("unused")
	private static void changeBatchFilePathIcub() {
		String path_L = "/informatik2/wtm/KT-Datasets/recs_extMic_iCubOff/amplified_16bit_16000Hz_mono_L/";
		String path_R = "/informatik2/wtm/KT-Datasets/recs_extMic_iCubOff/amplified_16bit_16000Hz_mono_R/";
		String path_mean = "/informatik2/wtm/KT-Datasets/recs_extMic_iCubOff/amplified_16bit_16000Hz_mono_mean/";
		for (int i = 0; i <= 180; i = i + 15) {
			Printer.reset();
			String formNumber = String.format("%03d", i);
			changeBatchFilePath(path_L + formNumber + "_batch", path_L + "rec_"
					+ formNumber + "/");
			changeBatchFilePath(path_R + formNumber + "_batch", path_R + "rec_"
					+ formNumber + "/");
			changeBatchFilePath(path_mean + formNumber + "_batch", path_mean
					+ "rec_" + formNumber + "/");
			Printer.printWithTimeImportant(TAG, i + "");
		}

	}
	/**
	 * 
	 * @param path path to batch file
	 * @param batchfile name of batchfile
	 * @return a set of words contained in the batchfile
	 */
	public static HashSet<String> getWordList(String path, String batchfile) {
		HashSet<String> words = new HashSet<String>();
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
			while ((strLine = br.readLine()) != null) {
				int endName = strLine.indexOf(" ");
				String sentence = strLine.substring(endName + 1);
				sentence = sentence.replaceAll("[^a-zA-Z 0-9;]", "");
				sentence = sentence.replaceAll(" +", " ");
				if (!sentence.equals(""))
					if (sentence.charAt(0) == ' ')
						sentence = sentence.substring(1);
				String[] sentenceWords = sentence.split(" ");
				for (String s : sentenceWords)
					words.add(s);

			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return words;
	}
	
	
	//get an output file writer
	private static BufferedWriter getWriter(String file) {
		try {
			return new BufferedWriter(new FileWriter(file));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	//create a dictionary from a list of words
	private static void wordListToDictionary(String basepath,String wordList, String dicfile) {
		PhonemeCreator pc = PhonemeCreator.getInstance(wordList);

		HashSet<String> words = new HashSet<String>();
		FileInputStream fstream = null;
		try {
			fstream = new FileInputStream(wordList + ".txt");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));

		String strLine;
		try {
			while ((strLine = br.readLine()) != null) {

				String sentence = strLine;

				sentence = sentence.replaceAll(",", "");

				String[] sentenceWords = sentence.split(" ");
				for (String s : sentenceWords)
					words.add(s);

			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		BufferedWriter out = getWriter(dicfile);
		Result r;
		for (String s : words) {
			try {
				out.write(s + "\t");
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			r = new Result();
			r.addResult(s);
			String[] pho = pc.getPhonemes(r).get(0).getPhonemes();
			for (int i = 0; i < pho.length; i++) {
				if (i == pho.length - 1)
					try {
						out.write(pho[i] + "\n");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				else
					try {
						out.write(pho[i] + " ");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			}
		}
		try {
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	//create a language model from a sent file
	private static void sentFileToLanguageModel(String basepath,String sentFile, String configname)
	{
		 ProcessBuilder builder = new ProcessBuilder("./tools/quick_lm.pl",
		 "-s",sentFile);

		 Process p = null;
		 try {
		 p = builder.start();
		 p.waitFor();
		 (new File(basepath +"model/"+configname+".sent.arpabo")).renameTo(new
		 File(basepath +"model/"+configname+".lm"));
		 } catch (IOException e) {
		 // TODO Auto-generated catch block
		 e.printStackTrace();
		 } catch (InterruptedException e) {
		 // TODO Auto-generated catch block
		 e.printStackTrace();
		 }
	}
	
	//create a sent file from a sentence list file
	private static void sentenceListToSentFile(String basepath, String sentenceList,
			String configname) {

		FileInputStream fstream = null;
		try {
			fstream = new FileInputStream(sentenceList);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		FileWriter out = null;
		try {
			out = new FileWriter(basepath+"model/"+configname+".sent");

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		String strLine;
		try {
			while ((strLine = br.readLine()) != null) {
				String sentence = strLine;

				out.write("<s> " +sentence+ " </s>\n");


			}
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	

	}
	
	//create a wordlist from a sentencelist
	private static void sentenceListToWordList(String sentenceList,
			String wordList) {
		HashSet<String> words = new HashSet<String>();
		FileInputStream fstream = null;
		try {
			fstream = new FileInputStream(sentenceList);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));

		String strLine;
		try {
			while ((strLine = br.readLine()) != null) {
				String sentence = strLine;

				sentence = sentence.replaceAll(",", "");

				String[] sentenceWords = sentence.split(" ");
				for (String s : sentenceWords)
					words.add(s);

			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		FileWriter out;
		try {
			out = new FileWriter(wordList);
			for (String word : words)
				out.write(word + "\n");
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
