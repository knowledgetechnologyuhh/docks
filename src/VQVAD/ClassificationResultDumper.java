package VQVAD;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DataStartSignal;
import edu.cmu.sphinx.frontend.endpoint.SpeechClassifiedData;

/**
 * Write the classification result (speech/non-speech) to the file
 * specified in the given path.
 *
 * The output consists of one line with either the character 'O'
 * for a frame that was classified as speech or '_' for a frame
 * that was classified as non-speech.
 */
public class ClassificationResultDumper extends BaseDataProcessor {

	protected boolean dumpClassificationResult = false;
	protected PrintWriter resultWriter;
	protected String resultDestinationPath;

	protected final String audioFilePath;
	protected final float frameLengthMs;
	protected final float frameShiftMs;
	protected final boolean writeMetaData;

	/**
	 * Dump the classification results along with the specified meta data to the specified file.
	 *
	 * The file will contain two lines. The first is the meta data in matlab parsable format (using eval).
	 * The second line consists of _ (nonspeech) and O (speech) per frame.
	 */
	public ClassificationResultDumper(String audioFilePath, float frameLengthMs, float frameShiftMs, String path) {
		this(audioFilePath, frameLengthMs, frameShiftMs, path, true);
	}

	/**
	 * Just dump the classification results to the specified file.
	 *
	 * The file will contain one line consisting of _ (nonspeech) and O (speech) per frame.
	 */
	public ClassificationResultDumper(String path) {
		this("", 0, 0, path, false);
	}

	protected ClassificationResultDumper(String audioFilePath, float frameLengthMs, float frameShiftMs, String path, boolean writeMetaData) {
		try {
			enableResultWriting(path);
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException("Classification file location does not exist. Original message: " + e.getMessage());
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("Programming error: UTF-8 encoding is not supported by environment. Original message: " + e.getMessage());
		}

		this.audioFilePath = audioFilePath;
		this.frameLengthMs = frameLengthMs;
		this.frameShiftMs = frameShiftMs;

		this.writeMetaData = writeMetaData;
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

		if (d instanceof DataStartSignal) {
			if (dumpClassificationResult) {
				System.out.println("input_filename = '" + audioFilePath + "'; frame_length_ms = " + frameLengthMs + "; frame_shift_ms = " + frameShiftMs + ";");
				resultWriter.println("input_filename = '" + audioFilePath + "'; frame_length_ms = " + frameLengthMs + "; frame_shift_ms = " + frameShiftMs + ";");
			}
		} else if (d instanceof SpeechClassifiedData) {
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
