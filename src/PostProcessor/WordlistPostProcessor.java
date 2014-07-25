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
package PostProcessor;
import Data.Result;
import Recognizer.StandardRecognizer;

/**
 * word list postprocessor using a list of words to postprocess a result word by word
 * @author 7twiefel
 *
 */
public class WordlistPostProcessor  implements StandardRecognizer{


	private SentencelistPostProcessor lr;
	private int referenceRecognizer;
	private String name = "LexiconLookupRecognizer";
	
	/**
	 * creates a new wordlist postprocessor
	 * @param wordFile path to list of words
	 * @param referenceRecognizer recognizer the result is postprocessed from
	 * @param name of this recognizer
	 */
	public WordlistPostProcessor(String wordFile,int referenceRecognizer, String name,String key) {
		this(wordFile,key);
		this.referenceRecognizer= referenceRecognizer;
	 	this.name=name;
	}
	/**
	 * create a new wordlist postprocessor
	 * @param wordFile path to word list
	 */
	public WordlistPostProcessor(String wordFile,String key) {
		super();
		//use a Sentencelist postprocessor internally
		this.lr = new SentencelistPostProcessor(wordFile,1,key);
		referenceRecognizer= -1;
	}
	
	/**
	 * postprocess a result from another ASR
	 * @param r the result
	 */
	public Result recognizeFromResult(Result r)
	{
		//split the best result into words
		Result result = new Result();
		String hyp = r.getBestResult();
		String[] words = hyp.split(" ");
		String res = null;
		//match each word against the list of words
		for(String s: words)
		{
			Result rTemp = new Result();
			rTemp.addResult(s);
			rTemp = lr.recognizeFromResult(rTemp);
			if(res==null)
				res=rTemp.getBestResult();
			else
				res=res+" "+rTemp.getBestResult();
		}
		result.addResult(res);
		
		
		
		
		
		return result;
	}

	@Override
	public Result recognizeFromFile(String fileName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getReferenceRecognizer() {
		// TODO Auto-generated method stub
		return referenceRecognizer;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return name;
	}

}
