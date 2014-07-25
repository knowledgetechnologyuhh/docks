/**
 * DOCKS is a framework for post-processing results of Cloud-based speech 
 * recognition systems.
 * Copyright (C) 2014 Johannes Twiefel
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
 * 7twiefel@informatik.uni-hamburg.de
 */
package Frontend;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;



import Utils.Printer;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataBlocker;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.endpoint.NonSpeechDataFilter;
import edu.cmu.sphinx.frontend.endpoint.SpeechClassifier;
import edu.cmu.sphinx.frontend.endpoint.SpeechEndSignal;
import edu.cmu.sphinx.frontend.endpoint.SpeechMarker;
import edu.cmu.sphinx.frontend.util.AudioFileDataSource;;

/**
 * Can be used in live mode to remove non speech.
 * @author 7twiefel
 *
 */
public class VoiceActivityDetector extends AudioInputStream {
	private String TAG="VoiceActivityDetector";
	private FrontEnd frontend;
	
	@Override
	public AudioFormat getFormat() {
		float sampleRate = 16000;
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
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public VoiceActivityDetector(AudioInputStream ais, String AisName) {
		super(null,new AudioFormat(16000, 16,	1, true, false), 0);


		//audio source with 3200 byte read per read
		AudioFileDataSource audioDataSource = new AudioFileDataSource(3200,
				null);
		audioDataSource.setInputStream(ais, AisName);
		
		ArrayList pipeline = new ArrayList();
		pipeline.add(audioDataSource);
		//blocks data into frames
		pipeline.add(new DataBlocker(10));
		//classifies speech frames
		pipeline.add(new SpeechClassifier(10, 0.015, 10, 0));
		//marks as speech
		pipeline.add(new SpeechMarker(200, 300, 100, 30, 100, 15.0));
		//removes non speech
		pipeline.add(new NonSpeechDataFilter());

		frontend = new FrontEnd(pipeline);

	}


	
	@Override
	public int read(byte[] buf, int off, int len) {
		Printer.printWithTimeF(TAG, "reading");
		Data d=null; 
		
		//if still in speech get data from frontend
		if(!speechEnd)
		{
			d= frontend.getData();
		} else speechEnd=false;
		
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		int framesize = -1;
		
		//do this while data from frontend is not null
		while (d != null) {
			Printer.printWithTimeF(TAG, d.getClass().getName()+" "+i);
			i++;
			
			//check if data is DoubleData which means audio data
			if (d instanceof DoubleData) {
				
				//convert frame data back to raw data
				DoubleData dd = (DoubleData) d;
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
				} else
					d = null;
			} else if(d instanceof SpeechEndSignal)
			{
				//stopp pulling if end of speech is reached
				speechEnd = true;
				break;
			}
			else	//get data from frontend if data is not yet containing audio data or an end signal
				d = frontend.getData();

		}
		//write the converted data to the output buffer
		System.arraycopy(baos.toByteArray(), 0, buf, 0, baos.size());

		// TODO Auto-generated method stub
		return baos.size();
	}



}
