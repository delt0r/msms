/*
This code is licensed under the LGPL v3 or greater with the classpath exception, 
with the following additions and exceptions.  

packages cern.* have retained the original cern copyright notices.

packages at.mabs.cmdline and at.mabs.util.* 
have the option to be licensed under a BSD(simplified) or Apache 2.0 or greater  license 
in addition to LGPL. 

Note that you have permission to replace this license text to any of the permitted licenses. 

Main text for LGPL can be found here:
http://www.opensource.org/licenses/lgpl-license.php

For BSD:
http://www.opensource.org/licenses/bsd-license.php

for Apache:
http://www.opensource.org/licenses/apache2.0.php

classpath exception:
http://www.gnu.org/software/classpath/license.html
 */
package at.mabs.testing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import at.ProgressControl;
import at.mabs.segment.InfinteMutation;
import at.mabs.segment.SegmentEventRecoder;
import at.mabs.stats.StringStatsCollector;

/**
 * runs the basic tests. Thats matches ms to msms with average tree height and tree length and seg
 * sites. adding sigletons.
 * 
 * @author greg
 * 
 */
public class BasicTests {
	public static String[][] ARGS = { { "-ms", "10", "10000", "-t", "5" }, 
			{ "-ms", "10", "10000", "-t", "5", "-r", "10", "10" },
			{ "-ms", "10", "10000", "-t", "5", "-I", "2", "4", "6", "1" }, 
			{ "-ms", "10", "10000", "-t", "5", "-I", "2", "4", "6", "1" ,"-en",".5","1","1"}, 
			{ "-ms", "10", "10000", "-t", "5", "-I", "2", "4", "6", "0", "-eM", "0", "1" },
			{ "-ms", "10", "10000", "-t", "5", "-I", "3", "2", "2", "6", "2.5" },
			{ "-ms", "10", "10000", "-t", "5", "-I", "3", "2", "2", "6", "-eM", "0", "2.5" },
			{ "-ms", "10", "10000", "-t", "5", "-I", "3", "2", "2", "6", "-eM", "3", "2.5" },
			{ "-ms", "10", "10000", "-s", "1000", "-I", "2", "10", "0", "0", "-m", "2", "1", ".5" },
			{ "-ms", "10", "10000", "-s", "1000", "-I", "2", "10", "0", "0", "-m", "1", "2", ".5" }, 
			{ "-ms", "10", "10000", "-t", "5", "-G", "1" },
			{ "-ms", "10", "10000", "-t", "5","-I","1","10","0","-eg","0","1", "1" },
			{ "-ms", "10", "10000", "-t", "5","-I","1","10","0", "-eng2s", "0","1","1","1",""+Math.exp(-1) },
			{ "-ms", "10", "10000", "-t", "5", "-I", "2", "4", "6", ".5", "-n", "1", ".1" },
			{ "-ms", "10", "10000", "-t", "5", "-I", "2", "4", "6", ".5", "-en", ".5","1", ".1" },
			{ "-ms", "10", "10000", "-t", "5", "-I", "2", "4", "6", ".5", "-n", "1", ".1", "-ej", ".1", "2", "1" },
			{ "-ms", "10", "10000", "-t", "5", "-I", "2", "4", "6", ".5", "-n", "1", ".1", "-ej", ".1", "2", "1" ,"-en",".1","1",".1"},
			{ "-N", "50000", "-ms", "10", "10000", "-t", "10", "-r", "20", "1000", "-SAA", "2000", "-SaA", "1000", "-SF", "0" },
			{ "-N", "50000", "-ms", "10", "10000", "-t", "100", "-r", "20", "1000", "-SAA", "2000", "-SaA", "1000", "-SF", "0" },
			{ "-N", "50000", "-ms", "10", "10000", "-t", "100", "-r", "50", "1000", "-SAA", "2000", "-SaA", "1000", "-SF", "0" },
			{ "-N", "50000", "-ms", "10", "10000", "-t", "10", "-r", "50", "1000", "-SAA", "2000", "-SaA", "1000", "-SF", "0", "-Sp", ".4" },
			{ "-N", "1000", "-ms", "10", "10000", "-t", "100", "-SAA", "500", "-SaA", "1000", "-SI", "3", "1", "0.1" },
			{ "-N", "1000", "-ms", "10", "10000", "-t", "100", "-SAA", "500", "-SaA", "1000", "-SI", "3", "1", "0" }};
	// tree hight mean var, tree length mean var, segsites mean, var
	public static double[][] TEST_VALUES = { { 0.8956, .28374, 2.8189, 1.5282, 14.151, 52.06, 5.036, 12.2 },
			{ 0.9007037, 0.2841370, 2.8254273, 0.5749942, 14.12, 27.9834, 4.97, 8.1 },
			{ 2.270628, 1.836664, 6.857068, 9.259575, 34.323, 274.3131, 10.82, 49.5 },
			{ 2.270628, 1.836664, 6.857068, 9.259575, 34.323, 274.3131, 10.82, 49.5 },
			{ 2.270628, 1.836664, 6.857068, 9.259575, 34.323, 274.3131, 10.82, 49.5 },
			{ 3.203485, 3.810249, 9.808890, 19.577661, 49.0864, 522.3404, 16.509, 101.71 },
			{ 3.203485, 3.810249, 9.808890, 19.577661, 49.0864, 522.3404, 16.509, 101.71 },
			{ 5.502693,3.479098,17.17216,16.66938,85.8571,519.8391,15.0613,75.6107},
			{ 0.9007689, 0.295737, 2.830319, 1.575863, 1000, Double.NaN, 380.8632, 24088.03 },
			{ 2.490973, 4.438121, 6.380874, 20.28845, 1000, Double.NaN, 313.292, 33509.46 },
			{ 0.6076639, 0.06312147, 2.1224309, 0.4277378, 10.545, 21.31391, 4.297, 7.59 },
			{ 0.6076639, 0.06312147, 2.1224309, 0.4277378, 10.545, 21.31391, 4.297, 7.59 },
			{ 0.6076639, 0.06312147, 2.1224309, 0.4277378, 10.545, 21.31391, 4.297, 7.59 },
			{ 1.950568, 1.905582, 5.170878, 8.559953, 25.944, 231.7876, 5.8934, 23.155 },
			{ 2.087171,1.942508,6.234696,8.73912,31.2349,233.9977,9.4631,31.42878},
			{ 0.1795477, 0.002848286, 0.8879972, 0.023647454, 4.4535, 5.209159, 2.71, 3.187 },
			{ 0.1795477, 0.002848286, 0.8879972, 0.023647454, 4.4535, 5.209159, 2.71, 3.187 },
			{ Double.NaN, Double.NaN, Double.NaN, Double.NaN, 3.0421, 19.33672, 1.5733, 4.75442711 },
			{ Double.NaN, Double.NaN, Double.NaN, Double.NaN, 31.1223, 1737.39, 15.8794, 340.593 },
			{ Double.NaN, Double.NaN, Double.NaN, Double.NaN, 58.6456, 2505.10, 25.9075, 486.72 },
			{ Double.NaN, Double.NaN, Double.NaN, Double.NaN, 3.6803, 13.84, 1.8231, 3.787 },
			{ 3.44284093673, 0.41683554, 8.172259, 2.0652019, 817.5161, 21532.8465, 128.7607, 11097.2766 },
			{ 0.90169215, 0.2864544, 2.82504, 1.5307011, 282.4168, 15645.953, 99.0115, 2894.273 } };

	public static void main(String[] args) {
		long time=System.nanoTime();
		System.out.println("parameter\t\tms\t\tmsms");
		int totalFailCount =0;
		int currentFailCount =0;
		int total =0;
		String warnings="";
		for (int i =0; i < ARGS.length; i++) {
			String[] msmsArgs =ARGS[i];
			double[] correctResults =TEST_VALUES[i];

			AveragesVars av =new AveragesVars();
			ArrayList<StringStatsCollector> list =new ArrayList<StringStatsCollector>();

			list.add(av);

			for (int j =0; j < msmsArgs.length; j++) {
				System.out.print(msmsArgs[j] + " ");
			}
			System.out.println();

			String[] addT =new String[msmsArgs.length + args.length + 1];
			System.arraycopy(msmsArgs, 0, addT, 0, msmsArgs.length);
			System.arraycopy(args, 0, addT, msmsArgs.length, args.length);
			addT[addT.length - 1] ="-b";
			System.out.println(Arrays.toString(addT));
			ProgressControl pg =new ProgressControl() {
				private int reps;
				private AtomicInteger count =new AtomicInteger();
				private int percent;

				@Override
				public void setReps(int reps) {
					this.reps =reps;
				}

				@Override
				public void iterationComplete() {
					int count =this.count.incrementAndGet();
					if (count == 1) {
						System.out.println("|===================|===================|===================|===================|");
					}

					if ((double) count * 80 / (reps) >= percent + 1) {
						percent++;
						System.out.print("*");
					}
					if (percent >= 80) {
						System.out.println();
					}
					// System.out.println(count);
				}

				@Override
				public boolean isCanceled() {
					return false;
				}

				@Override
				public void error(Exception e) {
				}
			};
			at.MSLike.main(addT, list, System.out, pg);

			double[] msmsResults =av.getAllStats();

			System.out.println("Tree Height Mean:" + correctResults[0] + "\t" + msmsResults[0]);
			System.out.println("Tree Height Var:" + correctResults[1] + "\t" + msmsResults[1]);
			System.out.println("Tree Length Mean:" + correctResults[2] + "\t" + msmsResults[2]);
			System.out.println("Tree Length Var:" + correctResults[3] + "\t" + msmsResults[3]);
			System.out.println("SegSites Mean:" + correctResults[4] + "\t" + msmsResults[4]);
			System.out.println("SegSites Var:" + correctResults[5] + "\t" + msmsResults[5]);
			if (correctResults.length > 6) {
				System.out.println("Singletons Mean:" + correctResults[6] + "\t" + msmsResults[6]);
				System.out.println("Singletons Var:" + correctResults[7] + "\t" + msmsResults[7]);
			}
			if (checkApprox(correctResults, msmsResults, .1)) {
				System.out.println("PASS");
				currentFailCount=0;
			} else {
				currentFailCount++;
				totalFailCount++;
				System.out.println("FAIL " + currentFailCount);
				if (currentFailCount < 4) {
					i--;// try again.
				} else {
					warnings+="\n\t A test has failed 4 times! "+Arrays.toString(addT);
					currentFailCount =0;
				}

			}
			System.out.println();
			total++;
		}
		System.out.println("Failed " + totalFailCount + " out of " + total+"\n"+warnings);
		System.out.println("Test took "+(System.nanoTime()-time)*1e-9+" seconds to complete");
	}

	private static boolean checkApprox(double[] correct, double[] test, double tol) {
		for (int i =0; i < correct.length; i++) {
			if (Double.isNaN(correct[i]))
				continue;
				
			double delta =Math.abs(correct[i] - test[i]);
			if (Double.isNaN(test[i]) || delta > tol * Math.abs(correct[i])) {
				System.out.println("Falied At test " + i);
				return false;
			}
		}
		return true;
	}

	static class AveragesVars implements StringStatsCollector {
		private double segSum, segSum2, tlSum, tlSum2, thSum, thSum2;
		private double singleton, singlton2;
		private int count;

		@Override
		public void collectStats(SegmentEventRecoder recorder, StringBuilder builder) {
			double segSites =recorder.getTotalMutationCount();
			segSum +=segSites;
			segSum2 +=segSites * segSites;

			double[] treeLengths =recorder.getTreeLengthDataAt(.5);

			tlSum +=treeLengths[1];
			tlSum2 +=treeLengths[1] * treeLengths[1];

			thSum +=treeLengths[0];
			thSum2 +=treeLengths[0] * treeLengths[0];
			count++;

			List<InfinteMutation> mutations =recorder.getMutationsUnsorted();
			int s =0;
			for (InfinteMutation im : mutations) {
				if (im.leafSet.countSetBits() == 1)
					s++;
			}
			singleton +=s;
			singlton2 +=s * s;

		}
		
		

		@Override
		public void summary(StringBuilder builder) {

		}

		public double[] getAllStats() {
			double thAve =thSum / count;
			double thVar =thSum2 / count - thAve * thAve;

			double tlAve =tlSum / count;
			double tlVar =tlSum2 / count - tlAve * tlAve;

			double segAve =segSum / count;
			double segVar =segSum2 / count - segAve * segAve;

			double sAve =singleton / count;
			double sVar =singlton2 / count - sAve * sAve;

			return new double[] { thAve, thVar, tlAve, tlVar, segAve, segVar, sAve, sVar };
		}
	}
}
