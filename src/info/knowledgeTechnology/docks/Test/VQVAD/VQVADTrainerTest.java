package info.knowledgeTechnology.docks.Test.VQVAD;

import static org.junit.Assert.assertArrayEquals;
import info.knowledgeTechnology.docks.VQVAD.VQVADTrainer;

import org.junit.Test;

public class VQVADTrainerTest {

	@Test
	public void testSortedEnergyIndices() {
		VQVADTrainer t = new VQVADTrainer();

		{
			double[] energies = new double[]{
					9,8,7,6,5,4,3,2,1,0
			};

			Integer[] result = t.sortedEnergyIndices(energies);

			int[] resultUnboxed = new int[result.length];
			for (int i=0; i < result.length; i++) resultUnboxed[i] = result[i].intValue();

			int[] expecteds = new int[]{
					9,8,7,6,5,4,3,2,1,0
			};

			assertArrayEquals(expecteds, resultUnboxed);
		}

		{
			double[] energies = new double[]{
					0,1,2,3,4,5,6,7,8,9
			};

			Integer[] result = t.sortedEnergyIndices(energies);

			int[] resultUnboxed = new int[result.length];
			for (int i=0; i < result.length; i++) resultUnboxed[i] = result[i].intValue();

			int[] expecteds = new int[]{
					0,1,2,3,4,5,6,7,8,9
			};

			assertArrayEquals(expecteds, resultUnboxed);
		}
	}

}
