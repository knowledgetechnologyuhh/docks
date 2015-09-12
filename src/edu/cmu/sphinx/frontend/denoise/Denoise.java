/*
 * Copyright 2015 Marian Tietz
 * Portions Copyright 2013 Carnegie Mellon University.
 * All Rights Reserved.  Use is subject to license terms.
 */
package edu.cmu.sphinx.frontend.denoise;

import java.util.Arrays;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DataStartSignal;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Double;

/**
 *
 * Noise tracking is, as Sphinx' original implementation, inspired by [2].
 * Usage and implementation of spectral subtraction and the parametrization
 * was taken from [1].
 *
 * [1]: 'A Practical, Self-Adaptive Voice Activity Detector for Speaker Verification
 * 		 with Noisy Telephone and Microphone Data' by Tomi Kinnunen and Padmanabhan Rajan
 *
 * [2]: 'Computationally Efficient Speech Enchancement by Spectral Minina Tracking'
 * 		 by G. Doblinger
 */
public class Denoise extends BaseDataProcessor {

    protected double[] power;
    protected double[] noise;

    @S4Double(defaultValue = 0.7)
    public final static String LAMBDA_POWER = "lambdaPower";
    double lambdaPower;

    @S4Double(defaultValue = 0.999)
    public final static String LAMBDA_A = "lambdaA";
    double lambdaA;

    @S4Double(defaultValue = 0.5)
    public final static String LAMBDA_B = "lambdaB";
    double lambdaB;

    public Denoise(double lambdaPower, double lambdaA, double lambdaB,
            double lambdaT, double muT, double excitationThreshold,
            double maxGain, int smoothWindow) {
        this.lambdaPower = lambdaPower;
        this.lambdaA = lambdaA;
        this.lambdaB = lambdaB;
    }

    public Denoise() {
    }

    @Override
    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);
        lambdaPower = ps.getDouble(LAMBDA_POWER);
        lambdaA = ps.getDouble(LAMBDA_A);
        lambdaB = ps.getDouble(LAMBDA_B);
    }

    @Override
    public Data getData() throws DataProcessingException {
        Data inputData = getPredecessor().getData();
        int i;

        if (inputData instanceof DataStartSignal) {
            power = null;
            noise = null;
            return inputData;
        }
        if (!(inputData instanceof DoubleData)) {
            return inputData;
        }

        DoubleData inputDoubleData = (DoubleData) inputData;
        double[] input = inputDoubleData.getValues();
        int length = input.length;

        if (power == null)
            initStatistics(input, length);

        // input is the power spectrum of the frame computed by the DiscreteFourierTransform component.
        // power is the short time power spectrum.
        // noise is the short time noise power spectrum estimate.

        updatePower(input);

        estimateEnvelope(power, noise);

        // Parameters taken from [1].
        double g_h = 1.0;
        double a = 10;
        double b = 0.01;

        for (i = 0; i < length; i++) {
	        double bf = Math.min(g_h, b * noise[i] / power[i]); // noise floor gain
	        double af = 1 - a * (noise[i] / power[i]); // subtraction gain
	        input[i] *= Math.max(af, bf);
        }

        return inputData;
    }

    private void updatePower(double[] input) {
        for (int i = 0; i < input.length; i++) {
            power[i] = lambdaPower * power[i] + (1 - lambdaPower) * input[i];
        }
    }

    private void estimateEnvelope(double[] signal, double[] envelope) {
        for (int i = 0; i < signal.length; i++) {
            if (signal[i] > envelope[i])
                envelope[i] = lambdaA * envelope[i] + (1 - lambdaA) * signal[i];
            else
                envelope[i] = lambdaB * envelope[i] + (1 - lambdaB) * signal[i];
        }
    }

    private void initStatistics(double[] input, int length) {
        /* no previous data, initialize the statistics */
        power = Arrays.copyOf(input, length);
        noise = Arrays.copyOf(input, length);
    }
}
