package at.mabs.model.selection;

import java.util.Arrays;
import java.util.HashSet;

/**
 * sick of the buggy frequency trace. We are going to replace it with this.
 * 
 * The idea is much greater simplicity.
 * 
 * We set this to the size of max tracked demes. ie every -es option adds a deme
 * compared to sampling time.
 * 
 * Finally to solve some of the memory issues i have seen with the old version
 * we use a single flat array and assume no one wants to use #allele * #demes *
 * generations >2^31.
 * 
 * The interface is going to change a bit too. We keep it simpler than last
 * time. You don't have nexts and prev. You have time t. Thus simulators will
 * need to change. But the "copy state" should not (perhaps there is a n-1 error
 * in there.)
 * 
 * note we use n-1 alleles.
 * 
 * @author bob
 * 
 */
public class SuperFrequencyTrace {
	
	private double[] freq; // index=gen*(#alleles*#deme)+deme*(#alleles)+alleles;
	private final int alleles = 1;// count of *non* wild type alleles
	private final int demes;
	private final int startIndexTime;// time at index=0;
	private int offsetTime;// when we "shift the start" a bit.

	private final int genDelta;
	private int index = 0;
	private long endTime;

	public SuperFrequencyTrace(int maxDemes, long startPastWard) {
		assert startPastWard < Integer.MAX_VALUE;
		startIndexTime = (int) startPastWard;

		demes = maxDemes;
		freq = new double[demes * alleles * 5000];
		genDelta = demes * alleles;
	}

	/**
	 * 
	 * @param maxDemes
	 * @param startPastWard
	 * @param generations
	 */
	public SuperFrequencyTrace(int maxDemes, long startPastWard, int generations) {
		assert startPastWard < Integer.MAX_VALUE;
		//System.err.println("CreateSuperFreq:"+generations+"\tstartPastward:"+startPastWard+"\t"+this.hashCode());
		startIndexTime = (int) startPastWard;
		demes = maxDemes;
		freq = new double[demes * alleles * generations];
		genDelta = demes * alleles;
	}

	public void reset() {
		offsetTime = 0;
		index = 0;
		endTime = -1;
	}

	public void setIndexTime(long time) {

		index = ((int) time - startIndexTime + offsetTime) * genDelta;
			
		//System.err.println("SetIndexTo:"+index+"\t"+freq.length+"\tfromtime:"+time+"\tOF:"+offsetTime+"\tSI:"+startIndexTime+"\tHC:"+this.hashCode());
		if(index>=freq.length)
			throw new ArrayIndexOutOfBoundsException(index+"\t"+freq.length);
		
	}

	public void setEndTime() {
		endTime = getIndexTime();
	}

	public long getEndTime() {
		return endTime;
	}

	public long getIndexTime() {
		return ((index/genDelta - offsetTime) ) + startIndexTime;
	}

	public long getTimeMostPastward() {
		int index = freq.length - genDelta;// shadow!
		return ((index/genDelta - offsetTime)) + startIndexTime;
	}

	public void setIndexMostPastward() {
		index = freq.length - genDelta;
	}

	/**
	 * move the the oldest generation counted from present. This may or may not
	 * be resizable.
	 */
	public void movePastward() {
		index += genDelta;
		assert index < freq.length;
	}

	/**
	 * move forward in time. ie one generation closer to present. The oposite of
	 * pastward. Will resize if this results in out of bounds. Perhaps it
	 * shouldn't!
	 */
	public void moveForward() {
		index -= genDelta;
		if (index < 0) {
			resize();
		}
	}

	/**
	 * does not consider resizing.
	 * 
	 * @return
	 */
	public boolean hasMoreForward() {
		if (endTime >= 0)
			return getIndexTime() > endTime;//
		return index > offsetTime * genDelta;
	}

	public boolean hasMorePastward() {
		return index < freq.length - genDelta;
	}

	/**
	 * simple list of frequencys as follows,
	 * 
	 * @param data
	 * @return copy of the data. new instance if data==null
	 */
	public double[] getFrequencys(double[] data) {
		if (data == null || data.length!=genDelta)
			data = new double[genDelta];
		//System.err.println("getFreq: "+index+"\t"+freq.length);
		System.arraycopy(freq, index, data, 0, data.length);
		return data;
	}

	public void setFrequencys(double[] data) {
		System.arraycopy(data, 0, freq, index, genDelta);
	}

	public void setCurrentIndexAsStart() {
		//System.err.println("SETTING OFFSETTIME:"+index);
		offsetTime = (index / genDelta);
	}

	public void setIndexToStart() {
		index = offsetTime * genDelta;
	}

	/**
	 * should only really be needed when we have time homogeneous models --ie
	 * condition on fixation.
	 */
	private void resize() {
		//System.out.println("ReSize ");
		double[] newData = new double[freq.length * 2];
		//System.arraycopy(freq, 0, newData, freq.length, freq.length);
		index += freq.length;
		freq = newData;
	}

	@Override
	public String toString() {
//		StringBuilder bs = new StringBuilder();
//		bs.append("StartTraceDumpRaw:");
//		for (double d : freq) {
//			bs.append(d + "\n");
//		}
//		bs.append("END");
//		return bs.toString();
		
		return "SuperFreq:["+this.demes+","+this.alleles+","+this.genDelta+","+freq.length+","+this.hashCode()+"]";
	}

	public int getIndex() {
		// System.err.println("SuperTData:"+this.startOffset+"\t"+this.index+"\t"+this.freq.length);
		return index;
	}

	public void clear() {
		Arrays.fill(this.freq, 0);// clear us

	}
}
