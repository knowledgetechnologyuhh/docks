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
import java.io.Serializable;
import java.util.ArrayList;


/**
 * used by the phoneme creator to store phoneme sequences
 * @author 7twiefel
 *
 */
public class PhonemeContainer implements Serializable{

	private static final long serialVersionUID = 3851781084055799229L;
	private String[] phonemes;
	private String[] words;
	private String[] phonemesSorted;
	private ArrayList<String> phonemesList = new ArrayList<String>();


	public void addPhonemesNoJep(String[] phonemeSequence)
	{
		phonemes=phonemeSequence;
	}

/**
 * 
 * @param words single words as an array
 */
	public PhonemeContainer(String[] words) {
		super();
		this.words = words;
		phonemesSorted = new String[words.length];

	}
	
/**
 * adds a phoneme for a word to the phoneme sequence
 * @param label word as grapheme sequence
 * @param seq word as phoneme sequence
 */
	public void tryAddPhonemeSequence(String label,String seq)
	{
		for(int i = 0; i<words.length;i++)
		{
			
			if(label.equals(words[i]))
			{
				phonemesSorted[i]=seq;
			}
				
		}
		//System.out.println(Arrays.toString(words)+" "+Arrays.toString(phonemesSorted)) ;
		
	}
	/**
	 * 
	 * @return the final phoneme sequence
	 */
	public String[] getPhonemes() {
		return phonemes;
	}
	public String[] getWords() {
		return words;
	}

	/**
	 * after adding all phonemes, they need to be finalized
	 */
	public void finalizePhonemes()
	{
		
		for(int i = 0; i<phonemesSorted.length;i++)
		{

			String[] s = phonemesSorted[i].split(" ");
			for(String ss:s)
			{
				phonemesList.add(ss);
			}




		}
		phonemes=new String[phonemesList.size()];
		phonemesList.toArray(phonemes);
	}
	
	/**
	 * prints out word sequences with their phoneme representation
	 */
	public void print()
	{
		for(String w : words)
		{
			if(w==null)
				break;
			System.out.print(w+" ");

		}
		System.out.print(": ");
		for(String p : phonemes)
		{			
			if(p==null)
				break;
			System.out.print(p+" ");

		}
		System.out.print("\n");
	}
	
	/**
	 * prints out word sequences
	 */
	public void printShort()
	{
		for(String w : words)
		{
			if(w==null)
				break;
			System.out.print(w+" ");

		}
		System.out.print("\n");
	}
	
	/**
	 * 
	 * @return the word sequences
	 */
	public String getResult()
	{
		String result = "";
		for(String w : words)
		{
			if(w==null)
				break;
			if(result.equals(""))
				result = w;
			else result = result+" "+w;

		}
		return result;
	}

}
