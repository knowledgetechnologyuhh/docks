package VQVAD;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;

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

		VQVoiceActivityDetector vac = new VQVoiceActivityDetector(null, "foo");

		AudioInputStream debugAis = copyAis(vac, vac.getFormat());

		Player.playStream(debugAis);


	}

}
