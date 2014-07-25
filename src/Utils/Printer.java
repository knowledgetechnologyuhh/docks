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
/**
 * utility class
 * @author 7twiefel
 *
 */
public class Printer {
	static long startTime;
	public static boolean verbose = true;
	public static int logLevel = 0;
	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_BLACK = "\u001B[30m";
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_YELLOW = "\u001B[33m";
	public static final String ANSI_BLUE = "\u001B[34m";
	public static final String ANSI_PURPLE = "\u001B[35m";
	public static final String ANSI_CYAN = "\u001B[36m";
	public static final String ANSI_WHITE = "\u001B[37m";
	
	public static final int NONE = 0;
	public static final int INFO = 1;
	public static final int FINE = 2;
	
	static void printGreen(String s)
	{
		System.out.print(ANSI_GREEN);
		System.out.print(s);
		System.out.println(ANSI_RESET);
	}
	public static void printColor(String color,String text)
	{
		System.out.print(color);
		System.out.print(text);
		System.out.println(ANSI_RESET);
		
	}
	public static void printRed(String s)
	{
		System.out.print(ANSI_RED);
		System.out.print(s);
		System.out.println(ANSI_RESET);
	}
	public static synchronized void printWithTime(String TAG, String s) {
		if (verbose) {
			System.out.print("[" + (System.currentTimeMillis() - startTime)
					+ " MS]");
			System.out.println(TAG + ": " + s);
		}
	}
	
	public static synchronized void printWithTimeI(String TAG, String s) {
		if (logLevel>=INFO) {
			System.out.print("[" + (System.currentTimeMillis() - startTime)
					+ " MS]");
			System.out.println(TAG + ": " + s);
		}
	}
	
	public static synchronized void printWithTimeF(String TAG, String s) {
		if (logLevel>=FINE) {

			System.out.print("[" + (System.currentTimeMillis() - startTime)
					+ " MS]");
			System.out.println(TAG + ": " + s);
		}
	}
	public static synchronized void printfWithTimeF(String TAG, String s) {
		if (logLevel>=FINE) {
			System.out.print("[" + (System.currentTimeMillis() - startTime)
					+ " MS]");
			System.out.print(TAG + ": " + s);
		}
	}
	public static synchronized void printfWithTime(String TAG, String s) {
		if (verbose) {
			System.out.print("[" + (System.currentTimeMillis() - startTime)
					+ " MS]");
			System.out.print(TAG + ": " + s);
		}
	}

	public static synchronized void printRedWithTime(String TAG, String s) {
		if (verbose) {

			System.out.print("[" + (System.currentTimeMillis() - startTime)
					+ " MS]");
			System.out.print(TAG + ": ");
			System.err.println(s);
		}
	}
	static synchronized void printWithTimeImportant(String TAG, String s) {
		System.out.print("[" + (System.currentTimeMillis() - startTime)
					+ " MS]");
			System.out.println(TAG + ": " + s);

	}
	public static long reset()
	{
		long result = System.currentTimeMillis() - startTime;
		startTime=System.currentTimeMillis();
		return result;
	}
	static synchronized void printTime() {
		System.out.print("[" + (System.currentTimeMillis() - startTime)
					+ " MS]");
	}

}
