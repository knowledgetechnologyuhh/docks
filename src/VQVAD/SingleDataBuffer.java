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
