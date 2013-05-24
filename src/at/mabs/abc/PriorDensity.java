package at.mabs.abc;

import java.util.Random;
import java.util.StringTokenizer;

import at.mabs.util.random.Random64;

/**
 * parses and generates a random variable from a prior.
 * 
 * Could combine this with proposal distribution.
 * 
 * Using for SGA as well.
 * 
 * 
 * @author bob
 * 
 */
public class PriorDensity {
	private boolean log;
	private double min, max, span;
	private double logParam;
	private double proposialWindow = 0.1;
	private Random random = new Random64();// really random...and 100% thread
											// safe.
	private int argIndex = -1;
	private double value;
	private int transform = 0;
	private double factor=1;
	private boolean integer=false;
	private static final int ONE_OVER = 1;

	protected PriorDensity() {

	}

	/**
	 * String format: start%end[%lg]
	 * 
	 * this is for rejection sampling.
	 * 
	 * @param args
	 */
	public PriorDensity(String args, int argIndex) {
		StringTokenizer tokens = new StringTokenizer(args, "%");
		min = Double.parseDouble(tokens.nextToken());
		max = Double.parseDouble(tokens.nextToken());
		if (tokens.hasMoreTokens()) {
			String last = tokens.nextToken();
			if (last.startsWith("lg")) {
				log = true;
			} else if (last.startsWith("/")) {
				transform=ONE_OVER;
				if(tokens.hasMoreTokens()){
					factor=Double.parseDouble(tokens.nextToken());
				}
			} else if(last.startsWith("i")){
				integer=true;
			}else {
				proposialWindow = Double.parseDouble(last);
			}
		}
		span = max - min;
		logParam = Math.exp(-span);
		this.argIndex = argIndex;
	}

	public void updateMinMax(double min, double max) {
		this.min = min;
		this.max = max;
		span = max - min;
		logParam = Math.exp(-span);
	}

	public void generateRandom() {
		double u = random.nextDouble();
		if (!log) {
			value = u * span + min;

		} else {
			u = u * (1 - logParam) + logParam; // U[exp(-span),1]
			value = min - Math.log(u);

		}
		if(integer){
			value=Math.rint(value);
		}
	}

	// propose a value, does the clamping.
	public double nextProp(double v) {
		assert !log;
		double nmin = Math.max(v - span * proposialWindow, min);
		double nmax = Math.min(v + span * proposialWindow, max);
		value = random.nextDouble() * (nmax - nmin) + nmin;
		// System.out.println("Parameter:"+lastValue);
		if(integer){
			value=Math.rint(value);
		}
		return value;
	}

	public double getMax() {
		return max;
	}

	public double getMin() {
		return min;
	}

	public int getArgIndex() {
		return argIndex;
	}

	public double getValue() {
		return value;
	}

	public double getTransformedValue() {
		switch (transform) {
		case ONE_OVER:
			return factor / value;
		default:
			return value;
		}
	}

	public void setValue(double lastValue) {
		lastValue = Math.min(lastValue, max);
		lastValue = Math.max(lastValue, min);
		this.value = lastValue;
		if(integer){
			value=Math.rint(value);
		}
	}

	public double getLastValueUI() {
		if(max-min<1e-15)
			return min;
		return (value - min) / (max - min);
	}

	public void setLastValueUI(double uiValue) {
		double lv = min + (max - min) * uiValue;
		setValue(lv);
	}
	
	public boolean isInteger() {
		return integer;
	}
}
