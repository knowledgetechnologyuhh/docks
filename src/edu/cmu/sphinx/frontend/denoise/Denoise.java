/*
 * Copyright 2015 Marian Tietz
 * Copyright 2013 Carnegie Mellon University.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 */
package edu.cmu.sphinx.frontend.denoise;

import java.util.Arrays;

import Utils.Debug;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DataStartSignal;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Double;
import edu.cmu.sphinx.util.props.S4Integer;

/**
 * The noise filter, same as implemented in sphinxbase/sphinxtrain/pocketsphinx.
 *
 * Noise removal algorithm is inspired by the following papers Computationally
 * Efficient Speech Enchancement by Spectral Minina Tracking by G. Doblinger
 *
 * Power-Normalized Cepstral Coefficients (PNCC) for Robust Speech Recognition
 * by C. Kim.
 *
 * For the recent research and state of art see papers about IMRCA and A
 * Minimum-Mean-Square-Error Noise Reduction Algorithm On Mel-Frequency Cepstra
 * For Robust Speech Recognition by Dong Yu and others
 *
 */
public class Denoise extends BaseDataProcessor {

    double[] power;
    double[] noise;
    double[] floor;
    double[] peak;

    @S4Double(defaultValue = 0.7)
    public final static String LAMBDA_POWER = "lambdaPower";
    double lambdaPower;

    @S4Double(defaultValue = 0.999)
    public final static String LAMBDA_A = "lambdaA";
    double lambdaA;

    @S4Double(defaultValue = 0.5)
    public final static String LAMBDA_B = "lambdaB";
    double lambdaB;

    @S4Double(defaultValue = 0.85)
    public final static String LAMBDA_T = "lambdaT";
    double lambdaT;

    @S4Double(defaultValue = 0.2)
    public final static String MU_T = "muT";
    double muT;

    @S4Double(defaultValue = 2.0)
    public final static String EXCITATION_THRESHOLD = "excitationThreshold";
    double excitationThreshold;

    @S4Double(defaultValue = 20.0)
    public final static String MAX_GAIN = "maxGain";
    double maxGain;

    @S4Integer(defaultValue = 4)
    public final static String SMOOTH_WINDOW = "smoothWindow";
    int smoothWindow;

    final static double EPS = 1e-10;

    public Denoise(double lambdaPower, double lambdaA, double lambdaB,
            double lambdaT, double muT, double excitationThreshold,
            double maxGain, int smoothWindow) {
        this.lambdaPower = lambdaPower;
        this.lambdaA = lambdaA;
        this.lambdaB = lambdaB;
    }

    public Denoise() {
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util
     * .props.PropertySheet)
     */
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
        floor = new double[length];
        peak = new double[length];
        for (int i = 0; i < length; i++) {
            floor[i] = input[i] / maxGain;
        }
    }
}
