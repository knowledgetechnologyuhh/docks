package VQVAD;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import Utils.Player;

import Frontend.VQVoiceActivityDetector;

public class PlaybackTest {

	private static AudioInputStream copyAis(AudioInputStream source, AudioFormat format) {
		boolean run = true;
		int i = 0;
		int buffer_size = 4000;
		byte tempBuffer[] = new byte[buffer_size];
		ByteArrayOutputStream debugStream = new ByteArrayOutputStream();

		while (run) {
			int cnt = -1;
			try {//read data from the audio input stream
				cnt = source.read(tempBuffer, 0, buffer_size);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			if (cnt > 0) {//if there is data
				try {
					debugStream.write(tempBuffer);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				run = false;
			}
		}

		return new AudioInputStream (new ByteArrayInputStream(debugStream.toByteArray()), format, debugStream.size());
	}

	/**
	 * @param args
	 * @throws MalformedURLException
	 */
	public static void main(String[] args) throws MalformedURLException {

		//URL path = new URL("file:///home/nemo/Documents/Studium/Master/study/code/VQVAD/foo.wav");
		//URL path = new URL("file:///home/nemo/Documents/Studium/Master/study/code/VQVAD/trainset/noizeus_train/10dB/sp12_train_sn10.wav");
		URL path = new URL("file:///home/nemo/Documents/Studium/Master/study/code/VQVAD/trainset/noise_only.wav");

		VQVoiceActivityDetector vac = new VQVoiceActivityDetector(path, 8000, "foo");

		AudioInputStream debugAis = copyAis(vac, vac.getFormat());

		Player.playStream(debugAis);


	}

}
