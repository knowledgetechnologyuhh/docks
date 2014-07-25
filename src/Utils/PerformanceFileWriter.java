/**
 * DOCKS is a framework for post-processing results of Cloud-based speech 
 * recognition systems.
 * Copyright (C) 2014 Johannes Twiefel
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Contact:
 * 7twiefel@informatik.uni-hamburg.de
 */
package Utils;

import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import edu.cmu.sphinx.util.NISTAlign;
/**
 * utility class
 * @author 7twiefel
 *
 */
public class PerformanceFileWriter {

	class Entry {
		long avgTime;

		public Entry(String method, double languageWeight,
				double wordInsertionProbability, NISTAlign aligner,
				int emptyResults, long avgTime) {
			super();
			this.method = method;
			this.languageWeight = languageWeight;
			this.wordInsertionProbability = wordInsertionProbability;
			this.aligner = aligner;
			this.emptyResults = emptyResults;
			this.avgTime = avgTime;
		}

		public String getMethod() {
			return method;
		}

		public double getLanguageWeight() {
			return languageWeight;
		}

		public double getWordInsertionProbability() {
			return wordInsertionProbability;
		}

		public NISTAlign getAligner() {
			return aligner;
		}

		public long getAvgTime() {
			return avgTime;
		}

		public int getEmptyResults() {
			return emptyResults;
		}

		String method;
		double languageWeight;
		double wordInsertionProbability;
		NISTAlign aligner;
		int emptyResults;
	}

	ArrayList<Entry> rows;

	public PerformanceFileWriter() {
		rows = new ArrayList<Entry>();

	}

	public boolean add(String method, double languageWeight,
			double wordInsertionProbability, NISTAlign aligner) {
		return add(method, languageWeight, wordInsertionProbability, aligner,
				0, 0);
	}

	public boolean add(String method, double languageWeight,
			double wordInsertionProbability, NISTAlign aligner, int emptyResults) {
		return rows.add(new Entry(method, languageWeight,
				wordInsertionProbability, aligner, emptyResults, 0));
	}

	public boolean add(String method, double languageWeight,
			double wordInsertionProbability, NISTAlign aligner,
			int emptyResults, long avgTime) {
		return rows.add(new Entry(method, languageWeight,
				wordInsertionProbability, aligner, emptyResults, avgTime));
	}

	static double roundPercent(double wert, int stellen) {
        return  Math.round(100 * wert * Math.pow(10, stellen)) / Math.pow(10, stellen);
    }
	
	static double round(double wert, int stellen) {
        return  Math.round(wert * Math.pow(10, stellen)) / Math.pow(10, stellen);
    }
	
	public void createPerformanceTable(String title, boolean inPercent,
			boolean rounded, boolean withDate) {
		String filename = title + ".csv";
		if (withDate) {
			DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd_HH:mm:ss");
			Date date = new Date();
			String dateAndTime = dateFormat.format(date);
			filename = dateAndTime + "_" + title + ".csv";
		}
		writeToPerformanceFileHeader(title, filename);
		for (Entry e : rows) {
			if (inPercent && rounded)
				writeToPerformanceFileRoundedInPercent(e.getMethod(),
						e.getLanguageWeight(), e.getWordInsertionProbability(),
						e.getAligner(), filename, e.getEmptyResults(),
						e.getAvgTime());
			else if (!inPercent && rounded)
				writeToPerformanceFileRounded(e.getMethod(),
						e.getLanguageWeight(), e.getWordInsertionProbability(),
						e.getAligner(), filename, e.getEmptyResults(),
						e.getAvgTime());
			else if (inPercent && !rounded)
				writeToPerformanceFileInPercent(e.getMethod(),
						e.getLanguageWeight(), e.getWordInsertionProbability(),
						e.getAligner(), filename, e.getEmptyResults(),
						e.getAvgTime());
			else if (!inPercent && !rounded)
				writeToPerformanceFile(e.getMethod(), e.getLanguageWeight(),
						e.getWordInsertionProbability(), e.getAligner(),
						filename, e.getEmptyResults(), e.getAvgTime());
		}

	}

	private void writeToPerformanceFileHeader(String title, String filename) {
		FileWriter out;
		try {
			out = new FileWriter("results/" + filename, true);
			out.write(";;;;;;;;;;\n");
			out.write(title + ";;;;;;;;;;\n");
			out.write("Approach;Language Weight;Word Insertion Probability;Ratio;Accuracy;WER;Sentence Accuracy;Substitutions;Insertions;Deletions;Empty Results;SER;Average Time\n");
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void writeToPerformanceFileRounded(String method,
			double languageWeight, double wordInsertionProbability,
			NISTAlign aligner, String filename, int emptyResults, long avgTime) {
		FileWriter out;
		try {
			out = new FileWriter("results/" + filename, true);
			out.write(method + ";");
			if (languageWeight != 0)
				out.write(Math.round(languageWeight * 100)
						/ 100.0
						+ ";"
						+ Math.round(wordInsertionProbability * 100)
						/ 100.0
						+ ";"
						+ Math.round((languageWeight / wordInsertionProbability) * 100)
						/ 100.0 + ";");
			else
				out.write(";;;");
			out.write(Math.round(aligner.getTotalWordAccuracy() * 100)
					/ 100.0
					+ ";"
					+ Math.round(aligner.getTotalWordErrorRate() * 100)
					/ 100.0
					+ ";"
					+ Math.round(aligner.getTotalSentenceAccuracy() * 100)
					/ 100.0
					+ ";"
					+ aligner.getTotalSubstitutions()
					+ ";"
					+ aligner.getTotalInsertions()
					+ ";"
					+ aligner.getTotalDeletions()
					+ ";"
					+ emptyResults
					+ ";"
					+ Math.round((1 - aligner.getTotalSentenceAccuracy()) * 100)
					/ 100.0 + ";" + avgTime + "\n");
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		}
	}

	private void writeToPerformanceFileRoundedInPercent(String method,
			double languageWeight, double wordInsertionProbability,
			NISTAlign aligner, String filename, int emptyResults, long avgTime) {
		FileWriter out;
		try {
			out = new FileWriter("results/" + filename, true);
			out.write(method + ";");
			if (languageWeight != 0)
				out.write(round(languageWeight,3)+ ";"
						+ round(wordInsertionProbability,3)+ ";"
						+ round((languageWeight / wordInsertionProbability) ,3) + ";");
			else
				out.write(";;;");
			out.write(roundPercent(aligner.getTotalWordAccuracy() ,3)
					+ ";"
					+ roundPercent(aligner.getTotalWordErrorRate() ,3)
					+ ";"
					+ roundPercent(aligner.getTotalSentenceAccuracy(), 3)
					+ ";"
					+ aligner.getTotalSubstitutions()
					+ ";"
					+ aligner.getTotalInsertions()
					+ ";"
					+ aligner.getTotalDeletions()
					+ ";"
					+ emptyResults
					+ ";"
					+ roundPercent((1 - aligner.getTotalSentenceAccuracy()) ,3)
					+ ";" + avgTime + "\n");
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void writeToPerformanceFileInPercent(String method,
			double languageWeight, double wordInsertionProbability,
			NISTAlign aligner, String filename, int emptyResults, long avgTime) {
		FileWriter out;
		try {
			out = new FileWriter("results/" + filename, true);
			out.write(method + ";");
			if (languageWeight != 0)
				out.write(languageWeight + ";" + wordInsertionProbability + ";"
						+ languageWeight / wordInsertionProbability + ";");
			else
				out.write(";;;");
			out.write(aligner.getTotalWordAccuracy() * 100 + ";"
					+ aligner.getTotalWordErrorRate() * 100 + ";"
					+ aligner.getTotalSentenceAccuracy() * 100 + ";"
					+ aligner.getTotalSubstitutions() + ";"
					+ aligner.getTotalInsertions() + ";"
					+ aligner.getTotalDeletions() + ";" + emptyResults + ";"
					+ (1 - aligner.getTotalSentenceAccuracy()) * 100 + ";"
					+ avgTime + "\n");
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void writeToPerformanceFile(String method, double languageWeight,
			double wordInsertionProbability, NISTAlign aligner,
			String filename, int emptyResults, long avgTime) {
		FileWriter out;
		try {
			out = new FileWriter("results/" + filename, true);
			out.write(method + ";");
			if (languageWeight != 0)
				out.write(languageWeight + ";" + wordInsertionProbability + ";"
						+ languageWeight / wordInsertionProbability + ";");
			else
				out.write(";;;");
			out.write(aligner.getTotalWordAccuracy() + ";"
					+ aligner.getTotalWordErrorRate() + ";"
					+ aligner.getTotalSentenceAccuracy() + ";"
					+ aligner.getTotalSubstitutions() + ";"
					+ aligner.getTotalInsertions() + ";"
					+ aligner.getTotalDeletions() + ";" + emptyResults + ";"
					+ (1 - aligner.getTotalSentenceAccuracy()) + ";" + avgTime
					+ "\n");
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
