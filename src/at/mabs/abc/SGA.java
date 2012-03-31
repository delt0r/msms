package at.mabs.abc;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
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
	// private double gammac =20;

	private double[] gammacs = { 50, 50, 50 };

	// private double gammaa =100e5;
	private double[] gammaas;

	// private int n = 400;
	// private int bootstrap =100;

	private String[] anotatedCmdLine;

	private String dataFileName = "data";

	private String outputFile = "ouput";

	private boolean writeData;// good for simulations.
	private String[] writeArgs;

	private List<StatsCollector> collectionStats = new ArrayList<StatsCollector>();
	private List<StatsCollector> dataStats = new ArrayList<StatsCollector>();
	private double[] data;

	private double[] bw;
	private int enn=10;
	private int iterations=10000;

	private Random random = new Random64();

	// private Kernal kernal = new Kernal.Gaussian();

	public void run() {
		List<PriorDensity> priors = new ArrayList<PriorDensity>();
		int realParamCount = 0;
		for (int i = 0; i < anotatedCmdLine.length; i++) {
			String arg = anotatedCmdLine[i];
			if (arg.contains("%") && !arg.startsWith("%")) {
				priors.add(new PriorDensity(arg, i));
				realParamCount++;
			} else if (arg.startsWith("%")) {
				int code = Integer.parseInt(arg.substring(1));
				priors.add(new CopyPriorDensity(priors.get(code), i));
			}
		}
		// now we have our parameter ranges...
		// next we bootstrap for our inital value.
		String[] args = anotatedCmdLine.clone();
		paste(args, priors, true);
		initStatCollectors(args);

		if (writeData) {
			int argIndex = 0;
			for (PriorDensity pd : priors) {
				args[pd.getArgIndex()] = writeArgs[argIndex++];
			}
			MSLike.main(args, null, (List<? extends StatsCollector>) collectionStats, new NullPrintStream(), null);
			writeDataStats(collectionStats);
			System.out.println("Wrote out Stats:" + Arrays.toString(args));
			// return;
		}

		initDataFile();

		// densityGrid(args, priors, 50, 300,new FullGradFunction());
		// nablaGrid(args, priors, 500, 300, 30, new FullGradFunction());
		// System.exit(1);
		GradFunction simple = new GradFunction();
		GradFunction better = new FullGradFunction();

		PointDensity[] startingPoints = new PointDensity[10000];
		// first generate a condition set. We do the 1000, 100 thing.
		for (int i = 0; i < startingPoints.length; i++) {
			double[] p = randomPoint(priors);
			double score = simple.density(args, priors, p, 1);
			startingPoints[i] = new PointDensity(p, score);
		}
		Arrays.sort(startingPoints);
		// now cut it down to the top 100.
		PointDensity[] betterStarts = new PointDensity[100];
		System.arraycopy(startingPoints, 0, betterStarts, 0, betterStarts.length);
		System.out.println("Best:" + betterStarts[0]);

		List<PointDensity> preconditioned = new ArrayList<PointDensity>();
		// now run the preconditioner on all of them.
		for (int i = 0; i < betterStarts.length; i++) {
			preconditioned.add(betterStarts[i]);
			double[] np = null;// genericKWAlgo(args, priors,
								// betterStarts[i].point, 1, 100, simple);
			if (np == null)
				continue;
			double ns = simple.density(args, priors, np, 1);
			System.out.println("Old" + betterStarts[i].score + "\tnew:" + ns + "\t@" + Arrays.toString(transform(betterStarts[i].point, priors)) + "\t"
					+ Arrays.toString(transform(np, priors)));
			preconditioned.add(new PointDensity(np, ns));

		}
		Collections.sort(preconditioned);
		for (PointDensity pd : preconditioned) {
			System.out.println(pd.score + " @ " + Arrays.toString(transform(pd.point, priors)));
		}

		try {
			Writer writer = new FileWriter(outputFile);

			List<double[]> found = new ArrayList<double[]>();
			for (PointDensity pd : preconditioned) {
				double[] np = genericKWAlgo(args, priors, pd.point, enn, iterations, better);
				if (np == null)
					continue;
				np=transform(np, priors);
				System.out.println("## " + Arrays.toString(np));
				found.add(np);
				for(int i=0;i<np.length;i++){
					writer.write(np[i]+"\t");
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
		System.out.println("init:" + Arrays.toString(transform(start, priors)));
		double alpha = .602;
		double gamma = .101;
		int n2 = 100;
		// first we need to get a c starting value;
		double sum = 0;
		double sum2 = 0;
		// paste(args,priors,start);
		for (int i = 0; i < n2; i++) {
			double d = (grad.density(args, priors, start, n));
			// System.out.println(d);
			if (Double.isInfinite(d) || Double.isNaN(d)) {
				System.out.println("stdUn*");
				return null;// d = MINLOG;
			}
			sum += d;
			sum2 += d * d;
		}
		double mean = sum / n2;

		double c = Math.sqrt((sum2 / n2) - mean * mean);
		// sensable min/max
		c = Math.max(.01, c);
		c = Math.min(.1, c);

		int A = maxK / 10;

		// now for a
		double[] nabla = grad.grad(args, priors, start, c, n);
		while (minabs(nabla) < 1e-10 && c < 1) {
			c *= 2;
			// System.out.println("BiggerC:"+c+"\t"+Arrays.toString(nabla));
			nabla = grad.grad(args, priors, start, c, n);
		}
		// System.out.println("Nab:" + Arrays.toString(nabla));
		if (c > 1 || containsNaNNill(nabla)) {
			System.out.println(" * ");
			return null;
		}
		// assume a b_i==1;
		double a = Double.MAX_VALUE;
		for (int i = 0; i < nabla.length; i++) {
			a = Math.min(a, .1 / Math.abs(nabla[i]));
		}
		// now put A in its place.
		a = Math.max(c * 1e-3, a);
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

		for (int k = 0; k < maxK; k++) {
			last = x.clone();
			double a_k = a / Math.pow(k + A + 1, alpha);
			double c_k = c / Math.pow(k + 1, gamma);
			if (k % 10 == 0)
				System.out.println("k, a_k & c_k\t" + k + "\t" + a_k + "\t" + c_k);

			nabla = grad.grad(args, priors, x, c_k, n);
			mul(nabla, a_k, nabla);
			add(x, nabla, x);
			clamp(x, priors, c_k);
			trace.add(transform(x, priors));

			if (containsNaNNill(nabla)) {
				if (degen == true) {
					System.out.println(" * ");
					return null;
				}
				// do the simplex thing.
				x = last.clone();
				int dim = largestIndex(nabla);
				if (dim > -1) {
					double sgn = Math.signum(nabla[dim]);
					x[dim] += sgn * c_k;
				}
				degen = true;
			} else {
				degen = false;
			}
			if (k % 10 == 0)
				System.out.println(Arrays.toString(transform(x, priors)));
			if (k % 2000 == 0 && k != 0) {
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
		//if (ttestIsBetter(args, priors, paramSums, start, n, grad))
		return paramSums;
		//return null;

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
			if (d == 0 || Double.isNaN(d))
				return true;
		}
		return false;
	}

	private void saveTrace(List<double[]> trace, boolean newTrace) {
		if (trace.isEmpty())
			return;
		try {
			Writer write = new FileWriter(outputFile+".traces", true);
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
			double xd = grad.density(args, priors, x, n);
			double yd = grad.density(args, priors, y, n);

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

	private void nablaGrid(String[] args, List<PriorDensity> priors, int n, int gridPoints, double fdDelta, GradFunction grad) {
		double[] x = new double[priors.size()];
		double[] delta = new double[priors.size()];
		for (int i = 0; i < x.length; i++) {
			PriorDensity pd = priors.get(i);
			x[i] = pd.getMin();
			delta[i] = (pd.getMax() - pd.getMin()) / gridPoints;
		}
		double stopV = priors.get(priors.size() - 1).getMax();
		while (x[x.length - 1] < stopV) {

			double[] nabla = grad.grad(args, priors, x, fdDelta, n);

			for (int i = 0; i < x.length; i++) {
				System.out.print(x[i] + "\t");
			}
			for (int j = 0; j < nabla.length; j++) {
				System.out.print((nabla[j]) + "\t");
			}
			System.out.println();
			x[0] += delta[0];
			// ripple counter
			for (int i = 0; i < x.length - 1; i++) {
				if (x[i] < priors.get(i).getMax())
					break;
				x[i] = priors.get(i).getMin();
				x[i + 1] += delta[i + 1];
			}
		}

	}

	private void densityGrid(String[] args, List<PriorDensity> priors, int n, int gridPoints, GradFunction grad) {

		double[] x = new double[priors.size()];

		double[] bestEst = new double[priors.size()];
		double best = Double.NEGATIVE_INFINITY;
		while (x[x.length - 1] < 1) {

			double density = grad.density(args, priors, x, n);
			if (density > best) {
				best = density;
				bestEst = transform(x, priors);
			}
			double[] t = transform(x, priors);
			for (int i = 0; i < x.length; i++) {
				System.out.print(t[i] + "\t");
			}
			System.out.println((density));

			x[0] += 1.0 / gridPoints;
			// ripple counter
			for (int i = 0; i < x.length - 1; i++) {
				if (x[i] < 1)
					break;
				x[i] = 0;
				x[i + 1] += 1 / gridPoints;
			}
		}
		System.out.println("\nBestEstimate:" + Arrays.toString(bestEst) + " @" + (best));
	}

	private double[] gradApprox(String[] args, List<PriorDensity> priors, double[] x, double delta, int n) {
		double[] xp = x.clone();
		double[] nabla = new double[x.length];
		for (int i = 0; i < x.length; i++) {
			xp[i] = x[i] + delta;
			xp[i] = Math.min(xp[i], 1);
			double realDelta = xp[i] - x[i];
			paste(args, priors, xp);// need to set n....
			double sump = 0;
			for (int j = 0; j < n; j++) {
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

	private double[] gradFull(String[] args, List<PriorDensity> priors, double[] x, double delta, int n) {
		// bwEstimation(args, priors, x, n);
		double[] xp = x.clone();
		double[] nabla = new double[x.length];
		for (int i = 0; i < xp.length; i++) {
			xp[i] = x[i] + delta;
			double lp = density(args, priors, xp, n);
			xp[i] = x[i] - delta;
			double ln = density(args, priors, xp, n);
			nabla[i] = (Math.log(lp) - Math.log(ln)) / (2 * delta);
			xp[i] = x[i];
		}
		return nabla;
	}

	/*
	 * the gradent in a single line. ie +-delta
	 */
	private double[] gradSlice(String[] args, List<PriorDensity> priors, double[] x, double[] delta, double deltaNorm, int n) {
		// generate a l esitmate for x+delta and x-delta;
		// the result is delta* (l(x+delta)-l(x-delta))/2||delta||
		double[] xp = x.clone();
		add(x, delta, xp);
		double lplus = density(args, priors, xp, n);
		sub(x, delta, xp);
		double lneg = density(args, priors, xp, n);

		double nabla = (Math.log(lplus) - Math.log(lneg)) / (2 * deltaNorm);
		mul(delta, nabla, xp);
		return xp;
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

	private double density(String[] args, List<PriorDensity> priors, double[] x, int n) {
		// esitmate the denstiy at x
		assert n > 1;

		double[][] stats = new double[n][0];

		paste(args, priors, x);// need to set n....
		for (int i = 0; i < n; i++) {
			// System.out.println("ARGS:"+Arrays.toString(args));
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
		double nfactor2 = Math.pow(n, -2 / (4.0 + data.length));// note it is
																// squared

		for (int i = 0; i < stds.length; i++) {

			stds[i] = Math.sqrt((stds[i] / n - means[i] * means[i]) * nfactor2);
		}
		this.bw = stds;

		// now stds has bw^2 for everything.
		double ksum = 0;
		double sigmaNorm = 1;
		for (int i = 0; i < n; i++) {
			double[] stat = stats[i];
			double sum = 0;
			for (int j = 0; j < data.length; j++) {
				if (bw[j] < 1e-16)
					continue;
				double d = data[j] - stat[j];
				sum += d * d / (bw[j] * bw[j]);
			}
			// System.out.println("SUM:"+sum);
			ksum += Math.exp(-.5 * sum);
		}
		// System.out.println("KSUM:" + ksum);
		double dens = ksum / (sigmaNorm * Math.pow(2 * Math.PI, data.length / 2.0));
		if (Double.isNaN(dens))
			return 0;// Double.MIN_VALUE;
		return dens;
	}

	private double densityLinear(String[] args, List<PriorDensity> priors, double[] x, int n) {
		// esitmate the denstiy at x
		assert n > 1;

		double[] stats = new double[n];

		paste(args, priors, x);// need to set n....
		for (int i = 0; i < n; i++) {
			// System.out.println("ARGS:"+Arrays.toString(args));
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

	private void bwEstimationOff(String[] args, List<PriorDensity> priors, double[] x, int n) {
		// esitmate the denstiy at x
		assert n > 1;

		double[][] stats = new double[n][0];

		paste(args, priors, x);// need to set n....
		for (int i = 0; i < n; i++) {
			// System.out.println("ARGS:"+Arrays.toString(args));
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
		double nfactor2 = Math.pow(n, -2 / (4 + data.length));// note it is
																// squared

		for (int i = 0; i < stds.length; i++) {

			stds[i] = Math.sqrt((stds[i] / n - means[i] * means[i]) * nfactor2);
		}
		this.bw = stds;
	}

	private double[] transform(double[] x, List<PriorDensity> priors) {
		double[] t = new double[x.length];
		for (int i = 0; i < x.length; i++) {
			PriorDensity pd = priors.get(i);
			pd.setLastValueUI(x[i]);
			t[i] = pd.getLastValue();
		}
		return t;
	}

	private void clamp(double[] x, List<PriorDensity> priors, double delta) {
		for (int i = 0; i < x.length; i++) {
			x[i] = Math.max(delta, x[i]);
			x[i] = Math.min(1 - delta, x[i]);
		}
	}

	private void paste(String[] args, List<PriorDensity> priors, double[] values) {
		// clamp(values, priors);
		for (int i = 0; i < priors.size(); i++) {
			PriorDensity pd = priors.get(i);
			double value = values[i];//
			pd.setLastValueUI(value);
			value = pd.getLastValue();
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
			double value = pd.getLastValue();
			if (rand)
				value = pd.next();
			args[pd.getArgIndex()] = "" + value;
		}
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
			System.out.println(Arrays.toString(data));
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
	
	@CLNames(names={"-data"})
	public void setDataFileName(String dataFileName) {
		this.dataFileName = dataFileName;
	}
	
	@CLNames(names={"-out"})
	public void setOutputFile(String outputFile) {
		this.outputFile = outputFile;
	}
	
	@CLNames(names={"-enn"})
	public void setEnn(int enn) {
		this.enn = enn;
	}
	
	@CLNames(names={"-iterations","-iter","-reps"})
	public void setIterations(int iterations) {
		this.iterations = iterations;
	}
	
	public static void main(String[] args) {
		// Matrix matrix=new Matrix(new double[] {-.75,.25,.25,.25
		// ,.25,-.75,.25,.25 ,.25,.25,-.75,.25 ,.25,.25,.25,-.75},4);
		// long time=System.nanoTime();
		// double[] d=null;
		// Matrix m=null;
		// EigenvalueDecomposition evd=matrix.eig();
		// Matrix vt=evd.getV().transpose();
		// for(int i=0;i<1000000;i++){
		// //d=evd.getRealEigenvalues();
		// m=evd.getV().times(evd.getD()).times(vt);
		// }
		// System.out.println("EIGEN:"+(System.nanoTime()-time)*1e-9);
		// m.print(m.getRowDimension(), m.getColumnDimension());
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
		public double[] grad(String[] args, List<PriorDensity> priors, double[] x, double delta, int n) {
			return gradApprox(args, priors, x, delta, n);
		}

		public double density(String[] args, List<PriorDensity> priors, double[] x, int n) {
			paste(args, priors, x);
			double sum = 0;
			for (int i = 0; i < n; i++) {
				MSLike.main(args, null, (List<? extends StatsCollector>) collectionStats, new NullPrintStream(), null);
				double[] sp = collectStatitics(collectionStats);
				sum += norm2(sp, data);
			}
			return -sum / n;
		}
	}

	public class FullGradFunction extends GradFunction {
		public double[] grad(String[] args, List<PriorDensity> priors, double[] x, double delta, int n) {
			return gradFull(args, priors, x, delta, n);
		}

		public double density(String[] args, List<PriorDensity> priors, double[] x, int n) {
			return Math.log(SGA.this.density(args, priors, x, n));
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
