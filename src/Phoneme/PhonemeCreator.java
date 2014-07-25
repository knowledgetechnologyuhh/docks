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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import java.util.Scanner;

import javax.xml.bind.JAXBException;

import edu.cmu.sphinx.linguist.g2p.G2PConverter;
import edu.cmu.sphinx.linguist.g2p.Path;


import Data.Result;
import Utils.Printer;
/**
 * class used to convert graphemes to phonemes.
 * calls SequiturG2P internally
 * @author 7twiefel
 *
 */
public class PhonemeCreator {

	/**
	 * the public phoneme data base created for a predefined list of sentences or words
	 */
	public PhonemeDB pdb;
	
	private String TAG = "PhonemeCreator";
	private static PhonemeCreator instance;



	/**
	 * 
	 * @param sentenceFile a list of sentences stored in a file (filename)
	 * @return an instance of phoneme creator
	 */
	public synchronized static PhonemeCreator getInstance(String sentenceFile) {
		if (instance == null) {
			instance = new PhonemeCreator(sentenceFile);
		}
		return instance;
	}

	/**
	 * creates a list of phonemes corresponding to the list of results contained in r
	 * @param r Result received from a speech recognizer or postprocessor. needs to contain 1 result as string as a minimum
	 * @return
	 */
	public ArrayList<PhonemeContainer> getPhonemes(Result r) {
		Printer.printWithTimeF(TAG, "getting Phonemes");
		
		if(r==null) return null;

		ArrayList<String> rawResults = r.getResultList();
		
		ArrayList<PhonemeContainer> resultsWithPhonemes = new ArrayList<PhonemeContainer>();
		Printer.printWithTimeF(TAG, "created lists");


			try {

				Printer.printWithTimeF(TAG, "formatting raw results");
				//convert all results to lowercase and remove special characters
				for (String s : rawResults) {
					Printer.printWithTimeF(TAG, "raw result: "+s);

					s=s.replaceAll("[^a-zA-Z 0-9]", "");
					s=s.replaceAll(" +", " ");
					if(s.equals(""))
					{
						rawResults.remove(s);
						continue;
					}
					if(s.charAt(0)==' ')
						s=s.substring(1);

					
					//System.out.println("S: "+s);
					//split the sentences to words and add theses to the args for SequiturG2P
					
					String[] words = s.toLowerCase().split(" ");
					
					PhonemeContainer pc = new PhonemeContainer(words);

					ArrayList<Path> paths = g2pDecoder.phoneticize(s, 1);
					//System.out.println("Sphinx G2P");

					
					for (Path p : paths) {
						 //System.out.println(p.getPath());

							String[] phonemes=new String[p.getPath().size()];
							p.getPath().toArray(phonemes);
							 pc.addPhonemesNoJep(phonemes);
						 break;
					}
				
					resultsWithPhonemes.add(pc);
				}

			} catch (Exception e) {
				System.err.println("Error: " + e.getMessage());
			}


		
		
		//return the phoneme containers
		return resultsWithPhonemes;
	}
	
	
	private G2PConverter g2pDecoder; 
	
	/**
	 * creates a new phoneme creator. used when no precached results of a list of sentences should be loaded
	 */
	public PhonemeCreator()
	{
		String sequiturSphinxModel = "g2p/sequitur/cmudict_sequitur.fst.ser";
		String sequiturFSAModel = "g2p/sequitur/cmudict_sequitur.fsa.txt";
		if(!(new File(sequiturSphinxModel)).exists())
			try {
				SequiturImport.importSequitur(sequiturFSAModel, sequiturSphinxModel);
			} catch (JAXBException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		g2pDecoder = new G2PConverter(sequiturSphinxModel);
		
	}	
	
	/**
	 * creates an instance of a phoneme creator. used when no precached results of a list of sentences should be loaded
	 */
	public synchronized static PhonemeCreator getInstance() {
		if (instance == null) {
			instance = new PhonemeCreator();
		}
		return instance;
	}

	/**
	 * creates a new phoneme creator and caches the results for the list of sentences
	 * @param sentenceFile
	 */
	public PhonemeCreator(String sentenceFile) {
		this();
		InputStream fis = null;



		try {

			//try to read the cached phonemes
			fis = new FileInputStream(sentenceFile + ".ser");
			ObjectInputStream o = new ObjectInputStream(fis);

			pdb = new PhonemeDB();
			pdb = (PhonemeDB) o.readObject();

		} catch (IOException e) {
			System.err.println(e);
			
			//if no cached phonemes are available cache ones
			fillDatabase(sentenceFile);
			
			//try to read the cached phonemes again
			try {
				fis = new FileInputStream(sentenceFile + ".ser");
				ObjectInputStream o = new ObjectInputStream(fis);

				pdb = new PhonemeDB();
				pdb = (PhonemeDB) o.readObject();

			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e3) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			

		} catch (ClassNotFoundException e) {
			System.err.println(e);
			
		} finally {
			try {
				fis.close();
				System.out.println("Loaded " + sentenceFile
						+ ".ser successfully");

			} catch (Exception e) {
			}
		}
	}

	private void fillDatabase(String sentenceFile) {

		Scanner in;
		try {
			//reads sentence file
			in = new Scanner(new FileReader(sentenceFile + ".txt"));
			in.useDelimiter("\n");

			Result r = new Result();
			String temp = "";
			
			//separate words and add the to the input arg
			while (in.hasNext()) {
				temp = in.next();
				if(temp.indexOf("\r")!=-1)
				{
					temp = temp.substring(0, temp.length() - 1);
				} else temp = temp.substring(0, temp.length());

				r.addResult(temp);
			}
			System.out.println("getting results");

			//get the phonemes
			ArrayList<PhonemeContainer> phonemes = getPhonemes(r);

			System.out.println("phoneme creation successful!");
			
			//set the public data base to the phonemes
			PhonemeDB pdb = new PhonemeDB();
			pdb.arrayContent = getPhonemes(r);

			//add the phonemes to the database
			for (PhonemeContainer res : phonemes) {
				String x = "";
				for (String w : res.getWords()) {
					if (w == null)
						break;
					if (x.equals(""))
						x = w;
					else
						x = x + " " + w;
				}
				pdb.hashContent.put(x, res.getPhonemes());
			}


			OutputStream fos = null;

			//serialize the database
			try {

				fos = new FileOutputStream(sentenceFile
						+ ".ser");
				ObjectOutputStream o = new ObjectOutputStream(fos);
				o.writeObject(pdb);

			} catch (IOException e) {
				System.err.println(e);
			} finally {
				try {
					fos.close();
				} catch (Exception e) {
				}
			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
