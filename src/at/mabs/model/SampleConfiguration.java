package at.mabs.model;

import java.util.*;

import at.mabs.segment.FixedBitSet;
import at.mabs.util.Util;
/**
 * Just a convenience object for passing around. Also perhaps a event factory for sampleAddEvents.  
 * @author bob
 *
 */
public class SampleConfiguration {
	private int maxSamples;
	private int demeCount;
	private int timePoints;
	private FixedBitSet[] timeMasks;
	private FixedBitSet[] demeMasks;
	private int[][] timeSamples;
	private long[] times;
	
	public SampleConfiguration(int maxSamples) {
		this.maxSamples=maxSamples;
		times=new long[] {0};
		timeSamples=new int[][] {{maxSamples}};
		timePoints=1;
		demeCount=1;
		FixedBitSet mask=new FixedBitSet(maxSamples);
		mask.invert();
		timeMasks=new FixedBitSet[] {mask};
		demeMasks=new FixedBitSet[] {mask};
	}
	
	/**
	 * single time point (t=0) with demes. A single entry is valid for one deme. 
	 * @param samples
	 */
	public void setDemeSamples(int[] samples){
		assert Util.sum(samples)==maxSamples;
		//System.out.println("SetSamples:"+Arrays.toString(samples));
		demeCount=samples.length;
		timePoints=1;
		FixedBitSet tmask=new FixedBitSet(maxSamples);
		tmask.invert();
		timeMasks=new FixedBitSet[] {tmask};
		demeMasks=new FixedBitSet[samples.length];
		int counter=0;
		for(int i=0;i<samples.length;i++){
			demeMasks[i]=new FixedBitSet(maxSamples);
			demeMasks[i].set(counter,counter+ samples[i]-1);
			counter+=samples[i];
		}
		timeSamples=new int[1][0];
		timeSamples[0]=samples.clone();
		times=new long[] {0};
	}
	
	public void setSamples(int timePoints,int demeCount,int[][] sampleList,long[] times){
		
		
		timeSamples=sampleList;//FIXME shallow copy.
		this.times=times.clone();
		
		//int[] sumDemes=new int[demeCount];
		
		timeMasks=new FixedBitSet[timePoints];
		demeMasks=new FixedBitSet[demeCount];
		
		for(int d=0;d<demeCount;d++){
			demeMasks[d]=new FixedBitSet(maxSamples);
		}
		int currentBit=0;
		int sampleSum=0;
		for(int t=0;t<timePoints;t++){
			FixedBitSet tmask=new FixedBitSet(this.maxSamples);
			for(int d=0;d<demeCount;d++){
				int n=sampleList[t][d];
				tmask.set(currentBit, currentBit+n-1);
				demeMasks[d].set(currentBit, currentBit+n-1);
				currentBit+=n;
				sampleSum+=n;
			}
			timeMasks[t]=tmask;
		}
		assert sampleSum==maxSamples;
		this.demeCount=demeCount;
		this.timePoints=timePoints;
		//System.out.println("DemeMasks:"+Arrays.toString(demeMasks));
		//System.out.println("TimeMasks:"+Arrays.toString(timeMasks));
	}
	
	public int getMaxSamples() {
		return maxSamples;
	}
	
	public FixedBitSet[] getDemeMasks() {
		return demeMasks;
	}
	
	public FixedBitSet[] getTimeMasks() {
		return timeMasks;
	}
	
	public int getDemeCount() {
		return demeCount;
	}
	
	public int getTimePoints() {
		return timePoints;
	}
	
	public int getTotalSamplesInDeme(int deme){
		assert deme<demeMasks.length;
		return demeMasks[deme].countSetBits();
	}
	
	public Collection<NewSampleEvent> getAllNewSampleEvents(){
		List<NewSampleEvent> events=new ArrayList<NewSampleEvent>();
		for(int t=0;t<timePoints;t++){
			long time=times[t];
			int[] s=timeSamples[t];
			events.add(new NewSampleEvent(time, s));
		}
		//System.out.println("Events:"+events);
		return events;
	}
}
