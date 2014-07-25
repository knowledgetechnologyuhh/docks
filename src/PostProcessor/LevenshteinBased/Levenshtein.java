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
package PostProcessor.LevenshteinBased;
/**
 * origin: http://www.java-blog-buch.de/c-levenshtein-distanz/
 * modified by Johannes Twiefel
 *
 */
public class Levenshtein {

 /**
  * calculates Levenshtein distance between two arrays of symbols
  * @param reference
  * @param input
  * @return
  */
  public int diff(String[] reference, String[] input) {
 
	//create a matrix for the distances
    int matrix[][] = new int[reference.length + 1][input.length + 1];
    
    //initilize the outer left and top borders with increasing values
    for (int i = 0; i < reference.length + 1; i++) {
      matrix[i][0] = i;
    }
    for (int i = 0; i < input.length + 1; i++) {
      matrix[0][i] = i;
    }
    //fill the rest of the matrix
    for (int a = 1; a < reference.length + 1; a++) {
      for (int b = 1; b < input.length + 1; b++) {
    	  
        int right = 0;
        //compare cell left to and over cell to fill, if match =0 otherwise 1
        if (!reference[a - 1].equals(input[b - 1])) {
          right = 1;
        }
        //check if going down or going to the right has a smaller distance
        int mini = matrix[a - 1][b] + 1;
        if (matrix[a][b - 1] + 1 < mini) {
          mini = matrix[a][b - 1] + 1;
        }
        
        //check if last cell adding the new distance compared to the direction chosen is smaller
        if (matrix[a - 1][b - 1] + right < mini) {
        	//if yes, set values from best direction to that
          mini = matrix[a - 1][b - 1] + right; 
        }
        //set actual cell to that value
        matrix[a][b] = mini;
      }
    }
    //return the bottom right value as minimal distance.
    return matrix[reference.length][input.length];
  }
}