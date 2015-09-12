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
package Test.VQVAD;

import java.net.MalformedURLException;
import java.net.URL;

import Data.Result;
import Frontend.VQVoiceActivityDetector;
import Recognizer.RawGoogleRecognizer;

public class RecognitionTest {

	/**
	 * @param args
	 * @throws MalformedURLException
	 */
	public static void main(String[] args) throws MalformedURLException {

		//URL path = new URL("file:///home/nemo/Documents/Studium/Master/study/code/VQVAD/foo.wav");
		URL path = new URL("file:///home/nemo/Documents/Studium/Master/study/code/VQVAD/trainset/noizeus_train/train_10dB/sp12_train_sn10.wav");
		//URL path = new URL("file:///home/nemo/Documents/Studium/Master/study/code/VQVAD/trainset/noizeus_train/car_10dB/sp12_car_sn10.wav");
		//URL path = new URL("file:///home/nemo/Documents/Studium/Master/study/code/VQVAD/trainset/noise_only.wav");



		VQVoiceActivityDetector vac = new VQVoiceActivityDetector(path, 8000, "foo");

		String key = "AIzaSyBOti4mM-6x9WDnZIjIeyEU21OpBXqWBgw";
		RawGoogleRecognizer rawGoogle = new RawGoogleRecognizer(key);

		Result r = rawGoogle.recognize(vac);

		if (r != null) r.print();

	}

}
