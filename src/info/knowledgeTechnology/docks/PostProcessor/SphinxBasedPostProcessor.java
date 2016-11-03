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
package info.knowledgeTechnology.docks.PostProcessor;

import info.knowledgeTechnology.docks.Data.Result;
import info.knowledgeTechnology.docks.Frontend.PhoneFrontEnd;
import info.knowledgeTechnology.docks.Phoneme.PhonemeContainer;
import info.knowledgeTechnology.docks.Phoneme.PhonemeCreator;
import info.knowledgeTechnology.docks.Recognizer.StandardRecognizer;
import info.knowledgeTechnology.docks.Utils.ConfigCreator;
import info.knowledgeTechnology.docks.Utils.ConsistencyManager;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.ConfidenceResult;
import edu.cmu.sphinx.result.ConfidenceScorer;
import edu.cmu.sphinx.result.Path;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;
/**
 * based on sphinx example
 * modified by Johannes Twiefel
 *
 */
public class SphinxBasedPostProcessor  implements StandardRecognizer{
	
	private ConfigurationManager cm;
	private Recognizer recognizer;

	private PhonemeCreator pc;
	private PhoneFrontEnd pfe;
	private int referenceRecognizer;

	private String name = "PhonemeNgramRecognizer";
	private ConfidenceScorer cs;
	private LogMath logMath;


	public SphinxBasedPostProcessor(String configname, String modelType)
	{
		this(configname,modelType,0,0,0);
	}
/**
 * creates a new sphinx based postprocessor
 * @param configName name of the config. this is used as a prefix for all xml config, languague model, sentence list word list files etc.
 * @param vocabFile path to the vocabulary
 * @param languageWeight parameter used internally, but 0 here if you don't know what you are doing
 * @param wordInsertionProbability parameter used internally, but 0 here if you don't know what you are doing
 * @param substitutionMethod parameter used internally, but 0 here if you don't know what you are doing
 */
	public SphinxBasedPostProcessor(String configname, String modelType, float languageWeight, float wordInsertionProbability, int substitutionMethod)
	{
		String basepath = "config/"+configname+"/"; 
		if(modelType == "docks_ngram")
		{
		if(!ConsistencyManager.isConsistent(configname, modelType+"_training_sentences.txt"))
		{
			ConfigCreator.createNgramModelFromExampleSentences(basepath+modelType+"_training_sentences.txt", configname, modelType);
		}
		} else if(modelType == "docks_grammar"){
			ConfigCreator.updateGrammar(configname, modelType);
		} else{
			throw new IllegalStateException("Unsupported model type: "
					+ modelType);
		}
		//load config xml
		try {
			cm = new ConfigurationManager(new File(	basepath+modelType+".xml").toURI().toURL());
		} catch (PropertyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		referenceRecognizer = -1;
		
		if(languageWeight != 0)
			cm.setGlobalProperty("languageWeight", languageWeight+"");
		if(wordInsertionProbability != 0)
			cm.setGlobalProperty("wordInsertionProbability", wordInsertionProbability+"");
		System.out.println("LW: "+getLanguageWeight()+" WIP: "+getWIP());
		
		//initialize frontend, recognizer and phoneme creator
		pfe = (PhoneFrontEnd) cm.lookup("frontend");
		pfe.setSubstitutionMethod(substitutionMethod);
		
		recognizer = (Recognizer) cm.lookup("recognizer");
		recognizer.allocate();
		pc = new PhonemeCreator(basepath+"model/"+modelType+".vocab");
		this.cs = (ConfidenceScorer) cm.lookup("confidenceScorer");
		//this.logMath = (LogMath) cm.lookup("logMath");

	}
	/**
	 * 
	 * @return Language Weight
	 */
	public float getLanguageWeight()
	{
		return Float.parseFloat(cm.getGlobalProperty("languageWeight"));
	}
	/**
	 * 
	 * @return Word Insertion Probability
	 */
	public float getWIP()
	{
		return Float.parseFloat(cm.getGlobalProperty("wordInsertionProbability"));
	}
	
	/**
	 * postprocesses the results of an ASR
	 * @param r result of an ASR like Google ASR
	 */
	@Override
	public Result recognizeFromResult(Result r)
	{
		//get phonemes
		ArrayList<PhonemeContainer> phonemesSpeech = pc.getPhonemes(r);
		
		//get best result
		String[] phonemes = phonemesSpeech.get(0).getPhonemes();

		//ad to phone frontend
		pfe.addPhonemes(phonemes);

		//start postprocessing
        r = null;
        edu.cmu.sphinx.result.Result result;
        while ((result = recognizer.recognize())!= null) {
        		if(r == null)
        			r= new Result();
        		

        		String refPhoneme = "";
        		for(String s: phonemes)
        			refPhoneme = refPhoneme + s + " ";

        		//get phoneme sequence of hypotheses
        		String[] s2 = result.getBestPronunciationResult().split(" ");
        		Pattern p = Pattern.compile(".*\\[(.*)\\]");
        		
        		String hypPhoneme = "";
        		//get phonemes for hypotheses phoneme sequence
        		for(String s: s2)
        		{
        			Matcher m = p.matcher(s);
        			if(m.find()){
        				String[] s3 = m.group(1).split(",");
        				for(String s4: s3)
        					hypPhoneme = hypPhoneme + s4 + " ";
        			}
        		}

        		//set the phoneme sequences in the result
        		r.setRefPhoneme(refPhoneme);
        		r.setHypPhoneme(hypPhoneme);
        	    
  
        		//get best result
                String resultText = result.getBestFinalResultNoFiller();
                if(resultText.equals(""))
                	return null;
                r.addResult(resultText);
//                ConfidenceResult cr = cs.score(result);
//                Path best = cr.getBestHypothesis();
//                double logConfidence = best.getConfidence();
//                System.out.println("Score: "+logConfidence);
//                double confidence = LogMath.getInstance().logToLinear(logConfidence);
//                System.out.println("Score: "+confidence);
//                float confidenceScore = (float) best.getConfidence();
                r.setConfidence(-1);

                //add rest to 10-best list
                int i = 0;
                for(Token t: result.getResultTokens())
                {

                	
                	if(i>=9)
                		break;
                	r.addResult(t.getWordPathNoFiller());
                	i++;
                }
                

        }
       return r;

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
