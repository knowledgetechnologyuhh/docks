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
package Frontend;

import java.util.LinkedList;
import java.util.List;

import Data.PhoneData;

import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.DataStartSignal;
import edu.cmu.sphinx.frontend.FrontEnd;
/**
 * This class is used as Frontend for the SphinxBasedPostProcessor.
 * @author 7twiefel
 *
 */
public class PhoneFrontEnd extends FrontEnd {

	private List<Data> phones;
	private int substitutionMethod;
	
	
	public PhoneFrontEnd() {
		super();
		phones = new LinkedList<Data>();
	}
	
	/**
	 * sets the substitution method
	 * @param substitutionMethod  (see PhonemeSubstitution)
	 */
	public void setSubstitutionMethod( int substitutionMethod)
	{
		this.substitutionMethod = substitutionMethod;
	}
	
	/**
	 * adds the phoneme sequence to the input
	 * @param phonemes phoneme sequence
	 */
	public void addPhonemes(String[] phonemes)
	{
		phones.add(new DataStartSignal(0));
		phones.add(new PhoneData("SIL",substitutionMethod));
		for(String p: phonemes)
		{
			phones.add(new PhoneData(p,substitutionMethod));
			phones.add(new PhoneData(p,substitutionMethod));
		}
		phones.add(new PhoneData("SIL",substitutionMethod));
		phones.add(new PhoneData("SIL",substitutionMethod));
		phones.add(new PhoneData("SIL",substitutionMethod));
		phones.add(new DataEndSignal(100));
	}

	/**
	 * used internally
	 */
    public void setDataSource(DataProcessor dataSource) {
    }
    
	@Override
	public Data getData() throws DataProcessingException {
		Data d = phones.isEmpty() ? null : phones.remove(0);
		return d;
	}
	
}
