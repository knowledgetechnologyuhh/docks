package info.knowledgeTechnology.docks.Test.VQVAD;

import static org.junit.Assert.assertEquals;
import info.knowledgeTechnology.docks.VQVAD.EnergyUtility;

import org.junit.Test;

import edu.cmu.sphinx.frontend.DoubleData;

public class EnergyUtilityTest {

	@Test
	public void testStd() {
		double res;

		// std([1,2,3,4]) = 1.2910
		res = EnergyUtility.std(new DoubleData(new double[]{1,2,3,4}));
		assertEquals(1.2910, res, 0.0001);

		// std([0,0,0,0]) = 0
		res = EnergyUtility.std(new DoubleData(new double[]{0,0,0,0}));
		assertEquals(0, res, 0.0001);
	}

	@Test
	public void testComputeEnergy() {
		double res;

		// 20 * log10(std([1,2,3,4]) + eps) = 2.2185
		res = EnergyUtility.computeEnergy(new DoubleData(new double[]{1,2,3,4}));
		assertEquals(2.2185, res, 0.0001);

		// 20 * log10(std([1,1,1,1]) + eps) = -313.07
		res = EnergyUtility.computeEnergy(new DoubleData(new double[]{1,1,1,1}));
		assertEquals(-313.07, res, 0.01);

		// 20 * log10(std([0,0,0,0]) + eps) = -313.07
		res = EnergyUtility.computeEnergy(new DoubleData(new double[]{0,0,0,0}));
		assertEquals(-313.07, res, 0.01);
	}

	@Test
	public void testComputeEnergyPerFrame() {
		DoubleData[] frames = new DoubleData[]{
			new DoubleData(new double[]{1,2,3,4}),
			new DoubleData(new double[]{1,1,1,1}),
			new DoubleData(new double[]{0,0,0,0}),
		};

		double[] energies = EnergyUtility.computeEnergyPerFrame(frames);

		// 20 * log10(std([1,2,3,4]) + eps) =    2.2185
		// 20 * log10(std([1,1,1,1]) + eps) = -313.07
		// 20 * log10(std([0,0,0,0]) + eps) = -313.07
		assertEquals(2.2185, energies[0], 0.0001);
		assertEquals(-313.07, energies[1], 0.01);
		assertEquals(-313.07, energies[2], 0.01);
	}
}
