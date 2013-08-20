package at.mabs.external;

import java.util.Random;

import at.mabs.util.Util;
import at.mabs.util.random.RandomGenerator;
import at.mabs.util.random.Statistics;
import cern.jet.random.Binomial;
import cern.jet.random.Poisson;

public class FastForward {
	private Binomial bin = RandomGenerator.getBinomial();
	private Random rand = RandomGenerator.getRandom();
	private Poisson poi = RandomGenerator.getPoisson();

	private int N = 1000;
	private int gens = 4 * N;
	private double s = 1e-3;
	private double mu = 2e-3;
	private double notmu = 1 - mu;
	private int maxMut = 10;// counting zero.

	private double[] trace = new double[maxMut * gens];

	public FastForward() {
		trace[0] = 1;
	}

	private void stepForward(int gen) {
		int genIndex = gen * maxMut;
		int genIndexp = genIndex + maxMut;
		// first mutation. drift...
		// we use gen and a write to gen+1
		double sum = trace[genIndex] * notmu;
		trace[genIndex + maxMut] = sum;
		for (int i = 1; i < maxMut; i++) {
			double w = 1 - s * i;
			double a = (trace[genIndex + i - 1] * mu + trace[genIndex + i] * notmu) * w;
			sum += a;
			trace[genIndexp + i] = a;
			// System.out.println(a);
		}
		// toString(gen+1);
		// normalize multinomail;
		double p = 0;
		int n = N;
		for (int i = 0; i < maxMut - 1; i++) {
			double np = trace[genIndexp + i] / sum;
			int nn = bin.generateBinomial(n, np / (1 - p));
			n -= nn;
			p += np;
			trace[genIndexp + i] = (double) nn / N;
		}
		trace[genIndexp + maxMut - 1] = (double) (n) / N;
		// toString(gen+1);
	}

	public void run() {
		for (int i = 0; i < gens - 1; i++) {
			stepForward(i);
			

		}
		toString(gens-1);
	}

	private void toString(int gen) {
		for (int j = 0; j < maxMut; j++) {
			System.out.print(trace[gen * maxMut + j] + "\t");
		}
		System.out.println();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		FastForward ff = new FastForward();
		long t=System.nanoTime();
		for (int i = 0; i < 1000; i++) {
			ff.run();
			
		}
		//ff.toString(1);
		System.out.println((System.nanoTime()-t)*1e-9);
	}

}
