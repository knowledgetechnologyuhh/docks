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




import Phoneme.PhonemeSubstitution;

import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.util.LogMath;
/**
 * This class is used by the Phone Frontend as input
 * @author 7twiefel
 *
 */
@SuppressWarnings("serial")
public class PhoneData implements Data {
	
	/**
	 * 
	 */
	String phoneID;
	static LogMath logMath =  LogMath.getInstance();
	int method;
	
	/**
	 * @see Phoneme.Categories.PhonemeSubstitution
	 * @param phoneID the phoneme representation
	 * @param method the phoneme substitution method 
	 */
	public PhoneData(String phoneID, int method) {
		this.phoneID = phoneID;
		this.method = method;

  

		
	}
	
	/**
	 * calculates score between two phonemes 
	 * @param otherPhone
	 * @param numberOfTimesUsed
	 * @return
	 */
	public float getConfusionScore(String otherPhone, int numberOfTimesUsed) {
		return logMath.linearToLog(PhonemeSubstitution.getInstance(method).getScore(phoneID, otherPhone));
	}
	
	public String toString()
	{
		return phoneID;
	}
	
}
