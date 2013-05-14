package at.mabs.model;

import java.util.*;

import at.mabs.segment.FixedBitSet;
import at.mabs.util.Util;

/**
 * Just a convenience object for passing around. Also perhaps a event factory for sampleAddEvents.
 * 
 * @author bob
 * 
 */
public class SampleConfiguration {
	private int maxSamples;
	private int demeCount;
	private int[] demeSamples;
	private int timePoints;
	private FixedBitSet[] masks;

	private int[][] timeSamples;
	private long[] times;

	public SampleConfiguration(int maxSamples) {
		this.maxSamples = maxSamples;
		times = new long[] { 0 };
		timeSamples = new int[][] { { maxSamples } };
		timePoints = 1;
		demeCount = 1;
		FixedBitSet mask = new FixedBitSet(maxSamples);
		mask.invert();
		masks = new FixedBitSet[] { mask };

	}

	/**
	 * single time point (t=0) with demes. A single entry is valid for one deme.
	 * 
	 * @param samples
	 */
	public void setDemeSamples(int[] samples) {
		assert Util.sum(samples) == maxSamples;
		// System.out.println("SetSamples:"+Arrays.toString(samples));
		demeCount = samples.length;
		timePoints = 1;
		FixedBitSet tmask = new FixedBitSet(maxSamples);
		tmask.invert();
		
		masks = new FixedBitSet[samples.length];
		int counter = 0;
		for (int i = 0; i < samples.length; i++) {
			masks[i] = new FixedBitSet(maxSamples);
			masks[i].set(counter, counter + samples[i] - 1);
			counter += samples[i];
		}
		timeSamples = new int[1][0];
		timeSamples[0] = samples.clone();
		times = new long[] { 0 };
		demeSamples=samples.clone();
		//System.out.println("Masks:"+Arrays.toString(masks));
		//System.out.println("DemeSizes:"+Arrays.toString(demeSamples));
	}

	public void setSamples(int timePoints, int demeCount, int[][] sampleList, long[] times) {

		timeSamples = sampleList;// FIXME shallow copy.
		this.times = times.clone();

		demeSamples=new int[demeCount];

		
		masks = new FixedBitSet[demeCount*timePoints];

		for (int d = 0; d < masks.length; d++) {
			masks[d] = new FixedBitSet(maxSamples);
		}
		int currentBit = 0;
		int sampleSum = 0;
		for (int t = 0; t < timePoints; t++) {
			
			for (int d = 0; d < demeCount; d++) {
				int n = sampleList[t][d];
				demeSamples[d]+=n;
				masks[d+t*demeCount].set(currentBit, currentBit + n - 1);
				currentBit += n;
				sampleSum += n;
			}
			
		}
		assert sampleSum == maxSamples;
		this.demeCount = demeCount;
		this.timePoints = timePoints;
		//System.out.println("Masks:"+Arrays.toString(masks));
		//System.out.println("DemeSizes:"+Arrays.toString(demeSamples));
	}

	public int getMaxSamples() {
		return maxSamples;
	}

	public FixedBitSet[] getMasks() {
		return masks;
	}

	

	public int getDemeCount() {
		return demeCount;
	}

	public int getTimePoints() {
		return timePoints;
	}

	public int getTotalSamplesInDeme(int deme) {
		return demeSamples[deme];
	}

	public Collection<NewSampleEvent> getAllNewSampleEvents() {
		List<NewSampleEvent> events = new ArrayList<NewSampleEvent>();
		for (int t = 0; t < timePoints; t++) {
			long time = times[t];
			int[] s = timeSamples[t];
			events.add(new NewSampleEvent(time, s));
		}
		// System.out.println("Events:"+events);
		return events;
	}
}
