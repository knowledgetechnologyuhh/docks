package info.knowledgeTechnology.docks.Frontend;

import info.knowledgeTechnology.docks.Utils.Printer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public class MicrophoneCalibrator {

	// utility to play audio
	private static void playSound(String filename) {

		File file = new File(filename);

		try {

			Clip clip = AudioSystem.getClip();
			AudioInputStream inputStream = AudioSystem
					.getAudioInputStream(file);

			AudioFormat format = inputStream.getFormat();
			DataLine.Info info = new DataLine.Info(Clip.class, format);

			clip = (Clip) AudioSystem.getLine(info);

			clip.open(inputStream);
			clip.start();

		} catch (UnsupportedAudioFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// Get a sound clip resource.
		catch (LineUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public static void calibrate(AudioInputStream ais) {
		//Printer.logLevel = Printer.FINE;
		String path = "tmp/audio_calibration/";
		String testfile = path + "test.wav";
		(new File(path)).mkdirs();

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		// Try to read numBytes bytes from the file.
		try {
			FileOutputStream out = new FileOutputStream(new File(testfile));
			byte[] audioBytes = new byte[4096];
			int numBytesRead;
			while ((numBytesRead = ais.read(audioBytes)) > 0) {
				System.out.println("read " + numBytesRead);
				// out.write(audioBytes);
				baos.write(audioBytes);
			}
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		byte[] audioData = baos.toByteArray();
		ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
		AudioFormat af = ais.getFormat();
		AudioInputStream aisOut = new AudioInputStream(bais, af,
				audioData.length / af.getFrameSize());

		File outWavFile = new File(testfile);
		try {
			AudioSystem.write(aisOut, AudioFileFormat.Type.WAVE, outWavFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("generated test audio file in:");
		System.out.println(testfile);
		System.out.println("playing audiofile, you should here your voice...");
		playSound(testfile);
	}

}
