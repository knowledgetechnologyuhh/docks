package info.knowledgeTechnology.docks.Utils;

import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;

public class Player {
	public static void playStream(AudioInputStream inputStream) {
		AudioFormat format = inputStream.getFormat();
		DataLine.Info info = new DataLine.Info(Clip.class, format);
		Clip clip;

		try {
			clip = (Clip) AudioSystem.getLine(info);
			clip.open(inputStream);
			clip.start();
		} catch (LineUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
