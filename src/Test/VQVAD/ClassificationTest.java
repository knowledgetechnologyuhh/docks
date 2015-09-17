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

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import VQVAD.ClassificationResultDumper;
import VQVAD.VQVADPipeline;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.util.AudioFileDataSource;

/**
 * Just do classification using VQVAD and use the {@link ClassificationResultDumper} to
 * write the result to a file that can be analyzed later on.
 *
 */
public class ClassificationTest {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {

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
		pipeline.add(new VQVADPipeline(audioDataSource));
		pipeline.add(new ClassificationResultDumper(path.getFile(), 30, 10, "/tmp/vqvad_classification_result"));

		FrontEnd frontend = new FrontEnd(pipeline);
		Data d;

		do {
			d = frontend.getData();
		} while(d != null && !(d instanceof DataEndSignal));
	}

}
