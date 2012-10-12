package at.mabs.abc;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.math.BigInteger;
import java.util.*;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;
import at.MSLike;
import at.mabs.cmdline.CLDescription;
import at.mabs.cmdline.CLNames;
import at.mabs.cmdline.CLUsage;
import at.mabs.cmdline.CmdLineBuildException;
import at.mabs.cmdline.CmdLineParseException;
import at.mabs.cmdline.CmdLineParser;
import at.mabs.config.CommandLineMarshal;
import at.mabs.model.SampleConfiguration;
import at.mabs.stats.StatsCollector;
import at.mabs.util.NullPrintStream;
import at.mabs.util.Util;
import at.mabs.util.random.Random64;

import com.esotericsoftware.yamlbeans.YamlReader;
import com.esotericsoftware.yamlbeans.YamlWriter;

/**
 * same idea as ABC. Use a annotated command line and a few parameters to add.
 * However output is much smaller. There is a Trace and just the output
 * parameters. File and std out are the bulk options. Still a lot of cut/paste
 * from NewABC. Need to refactor.
 * 
 * @author bob
 * 
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class SGA {
	private static final double MINLOG = Math.log(Double.MIN_VALUE);

	private String[] anotatedCmdLine;

	private String dataFileName = "data";

	private String outputFile = "ouput";

	private boolean mEstimate;
	private double mCutoff = 10;

	private double alpha = 1;// .602;
	private double gamma = .101;
	private double c = .01;
	private int initalStarting = 5000;
	private int bestOfCount = 10;
	private double initalJumpSize = .05;
	private int bwEstimationInterval = 20;
	private boolean isDirInSphere=false;
	private double clampLength=.01;

	private boolean verbose;

	private boolean writeData;// good for simulations.
	private String[] writeArgs;

	private List<StatsCollector> collectionStats = new ArrayList<StatsCollector>();
	private List<StatsCollector> dataStats = new ArrayList<StatsCollector>();
	private double[] data;

	private double[] bw;
	private int enn = 10;

	private int iterations = 5000;

	private long seed = System.nanoTime();

	private Random random;

	// private Kernal kernal = new Kernal.Gaussian();

	public void run() {
		System.out.println("Version:" + this.getClass().getPackage().getImplementationVersion() + " MSMSARGS:" + Arrays.toString(anotatedCmdLine));
		System.out.println("n=" + enn + "\titerMax:" + iterations + "\tM-Estimator? " + mEstimate);
		System.out.println("Seed:" + Long.toHexString(seed));
		random = new Random64(seed);

		List<PriorDensity> priors = new ArrayList<PriorDensity>();
		int realParamCount = 0;
		for (int i = 0; i < anotatedCmdLine.length; i++) {
			String arg = anotatedCmdLine[i];
			if (arg.contains("%") && !arg.startsWith("%")) {
				priors.add(new PriorDensity(arg, i));
				realParamCount++;
			} else if (arg.startsWith("%")) {
				int code = Integer.parseInt(arg.substring(1));
				priors.add(new CopyPriorDensity(priors.get(code-1), i));
			}
		}
		// now we have our parameter ranges...
		// next we bootstrap for our inital value.
		// add a seed parameter! Disables any provided seed

		String[] args = new String[anotatedCmdLine.length + 2];
		for (int i = 0; i < anotatedCmdLine.length; i++) {
			args[i] = anotatedCmdLine[i];
		}
		args[args.length - 2] = "-seed";// paste does the seed thing
		paste(args, priors, true);
		initStatCollectors(args);

		if (writeData) {
			int argIndex = 0;
			for (PriorDensity pd : priors) {
				args[pd.getArgIndex()] = writeArgs[argIndex++];
			}
			pasteSeed(args);
			MSLike.main(args, null, (List<? extends StatsCollector>) collectionStats, new NullPrintStream(), null);
			writeDataStats(collectionStats);
			System.out.println("DataGen With msms cmd Line:" + Arrays.toString(args));
			// return;
		}

		initDataFile();

		// densityGrid(args, priors, 50, 300,new FullGradFunction());
		// nablaGrid(args, priors, 500, 300, 30, new FullGradFunction());
		// System.exit(1);
		GradFunction simple = new GradFunction();
		GradFunction better = new FastGradFunction();// FullGradFunction();//
														// FastGradFunction();

		PointDensity[] startingPoints = new PointDensity[initalStarting];
		// first generate a condition set. We do the 1000, 100 thing.
		for (int i = 0; i < startingPoints.length; i++) {
			double[] p = randomPoint(priors);
			double score = simple.density(args, priors, p, 1, false);
			startingPoints[i] = new PointDensity(p, score);
			if ( i % (1+startingPoints.length / 80) == 0)
				System.out.print("*");
		}
		System.out.println();
		Arrays.sort(startingPoints);
		// now cut it down to the top 100.
		PointDensity[] betterStarts = new PointDensity[bestOfCount];
		System.arraycopy(startingPoints, 0, betterStarts, 0, betterStarts.length);
		System.out.println("Best:" + betterStarts[0]);

		List<PointDensity> preconditioned = new ArrayList<PointDensity>();
		// now run the preconditioner on all of them.
		for (int i = 0; i < betterStarts.length; i++) {
			preconditioned.add(betterStarts[i]);
			double[] np = null;// genericKWAlgo(args, priors,
								// betterStarts[i].point, 1, 2000,
								// simple);
			if (np == null)
				continue;
			double ns = simple.density(args, priors, np, 1, false);
			System.out.println("Old" + betterStarts[i].score + "\tnew:" + ns + "\t@" + Arrays.toString(transform(betterStarts[i].point, priors)) + "\t"
					+ Arrays.toString(transform(np, priors)));
			preconditioned.add(new PointDensity(np, ns));

		}
		Collections.sort(preconditioned);
		System.out.println("Best starting points and paramters:");
		for (PointDensity pd : preconditioned) {
			System.out.println(pd.score + " @ " + Arrays.toString(transform(pd.point, priors)));
		}
		System.out.println();
		// System.exit(0);
		clearTrace();
		try {
			Writer writer = new FileWriter(outputFile);

			List<double[]> found = new ArrayList<double[]>();
			for (PointDensity pd : preconditioned) {
				double[] np = genericKWAlgo(args, priors, pd.point, enn, iterations, better);
				if (np == null)
					continue;
				np = transform(np, priors);
				System.out.println("## " + Arrays.toString(np));
				found.add(np);
				for (int i = 0; i < np.length; i++) {
					writer.write(np[i] + "\t");
				}
				writer.write("\n");
				writer.flush();

			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private double[] randomPoint(List<PriorDensity> priors) {
		double[] p = new double[priors.size()];
		for (int i = 0; i < p.length; i++) {
			p[i] = random.nextDouble();
		}
		return p;
	}

	/**
	 * basic kw algo from a single point till stopping conditions are meet.
	 * 
	 * @param args
	 * @param priors
	 * @param n
	 * @param maxK
	 */
	private double[] genericKWAlgo(String[] args, List<PriorDensity> priors, double[] start, int n, int maxK, GradFunction grad) {
		System.out.println("\ninit starting parameters:" + Arrays.toString(transform(start, priors)));

		System.out.println("KWMethod with alpha:" + alpha + "\tand gamma:" + gamma);

		int n2 = 100;
		// first we need to get a c starting value;
		double sum = 0;
		double sum2 = 0;
		// paste(args,priors,start);
		int breakCount = 0;
		bwEstimation(args, priors, start, n);
		//
		System.out.println("c:" + c);
		// c = Math.max(.001, c);
		// c = Math.min(.01, c);

		int A = maxK / 10;// maxK / 100;// maxK / 10;
		System.out.println("Big A & c:" + A + "\t" + c);
		// now for a

		// distributions have huge outliers and are still very close to mean 0.
		// we use a 75% abs rank stat instead.
		GradFunction lgrad = new FullGradFunction();
		double[] nabla = lgrad.grad(args, priors, start, c, n, true);

		List<Double>[] nabList = new ArrayList[nabla.length];
		for (int i = 0; i < nabList.length; i++)
			nabList[i] = new ArrayList<Double>();
		int nabCount = 100;
		for (int i = 0; i < nabCount; i++) {
			double[] nnab = lgrad.grad(args, priors, start, c, n, true);
			for (int j = 0; j < nnab.length; j++) {
				nabList[j].add(Math.abs(nnab[j]));
				// System.out.print(nnab[j] + "\t");

			}
			// System.out.println();

		}
		for (int i = 0; i < nabList.length; i++) {
			Collections.sort(nabList[i]);
			nabla[i] = nabList[i].get(nabCount / 2);
		}

		// while (minabs(nabla) < 1e-10 && c < 1) {
		// c *= 2;
		// // System.out.println("BiggerC:"+c+"\t"+Arrays.toString(nabla));
		// nabla = lgrad.grad(args, priors, start, c, n, true);
		// }
		// System.out.println("Nab:" + Arrays.toString(nabla));
		if (c > 1 || containsNaNNill(nabla)) {
			System.out.println(" * NaN at calb");
			return null;
		}
		// assume a b_i==1;
		double[] a = new double[nabla.length];// Double.MAX_VALUE;
		double pa = Double.MAX_VALUE;
		for (int i = 0; i < nabla.length; i++) {

			a[i] = Math.min(Double.MAX_VALUE, initalJumpSize * Math.pow(A + 1, alpha) / Math.abs(nabla[i]));
			System.out.println("a for parameter" + (i + 1) + ":" + a[i] + "\t" + nabla[i]);
			pa = Math.min(pa, .03 * Math.pow(A + 1, alpha) / Math.abs(nabla[i]));
		}
		// Arrays.fill(a,pa);
		// a[2]=a[2]*100;
		// a*=10;
		// now put A in its place.
		// a = Math.max(c , a);
		// now we have a.
		// System.out.println("a:" + a + "\tc:" + c);
		List<double[]> trace = new ArrayList<double[]>();
		double[] x = start.clone();
		double[] lastChecked = x.clone();
		double[] last = x.clone();
		int biggerA = 0;
		trace.add(transform(x, priors));
		boolean degen = false;

		LinkedList<double[]> fifoParams = new LinkedList<double[]>();
		double[] paramSums = x.clone();
		fifoParams.addFirst(x.clone());

		boolean newTrace = true;

		double[] a_k = new double[a.length];
		for (int k = 0; k < maxK; k++) {
			for (int i = 0; i < a.length; i++) {
				a_k[i] = a[i] / Math.pow(k + A + 1, alpha);
			}
			double c_k = c / Math.pow(k + 1, gamma);
			//
			if (k % bwEstimationInterval == 0) {
				bwEstimation(args, priors, x, n);
			}
			nabla = grad.grad(args, priors, x, c_k, n, true);
			if (k % 10 == 0)
				System.out.println("k, a_k & c_k\t" + k + "\t" + Arrays.toString(a_k) + "\t" + c_k + "\nCurrentP:\t" + Arrays.toString(transform(x, priors))
						+ "\nCurrentNab\t" + Arrays.toString(nabla) + "\nEsimatedDensity:" + grad.density(args, priors, x, enn, true) + "\n");

			if (containsNaNNill(nabla)) {
				x = last.clone();
				continue;
				// if (degen == true) {
				// System.out.println(" * in loop @ " + k);
				// //return null;
				// continue;
				// }
				// do the simplex thing.
				//
				// degen = true;
			} else {
				degen = false;
			}
			last = x.clone();
			mul(nabla, a_k, nabla);
			clampLength(nabla, clampLength);
			add(x, nabla, x);
			clamp(x, priors, c_k);
			trace.add(transform(x, priors));

			// if (k % 10 == 0)
			// System.out.println(Arrays.toString(transform(x, priors)));
			if (k % 1000 == 0 && k != 0) {
				// boolean better = ttestIsBetter(args, priors, x, lastChecked,
				// n, grad);
				// //
				// System.out.println("                                                Better:"+better);
				// lastChecked = x.clone();
				// if (better) {
				// biggerA = 0;
				// } else if (biggerA >= 3) {
				// break;// we are done!
				// } else {
				// // try a bigger a.
				// // a = a * 1.1;
				// biggerA++;
				// }
				saveTrace(trace, newTrace);
				newTrace = false;
				trace.clear();
			}
			fifoParams.addFirst(x.clone());
			add(paramSums, fifoParams.getFirst(), paramSums);
			if (fifoParams.size() > 1000) {
				sub(paramSums, fifoParams.getLast(), paramSums);
				fifoParams.removeLast();
			}
			if (k % 10 == 0) {
				System.out.println("Moving Average:");
				for (int i = 0; i < paramSums.length; i++) {
					System.out.print(paramSums[i] / fifoParams.size() + "\t");
				}
				System.out.println();
			}
		}
		for (int i = 0; i < paramSums.length; i++) {
			paramSums[i] /= fifoParams.size();
		}
		System.out.println("Finished:" + Arrays.toString(transform(x, priors)));
		saveTrace(trace, newTrace);
		// if (ttestIsBetter(args, priors, paramSums, start, n, grad))
		return paramSums;
		// return null;

	}

	private int largestIndex(double[] nabla) {
		int max = -1;
		double maxV = Double.MIN_VALUE;
		for (int i = 0; i < nabla.length; i++) {
			double v = nabla[i];
			if (v != 0 && !Double.isNaN(v) && v > maxV) {
				max = i;
				maxV = v;
			}
		}
		return max;
	}

	private boolean containsNaNNill(double[] nabla) {
		for (double d : nabla) {
			if (Double.isNaN(d) || Double.isInfinite(d))
				return true;
		}
		return false;
	}

	private void clearTrace() {
		try {
			Writer write = new FileWriter(outputFile + ".traces", false);
			write.close();
		} catch (IOException e) {

			e.printStackTrace();
		}
	}

	private void saveTrace(List<double[]> trace, boolean newTrace) {
		if (trace.isEmpty())
			return;
		try {
			Writer write = new FileWriter(outputFile + ".traces", true);
			if (newTrace) {
				// first a simple delimiter.
				double[] s = trace.get(0);
				for (int i = 0; i < s.length; i++) {
					write.write("" + Double.NaN);
					if (i < s.length - 1) {
						write.write(" , ");
					}

				}
				write.write("\n");
			}
			for (double[] d : trace) {
				for (int i = 0; i < d.length; i++) {
					write.write("" + d[i]);
					if (i < d.length - 1) {
						write.write(" , ");
					}
				}
				write.write("\n");
			}

			write.flush();
			write.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private double minabs(double[] a) {
		double min = Double.MAX_VALUE;
		for (double v : a)
			min = Math.min(Math.abs(v), min);
		return min;
	}

	// use a .95 ttest. Pretty hacky for now.
	private boolean ttestIsBetter(String[] args, List<PriorDensity> priors, double[] x, double[] y, int n, GradFunction grad) {
		int k = 25;
		System.out.println("Testing:" + Arrays.toString(x) + "\t" + Arrays.toString(y));
		double xsum = 0;
		double xsum2 = 0;
		double ysum = 0;
		double ysum2 = 0;
		for (int i = 0; i < k; i++) {
			double xd = grad.density(args, priors, x, n, false);
			double yd = grad.density(args, priors, y, n, false);

			xsum += xd;
			xsum2 += xd * xd;
			ysum += yd;
			ysum2 += yd * yd;

		}
		double xmean = xsum / k;
		double xvar = (xsum2 - k * xmean * xmean) / (k - 1);
		double ymean = ysum / k;
		double yvar = (ysum2 - k * ymean * ymean) / (k - 1);

		// take care of the cases where density is a constant... aka p=0
		if (yvar == 0 && xvar != 0) {
			return true;
		}
		if (xvar == 0)
			return false;

		double cstd = Math.sqrt((xvar + yvar) / n);
		double t = (xmean - ymean) / cstd;
		System.out.println("Ttest:" + t + "\t" + xmean + "\t" + ymean + "\tvars:" + xvar + "\t" + yvar);
		return t > 1.7;
	}

	private double[] gradApprox(String[] args, List<PriorDensity> priors, double[] x, double delta, int n, boolean log) {
		double[] xp = x.clone();
		double[] nabla = new double[x.length];
		for (int i = 0; i < x.length; i++) {
			xp[i] = x[i] + delta;
			xp[i] = Math.min(xp[i], 1);
			double realDelta = xp[i] - x[i];
			paste(args, priors, xp);// need to set n....
			double sump = 0;
			for (int j = 0; j < n; j++) {
				pasteSeed(args);
				MSLike.main(args, null, (List<? extends StatsCollector>) collectionStats, new NullPrintStream(), null);
				double[] sp = collectStatitics(collectionStats);
				sump += norm2(sp, data);
			}

			xp[i] = x[i] - delta;
			xp[i] = Math.max(0, xp[i]);
			realDelta += x[i] - xp[i];

			paste(args, priors, xp);// need to set n....
			double sumn = 0;
			for (int j = 0; j < n; j++) {
				pasteSeed(args);
				MSLike.main(args, null, (List<? extends StatsCollector>) collectionStats, new NullPrintStream(), null);
				double[] sp = collectStatitics(collectionStats);
				sumn += norm2(sp, data);
			}
			nabla[i] = -(sump - sumn) / (realDelta * n);
			xp[i] = x[i];
		}
		return nabla;
	}

	private double norm2(double[] a, double[] b) {
		double acc = 0;
		for (int i = 0; i < a.length; i++) {
			double d = a[i] - b[i];
			acc += d * d;
		}

		return acc;
	}

	private double[] gradFull(String[] args, List<PriorDensity> priors, double[] x, double delta, int n, boolean log) {
		// bwEstimation(args, priors, x, n);
		double[] xp = x.clone();
		double[] nabla = new double[x.length];
		for (int i = 0; i < xp.length; i++) {
			xp[i] = x[i] + delta;
			double lp = density(args, priors, xp, n, log);
			xp[i] = x[i] - delta;
			double ln = density(args, priors, xp, n, log);
			nabla[i] = (lp - ln) / (2 * delta);
			xp[i] = x[i];
		}
		return nabla;
	}

	private void randomDir(double[] a, double[] b) {
		double n2 = 0;
		for (int i = 0; i < a.length; i++) {
			a[i] = random.nextGaussian();
			n2 += a[i] * a[i];
		}
		n2 = Math.sqrt(n2);
		for (int i = 0; i < a.length; i++) {
			a[i] /= n2;
			b[i] = -a[i];
		}

	}

	private double[] gradFast(String[] args, List<PriorDensity> priors, double[] x, double delta, int n, boolean log) {
		// random direction one.

		double[] rv1 = new double[x.length];
		double[] rv2 = new double[x.length];
		while (Arrays.equals(rv1, rv2)) {
			for (int i = 0; i < x.length; i++) {
				if (random.nextBoolean()) {
					rv1[i] = x[i] + delta;
				} else {
					rv1[i] = x[i] - delta;
				}
				if (random.nextBoolean()) {
					rv2[i] = x[i] + delta;
				} else {
					rv2[i] = x[i] - delta;
				}
			}
		}
		if (isDirInSphere) {
			randomDir(rv1, rv2);
			mul(rv1, delta, rv1);
			mul(rv2, delta, rv2);
			add(rv1, x, rv1);
			add(rv2, x, rv2);
		}
		// System.out.println("1:"+Arrays.toString(rv1));
		// System.out.println("2:"+Arrays.toString(rv2));
		double da = density(args, priors, rv1, n, log);
		double db = density(args, priors, rv2, n, log);
		if (da == 0 || Double.isInfinite(da) || Double.isNaN(da) || db == 0 || Double.isInfinite(db) || Double.isNaN(db)) {
			System.out.println("+:" + da + "\t" + db);
			return new double[x.length];
		}
		if (verbose) {
			System.out.println("Densities for Gradient:" + da + "\t" + db);
		}

		double g = (da - db) / (2 * delta);
		// need the direction..magnatude thing.

		double[] nabla = new double[x.length];
		double l2 = 0;
		for (int i = 0; i < x.length; i++) {
			double d = rv1[i] - rv2[i];
			l2 += d * d;
			nabla[i] = d;
		}
		g = g / Math.sqrt(l2);
		// System.out.println("WhereNaN:"+g+"\t"+l2+"\tda"+da+"\t"+db);
		for (int i = 0; i < x.length; i++)
			nabla[i] *= g;
		return nabla;
	}

	private void add(double[] x, double[] y, double[] r) {
		for (int i = 0; i < r.length; i++) {
			r[i] = x[i] + y[i];
		}
	}

	private void sub(double[] x, double[] y, double[] r) {
		for (int i = 0; i < r.length; i++) {
			r[i] = x[i] - y[i];
		}
	}

	private void mul(double[] x, double y, double[] r) {
		for (int i = 0; i < r.length; i++) {
			r[i] = x[i] * y;
		}
	}

	private void mul(double[] x, double[] y, double[] r) {
		for (int i = 0; i < r.length; i++) {
			r[i] = x[i] * y[i];
		}
	}

	private double density(String[] args, List<PriorDensity> priors, double[] x, int n, boolean log) {
		// esitmate the denstiy at x
		assert n > 1;

		double[][] stats = new double[n][0];

		paste(args, priors, x);// need to set n....
		for (int i = 0; i < n; i++) {
			// System.out.println("ARGS:"+Arrays.toString(args));
			pasteSeed(args);
			MSLike.main(args, null, (List<? extends StatsCollector>) collectionStats, new NullPrintStream(), null);
			stats[i] = collectStatitics(collectionStats);
			// System.out.println("CollectedStats:"+Arrays.toString(stats[i]));

		}

		double ksum = 0;

		double sigmaNorm = 1;
		for (int j = 0; j < data.length; j++) {
			if (bw[j] > 1e-10 && !Double.isNaN(bw[j])) {
				sigmaNorm *= bw[j];
			}
		}

		double bestPoint = Double.MAX_VALUE;
		for (int i = 0; i < n; i++) {
			double[] stat = stats[i];
			double sum = 0;

			for (int j = 0; j < data.length; j++) {
				double d = (data[j] - stat[j]) / bw[j];
				if (bw[j] < 1e-10 || Double.isNaN(bw[j]))
					continue;

				if (Math.abs(d) < mCutoff || !mEstimate) {
					sum += (d * d);
				} else {
					sum += mCutoff * Math.abs(d);
				}
			}
			bestPoint = Math.min(sum, bestPoint);
			// System.out.println("SUM:"+sum);
			ksum += Math.exp(-.5 * sum);
		}
		// sigmaNorm = Math.sqrt(sigmaNorm);
		// System.out.println("SigNorm:" + sigmaNorm);
		double dens = ksum / sigmaNorm;// / (sigmaNorm * Math.pow(2 * Math.PI,
										// data.length /
		// 2.0));
		if (Double.isNaN(dens) || Double.isInfinite(dens) || dens <= 0) {
			// System.out.print("*");//****DEGEN****:"+dens+"\t"+Math.exp(-.5*bestPoint)+"\t"+bestPoint+"\n");
			if (log) {
				return (-.5 * bestPoint) - Math.log(sigmaNorm);
			}
			return Math.exp(-.5 * bestPoint) / sigmaNorm;
		}
		if (log) {
			dens = Math.log(dens);
		}
		return dens;
	}

	private double densityLinear(String[] args, List<PriorDensity> priors, double[] x, int n) {
		// esitmate the denstiy at x
		assert n > 1;

		double[] stats = new double[n];

		paste(args, priors, x);// need to set n....
		for (int i = 0; i < n; i++) {
			// System.out.println("ARGS:"+Arrays.toString(args));
			pasteSeed(args);
			MSLike.main(args, null, (List<? extends StatsCollector>) collectionStats, new NullPrintStream(), null);
			double[] v = collectStatitics(collectionStats);
			stats[i] = norm2(v, data);

		}
		double stds = 0;
		double means = 0;
		for (int i = 0; i < n; i++) {
			double d = stats[i];

			stds += d * d;
			means += d / n;

		}
		double nfactor2 = Math.pow(n, -2 / 5.0);// note it is
												// squared

		double bw2 = (stds / n - means * means) * nfactor2;

		// now stds has bw^2 for everything.
		double ksum = 0;
		double sigmaNorm = bw2;
		for (int i = 0; i < n; i++) {
			double stat = stats[i];

			// System.out.println("SUM:"+sum);
			ksum += Math.exp(-.5 * stat * stat / bw2);
		}
		// System.out.println("KLinSUM:" + ksum);
		double dens = ksum / (sigmaNorm * Math.pow(2 * Math.PI, data.length / 2.0));
		if (dens == 0 || Double.isNaN(dens))
			return Double.MIN_VALUE;
		return dens;
	}

	private void bwEstimation(String[] args, List<PriorDensity> priors, double[] x, int n) {
		// esitmate the denstiy at x
		assert n > 1;

		double[][] stats = new double[n][0];

		paste(args, priors, x);// need to set n....
		for (int i = 0; i < n; i++) {
			// System.out.println("ARGS:"+Arrays.toString(args));
			pasteSeed(args);
			MSLike.main(args, null, (List<? extends StatsCollector>) collectionStats, new NullPrintStream(), null);
			stats[i] = collectStatitics(collectionStats);
			// System.out.println("CollectedStats:"+Arrays.toString(stats[i]));

		}
		double[] stds = new double[stats[0].length];
		double[] means = new double[stds.length];
		for (int i = 0; i < n; i++) {
			double[] stat = stats[i];
			for (int j = 0; j < means.length; j++) {
				double d = data[j] - stat[j];
				stds[j] += d * d;
				means[j] += d / n;
			}
		}
		double nfactor2 = Math.pow(4.0 / (2 + data.length), 2.0 / (4 + data.length)) * Math.pow(n, -2 / (4.0 + data.length));
		for (int i = 0; i < stds.length; i++) {

			stds[i] = Math.sqrt((stds[i] / n - means[i] * means[i]) * nfactor2);
		}
		System.out.println("BW:" + Arrays.toString(stds));
		this.bw = stds;
	}

	private double[] transform(double[] x, List<PriorDensity> priors) {
		double[] t = new double[x.length];
		for (int i = 0; i < x.length; i++) {
			PriorDensity pd = priors.get(i);
			pd.setLastValueUI(x[i]);
			t[i] = pd.getTransformedValue();
		}
		return t;
	}

	private void clamp(double[] x, List<PriorDensity> priors, double delta) {
		for (int i = 0; i < x.length; i++) {
			x[i] = Math.max(delta, x[i]);
			x[i] = Math.min(1 - delta, x[i]);
		}
	}

	private void clampLength(double[] x, double delta) {
		// System.out.println("CLAMP:\n"+Arrays.toString(x));
		for (int i = 0; i < x.length; i++) {
			x[i] = Math.min(delta, x[i]);
			x[i] = Math.max(-delta, x[i]);
		}
		// System.out.println(Arrays.toString(x)+"\nENDCLAMP");
	}

	private void paste(String[] args, List<PriorDensity> priors, double[] values) {
		// clamp(values, priors);
		for (int i = 0; i < priors.size(); i++) {
			PriorDensity pd = priors.get(i);
			double value = values[i];//
			pd.setLastValueUI(value);
			value = pd.getTransformedValue();
			args[pd.getArgIndex()] = "" + value;
		}
	}

	private void writeDataStats(List<StatsCollector> stats) {
		try {
			Writer writer = new FileWriter(dataFileName);
			saveStats(writer, stats);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private double[] collectStatitics(List<StatsCollector> stats) {
		double[][] all = new double[stats.size()][0];
		int count = 0;
		for (int i = 0; i < stats.size(); i++) {
			StatsCollector stat = stats.get(i);
			all[i] = stat.summaryStats();
			count += all[i].length;
		}
		double[] allLinear = new double[count];
		int index = 0;
		for (int i = 0; i < all.length; i++) {
			System.arraycopy(all[i], 0, allLinear, index, all[i].length);
			index += all[i].length;
		}
		// System.out.println("*"+Arrays.toString(allLinear));
		return allLinear;
	}

	private double[] distance(StatsCollector sim, StatsCollector data) {
		double[] s = sim.summaryStats();
		double[] d = data.summaryStats();
		double[] delta = new double[s.length];
		assert s.length == d.length;

		double dis = 0;
		for (int i = 0; i < s.length; i++) {
			delta[i] = s[i] - d[i];

		}
		return delta;
	}

	private void paste(String[] args, List<PriorDensity> priors, boolean rand) {
		for (int i = 0; i < priors.size(); i++) {
			PriorDensity pd = priors.get(i);
			if (rand) {
				pd.generateRandom();
			}
			double value = pd.getTransformedValue();
			args[pd.getArgIndex()] = "" + value;
		}
		pasteSeed(args);
	}

	private void pasteSeed(String[] args) {
		// now the seed thing.
		args[args.length - 1] = "" + random.nextLong();
		// System.out.println(Arrays.toString(args));
	}

	private void initStatCollectors(String[] msmsArgs) {
		CommandLineMarshal msmsparser = new CommandLineMarshal();
		try {
			CmdLineParser<CommandLineMarshal> marshel = CommandLineMarshal.getCacheParser();// new
																							// CmdLineParser<CommandLineMarshal>(msmsparser);
			marshel.processArguments(msmsArgs, msmsparser);
			SampleConfiguration sampleConfig = msmsparser.getSampleConfig();
			// for (StatsCollector stat : collectionStats)
			// stat.init(sampleConfig); //FIXME
			List<StatsCollector> defaultCollectors = msmsparser.getStatsCollectors();

			this.collectionStats.addAll(defaultCollectors);

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void initDataFile() {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(dataFileName));
			dataStats.clear();
			loadStats(reader, dataStats);

			Collections.sort(dataStats, new ClassNameOrder());
			Collections.sort(collectionStats, new ClassNameOrder());
			// we now remove anything that dataStats has that Collection doesn't
			// and vice versa
			ListIterator<StatsCollector> iter = dataStats.listIterator();
			while (iter.hasNext()) {
				StatsCollector stat = iter.next();
				if (!containsClass(stat.getClass(), collectionStats)) {
					System.out.println("warrning: Stat in Data ignored:" + stat);
					iter.remove();
				}
			}
			iter = collectionStats.listIterator();
			while (iter.hasNext()) {
				StatsCollector stat = iter.next();
				if (!containsClass(stat.getClass(), dataStats)) {
					System.out.println("warrning: Stat in Collection ignored:" + stat);
					iter.remove();
				}
			}
			List<Double> rawData = new ArrayList<Double>();
			for (StatsCollector sc : dataStats) {
				double[] dd = sc.summaryStats();
				for (double d : dd) {
					rawData.add(d);
				}
			}
			this.data = Util.toArrayPrimitiveDouble(rawData);
			System.out.println("Data Summary Stats:" + Arrays.toString(data));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private boolean containsClass(Class type, List list) {
		for (Object o : list) {
			if (o.getClass().equals(type))
				return true;
		}
		return false;
	}

	private void loadStats(BufferedReader reader, List<StatsCollector> stats) throws IOException {
		YamlReader yamlReader = new YamlReader(reader);
		yamlReader.getConfig().setPrivateFields(true);
		yamlReader.getConfig().setPrivateConstructors(true);
		List<StatsCollector> readStats = (List<StatsCollector>) yamlReader.read();
		if (readStats != null)
			stats.addAll(readStats);
		yamlReader.close();
	}

	private void saveStats(Writer writer, List<StatsCollector> stats) throws IOException {
		YamlWriter yamlWriter = new YamlWriter(writer);
		yamlWriter.getConfig().setPrivateFields(true);
		yamlWriter.getConfig().setPrivateConstructors(true);
		yamlWriter.write(stats);
		yamlWriter.close();
	}

	@CLNames(names = { "-msms", "-MSMS" }, rank = 2, required = true)
	@CLDescription("Set the command line for msms in a anontated format. The anotation is FromValue%toValue[%lg] for all parameters that are extimated.")
	@CLUsage("-msms {any valid command line}")
	public void setMSMSAnotatedCmdLine(String[] anotatedCmdLine) {
		this.anotatedCmdLine = anotatedCmdLine;
	}

	@CLNames(names = { "-abcstat", "-STAT" })
	public void addStat(String[] statAndConfig) {
		String statName = statAndConfig[0];
		// turn this into a class in 2 ways. if it contains no . try using this
		// package as a prefix
		Class type = null;
		try {
			String packageName = this.getClass().getPackage().getName();
			type = Class.forName(packageName + "." + statName);
		} catch (ClassNotFoundException e) {
			try {
				type = Class.forName(statName);
			} catch (ClassNotFoundException e1) {
				throw new RuntimeException("Could not find ABCStat class " + statName);
			}
		}
		if (!StatsCollector.class.isAssignableFrom(type)) {
			throw new RuntimeException("Specified -stat class does not load a ABCStat object but rather a " + type);
		}
		StatsCollector stat = null;
		try {
			stat = (StatsCollector) type.newInstance();
		} catch (Exception e) {
			throw new RuntimeException("Specified -stat class does not have a default constructor", e);
		}

		if (statAndConfig.length == 1) {
			collectionStats.add(stat);
			return;
		}

		try {
			CmdLineParser<StatsCollector> parser = new CmdLineParser<StatsCollector>((Class<StatsCollector>) stat.getClass());
			if (statAndConfig.length == 2 && statAndConfig[1].contains("help")) {
				System.err.println("Help for statCollector:" + statName + "\n" + parser.longUsage());
				return;
			}
			String[] args = new String[statAndConfig.length - 1];
			System.arraycopy(statAndConfig, 1, args, 0, args.length);
			// System.out.println("StatsARGS:"+Arrays.toString(args));
			parser.processArguments(args, stat);
			// System.out.println("Object:"+stat+"\t"+parser.longUsage());
		} catch (CmdLineBuildException e) {
			throw new RuntimeException(statName + " does not take options or we have an error", e);
		} catch (CmdLineParseException e) {
			throw new RuntimeException("Error With stats options:" + statName, e);
		}
		collectionStats.add(stat);

	}

	@CLNames(names = { "-write" })
	public void setWriteDataTrue(String[] writeArgs) {
		this.writeData = true;
		this.writeArgs = writeArgs;
	}

	@CLNames(names = { "-data" })
	public void setDataFileName(String dataFileName) {
		this.dataFileName = dataFileName;
	}

	@CLNames(names = { "-out" })
	public void setOutputFile(String outputFile) {
		this.outputFile = outputFile;
	}

	@CLNames(names = { "-enn" })
	public void setEnn(int enn) {
		this.enn = enn;
	}

	@CLNames(names = { "-mestimate", "-Mest" })
	public void setMEstimateTrue() {
		this.mEstimate = true;
	}

	@CLNames(names = { "-v", "-verbose" })
	public void setVerboseTrue() {
		this.verbose = true;
	}

	@CLNames(names = { "-iterations", "-iter", "-reps" })
	public void setIterations(int iterations) {
		this.iterations = iterations;
	}

	@CLNames(names = { "-seed" })
	@CLDescription("Set the random seed. Can be decimal or hex (eg 0xC0FFEBABE)")
	@CLUsage("SEED")
	public void setSeed(String seedString) {
		if (seedString.startsWith("0x")) {
			seed = (new BigInteger(seedString.substring(2), 16)).longValue();
		} else {
			this.seed = Long.parseLong(seedString);
		}
	}

	
	@CLNames(names={"-mestCutoff","-mescutoff","-mcutoff"})
	@CLDescription("Sets the \\delta/h cutoff value for the M Estinator kernel function")
	public void setmCutoff(double mCutoff) {
		this.mCutoff = mCutoff;
	}
	@CLNames(names={"-alpha"})
	public void setAlpha(double alpha) {
		this.alpha = alpha;
	}
	@CLNames(names={"-gamma"})
	public void setGamma(double gamma) {
		this.gamma = gamma;
	}
	@CLNames(names={"-c"})
	public void setC(double c) {
		this.c = c;
	}

	@CLNames(names={"-initalRandomStarts","-irs"})
	@CLDescription("How many random points to rank for starting positions")
	public void setInitalStarting(int initalStarting) {
		this.initalStarting = initalStarting;
	}

	@CLNames(names={"-bestOfKeep"})
	@CLDescription("From all the random starting positions, how many to use starting from the best match")
	public void setBestOfCount(int bestOfCount) {
		this.bestOfCount = bestOfCount;
	}

	@CLNames(names={"-initJumpSize","-ijs"})
	@CLDescription("How far do we want the first a_k to go in the first interation?")
	public void setInitalJumpSize(double initalJumpSize) {
		this.initalJumpSize = initalJumpSize;
	}

	@CLNames(names={"-bwInterval","-bwi"})
	@CLDescription("iteration count between BW estimation")
	public void setBwEstimationInterval(int bwEstimationInterval) {
		this.bwEstimationInterval = bwEstimationInterval;
	}

	@CLNames(names={"-sphere"})
	@CLDescription("Generate the slice as a random direction in the sphere")
	public void setDirInSphereTrue() {
		this.isDirInSphere = true;
	}

	@CLNames(names={"-clampLength","-cl"})
	@CLDescription("length (hypercube) to clamp movement too. Prevents rare but expensive excursions")
	public void setClampLength(double clampLength) {
		this.clampLength = clampLength;
	}
	
	public static void main(String[] args) {
		
		SGA sga = new SGA();
		CmdLineParser<SGA> parser = null;
		try {
			parser = new CmdLineParser<SGA>(SGA.class);
		} catch (Exception e1) {
			e1.printStackTrace();
			return;
		}

		try {
			parser.processArguments(args, sga);
			sga.run();
		} catch (Exception e) {
			System.err.println(parser.longUsage());
			e.printStackTrace();

		}
	}

	private static class ClassNameOrder implements Comparator<Object> {
		@Override
		public int compare(Object o1, Object o2) {
			String name1 = o1.getClass().getName();
			String name2 = o2.getClass().getName();
			return name1.compareTo(name2);
		}
	}

	public class GradFunction {
		public double[] grad(String[] args, List<PriorDensity> priors, double[] x, double delta, int n, boolean log) {
			return gradApprox(args, priors, x, delta, n, log);
		}

		public double density(String[] args, List<PriorDensity> priors, double[] x, int n, boolean log) {
			paste(args, priors, x);
			double sum = 0;
			for (int i = 0; i < n; i++) {
				pasteSeed(args);
				MSLike.main(args, null, (List<? extends StatsCollector>) collectionStats, new NullPrintStream(), null);
				double[] sp = collectStatitics(collectionStats);
				sum += norm2(sp, data);
			}
			return -sum / n;
		}
	}

	public class FullGradFunction extends GradFunction {
		public double[] grad(String[] args, List<PriorDensity> priors, double[] x, double delta, int n, boolean log) {
			return gradFull(args, priors, x, delta, n, log);
		}

		public double density(String[] args, List<PriorDensity> priors, double[] x, int n, boolean log) {
			return SGA.this.density(args, priors, x, n, log);
		}
	}

	public class FastGradFunction extends GradFunction {
		public double[] grad(String[] args, List<PriorDensity> priors, double[] x, double delta, int n, boolean log) {
			// System.out.println("FAST");
			return gradFast(args, priors, x, delta, n, log);
		}

		public double density(String[] args, List<PriorDensity> priors, double[] x, int n, boolean log) {
			return (SGA.this.density(args, priors, x, n, log));
		}
	}

	public class PointDensity implements Comparable<PointDensity> {
		double[] point;
		double score;

		public PointDensity(double[] point, double score) {
			this.point = point;
			this.score = score;

		}

		@Override
		public int compareTo(PointDensity o) {
			if (score > o.score)
				return -1;
			if (score < o.score)
				return 1;
			return 0;
		}

		@Override
		public String toString() {
			// TODO Auto-generated method stub
			return "PD:" + score + "\n";
		}
	}

	// public class ApproxGradFunction extends GradFunction{
	// public double[] grad(String[] args, List<PriorDensity> priors, double[]
	// x, double delta, int n) {
	// return gradSlice(args, priors, x, delta, 1, n);
	// }
	//
	// public double density(String[] args, List<PriorDensity> priors, double[]
	// x, int n) {
	// return SGA.this.density(args, priors, x, n);
	// }
	// }
}
