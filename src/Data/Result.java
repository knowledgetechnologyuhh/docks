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
package Data;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
/**
 * Stores a result of speech recognition.
 * Can contain n-best list, phonemes.
 * Can also be serialized and loaded.
 * @author 7twiefel
 *
 */
public class Result implements Serializable {
	
	private static final long serialVersionUID = 1650789290776731090L;

	private ArrayList<String> resultList = new ArrayList<String>();
	private float confidence;

	
	private String hypPhoneme;
	private String refPhoneme;
	
	/**
	 * 
	 * @return phonemes of hypothesis
	 */
	public String getHypPhoneme() {
		return hypPhoneme;
	}
/**
 * 
 * @param hypPhoneme phonemes of hypothesis
 */
	public void setHypPhoneme(String hypPhoneme) {
		this.hypPhoneme = hypPhoneme;
	}
/**
 * 
 * @return phonemes of reference
 */
	public String getRefPhoneme() {
		return refPhoneme;
	}
	/**
	 * 
	 * @return phonemes of reference
	 */
	public void setRefPhoneme(String refPhoneme) {
		this.refPhoneme = refPhoneme;
	}
/**
 * 
 * @return n-best list
 */
	public ArrayList<String> getResultList() {
		return resultList;
	}
/**
 * 
 * @param resultList sets n-best list
 */
	public void setResultList(ArrayList<String> resultList) {
		this.resultList = resultList;
	}



/**
 * 
 * @param s adds result string to n-best list
 */
	public void addResult(String s) {
		resultList.add(s);
	}

	/**
	 * 
	 * @return best result of n-best list
	 */
	public String getBestResult()
	{
		return resultList.get(0);
	}
	
	/**
	 * 
	 * @param f sets confidence for best result
	 */
	public void setConfidence(float f) {
		confidence = f;
	}

	/**
	 * 
	 * @return n-best list as array
	 */
	public String[] getResult() {
		String[] result = new String[resultList.size()];
		resultList.toArray(result);
		return result;
	}

	/**
	 * prints out n-best list
	 */
	public void print() {
		System.out.println("");
		System.out.println("= = = = = = = = =");
		System.out.println("Results");
		System.out.println("");
		System.out.println("Confidence: " + confidence);
		System.out.println("");
		System.out.println("N-Best List: ");
		System.out.println("");
		for (String s : resultList) {
			System.out.println(s);
		}
		System.out.println("= = = = = = = = =");
		System.out.println("");
	}

	/**
	 * writes n-best list to a words.txt
	 */
	public void writeToFile() {
		try {
			// Create file
			FileWriter fstream = new FileWriter("words.txt");
			BufferedWriter out = new BufferedWriter(fstream);
			for (String s : resultList) {
				out.write(s+"\n");
			}

			// Close the output stream
			out.close();
		} catch (Exception e) {// Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}
	}
	
	/**
	 * serializes result to a file
	 * @param file filename
	 */
	public void save(String file) {
		OutputStream fos = null;

		try {

			fos = new FileOutputStream(file);
			ObjectOutputStream o = new ObjectOutputStream(fos);
			o.writeObject(this);


		} catch (IOException e) {
			System.err.println(e);
		} finally {
			try {
				fos.close();
			} catch (Exception e) {
			}
		}
	}
	
	/**
	 * loads result from file
	 * @param file filename
	 * @return loaded result
	 */
	public static Result load(String file) {
		InputStream fis = null;
		Result r = null;

		try {

			fis = new FileInputStream(file);
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
}
