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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import Frontend.LocalMicrophone;
import Frontend.VQVoiceActivityDetector;
import Utils.Player;

public class MicrophoneTest {

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
	 * @throws MalformedURLException
	 */
	public static void main(String[] args) throws MalformedURLException {
		VQVoiceActivityDetector vac = new VQVoiceActivityDetector(new LocalMicrophone(), "foo");
		//VoiceActivityDetector vac = new VoiceActivityDetector(new LocalMicrophone(), "foo");

		AudioInputStream debugAis = copyAis(vac, vac.getFormat());

		Player.playStream(debugAis);
	}

}
