package info.knowledgeTechnology.docks.Examples;

import info.knowledgeTechnology.docks.Frontend.LocalMicrophone;
import info.knowledgeTechnology.docks.Frontend.VQVoiceActivityDetector;
import info.knowledgeTechnology.docks.Utils.ConfigCreator;

public class NewConfigExample {
	
	public static void main(String[] args) {

		System.out.println();
		 //ConfigCreator.createConfig("elpmaxe", "./batch");
		new VQVoiceActivityDetector(new LocalMicrophone(), "LocalMicrophone");
 

	}
}
