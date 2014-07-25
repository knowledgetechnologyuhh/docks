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

/**
 * This class is a utility class to be able to compare and sort Levenshtein distances of a Collection
 * @author 7twiefel
 *
 */

@SuppressWarnings("rawtypes")
public class LevenshteinResult implements Comparable {
	private int distance;
	private int id;
	
	private int matchingId;
	
	/**
	 * 
	 * @return input index
	 */
	public int getMatchingId() {
		return matchingId;
	}
/**
 * 
 * @param distance Levenshtein distance
 * @param id index of reference in a given list 
 * @param matchingId of the input index in a given list 
 */
	public LevenshteinResult(int distance, int id, int matchingId) {
		super();
		this.distance = distance;
		this.id = id;
		this.matchingId = matchingId;
	}
/**
 * 
 * @return Levenshtein distance between input and reference
 */
	public int getDistance() {
		return distance;
	}
	
	/**
	 * 
	 * @return reference index
	 */
	public int getId() {
		return id;
	}
	@Override
	public int compareTo(Object arg0) {
		// TODO Auto-generated method stub
		LevenshteinResult otherResult = (LevenshteinResult)arg0;
		if(this.distance<otherResult.distance)
			return -1;
		else if(this.distance==otherResult.distance)
			return 0;
		else 
			return 1;
	}
	
}
