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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

/**
 * used to choose random example sentences from a list of sentences
 * @author 7twiefel
 *
 */
public class ExampleChooser {
	
	String sentenceFile;
	ArrayList<String> sentences = new ArrayList<String>();
	Random gen = new Random();
	int size=0;

	//creates a new example chooser
	public ExampleChooser(String sentenceFile) {
		super();
		this.sentenceFile = sentenceFile;

		Scanner in;
			try {
				in = new Scanner(new FileReader(sentenceFile + ".txt"));
	
			in.useDelimiter("\n");

			String temp = "";
			while (in.hasNext()) {
				temp = in.next();
				if(temp.indexOf("\r")!=-1)
				{
					temp = temp.substring(0, temp.length() - 1);
				} else temp = temp.substring(0, temp.length());

				temp=temp.replaceAll("[^a-zA-Z 0-9]", "");
				temp=temp.replaceAll(" +", " ");
				if(temp.charAt(0)==' ')
					temp=temp.substring(1);
				
				sentences.add(temp.toLowerCase());
			}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			size=sentences.size();
			
	}
	
	/**
	 * prints out a random sentence from the sentencelist
	 * @return the random sentence
	 */
	public String printRandomExample()
	{
		System.out.println();
		int i = gen.nextInt(size-1);
		System.out.println("Please read the following sentence:");
		Printer.printColor(
				Printer.ANSI_BLUE,
				"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
		System.out.println();
		System.out.println(sentences.get(i));
		System.out.println();
		Printer.printColor(
				Printer.ANSI_BLUE,
				"\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0\u25a0");
		return sentences.get(i);
	}

}
