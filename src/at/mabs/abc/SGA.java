package at.mabs.abc;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.NumberFormat;
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
import at.mabs.util.random.CDF;
import at.mabs.util.random.Statistics;
import at.mabs.util.random.Random64;

import cern.jet.random.StudentT;

import com.esotericsoftware.yamlbeans.YamlReader;
import com.esotericsoftware.yamlbeans.YamlWriter;

/**
 * same idea as ABC. Use a annotated command line and a few parameters to add. However output is much smaller. There is a Trace and just the output parameters.
 * File and std out are the bulk options. Still a lot of cut/paste from NewABC. Need to refactor.
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
	private double mCutoff = 5;

	private double alpha = .602;
	private double gamma = .101;
	private double c = .01;
	private int initalStarting = 5000;
	private int bestOfCount = 20;
	private double initalJumpSize = .03;
	private int bwEstimationInterval = 1;
	private boolean isDirInSphere = false;
	private double clampLength = .1;

	private double talpha = .2;
	private double balpha = .05;// 1-Math.pow(1-talpha,.1);//FIXME
	private boolean verbose;

	private int bootstrap = 0;

	private boolean writeData;// good for simulations.
	private String[] writeArgs;

	private List<StatsCollector> collectionStats = new ArrayList<StatsCollector>();
	private List<StatsCollector> dataStats = new ArrayList<StatsCollector>();
	private double[] data;

	private double[] bw;
	private double[] bwSum;
	private LinkedList<double[]> bwFifo = new LinkedList<double[]>();
	private int enn = 10;

	private double[] likelihoodPoint;

	private int iterations = 100000;
	private int minIter = 2000;
	private boolean forceOut = false;

	private int checkInterval = 1000;
	private int A = 500;
	private double aFactor = 1.5;
	private double maxSpan = .8;
	private int intervalBlockRun = 2;

	private long seed = System.nanoTime() * (1 + (new Object()).hashCode());// good for clusters

	private Random random;

	// private NumberFormat numberFormat = new
	// DecimalFormat("#.################E###");

	// private Kernal kernal = new Kernal.Gaussian();

	public void run() {
		System.out.println("Version:" + this.getClass().getPackage().getImplementationVersion() + " MSMSARGS:" + Arrays.toString(anotatedCmdLine));
		System.out.println("n=" + enn + "\titerMax:" + iterations + "\tM-Estimator? " + mEstimate);
		System.out.println("Seed:" + Long.toHexString(seed));
		random = new Random64(seed);

		List<PriorDensity> priors = PriorDensity.parseAnnotatedStrings(anotatedCmdLine);
		// System.out.println(priors);
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
				if (pd instanceof CopyPriorDensity) {
					args[pd.getArgIndex()] = "" + pd.getValue();
				} else {
					String txtValue=writeArgs[argIndex++];
					if(txtValue.equals("RND")){
						pd.generateRandom();
					}else{
						pd.setValue(Double.parseDouble(txtValue));
					}
					args[pd.getArgIndex()] = "" + pd.getValue();
				}
			}
			pasteSeed(args);
			MSLike.main(args, null, (List<? extends StatsCollector>) collectionStats, new NullPrintStream(), null);
			writeDataStats(collectionStats);
			initDataFile();
			System.out.println("DataGen With msms cmd Line:" + Arrays.toString(args));
			System.out.println("Using Param Vector:"+Arrays.toString(transform(currentPoint(priors),priors)));
			// now lets calculate the Likelihood here.
			double[] p = currentPoint(priors);
			System.out.println("Estimating BW");
			bwFifo.clear();
			bw = null;
			for (int i = 0; i < 100; i++)
				bwEstimation(args, priors, p, enn);

			double[] likes = likelihoodEstimator(args, priors, p, enn * 2, 20, true);
			System.out.println("Likelihood @ truth:" + likes[0] + "\t" + likes[1]);
			bwFifo.clear();
			// return;
		}

		initDataFile();

		if (likelihoodPoint != null) {
			System.out.println("Likelihood at point:" + Arrays.toString(likelihoodPoint));
			System.out.println("Calulating BW");
			double[] tlp = itransform(likelihoodPoint, priors);
			for (int i = 0; i < 100; i++)
				bwEstimation(args, priors, tlp, enn);
			double[] lh = likelihoodEstimator(args, priors, tlp, enn * 2, 20, true);
			System.out.println("loglikelihood(std):" + lh[0] + "\t" + lh[1]);
			return;
		}

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
			if (i % (1 + startingPoints.length / 80) == 0)
				System.out.print("*");
		}
		System.out.println();
		Arrays.sort(startingPoints);
		// now cut it down to the top 100.
		bestOfCount = Math.min(bestOfCount, initalStarting);
		PointDensity[] betterStarts = new PointDensity[bestOfCount];
		System.arraycopy(startingPoints, 0, betterStarts, 0, betterStarts.length);
		if (betterStarts.length > 0)
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

			double ns = simple.density(args, priors, np, 10, false);
			System.out.println("Old" + betterStarts[i].score + "\tnew:" + ns + "\t@" + Arrays.toString(transform(betterStarts[i].point, priors)) + "\t" + Arrays.toString(transform(np, priors)));
			preconditioned.add(new PointDensity(np, ns));

		}
		Collections.sort(preconditioned);
		System.out.println("Best starting points and paramters:");
		for (PointDensity pd : preconditioned) {
			System.out.println(pd.score + " @ " + Arrays.toString(transform(pd.point, priors)));
		}
		System.out.println();
		// System.exit(0);
		if (!preconditioned.isEmpty())
			clearTrace();
		try {

			TreeSet<ResultEntry> found = new TreeSet<ResultEntry>();
			for (PointDensity pd : preconditioned) {
				double[] np = genericKWAlgo(args, priors, pd.point, enn, iterations, minIter, better);

				if (np == null)
					continue;

				double[] likes = likelihoodEstimator(args, priors, np, enn * 2, 20, true);
				np = transform(np, priors);
				double mll = likes[0];
				double stdll = likes[1];
				System.out.println("## " + mll + " (" + stdll + ")" + Arrays.toString(np));
				ResultEntry result = new ResultEntry(likes.clone(), np.clone());
				found.add(result);

				// now how many are significant?
				ResultEntry bestSoFar = found.first();
				int bestCount = 0;
				for (ResultEntry re : found) {
					if (Statistics.welchTestBigger(bestSoFar.getLikelihood(), re.getLikelihood(), 20, .05)) {
						break;
					}
					bestCount++;
				}

				Writer writer = new FileWriter(outputFile);

				for (ResultEntry re : found) {
					writer.write(re.toString() + "\n");
				}
				// writer.write("" + bestCount + "\n");
				writer.flush();
				writer.close();

			}
			// now for parametric bootstraps. The data is simulated, we start
			// from the best point each time.
			// first let see if we have a best point to start from. If not. try to read the found file!
			if (found.isEmpty()) {
				BufferedReader reader = new BufferedReader(new FileReader(outputFile));
				String line = reader.readLine();
				while (line != null) {
					System.out.println("Restarting bootstraps: Reading data line:" + line);
					found.add(new ResultEntry(line));
					line = reader.readLine();
				}
				reader.close();
			}

			TreeSet<ResultEntry> boots = new TreeSet<ResultEntry>();
			for (int i = 0; i < bootstrap; i++) {
				ResultEntry re = found.first();
				double[] point = itransform(re.parameters, priors);
				System.out.println("BSPoint:" + Arrays.toString(point));
				paste(args, priors, point);
				// this should give us new stats for the data!
				MSLike.main(args, null, (List<? extends StatsCollector>) dataStats, new NullPrintStream(), null);
				initDataStatsCollectionStats(); // puts the data stats into
												// data[]

				double[] np = genericKWAlgo(args, priors, point, enn, iterations, minIter, better);
				double[] likes = likelihoodEstimator(args, priors, np, enn * 2, 20, true);
				double[] bpoint = transform(np, priors);
				ResultEntry bentry = new ResultEntry(likes, bpoint);
				boots.add(bentry);

				Writer writer = new FileWriter(outputFile + ".bootstrap");

				for (ResultEntry e : boots) {
					writer.write(e.toString() + "\n");
				}
				writer.flush();
				writer.close();
			}

		} catch (IOException e) {

			e.printStackTrace();
		}
	}

	private double[] likelihoodEstimator(String[] args, List<PriorDensity> priors, double[] p, int n, int reps, boolean log) {
		double loglike = 0;
		double loglike2 = 0;
		for (int i = 0; i < reps; i++) {
			double ll = density(args, priors, p, n, log, false);
			loglike += ll;
			loglike2 += ll * ll;
		}
		double mll = loglike / reps;
		double stdll = Math.sqrt(loglike2 / (reps - 1) - ((double) reps / (reps - 1)) * mll * mll);
		return new double[] { mll, stdll };
	}

	private double[] randomPoint(List<PriorDensity> priors) {
		do {
			for (PriorDensity pd : priors) {
				if (pd instanceof CopyPriorDensity)
					continue;
				pd.setValueUI(random.nextDouble());
			}
		} while (isInvalid(priors));

		double[] p = new double[priors.size()];
		for (int i = 0; i < p.length; i++)
			p[i] = priors.get(i).getValueUI();
		return p;
	}

	private boolean isInvalid(List<PriorDensity> priors) {
		for (PriorDensity pd : priors) {
			if (!pd.isValid())
				return true;
		}
		return false;
	}

	private double[] currentPoint(List<PriorDensity> priors) {
		double[] p = new double[priors.size()];
		for (int i = 0; i < p.length; i++) {
			p[i] = priors.get(i).getValueUI();
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
	private double[] genericKWAlgo(String[] args, List<PriorDensity> priors, double[] start, int n, int maxK, int minK, GradFunction grad) {
		System.out.println("\ninit starting parameters:" + Arrays.toString(transform(start, priors)));

		System.out.println("KWMethod with alpha:" + alpha + "\tand gamma:" + gamma);

		bwFifo.clear();
		bw = null;
		for (int i = 0; i < 100; i++)
			bwEstimation(args, priors, start, n);

		System.out.println("c:" + c);
		// c = Math.max(.001, c);
		// c = Math.min(.01, c);

		// int A = 500;// maxK / 100;// maxK / 10;
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
		if (c > 1 || containsNaNInf(nabla)) {
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
		List<double[]> trace2 = new ArrayList<double[]>();
		double[] x = start.clone();
		double[] lastChecked = x.clone();
		double[] last = x.clone();

		int notBetterCount = 0;
		int[] biasCount = new int[x.length];
		trace.add(transform(x, priors));
		trace2.add(transform(x, priors));

		//boolean degen = false;

		LinkedList<double[]> fifoParams = new LinkedList<double[]>();
		double[] paramSums = x.clone();
		fifoParams.addFirst(x.clone());

		boolean newTrace = true;

		double[] a_k = new double[a.length];
		int k = 0;
		for (; (k < maxK && notBetterCount < intervalBlockRun) || k < minK; k++) {
			for (int i = 0; i < a.length; i++) {
				a_k[i] = a[i] / Math.pow(k + A + 1, alpha);
			}
			double c_k = c / Math.pow(k + 1, gamma);
			//
			if (k % bwEstimationInterval == 0) {
				// bwEstimation(args, priors, x, n);
			}
			nabla = grad.grad(args, priors, x, c_k, n, true);
			if (k % 10 == 0) {
				System.out.println("BW:" + Arrays.toString(bw));
				// for(double[] e:bwFifo){
				// System.out.println(Arrays.toString(e));
				// }
				System.out.println("k, a_k & c_k\t" + k + "\t" + Arrays.toString(a_k) + "\t" + c_k + "\nCurrentP:\t" + Arrays.toString(transform(x, priors)) + "\nCurrentNab\t"
						+ Arrays.toString(nabla) + "\nEsimatedDensity:" + grad.density(args, priors, x, enn, true) + "\n");
			}
			if (containsNaNInf(x)) {
				return null;//probably a bug!
			}
			if (containsNaNInf(nabla)) {
				x = last.clone();
				continue;
				
			} 
			last = x.clone();
			mul(nabla, a_k, nabla);
			clampLength(nabla, clampLength);
			add(x, nabla, x);
			clamp(x, priors, c_k);
			set(x,priors);
			if(isInvalid(priors)){//bugger!
				x=last.clone();
				continue;
			}
			trace.add(transform(x, priors));

			double[] movingAverageEstimate = new double[x.length];
			mul(paramSums, 1.0 / fifoParams.size(), movingAverageEstimate);
			trace2.add(transform(movingAverageEstimate, priors));

			// if (k % 10 == 0)
			// System.out.println(Arrays.toString(transform(x, priors)));
			if (k % checkInterval == 0 && k != 0) {

				// mul(paramSums, 1.0 / fifoParams.size(), movingAverageEstimate);
				boolean better = ttestIsBetter(args, priors, movingAverageEstimate, lastChecked, n, grad, talpha);
				lastChecked = movingAverageEstimate;
				System.out.println("Better:" + better);
				if (!better) {
					notBetterCount++;
				} else {
					notBetterCount = 0;
				}
				boolean ajump = false;
				for (int p = 0; p < x.length; p++) {
					if (Statistics.timeSeriesBiasTest(fifoParams, p, balpha)) {
						biasCount[p]++;
						notBetterCount = 0;// we never stop when there is bias.
						System.out.println("%% bias in parameter:" + p);
					} else {
						biasCount[p] = 0;
					}
					if (biasCount[p] > 0) {
						a[p] *= aFactor;
						System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@ Incressing A in param:" + p);
						ajump = true;
						biasCount[p] = 0;
					}
				}

				// if(worseCount>1 && !ajump){
				// check max deflections.
				double[] range = maxRange(fifoParams);
				for (int p = 0; p < range.length; p++) {
					if (range[p] > maxSpan && biasCount[p] == 0) {
						a[p] /= aFactor;
						System.out.println("!!!!!!!!!!!!!!!!!!!!!!!! Decressing A in param:" + p + "\t" + range[p]);
						ajump = true;
					}
				}
				// }
				if (ajump) {
					notBetterCount = 0;
				}

				saveTrace(trace, ".traces", newTrace);
				saveTrace(trace2, ".mave", newTrace);
				newTrace = false;
				trace.clear();
				trace2.clear();
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
		saveTrace(trace, ".traces", newTrace);
		saveTrace(trace2, ".mave", newTrace);
		// if (ttestIsBetter(args, priors, paramSums, start, n, grad))
		if (k == maxK && !forceOut) {
			return null;
		}
		return paramSums;
		// return null;

	}

	private double[] maxRange(Collection<double[]> data) {
		Iterator<double[]> iter = data.iterator();
		double[] vec = iter.next();
		double[] min = vec.clone();
		double[] max = vec.clone();
		while (iter.hasNext()) {
			vec = iter.next();
			for (int i = 0; i < vec.length; i++) {
				min[i] = Math.min(min[i], vec[i]);
				max[i] = Math.max(max[i], vec[i]);
			}
		}
		sub(max, min, max);
		return max;
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

	private boolean containsNaNInf(double[] nabla) {
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
			write = new FileWriter(outputFile + ".mave", false);
			write.close();
		} catch (IOException e) {

			e.printStackTrace();
		}
	}

	private void saveTrace(List<double[]> trace, String suffix, boolean newTrace) {
		if (trace.isEmpty())
			return;
		try {
			Writer write = new FileWriter(outputFile + suffix, true);
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

	// Welch's t test
	private boolean ttestIsBetter(String[] args, List<PriorDensity> priors, double[] x, double[] y, int n, GradFunction grad, double alpha) {
		int k = 25;
		double[] meanStdx = likelihoodEstimator(args, priors, x, n, k, true);
		double[] meanStdy = likelihoodEstimator(args, priors, y, n, k, true);
		System.out.println("TTest:\n\t" + Arrays.toString(x) + "\n\t" + Arrays.toString(y));
		return Statistics.welchTestBigger(meanStdx, meanStdy, k, alpha);
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
			double lp = density(args, priors, xp, n, log, false);
			xp[i] = x[i] - delta;
			double ln = density(args, priors, xp, n, log, false);
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
		bwUpdate();
		// System.out.println("POINT:"+Arrays.toString(x));
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
		double da = density(args, priors, rv1, n, log, true);
		double db = density(args, priors, rv2, n, log, true);
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
		// System.out.println("WhereNaN:"+g+"\t"+l2+"\tda "+da+"\t"+db+"\t"+Arrays.toString(rv1)+"\t"+Arrays.toString(rv2));
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

	private double density(String[] args, List<PriorDensity> priors, double[] x, int n, boolean log, boolean bwStat) {
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
		// if (bwStat)
		bwEstimation(stats, false);

		double ksum = 0;

		double sigmaNorm = 1;
		for (int j = 0; j < data.length; j++) {
			if (bw[j] > 1e-6 && !Double.isNaN(bw[j])) {
				sigmaNorm *= bw[j];
			}
		}

		double bestPoint = Double.MAX_VALUE;
		for (int i = 0; i < n; i++) {
			double[] stat = stats[i];
			double sum = 0;

			for (int j = 0; j < data.length; j++) {
				double d = (data[j] - stat[j]) / bw[j];
				if (bw[j] < 1e-6 || Double.isNaN(bw[j]))
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
		// System.out.println("BP:"+bestPoint);
		if (Double.isNaN(dens) || Double.isInfinite(dens) || dens <= 0) {
			System.out.print("*");// ****DEGEN****:"+dens+"\t"+Math.exp(-.5*bestPoint)+"\t"+bestPoint+"\n");
			// System.exit(-1);
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
		// System.out.println(Arrays.toString(args)+"\t"+Arrays.toString(x));
		for (int i = 0; i < n; i++) {
			// System.out.println("ARGS:"+Arrays.toString(args));
			pasteSeed(args);
			MSLike.main(args, null, (List<? extends StatsCollector>) collectionStats, new NullPrintStream(), null);
			stats[i] = collectStatitics(collectionStats);
			// System.out.println("CollectedStats:"+Arrays.toString(stats[i])+"\t"+Arrays.toString(args));

		}
		bwEstimation(stats, true);
	}

	private void bwEstimation(double[][] stats, boolean update) {
		// esitmate the denstiy at x
		int n = stats.length;
		double[] stds = new double[stats[0].length];
		double[] means = new double[stds.length];
		for (int i = 0; i < n; i++) {
			double[] stat = stats[i];
			// System.out.println(stat[20]);
			for (int j = 0; j < means.length; j++) {
				assert !Double.isNaN(data[j]) && !Double.isNaN(stat[j]) : "NaNs!" + data[j] + "\t" + stat[j] + "\t" + j;
				double d = data[j] - stat[j];
				stds[j] += d * d;
				means[j] += d / n;
			}
		}
		double nfactor2 = Math.pow(4.0 / (2 + data.length), 2.0 / (4 + data.length)) * Math.pow(n, -2 / (4.0 + data.length));
		for (int i = 0; i < stds.length; i++) {
			if ((stds[i] / n) - (means[i] * means[i]) < 0) {
				stds[i] = Math.sqrt(means[i]);// assume a poisson.
				// System.err.println("Std hack:"+means[i]);
			} else {
				stds[i] = Math.sqrt(((stds[i] / (n - 1)) - n * means[i] * means[i] / (n - 1)) * nfactor2);
			}

		}

		// add new bw
		bwFifo.addFirst(stds);
		if (bwSum == null || bw == null) {
			bwSum = stds.clone();
			bw = stds.clone();
		} else {
			add(stds, bwSum, bwSum);
		}
		if (bwFifo.size() > 2000) {
			double[] removed = bwFifo.removeLast();
			sub(bwSum, removed, bwSum);
		}
		// bw=stds;
		if (update)
			bwUpdate();
		// now do the moving average...
		// System.out.println("BW:" +
		// Arrays.toString(bw)+"\nNW:"+Arrays.toString(stds));

	}

	private void bwUpdate() {
		mul(bwSum, 1.0 / bwFifo.size(), bw);
	}

	private double[] transform(double[] x, List<PriorDensity> priors) {
		double[] t = new double[x.length];
		for (int i = 0; i < x.length; i++) {
			PriorDensity pd = priors.get(i);
			pd.setValueUI(x[i]);
			t[i] = pd.getValue();
		}
		return t;
	}

	private double[] itransform(double[] x, List<PriorDensity> priors) {
		double[] t = new double[x.length];
		for (int i = 0; i < x.length; i++) {
			PriorDensity pd = priors.get(i);
			pd.setValue(x[i]);
			t[i] = pd.getValueUI();
		}
		// System.out.println("itran: "+Arrays.toString(x)+"\t"+Arrays.toString(t));
		return t;// checking
	}

	private void clamp(double[] x, List<PriorDensity> priors, double delta) {
		for (int i = 0; i < x.length; i++) {
			x[i] = Math.max(delta, x[i]);
			x[i] = Math.min(1 - delta, x[i]);
		}
	}

	private void set(double[] x, List<PriorDensity> priors) {
		for (int i = 0; i < x.length; i++) {
			PriorDensity pd=priors.get(i);
			if(pd instanceof CopyPriorDensity)
				continue;
			pd.setValueUI(x[i]);
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
			pd.setValueUI(value);
			value = pd.getValue();
			if (pd.isInteger()) {
				args[pd.getArgIndex()] = Integer.toString((int) value);
			} else {
				args[pd.getArgIndex()] = "" + value;
			}
			// System.out.println("ARGS:" + Arrays.toString(args));
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
			if (rand && !(pd instanceof CopyPriorDensity)) {
				pd.generateRandom();
			}
			double value = pd.getValue();
			if (pd.isInteger()) {
				args[pd.getArgIndex()] = Integer.toString((int) value);
			} else {
				args[pd.getArgIndex()] = "" + value;
			}

			// System.out.println("ARGS:" + Arrays.toString(args));
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
			// System.out.println(Arrays.toString(msmsArgs));
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
			initDataStatsCollectionStats();
		} catch (Exception e) {

			e.printStackTrace();
		}
	}

	private void initDataStatsCollectionStats() {

		Collections.sort(dataStats, new ClassNameOrder());
		Collections.sort(collectionStats, new ClassNameOrder());
		// special case when stats are not set, use all in the data..
		if (collectionStats.isEmpty()) {
			for (StatsCollector sc : dataStats) {
				collectionStats.add(sc);// don't clone because we only read the dataStats once below.
			}
		}

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

	@CLNames(names = { "-LHP", "-LHPoint" })
	@CLDescription("Calulated the kernal likelihood at this point, then exit. Note this needs a full -msms argument and other parameters are used for this. ie BW and enn, also note that all % params must be specified.")
	public void setLHPoint(double[] point) {
		likelihoodPoint = point;
	}

	@CLNames(names = { "-abcstat", "-STAT" })
	@CLDescription("Deprecated. Do not use.")
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
	@CLDescription("Simulated a data set with these parameters. WARNING overwrites the date file.")
	public void setWriteDataTrue(String[] writeArgs) {
		this.writeData = true;
		this.writeArgs = writeArgs;
	}

	@CLNames(names = { "-data" })
	public void setDataFileName(String dataFileName) {
		this.dataFileName = dataFileName;
	}

	@CLNames(names = { "-out", "-o" })
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

	@CLNames(names = { "-minIter", "-mi", "-minReps" })
	public void setMinIter(int iterations) {
		this.minIter = iterations;
	}

	@CLNames(names = { "-bootstrap", "-bs" })
	public void setBootstrap(int reps) {
		this.bootstrap = reps;
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

	@CLNames(names = { "-mestCutoff", "-mescutoff", "-mcutoff" })
	@CLDescription("Sets the \\delta/h cutoff value for the M Estinator kernel function")
	public void setmCutoff(double mCutoff) {
		this.mCutoff = mCutoff;
	}

	@CLNames(names = { "-alpha" })
	public void setAlpha(double alpha) {
		this.alpha = alpha;
	}

	@CLNames(names = { "-balpha" })
	public void setBAlpha(double alpha) {
		this.balpha = alpha;
	}

	@CLNames(names = { "-gamma" })
	public void setGamma(double gamma) {
		this.gamma = gamma;
	}

	@CLNames(names = { "-c" })
	public void setC(double c) {
		this.c = c;
	}

	@CLNames(names = { "-initalRandomStarts", "-irs" })
	@CLDescription("How many random points to rank for starting positions")
	public void setInitalStarting(int initalStarting) {
		this.initalStarting = initalStarting;
	}

	@CLNames(names = { "-bestOfKeep", "-keep", "-k" })
	@CLDescription("From all the random starting positions, how many to use starting from the best match")
	public void setBestOfCount(int bestOfCount) {
		this.bestOfCount = bestOfCount;
	}

	@CLNames(names = { "-initJumpSize", "-ijs" })
	@CLDescription("How far do we want the first a_k to go in the first interation?")
	public void setInitalJumpSize(double initalJumpSize) {
		this.initalJumpSize = initalJumpSize;
	}

	@CLNames(names = { "-bwInterval", "-bwi" })
	@CLDescription("iteration count between BW estimation. DEPRECATED")
	public void setBwEstimationInterval(int bwEstimationInterval) {
		this.bwEstimationInterval = bwEstimationInterval;
	}

	@CLNames(names = { "-sphere", "-sp" })
	@CLDescription("Generate the slice as a random direction in the sphere")
	public void setDirInSphereTrue() {
		this.isDirInSphere = true;
	}

	@CLNames(names = { "-clampLength", "-cl" })
	@CLDescription("length (hypercube) to clamp movement too. Prevents rare but expensive excursions")
	public void setClampLength(double clampLength) {
		this.clampLength = clampLength;
	}

	@CLNames(names = { "-talpha", "-ta" })
	public void setTalpha(double talpha) {
		this.talpha = talpha;
	}

	@CLNames(names = { "-forceOut" })
	@CLDescription("Forces the output at last iteration regardless of convergance stats. NOT RECOMENDED.")
	public void setForceOutTrue() {
		this.forceOut = true;
	}

	// private int checkInterval=1000;
	@CLNames(names = { "-checkInterval", "-ci" })
	@CLDescription("Iterations between convergance tests. default=1000")
	public void setCheckInterval(int checkInterval) {
		this.checkInterval = checkInterval;
	}

	// private int A=500;
	@CLNames(names = { "-A", "-capA" })
	@CLDescription("The A parameter in the KW tuning method. Default=500")
	public void setA(int a) {
		A = a;
	}

	// private double aFactor=1.5;
	@CLNames(names = { "-aFactor", "-af" })
	@CLDescription("when a parameter is biased, adjust the a coeffecient by this factor. default=1.5")
	public void setaFactor(double aFactor) {
		this.aFactor = aFactor;
	}

	// private double maxSpan=.8;
	@CLNames(names = { "-maxSpan", "-mSpan" })
	@CLDescription("the max permissible span of a parameter within an interval check before reducing the a factor. default=.8")
	public void setMaxSpan(double maxSpan) {
		this.maxSpan = maxSpan;
	}

	// private int intervalBlockRun=2;
	@CLNames(names = { "-flatInterval", "-fi" })
	@CLDescription("How many not better and not biased \"intervals\" before we stop. default=2")
	public void setIntervalBlockRun(int intervalBlockRun) {
		this.intervalBlockRun = intervalBlockRun;
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
			return SGA.this.density(args, priors, x, n, log, false);
		}
	}

	public class FastGradFunction extends GradFunction {
		public double[] grad(String[] args, List<PriorDensity> priors, double[] x, double delta, int n, boolean log) {
			// System.out.println("FAST");
			return gradFast(args, priors, x, delta, n, log);
		}

		public double density(String[] args, List<PriorDensity> priors, double[] x, int n, boolean log) {
			return (SGA.this.density(args, priors, x, n, log, false));
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

	private static class ResultEntry implements Comparable<ResultEntry> {
		private final double[] likelihood;
		private final double[] parameters;

		public ResultEntry(double[] like, double[] p) {
			likelihood = like;
			parameters = p;
			// System.out.println("ADDEDPARAMS:"+Arrays.toString(p));
		}

		public ResultEntry(String s) {
			StringTokenizer st = new StringTokenizer(s);
			likelihood = new double[2];
			likelihood[0] = Double.parseDouble(st.nextToken());
			likelihood[1] = Double.parseDouble(st.nextToken());
			ArrayList<Double> parms = new ArrayList<Double>();
			while (st.hasMoreTokens()) {
				parms.add(Double.parseDouble(st.nextToken()));
			}
			parameters = Util.toArrayPrimitiveDouble(parms);
		}

		public double[] getLikelihood() {
			return likelihood;
		}

		public double[] getParameters() {
			return parameters;
		}

		@Override
		public int compareTo(ResultEntry o) {
			if (likelihood[0] > o.likelihood[0])
				return -1;
			if (likelihood[0] < o.likelihood[0])
				return 1;
			return 0;
		}

		@Override
		public String toString() {

			String out = likelihood[0] + "\t" + likelihood[1] + "\t";
			for (double p : parameters) {
				out += p + "\t";
			}
			return out.trim();
		}
	}
}
