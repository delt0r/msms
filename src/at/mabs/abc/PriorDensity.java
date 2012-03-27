package at.mabs.abc;

import java.util.Random;
import java.util.StringTokenizer;

import at.mabs.util.random.Random64;

/**
 * parses and generates a random variable from a prior. Just for rejection
 * sampling.
 * 
 * Could combine this with proposal distribution.
 * 
 * Using for SGA as well. However it is an error to use anything but uniform ranges.
 * 
 * 
 * @author bob
 * 
 */
public class PriorDensity {
	private boolean log;
	private double min, max, span;
	private double logParam;
	private double prop=0.1;
	private Random random = new Random64();// really random...and 100% thread
											// safe.
	private int argIndex = -1;
	private double lastValue;

	protected PriorDensity() {
		// TODO Auto-generated constructor stub
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
			String last=tokens.nextToken();
			if (last.startsWith("lg")) {
				log = true;
			}else{
				prop=Double.parseDouble(last);
			}
		}
		span = max - min;
		logParam = Math.exp(-span);
		this.argIndex = argIndex;
	}

	public void updateMinMax(double min,double max){
		this.min=min;
		this.max=max;
		span = max - min;
		logParam = Math.exp(-span);
	}
	
	public double next() {
		double u = random.nextDouble();
		if (!log) {
			lastValue = u * span + min;
			return lastValue;
		}
		u = u * (1 - logParam) + logParam; // U[exp(-span),1]
		lastValue = min - Math.log(u);
		//System.out.println("Parameter:"+lastValue);
		return lastValue;

	}

	public double nextProp(double v) {
		assert !log;
		double nmin=Math.max(v-span*prop, min);
		double nmax=Math.min(v+span*prop, max);
		lastValue=random.nextDouble()*(nmax-nmin)+nmin;
		//System.out.println("Parameter:"+lastValue);
		return lastValue;
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

	public double getLastValue() {
		return lastValue;
	}
	
	public double getLastValueUI(){
		return (lastValue-min)/(max-min);
	}
	
	public void setLastValue(double lastValue) {
		lastValue=Math.min(lastValue, max);
		lastValue=Math.max(lastValue, min);
		this.lastValue =lastValue;
	}
	
	public void setLastValueUI(double uiValue){
		double lv=min+(max-min)*uiValue;
		setLastValue(lv);
	}
}
