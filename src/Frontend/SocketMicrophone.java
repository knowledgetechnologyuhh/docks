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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import Utils.Printer;


/**
 * Socket microphone class used by the voice activity detection.
 * This class receives data from a microphone stream connected by socket on port 54015
 * Data line with 16000 kHz sample size, 1 channel, signed, little endian
 * @author 7twiefel
 *
 */
public class SocketMicrophone extends AudioInputStream {
	private String TAG = "SocketMicrophone";
	private static int port = 54015;
	private static int BUFFER_SIZE = 3201;

	private boolean reading = false;
	private DatagramSocket serverSocket = null;

	/**
	 * Sets if the socket microphone is reading data from the stream
	 * @param b
	 */
	synchronized void setReading(boolean b) {
		reading = b;
		Printer.printfWithTime(TAG, "reading: " + reading);
	}

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

	
	public SocketMicrophone() {
		super(null,new AudioFormat(16000, 16,
				1, true, false), 0);
		// TODO Auto-generated method stub

		try {
			serverSocket = new DatagramSocket(port);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public int read(byte[] buf, int off, int len) {
		
		byte[] receiveData = new byte[BUFFER_SIZE];

		DatagramPacket receivePacket = new DatagramPacket(receiveData,
				receiveData.length);
		Printer.printWithTime(TAG, "recving... ");

		try {
			serverSocket.receive(receivePacket);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(receivePacket.getData()[0]==-1)
		{
			Printer.printWithTime(TAG, "received abort packet");
			return 0;
		}

		System.arraycopy(receivePacket.getData(), 1, buf, 0,
				receivePacket.getLength() - 1);

		InetAddress address = receivePacket.getAddress();
		int port = receivePacket.getPort();
		int leng = receivePacket.getLength();
		Printer.printWithTime(TAG, "received packet with length: " + leng
				+ " from " + address + " on port " + port + " packet number: "
				+ receivePacket.getData()[0]);

		return receivePacket.getLength() - 1;

	}
	
}
