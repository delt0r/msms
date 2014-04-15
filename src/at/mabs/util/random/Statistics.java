package at.mabs.util.random;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;

import cern.jet.random.StudentT;

/**
 * critical value via numerical root finding. Need to use a few wrappers for the colt lib.
 * 
 * @author bob
 * 
 */
public class Statistics {
	private CDF cdf;
	private double eps = 1e-12;

	public Statistics(CDF cdf) {
		this.cdf = cdf;
	}

	public double critcalValue(double p) {
		assert p > eps && p < 1 - eps : 1 - eps;
		// find a parameter t such that cdf(t)=p.
		// crude bisection method. won't matter since this will almost never be
		// in a critial part of the code.
		// expect ~53 iterations for full double prescion. aka 53 bits.

		// first find a bracket. Start at -1,1 and double one untill we find it.
		double low = -1;
		double high = 1;
		double vlow = cdf.cdf(low) - p;
		double vhigh = cdf.cdf(high) - p;
		while ((vlow < 0) && (vhigh < 0) && high < Double.MAX_VALUE) {
			high *= 2;
			vhigh = cdf.cdf(high) - p;

		}
		while ((vlow > 0) && (vhigh > 0) && low > -Double.MAX_VALUE) {
			low *= 2;
			vlow = cdf.cdf(low) - p;

		}
		// System.out.println("Bracket:"+low+"\t"+high+"\t"+vhigh+"\t"+vlow);
		// now we have a bracket.
		while ((high - low) > eps) {
			double half = (high + low) / 2;
			if (half == low || half == high)
				break;
			double vhalf = cdf.cdf(half) - p;
			if (vhalf < 0) {
				low = half;
			} else {
				high = half;
			}

			// System.out.println("New Bracket "+low+"\t"+high+"\t"+(high-low));
		}

		return (high + low) / 2;
	}

	public static boolean timeSeriesBiasTest(Collection<double[]> timeData, int columb, double alpha) {
		double sum = 0;
		double sum2 = 0;
		int count = 0;
		Iterator<double[]> iter = timeData.iterator();
		double value = iter.next()[columb];
		while (iter.hasNext()) {
			double nextValue = iter.next()[columb];
			double delta = value - nextValue;
			sum += delta;
			sum2 += delta * delta;
			count++;
			value = nextValue;
		}

		double mean = sum / count;
		double var = sum2 / (count - 1) - mean * mean * count / (count - 1);
		double dof = count - 1;
		double t = mean / Math.sqrt(var / count);
		// System.out.println("##$$@@##$$ T stat:" + t);
		Statistics cv = new Statistics(new CDF.StudentTcdf(new StudentT(dof, null)));
		double low = cv.critcalValue(alpha / 2);
		double hi = cv.critcalValue(1 - alpha / 2);
		// System.out.println("##$$@@##$$ T Crit:" + low+"\t"+hi);
		return t <= low || t >= hi;
	}

	public static boolean welchTestBigger(double[] x, double[] y, int N, double alpha) {
		return welchTestBigger(x[0], x[1], y[0], y[1], N, alpha);
	}

	public static boolean welchTestBigger(double xbar, double xstd, double ybar, double ystd, int N, double alpha) {
		double norm2 = (xstd * xstd + ystd * ystd) / N;
		double t = (xbar - ybar) / Math.sqrt(norm2);

		double dof = norm2 * norm2;
		double denominator = xstd * xstd * xstd * xstd + ystd * ystd * ystd * ystd;
		denominator /= N * N * (N - 1);
		dof /= denominator;
		Statistics cv = new Statistics(new CDF.StudentTcdf(new StudentT(dof, null)));
		double c = cv.critcalValue(1 - alpha);
		// System.out.println("Ttest:" + t + "\t" + meanStdx[0] + "\t" +
		// meanStdy[0] + "\tdof:" + dof + "\tcrit:" + c);
		return t >= c;
	}

	public static boolean welchTestDifferent(double xbar, double xstd, double ybar, double ystd, int N, double alpha) {
		double norm2 = (xstd * xstd + ystd * ystd) / N;
		double t = (xbar - ybar) / Math.sqrt(norm2);

		double dof = norm2 * norm2;
		double denominator = xstd * xstd * xstd * xstd + ystd * ystd * ystd * ystd;
		denominator /= N * N * (N - 1);
		dof /= denominator;
		Statistics cv = new Statistics(new CDF.StudentTcdf(new StudentT(dof, null)));
		double hi = cv.critcalValue(1 - alpha / 2);
		double lo = cv.critcalValue(alpha / 2);
		// System.out.println("Ttest:" + t + "\t" + xbar + "\t" + ybar + "\tdof:" + dof + "\tcrit:" + t + "\t" + lo + " -> " + hi);
		return t <= lo || t >= hi;
	}

	/**
	 * 
	 * @param x
	 * @param y
	 * @param N
	 * @param sig
	 *            non corrected for mulitple tests.
	 * @return
	 */
	public static boolean manyWelchTestBigger(double[] x, double[] y, int N, double alpha) {
		assert x.length == y.length;
		int testCount = x.length / 2;
		alpha = 1 - Math.pow(1 - alpha, 1.0 / testCount);
		for (int i = 0; i < x.length; i += 2) {
			if (welchTestBigger(x[i], x[i + 1], y[i], y[i + 1], N, alpha)) {
				return true;
			}
		}
		return false;
	}

	public static boolean manyWelchTestDifferent(double[] x, double[] y, int N, double alpha) {
		assert x.length == y.length;
		int testCount = x.length / 2;
		alpha = 1 - Math.pow(1 - alpha, 1.0 / testCount);
		for (int i = 0; i < x.length; i += 2) {
			if (welchTestDifferent(x[i], x[i + 1], y[i], y[i + 1], N, alpha)) {
				// System.out.println("NullFail:" + x[i] + "\t" + x[i + 1] + "\t" + y[i] + "\t" + y[i + 1] + "\t" + i);
				return true;
			}
		}
		return false;
	}

	public double getEps() {
		return eps;
	}

	public void setEps(double eps) {
		assert eps > 0;
		this.eps = eps;
	}

	public static double[] meanStd(double[] data) {
		double sum = 0;
		double sum2 = 0;
		for (int i = 0; i < data.length; i++) {
			sum += data[i];
			sum2 += data[i] * data[i];
		}
		int n = data.length;
		double mean = sum / n;
		double var = sum2 / (n - 1) - n * mean * mean / (n - 1);
		return new double[] { mean, Math.sqrt(var) };
	}

	/**
	 * for each param returns the mean and stds.
	 * 
	 * @param data
	 * @return mean and stds of each
	 */
	public static double[] meanStd(Collection<double[]> data) {
		Iterator<double[]> iterator = data.iterator();
		double[] element = iterator.next();
		double[] sum1sum2 = new double[element.length * 2];
		int counter = 1;
		for (int i = 0; i < element.length; i++) {
			sum1sum2[i * 2] += element[i];
			sum1sum2[i * 2 + 1] += element[i] * element[i];
		}
		while (iterator.hasNext()) {
			element = iterator.next();
			for (int i = 0; i < element.length; i++) {
				sum1sum2[i * 2] += element[i];
				sum1sum2[i * 2 + 1] += element[i] * element[i];
			}
			counter++;
		}
		for (int i = 0; i < element.length; i++) {
			double mean = sum1sum2[i * 2] / counter;
			double var = sum1sum2[i * 2 + 1] / (counter - 1) - counter * mean * mean / (counter - 1);
			sum1sum2[i * 2] = mean;
			sum1sum2[i * 2 + 1] = Math.sqrt(var);
		}
		return sum1sum2;
	}

	public static void main(String[] args) {
		double alpha=0.025;
		for (int dof = 1; dof < 400; dof++) {
			Statistics cv = new Statistics(new CDF.StudentTcdf(new StudentT(dof, null)));
			double hi = cv.critcalValue(1 - alpha / 2);
			double lo = cv.critcalValue(alpha / 2);
			System.out.println(dof+"\t"+hi+"\t"+lo);
		}
	}

}
