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

/**
 * utility class
 * @author 7twiefel
 *
 */
public class PhonemePair {

	String phoneme1;
	String phoneme2;
	public PhonemePair(String phoneme1, String phoneme2) {
		super();
		this.phoneme1 = phoneme1;
		this.phoneme2 = phoneme2;
		
		
		
	}
	  @Override
	  public boolean equals( Object o )
	  {
	    if ( o == null )
	      return false;

	    if ( o == this )
	      return true;

	    if ( ! o.getClass().equals(getClass()) )
	      return false;

	    PhonemePair that = (PhonemePair) o;

	    return    this.phoneme1.equals(that.phoneme1)
	           && this.phoneme2.equals(that.phoneme2);
	  }
}
