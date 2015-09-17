package Test.sphinx;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import VQVAD.ClassificationResultDumper;
import VQVAD.GapSmoothing;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataBlocker;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.endpoint.SpeechClassifier;
import edu.cmu.sphinx.frontend.util.AudioFileDataSource;

public class ClassificationTest {


	public static void main(String[] args) throws MalformedURLException {
		//URL path = new URL("file:///home/nemo/Documents/Studium/Master/study/code/VQVAD/foo.wav");
		//URL path = new URL("file:///home/nemo/Documents/Studium/Master/study/code/VQVAD/trainset/noise_only.wav");

		//URL path = new URL("file:///home/nemo/Documents/Studium/Master/study/code/VQVAD/trainset/noizeus_train/clean/sp12.wav");
		//URL path = new URL("file:///home/nemo/Documents/Studium/Master/study/code/VQVAD/trainset/noizeus_train/train_10dB/sp12_train_sn10.wav");
		//URL path = new URL("file:///home/nemo/Documents/Studium/Master/study/code/VQVAD/trainset/noizeus_train/train_5dB/sp12_train_sn5.wav");
		//URL path = new URL("file:///home/nemo/Documents/Studium/Master/study/code/VQVAD/trainset/noizeus_train/car_10dB/sp12_car_sn10.wav");
		//URL path = new URL("file:///home/nemo/Documents/Studium/Master/study/code/VQVAD/trainset/sp12_train_sn10_then_noise_only.wav");
		//URL path = new URL("file:///home/nemo/Documents/Studium/Master/study/code/VQVAD/trainset/sp12_train_sn10_preceeding_noise.wav");
		URL path = new URL("file:///home/nemo/Documents/Studium/Master/study/code/VQVAD/trainset/sp12_train_sn10_preceeding_noise_sp12_train_sn10_again.wav");


		AudioFileDataSource audioDataSource = new AudioFileDataSource(3200, null);
		audioDataSource.setAudioFile(path, "in");

		ArrayList<DataProcessor> pipeline = new ArrayList<DataProcessor>();
		pipeline.add(audioDataSource);
		//blocks data into frames
		pipeline.add(new DataBlocker(10));
		//classifies speech frames
		pipeline.add(new SpeechClassifier(10, 0.015, 10, 0));
		pipeline.add(new GapSmoothing(20));
		pipeline.add(new ClassificationResultDumper("/tmp/vqvad_classification_result"));

		FrontEnd frontend = new FrontEnd(pipeline);
		Data d;

		do {
			d = frontend.getData();
		} while(d != null && !(d instanceof DataEndSignal));
	}
}
