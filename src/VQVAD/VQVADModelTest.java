package VQVAD;

import static org.junit.Assert.*;

import org.apache.commons.math3.ml.clustering.DoublePoint;
import org.junit.Test;

public class VQVADModelTest {

	@Test
	public void testMinDistance() {

		VQVADModel m = new VQVADModel(null, null, 0);

		DoublePoint[] centers = new DoublePoint[]{
			new DoublePoint(new double[] {8,1,6}),
			new DoublePoint(new double[] {3,5,7}),
			new DoublePoint(new double[] {4,9,2}),
		};

		double[] input = new double[] {1,2,3};

		double minDistance = m.minDistance(centers, input);

		assertEquals(29, minDistance, 0.001);
	}

}
