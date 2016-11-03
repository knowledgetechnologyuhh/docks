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
package info.knowledgeTechnology.docks.Test.VQVAD;

import info.knowledgeTechnology.docks.Frontend.VQVoiceActivityDetector;
import info.knowledgeTechnology.docks.Utils.Player;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import com.sun.media.sound.WaveFileWriter;

public class PlaybackTest {

	private static AudioInputStream copyAis(AudioInputStream source, AudioFormat format) {
		boolean run = true;
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
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {

		//URL path = new URL("file:///home/nemo/Documents/Studium/Master/study/code/VQVAD/foo.wav");
		//URL path = new URL("file:///home/nemo/Documents/Studium/Master/study/code/VQVAD/trainset/noise_only.wav");

		//URL path = new URL("file:///home/nemo/Documents/Studium/Master/study/code/VQVAD/trainset/noizeus_train/clean/sp12.wav");
		URL path = new URL("file:///home/nemo/Documents/Studium/Master/study/code/VQVAD/trainset/noizeus_train/train_10dB/sp12_train_sn10.wav");
		//URL path = new URL("file:///home/nemo/Documents/Studium/Master/study/code/VQVAD/trainset/noizeus_train/train_5dB/sp12_train_sn5.wav");
		//URL path = new URL("file:///home/nemo/Documents/Studium/Master/study/code/VQVAD/trainset/noizeus_train/car_10dB/sp12_car_sn10.wav");
		//URL path = new URL("file:///home/nemo/Documents/Studium/Master/study/code/VQVAD/trainset/sp12_train_sn10_then_noise_only.wav");
		//URL path = new URL("file:///home/nemo/Documents/Studium/Master/study/code/VQVAD/trainset/sp12_train_sn10_preceeding_noise.wav");
		//URL path = new URL("file:///home/nemo/Documents/Studium/Master/study/code/VQVAD/trainset/sp12_train_sn10_preceeding_noise_sp12_train_sn10_again.wav");

		VQVoiceActivityDetector vac = new VQVoiceActivityDetector(path, 8000, "foo");
		//DenoiseTestVQVoiceActivityDetector vac = new DenoiseTestVQVoiceActivityDetector(path, 8000, "foo");
		//VoiceActivityDetector vac = new VoiceActivityDetector(path, 8000, "foo");

		AudioInputStream debugAis = copyAis(vac, vac.getFormat());

		WaveFileWriter w = new WaveFileWriter();
		FileOutputStream of = new FileOutputStream("/home/nemo/Documents/Studium/Master/study/test_out.wav");

		w.write(debugAis, AudioFileFormat.Type.WAVE, of);
		of.close();
		debugAis.reset();

		Player.playStream(debugAis);
	}

}
