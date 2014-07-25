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
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

import Utils.Printer;

/**
 * Local microphone class used by the voice activity detection.
 * Data line with 16000 kHz sample size, 1 channel, signed, little endian
 * @author 7twiefel
 *
 */
public class LocalMicrophone extends AudioInputStream{
	private static String TAG = "LocalMicrophone";

	private static TargetDataLine line; 
	
	/**
	 * used internally
	 */
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
	
	


/**
 * creates a new audio data line with 16000 kHz sample size, 1 channel, signed, little endian
 */
	public LocalMicrophone() {
		super(null,new AudioFormat(16000, 16,
				1, true, false), 0);
		AudioFormat format = getFormat();

		DataLine.Info dataLineInfo = new DataLine.Info(
				TargetDataLine.class, format);
		line = null;
		try {
			line = (TargetDataLine) AudioSystem.getLine(dataLineInfo);
		} catch (LineUnavailableException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			line.open(format);
		} catch (LineUnavailableException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		Printer.printRedWithTime(TAG,"SPEAK!");
		line.start();
		Printer.printWithTime(TAG,"dataline started");
		


	}
	

	@Override
	public void close()
	{
		Printer.printWithTime(TAG,"dataline closed");
	}
	

	@Override
	public int read(byte[] buf, int off, int len) {
		if(line.isOpen())
		{
			return line.read(buf, off, len);
		}
		return -1;
	}

}
