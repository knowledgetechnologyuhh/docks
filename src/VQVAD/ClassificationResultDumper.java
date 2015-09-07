package VQVAD;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.endpoint.SpeechClassifiedData;

public class ClassificationResultDumper extends BaseDataProcessor {

	protected boolean dumpClassificationResult = false;
	protected PrintWriter resultWriter;
	protected String resultDestinationPath;

	public ClassificationResultDumper(String path) {
		try {
			enableResultWriting(path);
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException("Classification file location does not exist. Original message: " + e.getMessage());
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("Programming error: UTF-8 encoding is not supported by environment. Original message: " + e.getMessage());
		}
	}

	public void enableResultWriting(String path) throws FileNotFoundException, UnsupportedEncodingException {
		resultWriter = new PrintWriter(path, "UTF-8");
		dumpClassificationResult = true;
	}

	public void disableResultWriting() {
		if (resultWriter != null) {
			resultWriter.close();
		}
		dumpClassificationResult = false;
	}

	@Override
	public Data getData() throws DataProcessingException {
		Data d = getPredecessor().getData();

		if (d instanceof SpeechClassifiedData) {
			SpeechClassifiedData sd = (SpeechClassifiedData) d;

			if (dumpClassificationResult) {
				System.out.print(sd.isSpeech() ? "O" : "_");
				resultWriter.print(sd.isSpeech() ? "O" : "_");
			}
		} else if (d instanceof DataEndSignal) {
			if (dumpClassificationResult) {
				resultWriter.close();
			}
		}
		return d;
	}

}
