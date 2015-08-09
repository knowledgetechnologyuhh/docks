package VQVAD;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataBlocker;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.endpoint.NonSpeechDataFilter;
import edu.cmu.sphinx.frontend.endpoint.SpeechClassifier;
import edu.cmu.sphinx.frontend.endpoint.SpeechMarker;
import edu.cmu.sphinx.frontend.util.AudioFileDataSource;

public class PipelineTest {

	/**
	 * @param args
	 * @throws MalformedURLException
	 */
	public static void main(String[] args) throws MalformedURLException {

		//URL path = new URL("file:///home/nemo/Documents/Studium/Master/study/samplecode/VQVAD/foo.wav");
		URL path = new URL("file:///home/nemo/Documents/Studium/Master/study/samplecode/VQVAD/trainset/noizeus_train/10dB/sp12_train_sn10.wav");

		//audio source with 3200 byte read per read
		AudioFileDataSource audioDataSource = new AudioFileDataSource(3200, null);
		audioDataSource.setAudioFile(path, "pipelineTestAudioSource");

		ArrayList pipeline = new ArrayList();
		pipeline.add(audioDataSource);
		//blocks data into frames
		pipeline.add(new DataBlocker(10));
		//classifies speech frames
		pipeline.add(new VoiceActivityDetector());
		//marks as speeech
		pipeline.add(new SpeechMarker(200, 300, 100, 30, 100, 15.0));
		//removes non speech
		pipeline.add(new NonSpeechDataFilter());

		FrontEnd frontend = new FrontEnd(pipeline);

		Data d = null;
		do {
			d = frontend.getData();
		} while (d != null);

	}

}
