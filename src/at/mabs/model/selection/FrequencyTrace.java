/*
This code is licensed under the LGPL v3 or greater with the classpath exception, 
with the following additions and exceptions.  

packages cern.* have retained the original cern copyright notices.

packages at.mabs.cmdline and at.mabs.util.* 
have the option to be licensed under a BSD(simplified) or Apache 2.0 or greater  license 
in addition to LGPL. 

Note that you have permission to replace this license text to any of the permitted licenses. 

Main text for LGPL can be found here:
http://www.opensource.org/licenses/lgpl-license.php

For BSD:
http://www.opensource.org/licenses/bsd-license.php

for Apache:
http://www.opensource.org/licenses/apache2.0.php

classpath exception:
http://www.gnu.org/software/classpath/license.html
*/
package at.mabs.model.selection;



/**
 * abstraction of a frequency trace. 
 * 
 * We support shift and resize operations. Note time is still going backwards. 
 * 
 * ie next finds the next pastward entry while previous finds the next entry closer to present.
 * 
 * Resizing only happens with previous --
 * @author bob
 *
 */
public final class FrequencyTrace {
	private double[][][] freqs;//generations, demes, allels. 
	private int startIndex;//inclusive for the first (pastward value)
	private int initalStartTime;//for restarts. Final start time is not always "thee" start time.
	private int endIndex;//end freqs.length-1. 
	
	private int startTime; //Time @ start--ie index=start when t=startTime
	//this data removes any dependancy on other objects 
	private int demeCount;
	private int alleleCount;
	
	private int iteratorPosition=startIndex;//we can use next hasNext etc.
	
	private boolean used;
	/**
	 * fixed sized cases. Still can be "shifted" within bounds. 
	 * @param startTime
	 * @param endTime
	 * @param demes
	 * @param alleles
	 */
	public FrequencyTrace(int startTime,int endTime,int demes,int alleles) {
		assert endTime>startTime:endTime+"\t"+startTime;
		int delta=endTime-startTime;
		freqs=new double[delta][demes][alleles];
		this.startTime=startTime;
		startIndex=0;
		initalStartTime=startTime;
		endIndex=delta-1;
		
		demeCount=demes;
		alleleCount=alleles;
		
		System.out.println("CreateTrace:"+startTime+"\t"+endTime+"\t"+this.hashCode());
	}
	
	/**
	 * resizeable option.  growing the data in the directon of more present, will result in resizing its backing storage.
	 * @param demes
	 * @param alleles
	 */
	public FrequencyTrace(int demes,int alleles) {
		freqs=new double[5000][demes][alleles];
		startIndex=0;
		initalStartTime=0;
		endIndex=4999;
		demeCount=demes;
		alleleCount=alleles;
		System.out.println("CreateTraceGrowing:"+this.hashCode());
		//throw new RuntimeException();
		//System.out.println("DemesForTrace:"+demes);
		//StackTraceElement[] trace=Thread.currentThread().getStackTrace();
		//for(StackTraceElement e:trace){
		//	System.out.println("\t"+e);
		//}
	}
	
	/**
	 * clears all data as a side effect.
	 */
	void addDeme(){
		demeCount++;
		freqs=new double[freqs.length][demeCount][alleleCount];
		//System.out.println("AddDemeForTrace:"+demeCount);
	}
	
	/**
	 * reset the current state. ie deleted it. However this does not infact delete anything.  
	 */
	public void reset(){
		startIndex=0;//initalStartIndex;
		startTime=initalStartTime;
		
	}
	/**
	 * returns a reference to the next array position. Change the returned array, changes this object
	 * @return
	 */
	public double[][] next(){
		assert iteratorPosition<endIndex;
		iteratorPosition++;
		return freqs[iteratorPosition];
	}
	
	/**
	 * returns a reference to the next array position. Change the returned array, changes this object
	 * 
	 * Previous in the pastward sense. That is the returned entry is "older" in time or less pastward.
	 * @return
	 */
	public double[][] previous(boolean resize){
		if(iteratorPosition==0 && resize){
			resize();
		}
		assert iteratorPosition>startIndex;
		iteratorPosition--;
		return freqs[iteratorPosition];
	}

	/**
	 * 
	 * @return reference to the current iterator position entry.
	 */
	public double[][] current(){
		return freqs[iteratorPosition];
	}
	
	public boolean hasNext(){
		return iteratorPosition<endIndex;
	}
	
	public boolean hasPrevious(){
		
		return iteratorPosition>startIndex;
	}

	
	/**
	 * set position to the oldest sample. note that if this is a resizing trace then the last 
	 * sample can get "pushed" back further in time.
	 */
	public void setIteratorToEnd(){
		iteratorPosition=endIndex;
	}
	
	/**
	 * the most recent sample
	 * @return
	 */
	double[][] getStart(){
		if(startIndex<0)
			return freqs[0];
		return freqs[startIndex];
	}
	
	/**
	 * the most pastward sample.
	 * @return
	 */
	double[][] getEnd(){
		return freqs[endIndex];
	}
	
	/**
	 * note that if this is for a shifting time thing then this may not give good results. 
	 * @return
	 */
	public int getTime(){
		return startTime+(iteratorPosition-startIndex);
	}
	
	/**
	 * start is the closest to present sampling. ie start in a pastward direction.
	 */
	public void setIteratorToStart(){
		iteratorPosition=startIndex;
	}
	
	
	/**
	 * Sets the time such that the current position will be the current time. while the first entry will
	 * be at start time. Needs to be something better, at least name wise. 
	 * @param currentTime
	 * @param startTime
	 */
	public void shiftAndSet(int currentTime,int startTime){
		assert currentTime>=startTime:currentTime+"\t"+startTime;
		int delta=currentTime-startTime;
		startIndex=iteratorPosition-delta;
		while(startIndex<0){
			resize();//note that a side effect is to change iteratorPosition
			startIndex=iteratorPosition-delta;
		}
		//System.out.println("SHIFT:"+hasPrevious()+"\t"+currentTime+"\t"+startTime);
		this.startTime=startTime;
	}
	
//	void setStart(){
//		start=iteratorPostion;//assume that selection does apply outside this range. assertions will be throw
//		//if violated
//	}
	
	public int getEndTime(){
		
		return endIndex-startIndex+startTime;
	}
	
	public int getStartTime(){
		return startTime;//+(iteratorPosition-startIndex);
	}
	
	/**
	 * Relative to start index. NOT THE TIME!
	 * @param i 
	 * @return a referance to the frequency data, changing it changes this. 
	 */
	double[][] getAtRelativeIndex(int i){
		assert i+startIndex>=0 && i<=endIndex:"i "+i+"\t"+hashCode()+"\tstartIndex:"+startIndex+"\tendIndex:"+endIndex+"\tsize:"+freqs.length;
		assert i+startIndex<freqs.length:i+startIndex+"<"+freqs.length;
		return freqs[i+startIndex];
	}
	
	public double[][] getAtTime(int t){
		int i=t-startTime;
		return getAtRelativeIndex(i);
	}
	
	private void resize(){
		//System.out.println("RESIZE! "+freqs.length);
		if(freqs.length>500000000)
			throw new RuntimeException("Foward simulation not ending (larger than 500M gens). Can boundry conditions be meet?");
		double[][][] newData=new double[freqs.length*2][0][0];
		int delta=freqs.length;
		//move the top half
		
		for(int i=0;i<freqs.length;i++){
			newData[i]=new double[demeCount][alleleCount];
		}
		for(int i=0;i<freqs.length;i++){
			newData[i+delta]=freqs[i];//not a copy!
		}
		freqs=newData;
		//start+=delta;
		endIndex+=delta;
		iteratorPosition+=delta;
		//System.out.println("Done! "+freqs.length);
	}
	
	public int getDemeCount() {
		return demeCount;
	}
	
	public int getAlleleCount() {
		return alleleCount;
	}
	
	public boolean isUsed() {
		return used;
	}
	
	public void setUsed(boolean used) {
		this.used =used;
	}
	
	@Override
	public String toString() {

		return "FreqData:"+startTime+" Sindex:"+startIndex+" endIndex:"+endIndex;
	}
}
