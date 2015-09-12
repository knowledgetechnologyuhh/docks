/**
 * Copyright (C) 2015 Marian Tietz
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
 * vqvad@nemo.ikkoku.de
 */
package VQVAD;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataProcessingException;

/**
 * Buffers one packet
 *
 * The data packet you get when calling getBufferedData
 * is the packet that gets send the next time getData is called.
 *
 */
public class SingleDataBuffer extends BaseDataProcessor {

	protected Data lastData;

	public SingleDataBuffer() {	}

	/**
	 * Returns the data packet that gets sent the next time getData is called.
	 * @return
	 */
	public Data getBufferedData() {
		if (lastData == null) {
			lastData = getPredecessor().getData();
		}
		return lastData;
	}

	@Override
	public Data getData() throws DataProcessingException {
		Data toSend = getBufferedData();

		lastData = getPredecessor().getData();

		return toSend;
	}

}
