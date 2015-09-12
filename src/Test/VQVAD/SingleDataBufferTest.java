package Test.VQVAD;

import static org.junit.Assert.*;

import org.junit.Test;

import VQVAD.SingleDataBuffer;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DoubleData;

public class SingleDataBufferTest {

	@Test
	public void testGetData() {
		SingleDataBuffer s = new SingleDataBuffer();

		Data d1 = new DoubleData(new double[]{1}, 1, 1);
		Data d2 = new DoubleData(new double[]{2}, 1, 2);
		Data d3 = new DoubleData(new double[]{3}, 1, 3);

		final Data[] datas = new Data[]{d1, d2, d3};

		s.setPredecessor(new BaseDataProcessor() {

			int i = 0;

			@Override
			public Data getData() throws DataProcessingException {
				if (i < datas.length) {
					Data d = datas[i];
					i++;
					return d;
				}
				return new DataEndSignal(42);
			}
		});

		assertTrue(d1.equals(d1));
		assertTrue(!d1.equals(d2));

		assertEquals(d1, s.getBufferedData());
		assertEquals(d1, s.getData());
		assertEquals(d2, s.getBufferedData());
		assertEquals(d2, s.getBufferedData()); // should not modify
		assertEquals(d2, s.getBufferedData());
		assertEquals(d2, s.getData());
		assertEquals(d3, s.getBufferedData());
		assertEquals(d3, s.getData());
	}

}
