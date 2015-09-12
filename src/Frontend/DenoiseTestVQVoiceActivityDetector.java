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
package Frontend;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import Utils.Debug;
import Utils.Printer;
import VQVAD.Denoise;
import VQVAD.FrameOverlapFilter;
import VQVAD.MFCCPacket;
import VQVAD.MFCCPipeline;
import VQVAD.VQVADPipeline;
import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataBlocker;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.endpoint.NonSpeechDataFilter;
import edu.cmu.sphinx.frontend.endpoint.SpeechClassifiedData;
import edu.cmu.sphinx.frontend.endpoint.SpeechEndSignal;
import edu.cmu.sphinx.frontend.endpoint.SpeechMarker;
import edu.cmu.sphinx.frontend.filter.Dither;
import edu.cmu.sphinx.frontend.transform.DiscreteFourierTransform;
import edu.cmu.sphinx.frontend.util.AudioFileDataSource;
import edu.cmu.sphinx.frontend.window.RaisedCosineWindower;
import edu.cmu.sphinx.util.props.S4Double;

public class DenoiseTestVQVoiceActivityDetector extends AudioInputStream {
	private final String TAG="VQVoiceActivityDetector";
	private final FrontEnd frontend;

	protected float sampleRate = 8000;

	@Override
	public AudioFormat getFormat() {
		float sampleRate = this.sampleRate;
		int sampleSizeInBits = 16;
		int channels = 1;
		boolean signed = true;
		boolean bigEndian = false;
		AudioFormat format = new AudioFormat(sampleRate, sampleSizeInBits,
				channels, signed, bigEndian);
		return format;
	}

	private int i = 0;
	private boolean speechEnd = false;

	/**
	 * Creates a new voice activity detector
	 * @param ais input stream like LocalMicrophone or SocketMicrophone
	 * @param AisName name of the microphone LocalMicrophone or SocketMicrophone
	 * @throws MalformedURLException
	 */
	public DenoiseTestVQVoiceActivityDetector(URL path, float sampleRate, String AisName) throws MalformedURLException {
		super(null,new AudioFormat(sampleRate, 16,	1, true, false), 0);

		this.sampleRate = sampleRate;

		//audio source with 3200 byte read per read
		AudioFileDataSource audioDataSource = new AudioFileDataSource(3200, null);
		audioDataSource.setAudioFile(path, AisName);

		frontend = setupFrontend(audioDataSource);
	}

	public DenoiseTestVQVoiceActivityDetector(AudioInputStream ais, String AisName) {
		super(null,new AudioFormat(ais.getFormat().getSampleRate(), 16,	1, true, false), 0);

		this.sampleRate = ais.getFormat().getSampleRate();

		//audio source with 3200 byte read per read
		AudioFileDataSource audioDataSource = new AudioFileDataSource(3200, null);
		audioDataSource.setInputStream(ais, AisName);

		frontend = setupFrontend(audioDataSource);
	}

	public DenoiseTestVQVoiceActivityDetector(AudioFileDataSource audioDataSource, String AisName) {
		super(null,new AudioFormat(audioDataSource.getSampleRate(), 16,	1, true, false), 0);

		frontend = setupFrontend(audioDataSource);
	}

	protected FrontEnd setupFrontend(AudioFileDataSource audioDataSource) {
		ArrayList<DataProcessor> pipeline = new ArrayList<DataProcessor>();

		float frame_length_ms = 30;
		float frame_shift_ms = 10;
		double lower_freq = 0;
		double learning_rate = 0.995;

		pipeline.add(audioDataSource);
		//pipeline.add(new DataBlocker(10));
		pipeline.add(new RaisedCosineWindower(0, 30, 10));
		pipeline.add(new DiscreteFourierTransform(-1, false));
		pipeline.add(new Denoise(0.7, 0.995, 0.5));
		pipeline.add(new BaseDataProcessor() {
			@Override
			public Data getData() throws DataProcessingException {
				Data d = getPredecessor().getData();
				if (d != null && d instanceof DoubleData) {
					DoubleData dd = (DoubleData) d;
					double[] values = dd.getValues();
					for (int i=0; i < dd.dimension(); i++) {
						values[i] = Math.sqrt(values[i]);
					}
				}
				return d;
			}
		});
		pipeline.add(new DiscreteFourierTransform(-1, true));
		pipeline.add(new BaseDataProcessor() {
			@Override
			public Data getData() throws DataProcessingException {
				Data d = getPredecessor().getData();
				if (d != null && d instanceof DoubleData) {
					DoubleData dd = (DoubleData) d;
					double[] values = dd.getValues();
					for (int i=0; i < dd.dimension(); i++) {
						values[i] = Math.sqrt(values[i]);
					}
				}
				return d;
			}
		});


//		pipeline.add(new FrameOverlapFilter(30,10));

//		pipeline.add(new FrameOverlapFilter(frame_length_ms, frame_shift_ms));



//		// VQVAD pipeline
//		pipeline.add(new VQVADPipeline(audioDataSource));
//		//marks as speech
//		//pipeline.add(new SpeechMarker(100, 300, 100));
//		pipeline.add(new SpeechMarker(100, 300, 100, 30, 100, 15.0));
//		//removes non speech
//		pipeline.add(new NonSpeechDataFilter());

		return new FrontEnd(pipeline);
	}

	@Override
	public int read(byte[] buf, int off, int len) {
		Printer.printWithTimeF(TAG, "reading");
		Data d=null;

		//System.out.println(buf.length);
		//if (len == 0) return -1;

		//if still in speech get data from frontend
		if(!speechEnd)
		{
			d = frontend.getData();
		} else {
			speechEnd = false;
		}

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		int framesize = -1;

		//do this while data from frontend is not null
		while (d != null) {
			Printer.printWithTimeF(TAG, d.getClass().getName()+" "+i);
			i++;

			//check if data is DoubleData which means audio data
			//if (d instanceof SpeechClassifiedData) {
			if (d instanceof DoubleData) {
			//if (d instanceof MFCCPacket) {

				//convert frame data back to raw data
				//DoubleData dd = ((SpeechClassifiedData) d).getDoubleData();
				DoubleData dd = (DoubleData) d;
				//DoubleData dd = ((MFCCPacket) d).getAudioFrame();
				double[] values = dd.getValues();
				if (framesize == -1)
					framesize = values.length * 2;

				for (double value : values) {
					try {
						short be = new Short((short) value);
						dos.writeByte(be & 0xFF);
						dos.writeByte((be >> 8) & 0xFF);


					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				//read new data from frontend if frame size is not exceeded
				if (baos.size() + framesize <= len) {
					d = frontend.getData();
				} else {
					d = null;
				}
			//} else if(d instanceof SpeechEndSignal) {
			} else if(d instanceof DataEndSignal) {
				//stopp pulling if end of speech is reached
				speechEnd = true;
				break;
			} else {
				//get data from frontend if data is not yet containing audio data or an end signal
				d = frontend.getData();
			}
		}

//		System.out.println("baos length: " + baos.size());
//		System.out.println("offset: " + off + " buf length: " + len);

		// write the converted data to the output buffer
		System.arraycopy(baos.toByteArray(), 0, buf, 0, baos.size());

		// TODO Auto-generated method stub
		return baos.size();
	}
}
