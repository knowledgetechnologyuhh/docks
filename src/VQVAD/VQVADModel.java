package VQVAD;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dk.ange.octave.OctaveEngine;
import dk.ange.octave.OctaveEngineFactory;
import dk.ange.octave.type.Octave;
import dk.ange.octave.type.OctaveDouble;
import dk.ange.octave.type.OctaveObject;
import dk.ange.octave.type.OctaveString;
import edu.cmu.sphinx.frontend.DoubleData;

public class VQVADModel {

	protected boolean isTrained = false;

	protected OctaveEngine modelEngine;


	public VQVADModel() {
		this(false, null);
	}

	protected VQVADModel(boolean isTrained, OctaveEngine modelEngine) {
		this.isTrained = isTrained;
		this.modelEngine = modelEngine;
	}


	public static VQVADModel train(DoubleData[] frames) {
		OctaveEngine modelEngine = new OctaveEngineFactory().getScriptEngine();

		double sampleRate = frames[0].getSampleRate();
		int frameLength = frames[0].dimension();
		double secsPerFrame = frameLength / sampleRate;

		System.out.println("train(" + frames.length + ")");

		double[] unboxedSignal = concatenateAndUnbox(frames);

		OctaveDouble s = new OctaveDouble(unboxedSignal, unboxedSignal.length, 1);
		OctaveDouble fs = Octave.scalar(sampleRate);

		modelEngine.put("s", s);
		modelEngine.put("fs", fs);

		modelEngine.eval("cd /home/nemo/Documents/Studium/Master/study/code/VQVAD");
		modelEngine.eval("[~, ~, params] = VQVAD;");
		modelEngine.eval("params.frame_len = " + (secsPerFrame) + ";");
		modelEngine.eval("params.frame_shift = " + (secsPerFrame) + ";");
		modelEngine.eval("[speech_model, nonspeech_model, params] = vqvad_train(s, fs, params);");

		return new VQVADModel(true, modelEngine);
	}

	public int getNecessaryClassificationSampleCount() {
		return 80;
	}

	public boolean isTrained() {
		return isTrained;
	}

	public boolean classify(DoubleData[] frames) {
		if (!isTrained()) {
			return false;
		}

		double[] unboxedSignal = concatenateAndUnbox(frames);

		OctaveDouble signal = new OctaveDouble(unboxedSignal, unboxedSignal.length, 1);
		modelEngine.put("signal",  signal);
		modelEngine.eval("[~, LLR] = vqvad_classify(signal, fs, speech_model, nonspeech_model, params);");
		OctaveDouble llr = modelEngine.get(OctaveDouble.class, "LLR");

		if (llr.size(1) > 0 && llr.get(1) >= 0)
			return true;
		return false;
	}

	protected static double[] concatenateAndUnbox(DoubleData[] frames) {
		List<Double> samples = new ArrayList<Double>();

		for (DoubleData frame : frames) {
			Double[] boxedValues = new Double[frame.dimension()];
			double[] values = frame.getValues();

			for (int i=0; i < values.length; i++) {
				boxedValues[i] = Double.valueOf(values[i]);
			}

			Collections.addAll(samples, boxedValues);
		}

		Double[] boxedSignal = samples.toArray(new Double[]{});
		double[] unboxedSignal = new double[samples.size()];

		for (int i=0; i < samples.size(); i++) {
			unboxedSignal[i] = boxedSignal[i].doubleValue();
		}

		return unboxedSignal;
	}

	public static void main(String[] args) {
		System.out.println("LOL");
//		OctaveEngine octave = new OctaveEngineFactory().getScriptEngine();
//		octave.eval("cd ..");
//		octave.eval("pwd");


		DoubleData[] frames = new DoubleData[3];
		frames[0] = new DoubleData(new double[]{1,2,3,4,5,6,1,2,3,4,5,6,1,2,3,4,5,6,1,2,3,4,5,6}, 8000, 0);
		frames[1] = new DoubleData(new double[]{-1,-2,-3,-4,-5,-6,-1,-2,-3,-4,-5,-6,-1,-2,-3,-4,-5,-6,-1,-2,-3,-4,-5,-6}, 8000, 6);
		frames[2] = new DoubleData(new double[]{6,5,4,3,2,1,6,5,4,3,2,1,6,5,4,3,2,1,6,5,4,3,2,1}, 8000, 12);

		train(frames);
	}

}
